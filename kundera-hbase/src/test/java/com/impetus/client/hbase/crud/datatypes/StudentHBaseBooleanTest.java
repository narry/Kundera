package com.impetus.client.hbase.crud.datatypes;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.impetus.client.hbase.crud.datatypes.entities.StudentHBaseBoolean;
import com.impetus.kundera.query.QueryHandlerException;

public class StudentHBaseBooleanTest extends Base
{

    private static final String keyspace = "KunderaHbaseDataType";

    private EntityManagerFactory emf;

    @Before
    public void setUp() throws Exception
    {
        if (RUN_IN_EMBEDDED_MODE)
        {
            startCluster();
        }
        if (AUTO_MANAGE_SCHEMA)
        {
            createSchema();
        }
        emf = Persistence.createEntityManagerFactory("HbaseDataTypeTest");
    }

    @After
    public void tearDown() throws Exception
    {
        emf.close();
        if (AUTO_MANAGE_SCHEMA)
        {
            dropSchema();
        }
        if (RUN_IN_EMBEDDED_MODE)
        {
            stopCluster();
        }
    }

    @Test
    public void testExecuteUseSameEm()
    {
        testPersist(true);
        testFindById(true);
        testMerge(true);
        testFindByQuery(true);
        testNamedQueryUseSameEm(true);
        testDelete(true);
    }

    @Test
    public void testExecute()
    {
        testPersist(false);
        testFindById(false);
        testMerge(false);
        testFindByQuery(false);
        testNamedQuery(false);
        testDelete(false);
    }

    public void testPersist(Boolean useSameEm)
    {
        EntityManager em = emf.createEntityManager();

        // Insert max value of Boolean
        StudentHBaseBoolean studentMax = new StudentHBaseBoolean();
        studentMax.setAge((Short) getMaxValue(short.class));
        studentMax.setId((Boolean) getMaxValue(Boolean.class));
        studentMax.setName((String) getMaxValue(String.class));
        em.persist(studentMax);

        // Insert min value of Boolean
        StudentHBaseBoolean studentMin = new StudentHBaseBoolean();
        studentMin.setAge((Short) getPartialValue(short.class));
        studentMin.setId((Boolean) getMinValue(Boolean.class));
        studentMin.setName((String) getMinValue(String.class));
        em.persist(studentMin);

        em.close();
    }

    public void testFindById(Boolean useSameEm)
    {
        EntityManager em = emf.createEntityManager();

        StudentHBaseBoolean studentMax = em.find(StudentHBaseBoolean.class, getMaxValue(Boolean.class));
        Assert.assertNotNull(studentMax);
        Assert.assertEquals(getMaxValue(short.class), studentMax.getAge());
        Assert.assertEquals(getMaxValue(String.class), studentMax.getName());

        if (!useSameEm)
        {
            em.close();
            em = emf.createEntityManager();
        }
        StudentHBaseBoolean studentMin = em.find(StudentHBaseBoolean.class, getMinValue(Boolean.class));
        Assert.assertNotNull(studentMin);
        Assert.assertEquals(getPartialValue(short.class), studentMin.getAge());
        Assert.assertEquals(getMinValue(String.class), studentMin.getName());

        em.close();
    }

    public void testMerge(Boolean useSameEm)
    {
        EntityManager em = emf.createEntityManager();
        StudentHBaseBoolean student = em.find(StudentHBaseBoolean.class, getMaxValue(Boolean.class));
        Assert.assertNotNull(student);
        Assert.assertEquals(getMaxValue(short.class), student.getAge());
        Assert.assertEquals(getMaxValue(String.class), student.getName());

        student.setName("Kuldeep");
        em.merge(student);
        if (!useSameEm)
        {
            em.close();
            em = emf.createEntityManager();
        }
        StudentHBaseBoolean newStudent = em.find(StudentHBaseBoolean.class, getMaxValue(Boolean.class));
        Assert.assertNotNull(newStudent);
        Assert.assertEquals(getMaxValue(short.class), newStudent.getAge());
        Assert.assertEquals("Kuldeep", newStudent.getName());
    }

