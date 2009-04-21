package com.saliman.entitypruner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.collection.PersistentSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.saliman.entitypruner.testhelper.BaseDaoJpa;
import com.saliman.entitypruner.testhelper.junit.AbstractEjb3ContainerTest;
import com.saliman.entitypruner.testhelper.junit.Transactable;
import com.saliman.entitypruner.testhelper.set.TestSetChildDao;
import com.saliman.entitypruner.testhelper.set.TestSetChildEntity;
import com.saliman.entitypruner.testhelper.set.TestSetParentDao;
import com.saliman.entitypruner.testhelper.set.TestSetParentEntity;
import com.saliman.entitypruner.testhelper.set.TestSetUniChildDao;
import com.saliman.entitypruner.testhelper.set.TestSetUniChildEntity;

/**
 * The test class for the BaseDaoJpa class was getting ridiculously
 * long, so it has been split into 3 separate test classes.  This class tests
 * how the BaseDao operates when dealing with pruning and un-pruning.
 * <p>
 * This class uses the TestParentDaoJpa, TestChildDaoJpa, and
 * TestUniChildDaoJpa helper DAOs to test the methods in {@link BaseDaoJpa}
 * and {@link Persistable} classes.
 * <p>
 * It isn't generally a good idea to have a unit test that tests 2 things, 
 * but we really can't test the prune/unprune methods of the 
 * EntityPruner class without seeing how they save to the database.  This 
 * class has basically the same tests as the BaseDaoJpaTest class, but with
 * pruning and un-pruning.  Where possible, pruning and un-pruning happens
 * outside a transaction, because that is where pruning code needs to be in
 * a deployed Java EE application.
 * <p>
 * From time to time, you may want to see results of this test in the database.
 * To do this, you will need to do 2 things:<br>
 * 1: call <code>setDefaultRollback(false)</code>.
 * 2: Isolate the test you want to see by adding the <code>@Ignore </code> 
 * annotation to the rest of the tests. <b>Do not forget to remove them when
 * you are done!</b><br>
 * If you do choose to commit transactions, you will need to restore the 
 * database to its original state.
 * 
 * @author Steven C. Saliman
 */
public class EntityUtilSetTest extends AbstractEjb3ContainerTest  {
    // These constant names reflect OpenEjb naming conventions. We'll need to
    // change these to reflect embedded GlassFish when we switch.
    private static final String PARENT_DAO_NAME = "TestSetParentDaoLocal";
    private static final String CHILD_DAO_NAME = "TestSetChildDaoLocal";
    private static final String UNI_CHILD_DAO_NAME = "TestSetUniChildDaoLocal";
    private static final String PRUNER_NAME = "EntityPrunerLocal";
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
    private EntityPruner pruner;
    private TestSetParentEntity parent;
    private Set<TestSetChildEntity> children;
    private Set<TestSetUniChildEntity> uniChildren;
    private TestSetChildEntity child;

    /**
     * Default Constructor, initializes logging.
     */
    public EntityUtilSetTest() {
        super();
    }

    /**
     * Helper method to create the common data needed for each test.
     * Remember, the int_value column in the parent maps to a primitive, which
     * can't be null.
     */
    @SuppressWarnings("boxing")
    private void createData() throws Exception {
        // The first data set is a parent with both types of children.
        executeUpdate(PARENT_SQL, -1, "PARENT1", 0,
                "Test parent with children", USER, USER);
        executeUpdate(CHILD_SQL, -1, -1, "CHILD1", 
                "Test child 1", USER, USER);
        executeUpdate(CHILD_SQL, -2, -1, "CHILD2", 
                "Test child 2", USER, USER);
        executeUpdate(CHILD_SQL, -3, -1, "CHILD3",
                "Test child 3", USER, USER);
        executeUpdate(UNI_CHILD_SQL, -1, -1, "UNICHILD1",
                "Test uni_child 1", USER, USER);
        executeUpdate(UNI_CHILD_SQL, -2, -1, "UNICHILD2",
                "Test uni_child 2", USER, USER);
        executeUpdate(UNI_CHILD_SQL, -3, -1, "UNICHILD3",
                "Test uni_child 3", USER, USER);

        // Insert a parent with no uni-children
        executeUpdate(PARENT_SQL, -2, "PARENT2", 0,
                "Test parent with no uniChildren", USER, USER);
        executeUpdate(CHILD_SQL, -21, -2, "CHILD21",
                "Test child 2-1", USER, USER);
        executeUpdate(CHILD_SQL, -22, -2, "CHILD22",
                "Test child 2-2", USER, USER);
        executeUpdate(CHILD_SQL, -23, -2, "CHILD23",
                "Test child 2-3", USER, USER);

        // Insert a parent with no children at all.
        executeUpdate(PARENT_SQL, -3, "PARENT3", 0,
                "Test parent with no Children", USER, USER);
    }

