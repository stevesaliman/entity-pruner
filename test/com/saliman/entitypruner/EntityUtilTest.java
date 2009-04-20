package com.saliman.entitypruner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.PropertyConfigurator;
import org.hibernate.collection.PersistentSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.transaction.annotation.Transactional;

import com.saliman.entitypruner.testhelper.set.TestSetChildDao;
import com.saliman.entitypruner.testhelper.set.TestSetChildEntity;
import com.saliman.entitypruner.testhelper.set.TestSetParentDao;
import com.saliman.entitypruner.testhelper.set.TestSetParentEntity;
import com.saliman.entitypruner.testhelper.set.TestSetUniChildDao;
import com.saliman.entitypruner.testhelper.set.TestSetUniChildEntity;

/**
 * This class tests the methods of the EntityUtil class.
 * 
 * @author Steven C. Saliman
 */
@ContextConfiguration(locations={"classpath:applicationContext-test.xml"})
@Transactional
public class EntityUtilTest
       extends AbstractTransactionalJUnit4SpringContextTests  {
    /** the class variable used for logging */
    private static final String PARENT_DAO_NAME = "testSetParentDaoJpa";
    private static final String CHILD_DAO_NAME = "testSetChildDaoJpa";
    private static final String UNI_CHILD_DAO_NAME = "testSetUniChildDaoJpa";
    private static final BigInteger TEST_ID = new BigInteger("-1");
    private static final String USER = "JUNIT";
    private static final String PARENT_SQL = 
        "insert into test_parent(id, code, int_value, description, " +
        "                        create_user, update_user) " +
        "values (?, ?, ?, ?, ?, ?)";
    private static final String CHILD_SQL =
        "insert into test_child(id, test_parent_id, code, description, " +
        "                       create_user, update_user) " +
        "values(?, ?, ?, ?, ?, ?)";
    private static final String UNI_CHILD_SQL =
        "insert into test_uni_child(id, test_parent_id, code, description, " +
        "                       create_user, update_user) " +
        "values(?, ?, ?, ?, ?, ?)";
    
    
    private TestSetParentDao parentDao;
    private TestSetChildDao childDao;
    private TestSetUniChildDao uniChildDao;
    private TestSetParentEntity parent;
    private Set<TestSetChildEntity> children;
    private Set<TestSetUniChildEntity> uniChildren;

    /**
     * Default Constructor, initializes logging.
     */
    public EntityUtilTest() {
        super();
        URL url = null;
        String name = null;
        String filename = "log4j.properties";
        try {
            Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(filename);
            // Get the last file found.
            while (urls.hasMoreElements()) {
                url = urls.nextElement();
            }
            name = url.getFile();
            // If the last file was part of a container, don't use it
            if ( name.contains("run.jar") ) {
                // JBoss' log4j.properties file is in run.jar
                url = null;
                name = null;
            }

        } catch (IOException e) {
            System.out.println("Error configuring logging:");
            e.printStackTrace();
        }
        // From the code above, if we have a URL, we also have a name.
        if ( url != null ) {
            // We want to watch for changes unless:
            // 1) We are given a negative delay
            // 2) The config file was found inside a jar, indicated by the "!"
            if ( name.contains("!") ) {
                PropertyConfigurator.configure(url);
            } else {
                PropertyConfigurator.configureAndWatch(name, 60L*1000);
            }
        } else {
            System.out.println("Warning: a useable " + filename + " file " +
            "was not found in the classpath");
        }
    }
    
    /**
     * Helper method to create the common data needed for each test.
     * Remember, the int_value column in the parent maps to a primitive, which
     * can't be null.
     */
    @SuppressWarnings("boxing")
    private void createData() throws Exception {
        // The first data set is a parent with both types of children.
        simpleJdbcTemplate.update(PARENT_SQL, -1, "PARENT1", 0,
                                  "Test parent with children", USER, USER);
        simpleJdbcTemplate.update(CHILD_SQL, -1, -1, "CHILD1", 
                                  "Test child 1", USER, USER);
        simpleJdbcTemplate.update(CHILD_SQL, -2, -1, "CHILD2", 
                                  "Test child 2", USER, USER);
        simpleJdbcTemplate.update(CHILD_SQL, -3, -1, "CHILD3",
                                  "Test child 3", USER, USER);
        simpleJdbcTemplate.update(UNI_CHILD_SQL, -1, -1, "UNICHILD1",
                                  "Test uni_child 1", USER, USER);
        simpleJdbcTemplate.update(UNI_CHILD_SQL, -2, -1, "UNICHILD2",
                                  "Test uni_child 2", USER, USER);
        simpleJdbcTemplate.update(UNI_CHILD_SQL, -3, -1, "UNICHILD3",
                                  "Test uni_child 3", USER, USER);
        
        // Insert a parent with no uni-children
        simpleJdbcTemplate.update(PARENT_SQL, -2, "PARENT2", 0,
                                 "Test parent with no uniChildren", USER, USER);
        simpleJdbcTemplate.update(CHILD_SQL, -21, -2, "CHILD21",
                                  "Test child 2-1", USER, USER);
        simpleJdbcTemplate.update(CHILD_SQL, -22, -2, "CHILD22",
                                  "Test child 2-2", USER, USER);
        simpleJdbcTemplate.update(CHILD_SQL, -23, -2, "CHILD23",
                                  "Test child 2-3", USER, USER);

        // Insert a parent with no children at all.
        simpleJdbcTemplate.update(PARENT_SQL, -3, "PARENT3", 0,
                                  "Test parent with no Children", USER, USER);
    }

    /**
     * Helper method to delete old data before and after each test.
     * @throws Exception if anything goes badly.
     */
    private void deleteData() throws Exception {
        String sql = "delete from test_child where create_user = '" + USER + "'";
        simpleJdbcTemplate.update(sql);
        sql = "delete from test_uni_child where create_user = '" + USER + "'";
        simpleJdbcTemplate.update(sql);
        sql = "delete from test_parent where create_user = '" + USER + "'";
        simpleJdbcTemplate.update(sql);
    }
    
    /**
     * Lists up the test by getting what we need from Spring.
     * @throws Exception 
     */
    @Before
    public void setUp() throws Exception {
        deleteData(); // in case some other test did a commit.
        createData();
        parentDao = (TestSetParentDao)applicationContext.getBean(PARENT_DAO_NAME);
        assertNotNull("Unable to get TestParentDao", parentDao);
        childDao = (TestSetChildDao)applicationContext.getBean(CHILD_DAO_NAME);
        assertNotNull("Unable to get TestChildDao", childDao);
        uniChildDao = (TestSetUniChildDao)applicationContext.getBean(UNI_CHILD_DAO_NAME);
        assertNotNull("Unable to get TestUniChildDao", uniChildDao);
        parent = new TestSetParentEntity();
        children = new HashSet<TestSetChildEntity>(2);
        uniChildren = new HashSet<TestSetUniChildEntity>(2);
        parent.setCode("JPARENT1");
        parent.setDescription("Parent 1");
        parent.setAffirmative(Boolean.TRUE); // can't be null;
        parent.setCreateUser(USER);
        parent.setUpdateUser(USER);
        TestSetChildEntity child = new TestSetChildEntity();
        child.setParent(parent);
        child.setCode("JCHILD1");
        child.setDescription("Child 1");
        child.setCreateUser(USER);
        child.setUpdateUser(USER);
        children.add(child);
        parent.setChildren(children);
        TestSetUniChildEntity uniChild = new TestSetUniChildEntity();
        uniChild.setCode("JCHILD1");
        uniChild.setDescription("Child 1");
        uniChild.setCreateUser(USER);
        uniChild.setUpdateUser(USER);
        uniChildren.add(uniChild);
        parent.setUniChildren(uniChildren);
    }

    /**
     * Cleans up after a test by releasing memory used by the test.
     * @throws Exception if anything goes wrong
     */
    @After
    public void tearDown() throws Exception {
        deleteData();
        // no need to set complete if we're rolling back.
        if ( uniChildren != null ) {
            uniChildren.clear();
        }
        if ( children != null ) {
            children.clear();
        }
        uniChildren = null;
        children = null;
        parent = null;
        parentDao = null;
    }

    /**
     * See if we get the right thing when we check an initialized collection.
     */
    @Test
    public void initializedHibernate() {
        parent = parentDao.findById(TEST_ID);
        assertFalse("Children should not be initialized",
                    EntityUtil.initialized(parent.getChildren()));
        // force the children to load.
        assertTrue("Parent should have children",
                   parent.getChildren().size() > 0);
        // recheck the children
        assertTrue("Children should be initialized after count",
                EntityUtil.initialized(parent.getChildren()));
    }
    
    /**
     * Make sure a new PersistentList comes back uninitialized.
     */
    @Test
    public void initializedNewPersistentBag() {
        parent.setChildren(new PersistentSet());
        assertFalse("new PersistentLists shouldn't be initialized",
                EntityUtil.initialized(parent.getChildren()));
    }
    
    /**
     * Make sure an ordinary collection comes back initialized.
     */
    @Test
    public void initializedArrayList() {
        parent.setChildren(new HashSet<TestSetChildEntity>());
        assertTrue("new ArrayLists should be initialized",
                EntityUtil.initialized(parent.getChildren()));
        
    }
    
    /**
     * Make sure Null is treated as uninitialized.
     */
    @Test
    public void initializedNull() {
        parent.setChildren(null);
        assertFalse("null collections shouldn't be initialized",
                EntityUtil.initialized(parent.getChildren()));
    }
    
    /**
     * Test to make sure copyTransientData works
     */
    @Test
    public void copyTransientData() {
        // put the children in the transient collection.
        parent.setTransChildren(parent.getChildren());
        TestSetParentEntity copy = new TestSetParentEntity();
        EntityUtil.copyTransientData(parent, copy);
        assertEquals("Failed to copy transient children",
                parent.getTransChildren(), copy.getTransChildren());
        assertNull("Shouldn't have set the non-transient children",
                   copy.getChildren());
    }

    /**
     * Test to make sure copyTransientData works
     */
    @Test
    public void copyTransientDataNull() {
        // put the children in the transient collection.
        parent.setTransChildren(parent.getChildren());
        TestSetParentEntity copy = new TestSetParentEntity();
        // Our Unit Test utility won't handle static methods so ...
        try {
            EntityUtil.copyTransientData(parent, null);
            fail("Should have gotten a NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            EntityUtil.copyTransientData(null, copy);
            fail("Should have gotten a NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }
    
    /**
     * Test to make sure populate to depth works with a null entity.  It won't
     * do anything, but there should be no error.
     */
    @Test
    public void populateToDepthNull() {
        EntityUtil.populateToDepth(null, 3);
    }
    
    /**
     * Try populating an entity to a depth when we give a negative depth. It
     * won't do anything, but it shouldn't throw an error either.
     */
    @Test
    public void populateToDepthNegative() {
        parent = parentDao.findById(TEST_ID);
        assertFalse("Children should not be initialized",
                    EntityUtil.initialized(parent.getChildren()));
        EntityUtil.populateToDepth(parent, -1);
        // recheck the children, they should still be uninitialized.
        assertFalse("Children should not be initialized after negative depth",
                EntityUtil.initialized(parent.getChildren()));

    }

    /**
     * Try populating an entity to a depth when we give a depth greater than. 
     * the object graph will have.  It should load the children and there 
     * should be no issues with the large depth.
     */
    @Test
    public void populateToDepthPositive() {
        parent = parentDao.findById(TEST_ID);
        assertFalse("Children should not be initialized",
                    EntityUtil.initialized(parent.getChildren()));
        EntityUtil.populateToDepth(parent, 4);
        // recheck the children, they should still be uninitialized.
        assertTrue("Children should be initialized after populating",
                EntityUtil.initialized(parent.getChildren()));

    }
}