    public void testFindByQuery(Boolean useSameEm)
    {
        findAllQuery();
        findByName();
        findByAge();
        findByNameAndAgeGTAndLT();
        findByNameAndAgeGTEQAndLTEQ();
        findByNameAndAgeGTAndLTEQ();
        findByNameAndAgeWithOrClause();
        findByAgeAndNameGTAndLT();
        findByNameAndAGEBetween();
        findByRange();
    }

    private void findByAgeAndNameGTAndLT()
    {
        EntityManager em;
        String query;
        Query q;
        List<StudentHBaseBoolean> students;
        int count;
        em = emf.createEntityManager();
        query = "Select s From StudentHBaseBoolean s where s.age = " + getPartialValue(short.class)
                + " and s.name > Amresh and s.name <= " + getMaxValue(String.class);
        q = em.createQuery(query);
        students = q.getResultList();
        Assert.assertNotNull(students);
        Assert.assertEquals(1, students.size());
        count = 0;
        for (StudentHBaseBoolean student : students)
        {
            Assert.assertEquals(getMinValue(Boolean.class), student.getId());
            Assert.assertEquals(getPartialValue(short.class), student.getAge());
            Assert.assertEquals(getMinValue(String.class), student.getName());
            count++;

        }
        Assert.assertEquals(1, count);
        em.close();

    }

    private void findByRange()
    {
        EntityManager em;
        String query;
        Query q;
        List<StudentHBaseBoolean> students;
        em = emf.createEntityManager();
        query = "Select s From StudentHBaseBoolean s where s.id between ?1 and ?2";
        q = em.createQuery(query);
        q.setParameter(1, getMinValue(Boolean.class));
        q.setParameter(2, getMaxValue(Boolean.class));
        students = q.getResultList();
        Assert.assertNotNull(students);
        Assert.assertEquals(1, students.size());
        int count = 0;
        for (StudentHBaseBoolean student : students)
        {/*
            if (student.getId().equals(getMaxValue(Boolean.class)))
            {
                Assert.assertEquals(getMaxValue(short.class), student.getAge());
                Assert.assertEquals("Kuldeep", student.getName());
                count++;
            }
            else */if (student.getId().equals(getMinValue(Boolean.class)))
            {
                Assert.assertEquals(getPartialValue(short.class), student.getAge());
                Assert.assertEquals(getMinValue(String.class), student.getName());
                count++;
            }
        }
        Assert.assertEquals(1, count);
        em.close();
    }

    private void findByNameAndAgeWithOrClause()
    {
        EntityManager em;
        String query;
        Query q;
        List<StudentHBaseBoolean> students;
        int count;
        em = emf.createEntityManager();
        query = "Select s From StudentHBaseBoolean s where s.name = Kuldeep or s.age > " + getPartialValue(short.class);
        try
        {
            q = em.createQuery(query);
            students = q.getResultList();
            Assert.assertNotNull(students);
            Assert.assertEquals(1, students.size());
            count = 0;
            for (StudentHBaseBoolean student : students)
            {
                Assert.assertEquals(getMaxValue(Boolean.class), student.getId());
                Assert.assertEquals(getMaxValue(short.class), student.getAge());
                Assert.assertEquals("Kuldeep", student.getName());
                count++;
            }
            Assert.assertEquals(1, count);
            em.close();
        }
        catch (QueryHandlerException qhe)
        {
            Assert.assertEquals("unsupported clause OR for Hbase", qhe.getMessage());
        }
    }

    private void findByNameAndAgeGTAndLTEQ()
    {

        EntityManager em;
        String query;
        Query q;
        List<StudentHBaseBoolean> students;
        int count;
        em = emf.createEntityManager();
        query = "Select s From StudentHBaseBoolean s where s.name = Kuldeep and s.age > "
                + getPartialValue(short.class) + " and s.age <= " + getMaxValue(short.class);
        q = em.createQuery(query);
        students = q.getResultList();
        Assert.assertNotNull(students);
        Assert.assertEquals(1, students.size());
        count = 0;
        for (StudentHBaseBoolean student : students)
        {
            Assert.assertEquals(getMaxValue(Boolean.class), student.getId());
            Assert.assertEquals(getMaxValue(short.class), student.getAge());
            Assert.assertEquals("Kuldeep", student.getName());
            count++;
        }
        Assert.assertEquals(1, count);
        em.close();
    }

