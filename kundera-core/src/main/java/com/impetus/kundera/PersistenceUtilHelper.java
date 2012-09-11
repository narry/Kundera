/**
 * Copyright 2012 Impetus Infotech.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.impetus.kundera;

import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUtil;
import javax.persistence.spi.LoadState;

import org.hibernate.collection.PersistentCollection;

import com.impetus.kundera.intercept.FieldInterceptionHelper;
import com.impetus.kundera.intercept.FieldInterceptor;
import com.impetus.kundera.proxy.KunderaProxy;
import com.impetus.kundera.proxy.LazyInitializer;

/**
 * Helper for {@link PersistenceUtil}
 */
public class PersistenceUtilHelper
{
    public static LoadState isLoadedWithoutReference(Object proxy, String property, MetadataCache cache)
    {
        Object entity;
        if (proxy instanceof KunderaProxy)
        {
            LazyInitializer li = ((KunderaProxy) proxy).getKunderaLazyInitializer();
            if (li.isUninitialized())
            {
                return LoadState.NOT_LOADED;
            }
            else
            {
                entity = li.getImplementation();
            }
        }
        else
        {
            entity = proxy;
        }

        FieldInterceptor interceptor = FieldInterceptionHelper.extractFieldInterceptor(entity);
        final boolean isInitialized = interceptor == null || interceptor.isInitialized(property);
        LoadState state;
        if (isInitialized && interceptor != null)
        {
            // property is loaded according to bytecode enhancement, but is
            // it loaded as far as association?
            // it's ours, we can read
            state = isLoaded(get(entity, property, cache));
            // it's ours so we know it's loaded
            if (state == LoadState.UNKNOWN)
                state = LoadState.LOADED;
        }
        else if (interceptor != null && (!isInitialized))
        {
            state = LoadState.NOT_LOADED;
        }
        else
        { // interceptor == null
          // property is loaded according to bytecode enhancement, but is
          // it loaded as far as association?
          // it's ours, we can read
            state = isLoaded(get(entity, property, cache));
            // it's ours so we know it's loaded
            if (state == LoadState.UNKNOWN)
                state = LoadState.LOADED;
        }

        return state;

    }

    public static LoadState isLoadedWithReference(Object proxy, String property, MetadataCache cache)
    {
        // for sure we don't instrument and for sure it's not a lazy proxy
        Object object = get(proxy, property, cache);
        return isLoaded(object);
    }

    private static Object get(Object proxy, String property, MetadataCache cache)
    {
        final Class<?> clazz = proxy.getClass();

        try
        {
            Member member = cache.getMember(clazz, property);
            if (member instanceof Field)
            {
                return ((Field) member).get(proxy);
            }
            else if (member instanceof Method)
            {
                return ((Method) member).invoke(proxy);
            }
            else
            {
                throw new PersistenceException("Member object neither Field nor Method: " + member);
            }
        }
        catch (IllegalAccessException e)
        {
            throw new PersistenceException("Unable to access field or method: " + clazz + "#" + property, e);
        }
        catch (InvocationTargetException e)
        {
            throw new PersistenceException("Unable to access field or method: " + clazz + "#" + property, e);
        }  
                
    }

    private static void setAccessibility(Member member)
    {
        ((AccessibleObject) member).setAccessible(true);
    }

    public static LoadState isLoaded(Object o)
    {
        if (o instanceof KunderaProxy)
        {
            final boolean isInitialized = !((KunderaProxy) o).getKunderaLazyInitializer().isUninitialized();
            return isInitialized ? LoadState.LOADED : LoadState.NOT_LOADED;
        }
        else if (o instanceof PersistentCollection)
        {
            final boolean isInitialized = ((PersistentCollection) o).wasInitialized();
            return isInitialized ? LoadState.LOADED : LoadState.NOT_LOADED;
        }
        else
        {
            return LoadState.UNKNOWN;
        }
    }

    /**
     * Returns the method with the specified name or <code>null</code> if it
     * does not exist.
     * 
     * @param clazz
     *            The class to check.
     * @param methodName
     *            The method name.
     * 
     * @return Returns the method with the specified name or <code>null</code>
     *         if it does not exist.
     */
    private static Method getMethod(Class<?> clazz, String methodName)
    {
        try
        {
            char string[] = methodName.toCharArray();
            string[0] = Character.toUpperCase(string[0]);
            methodName = new String(string);
            try
            {
                return clazz.getDeclaredMethod("get" + methodName);
            }
            catch (NoSuchMethodException e)
            {
                return clazz.getDeclaredMethod("is" + methodName);
            }
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
    }

    /**
     * Cache hierarchy and member resolution in a weak hash map
     */
    public static class MetadataCache implements Serializable
    {
        private transient Map<Class<?>, ClassCache> classCache = new WeakHashMap<Class<?>, ClassCache>();

        private void readObject(java.io.ObjectInputStream stream)
        {
            classCache = new WeakHashMap<Class<?>, ClassCache>();
        }

        Member getMember(Class<?> clazz, String property)
        {
            ClassCache cache = classCache.get(clazz);
            if (cache == null)
            {
                cache = new ClassCache(clazz);
                classCache.put(clazz, cache);
            }
            Member member = cache.members.get(property);
            if (member == null)
            {
                member = findMember(clazz, property);
                cache.members.put(property, member);
            }
            return member;
        }

        private Member findMember(Class<?> clazz, String property)
        {
            final List<Class<?>> classes = getClassHierarchy(clazz);

            for (Class current : classes)
            {
                final Field field;
                try
                {
                    field = current.getDeclaredField(property);
                    setAccessibility(field);
                    return field;
                }
                catch (NoSuchFieldException e)
                {
                    final Method method = getMethod(current, property);
                    if (method != null)
                    {
                        setAccessibility(method);
                        return method;
                    }
                }
            }
            // we could not find any match
            throw new PersistenceException("Unable to find field or method: " + clazz + "#" + property);
        }

        private List<Class<?>> getClassHierarchy(Class<?> clazz)
        {
            ClassCache cache = classCache.get(clazz);
            if (cache == null)
            {
                cache = new ClassCache(clazz);
                classCache.put(clazz, cache);
            }
            return cache.classHierarchy;
        }

        private static List<Class<?>> findClassHierarchy(Class<?> clazz)
        {
            List<Class<?>> classes = new ArrayList<Class<?>>();
            Class<?> current = clazz;
            do
            {
                classes.add(current);
                current = current.getSuperclass();
            }
            while (current != null);
            return classes;
        }

        private static class ClassCache
        {
            List<Class<?>> classHierarchy;

            Map<String, Member> members = new HashMap<String, Member>();

            public ClassCache(Class<?> clazz)
            {
                classHierarchy = findClassHierarchy(clazz);
            }
        }
    }

}