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
package com.impetus.client.gis;

import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.impetus.kundera.gis.geometry.Point;

/**
 * Test case for GIS
 * 
 * @author amresh.singh
 */
public class MongoGISTest
{

    String persistenceUnit = "mongoTest";

    PersonGISDao dao;

    @Before
    public void setUp() throws Exception
    {
        dao = new PersonGISDao(persistenceUnit);
    }

    @Test
    public void executeTests() throws Exception
    {
        addPersons();
        findPerson();
        
        findWithinCircle();
        
        updatePerson();
        removePerson();
    }

    @After
    public void tearDown() throws Exception
    {
        dao.close();
    }

    private void addPersons()
    {
        dao.createEntityManager();
        for (int i = 0; i < 100; i++)
        {
            double x = i % 10;
            double y = Math.floor(i / 10);      
            
            
            Person person = new Person();
            person.setPersonId(i + 1);
            person.setName("Amresh_" + (i + 1));
            person.setCurrentLocation(new Point(x, y));

            Vehicle vehicle = new Vehicle();
            vehicle.setCurrentLocation(new Point(x + 1.0, y + 1.0));
            vehicle.setPreviousLocation(new Point(x + 2.0, y + 2.0));

            person.setVehicle(vehicle);
            
            
            dao.addPerson(person);
        }
        dao.closeEntityManager();
    }
    
    private void findPerson()
    {
        dao.createEntityManager();
        Person person = dao.findPerson(4);

        Assert.assertNotNull(person);
        Assert.assertEquals(4, person.getPersonId());
        Assert.assertEquals("Amresh_4", person.getName());

        Point currentLocation = person.getCurrentLocation();
        Assert.assertNotNull(currentLocation);
        Assert.assertEquals(3.0, currentLocation.getX());
        Assert.assertEquals(0.0, currentLocation.getY());

        Vehicle vehicleLocation = person.getVehicle();
        Assert.assertNotNull(vehicleLocation);
        Assert.assertNotNull(vehicleLocation.getCurrentLocation());
        Assert.assertEquals(4.0, vehicleLocation.getCurrentLocation().getX());
        Assert.assertEquals(1.0, vehicleLocation.getCurrentLocation().getY());
        Assert.assertNotNull(vehicleLocation.getPreviousLocation());
        Assert.assertEquals(5.0, vehicleLocation.getPreviousLocation().getX());
        Assert.assertEquals(2.0, vehicleLocation.getPreviousLocation().getY());

        dao.closeEntityManager();
    }
    
    private void findWithinCircle()
    {
        dao.createEntityManager();
        
        List<Person> persons = dao.findWithinCircle(5.0, 5.0, 2.0);
        Assert.assertNotNull(persons);
        Assert.assertFalse(persons.isEmpty());
        Assert.assertTrue(persons.size() == 13);
        dao.closeEntityManager();
    }

    /**
     * 
     */
    private void updatePerson()
    {
        dao.createEntityManager();
        Person person = dao.findPerson(4);

        Assert.assertNotNull(person);
        Assert.assertEquals(4, person.getPersonId());
        Assert.assertEquals("Amresh_4", person.getName());

        person.setCurrentLocation(new Point(9.3, 5.8));

        Vehicle vehicle = person.getVehicle();
        vehicle.setCurrentLocation(new Point(5.67, 11.59));
        vehicle.setPreviousLocation(new Point(15.67, 21.59));

        person.setVehicle(vehicle);

        dao.mergePerson(person);
        dao.closeEntityManager();
        dao.createEntityManager();

        person = dao.findPerson(4);

        Assert.assertNotNull(person);
        Assert.assertEquals(4, person.getPersonId());
        Assert.assertEquals("Amresh_4", person.getName());
        Point currentLocation = person.getCurrentLocation();
        Assert.assertNotNull(currentLocation);
        Assert.assertEquals(9.3, currentLocation.getX());
        Assert.assertEquals(5.8, currentLocation.getY());

        vehicle = person.getVehicle();
        Assert.assertNotNull(vehicle);
        Assert.assertNotNull(vehicle.getCurrentLocation());
        Assert.assertEquals(5.67, vehicle.getCurrentLocation().getX());
        Assert.assertEquals(11.59, vehicle.getCurrentLocation().getY());
        Assert.assertNotNull(vehicle.getPreviousLocation());
        Assert.assertEquals(15.67, vehicle.getPreviousLocation().getX());
        Assert.assertEquals(21.59, vehicle.getPreviousLocation().getY());

        dao.closeEntityManager();

    }

    /**
     * 
     */
    private void removePerson()
    {
        dao.createEntityManager();
        Person person = dao.findPerson(1);

        Assert.assertNotNull(person);

        dao.removePerson(person);

        dao.closeEntityManager();
        dao.createEntityManager();

        person = dao.findPerson(1);

        Assert.assertNull(person);
        dao.closeEntityManager();
    }
}