    public void testNamedQueryUseSameEm(Boolean useSameEm)
    {
        updateNamed(true);
        deleteNamed(true);
    }

    public void testNamedQuery(Boolean useSameEm)
    {
        updateNamed(false);
        deleteNamed(false);
    }

    public void testDelete(Boolean useSameEm)
    {
        EntityManager em = emf.createEntityManager();

        StudentHBaseBoolean studentMax = em.find(StudentHBaseBoolean.class, getMinValue(Boolean.class));
        Assert.assertNotNull(studentMax);
        Assert.assertEquals(getPartialValue(short.class), studentMax.getAge());
        Assert.assertEquals("Kuldeep", studentMax.getName());
        em.remove(studentMax);
        if (!useSameEm)
        {
            em.close();
            em = emf.createEntityManager();
        }
        studentMax = em.find(StudentHBaseBoolean.class, getMinValue(Boolean.class));
        Assert.assertNull(studentMax);
        em.close();
    }

    /**
     * 
     */
    private void deleteNamed(Boolean useSameEm)
    {

        String deleteQuery = "Delete From StudentHBaseBoolean s where s.id=true";
        EntityManager em = emf.createEntityManager();
        Query q = em.createQuery(deleteQuery);
        q.executeUpdate();
        if (!useSameEm)
        {
            em.close();
            em = emf.createEntityManager();
        }
        StudentHBaseBoolean newStudent = em.find(StudentHBaseBoolean.class, getMaxValue(Boolean.class));
        Assert.assertNull(newStudent);
        em.close();
    }

    /**
     * @return
     */
    private void updateNamed(Boolean useSameEm)
    {
        EntityManager em = emf.createEntityManager();
        String updateQuery = "Update StudentHBaseBoolean s SET s.name=Vivek where s.id=true";
        Query q = em.createQuery(updateQuery);
        q.executeUpdate();
        if (!useSameEm)
        {
            em.close();
            em = emf.createEntityManager();
        }
        StudentHBaseBoolean newStudent = em.find(StudentHBaseBoolean.class, getMaxValue(Boolean.class));
        Assert.assertNotNull(newStudent);
        Assert.assertEquals(getMaxValue(short.class), newStudent.getAge());
        Assert.assertEquals("Vivek", newStudent.getName());
        em.close();
    }

    private void findByNameAndAGEBetween()
    {
        EntityManager em;
        String query;
        Query q;
        List<StudentHBaseBoolean> students;
        em = emf.createEntityManager();
        query = "Select s From StudentHBaseBoolean s where s.name = Kuldeep and s.age between "
                + getPartialValue(short.class) + " and " + getMaxValue(short.class);
        q = em.createQuery(query);
        students = q.getResultList();
        Assert.assertNotNull(students);
        Assert.assertEquals(2, students.size());
        int count = 0;
        for (StudentHBaseBoolean student : students)
        {
            if (student.getId() == ((Boolean) getMaxValue(Boolean.class)).booleanValue())
            {
                Assert.assertEquals(getMaxValue(short.class), student.getAge());
                Assert.assertEquals("Kuldeep", student.getName());
                count++;
            }
            else if (student.getId() == ((Boolean) getMinValue(Boolean.class)).booleanValue())
            {
                Assert.assertEquals(getPartialValue(short.class), student.getAge());
                Assert.assertEquals(getMinValue(String.class), student.getName());
                count++;
            }
        }
        Assert.assertEquals(2, count);

        em.close();
    }

    private void findByNameAndAgeGTAndLT()
    {
        EntityManager em;
        String query;
        Query q;
        List<StudentHBaseBoolean> students;
        em = emf.createEntityManager();
        query = "Select s From StudentHBaseBoolean s where s.name = Amresh and s.age > " + getPartialValue(short.class)
                + " and s.age < " + getMaxValue(short.class);
        q = em.createQuery(query);
        students = q.getResultList();
        Assert.assertNull(students);

        em.close();
    }