    /**
     * Helper method to delete old data before and after each test.
     * @throws Exception if anything goes badly.
     */
    private void deleteData() throws Exception {
        // Make sure the DAOs aren't holding on to anything.
//        purgeDaos();
        String sql = "delete from test_child where create_user = '" + USER + "'";
        executeUpdate(sql);
        sql = "delete from test_uni_child where create_user = '" + USER + "'";
        executeUpdate(sql);
        sql = "delete from test_parent where create_user = '" + USER + "'";
        executeUpdate(sql);
    }

    /**
     * Lists up the test by getting what we need from the container.
     * @throws Exception 
     */
    @Before
    public void setUp() throws Exception {
        parentDao = (TestSetParentDao)getBean(PARENT_DAO_NAME);
        assertNotNull("Can't load parent Dao", parentDao);
        childDao = (TestSetChildDao)getBean(CHILD_DAO_NAME);
        assertNotNull("Can't load child Dao", childDao);
        uniChildDao = (TestSetUniChildDao)getBean(UNI_CHILD_DAO_NAME);
        assertNotNull("Can't load uniChildDao", uniChildDao);
        pruner = (EntityPruner)getBean(PRUNER_NAME);
        assertNotNull("Can't load entityPruner", pruner);
        parent = new TestSetParentEntity();
        children = new HashSet<TestSetChildEntity>(2);
        uniChildren = new HashSet<TestSetUniChildEntity>(2);
        parent.setCode("JPARENT1");
        parent.setDescription("Parent 1");
        parent.setAffirmative(Boolean.TRUE); // can't be null;
        parent.setCreateUser(USER);
        parent.setUpdateUser(USER);
        child = new TestSetChildEntity();
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
     * Cleans up after a test by releasing memory used by the test.  We can't 
     * really clear collections, because we may have un-pruned children.
     * @throws Exception if anything goes wrong
     */
    @After
    public void tearDown() throws Exception {
        uniChildren = null;
        children = null;
        parent = null;
        parentDao = null;
        pruner = null;
    }

    /**
     * See if we get the right thing when we check an initialized collection.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void initializedHibernate() throws Exception {
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parent = parentDao.findById(TEST_ID);
                assertFalse("Children should not be initialized",
                        EntityUtil.initialized(parent.getChildren()));
                // force the children to load.
                assertTrue("Parent should have children",
                        parent.getChildren().size() > 0);
                // recheck the children
            }
        });
        assertTrue("Children should be initialized after count",
                EntityUtil.initialized(parent.getChildren()));
    }

    /**
     * Make sure a new PersistentSet comes back uninitialized.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void initializedNewPersistentSet() throws Exception {
        parent.setChildren(new PersistentSet());
        assertFalse("new PersistentSets shouldn't be initialized",
                EntityUtil.initialized(parent.getChildren()));
    }

    /**
     * Make sure an ordinary collection comes back initialized.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void initializedHashSet() throws Exception {
        parent.setChildren(new HashSet<TestSetChildEntity>());
        assertTrue("new HashSets should be initialized",
                EntityUtil.initialized(parent.getChildren()));

    }

    /**
     * Make sure Null is treated as uninitialized.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void initializedNull() throws Exception {
        parent.setChildren(null);
        assertFalse("null collections shouldn't be initialized",
                EntityUtil.initialized(parent.getChildren()));
    }

    /**
     * Test to make sure copyTransientData works
     * @throws Exception if anything goes badly.
     */
    @Test
    public void copyTransientData() throws Exception {
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
     * @throws Exception if anything goes badly.
     */
    @Test
    public void copyTransientDataNull() throws Exception {
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
     * @throws Exception if anything goes badly.
     */
    @Test
    public void populateToDepthNegative() throws Exception {
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parent = parentDao.findById(TEST_ID);
                assertFalse("Children should not be initialized",
                        EntityUtil.initialized(parent.getChildren()));
                EntityUtil.populateToDepth(parent, -1);
                // recheck the children, they should still be uninitialized.
                assertFalse("Children should not be initialized after negative depth",
                        EntityUtil.initialized(parent.getChildren()));

            }
        });
    }

    /**
     * Try populating an entity to a depth when we give a depth greater than. 
     * the object graph will have.  It should load the children and there 
     * should be no issues with the large depth.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void populateToDepthPositive() throws Exception {
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parent = parentDao.findById(TEST_ID);
                assertFalse("Children should not be initialized",
                        EntityUtil.initialized(parent.getChildren()));
                EntityUtil.populateToDepth(parent, 4);
                // recheck the children, they should still be uninitialized.
                assertTrue("Children should be initialized after populating",
                        EntityUtil.initialized(parent.getChildren()));

            }
        });
    }
}