    private void findByNameAndAgeGTEQAndLTEQ()
    {
        EntityManager em;
        String query;
        Query q;
        List<StudentHBaseBoolean> students;
        int count;
        em = emf.createEntityManager();
        query = "Select s From StudentHBaseBoolean s where s.name = Kuldeep and s.age >= "
                + getPartialValue(short.class) + " and s.age <= " + getMaxValue(short.class);
        q = em.createQuery(query);
        students = q.getResultList();
        Assert.assertNotNull(students);
        Assert.assertEquals(2, students.size());
        count = 0;
        for (StudentHBaseBoolean student : students)
        {
            if (student.getId().equals(getMaxValue(Boolean.class)))
            {
                Assert.assertEquals(getMaxValue(short.class), student.getAge());
                Assert.assertEquals("Kuldeep", student.getName());
                count++;
            }
            else
            {
                Assert.assertEquals(getMinValue(Boolean.class), student.getId());
                Assert.assertEquals(getPartialValue(short.class), student.getAge());
                Assert.assertEquals(getMinValue(String.class), student.getName());
                count++;
            }

        }
        Assert.assertEquals(2, count);
        em.close();

    }

    private void findByAge()
    {
        EntityManager em;
        String query;
        Query q;
        List<StudentHBaseBoolean> students;
        int count;
        em = emf.createEntityManager();
        query = "Select s From StudentHBaseBoolean s where s.age = " + getPartialValue(short.class);
        q = em.createQuery(query);
        students = q.getResultList();
        Assert.assertNotNull(students);
        Assert.assertEquals(1, students.size());
        count = 0;
        for (StudentHBaseBoolean student : students)
        {
            Assert.assertEquals(getMinValue(Boolean.class), student.getId());
            Assert.assertEquals(getPartialValue(short.class), student.getAge());
            Assert.assertEquals(getMinValue(String.class), student.getName());
            count++;
        }
        Assert.assertEquals(1, count);
        em.close();
    }

    /**
     * 
     */
    private void findByName()
    {
        EntityManager em;
        String query;
        Query q;
        List<StudentHBaseBoolean> students;
        int count;
        em = emf.createEntityManager();
        query = "Select s From StudentHBaseBoolean s where s.name = Kuldeep";
        q = em.createQuery(query);
        students = q.getResultList();
        Assert.assertNotNull(students);
        Assert.assertEquals(2, students.size());
        count = 0;
        for (StudentHBaseBoolean student : students)
        {
            if (student.getId().equals(getMaxValue(Boolean.class)))
            {
                Assert.assertEquals(getMaxValue(short.class), student.getAge());
                Assert.assertEquals("Kuldeep", student.getName());
                count++;
            }
            else
            {
                Assert.assertEquals(getMinValue(Boolean.class), student.getId());
                Assert.assertEquals(getPartialValue(short.class), student.getAge());
                Assert.assertEquals(getMinValue(String.class), student.getName());
                count++;
            }
        }
        Assert.assertEquals(2, count);
        em.close();
    }

    /**
     * 
     */
    private void findAllQuery()
    {
        EntityManager em = emf.createEntityManager();
        // Selet all query.
        String query = "Select s From StudentHBaseBoolean s ";
        Query q = em.createQuery(query);
        List<StudentHBaseBoolean> students = q.getResultList();
        Assert.assertNotNull(students);
        Assert.assertEquals(2, students.size());
        int count = 0;
        for (StudentHBaseBoolean student : students)
        {
            if (student.getId().equals(getMaxValue(Boolean.class)))
            {
                Assert.assertEquals(getMaxValue(short.class), student.getAge());
                Assert.assertEquals("Kuldeep", student.getName());
                count++;
            }
            else if (student.getId().equals(getMinValue(Boolean.class)))
            {
                Assert.assertEquals(getPartialValue(short.class), student.getAge());
                Assert.assertEquals(getMinValue(String.class), student.getName());
                count++;
            }
        }
        Assert.assertEquals(2, count);
        em.close();
    }

    public void startCluster()
    {
    }

    public void stopCluster()
    {
        // TODO Auto-generated method stub

    }

    public void createSchema()
    {
    }

    public void dropSchema()
    {
    }

}
