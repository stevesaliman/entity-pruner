package com.saliman.entitypruner;

import static com.saliman.entitypruner.testhelper.junit.JunitUtil.assertEjbThrows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;

import org.hibernate.collection.PersistentList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.saliman.entitypruner.testhelper.BaseDaoJpa;
import com.saliman.entitypruner.testhelper.junit.AbstractEjb3ContainerTest;
import com.saliman.entitypruner.testhelper.junit.Transactable;
import com.saliman.entitypruner.testhelper.list.TestListChildDao;
import com.saliman.entitypruner.testhelper.list.TestListChildEntity;
import com.saliman.entitypruner.testhelper.list.TestListParentDao;
import com.saliman.entitypruner.testhelper.list.TestListParentEntity;
import com.saliman.entitypruner.testhelper.list.TestListUniChildDao;
import com.saliman.entitypruner.testhelper.list.TestListUniChildEntity;

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
public class EntityPrunerListTest extends AbstractEjb3ContainerTest  {
    // These constant names reflect OpenEjb naming conventions. We'll need to
    // change these to reflect embedded GlassFish when we switch.
    private static final String PARENT_DAO_NAME = "TestListParentDaoLocal";
    private static final String CHILD_DAO_NAME = "TestListChildDaoLocal";
    private static final String UNI_CHILD_DAO_NAME = "TestListUniChildDaoLocal";
    private static final String PRUNER_NAME = "EntityPrunerLocal";
    private static final String PARENT_TABLE = "TEST_PARENT";
    private static final String CHILD_TABLE = "TEST_CHILD";
    private static final String UNI_CHILD_TABLE = "TEST_UNI_CHILD";
    private static final BigInteger TEST_ID = new BigInteger("-1");
    private static final BigInteger TEST_UNICHILDLESS_ID = new BigInteger("-2");
    private static final BigInteger TEST_CHILDLESS_ID = new BigInteger("-3");
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


    private TestListParentDao parentDao;
    private TestListChildDao childDao;
    private TestListUniChildDao uniChildDao;
    private EntityPruner pruner;
    private TestListParentEntity parent;
    private List<TestListChildEntity> children;
    private List<TestListUniChildEntity> uniChildren;
    private List<TestListChildEntity> transChildren;
    private TestListChildEntity child;
    private List<TestListChildEntity> childList;
    private int parentRows = -1;
    private int childRows = -1;
    private int uniChildRows = -1;
    private int numTransChild = -1;

    /**
     * Default Constructor, initializes logging.
     */
    public EntityPrunerListTest() {
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
        parentDao = (TestListParentDao)getBean(PARENT_DAO_NAME);
        assertNotNull("Can't load parent Dao", parentDao);
        childDao = (TestListChildDao)getBean(CHILD_DAO_NAME);
        assertNotNull("Can't load child Dao", childDao);
        uniChildDao = (TestListUniChildDao)getBean(UNI_CHILD_DAO_NAME);
        assertNotNull("Can't load uniChildDao", uniChildDao);
        pruner = (EntityPruner)getBean(PRUNER_NAME);
        assertNotNull("Can't load entityPruner", pruner);
        parent = new TestListParentEntity();
        children = new ArrayList<TestListChildEntity>(2);
        uniChildren = new ArrayList<TestListUniChildEntity>(2);
        parent.setCode("JPARENT1");
        parent.setDescription("Parent 1");
        parent.setAffirmative(Boolean.TRUE); // can't be null;
        parent.setCreateUser(USER);
        parent.setUpdateUser(USER);
        child = new TestListChildEntity();
        child.setParent(parent);
        child.setCode("JCHILD1");
        child.setDescription("Child 1");
        child.setCreateUser(USER);
        child.setUpdateUser(USER);
        children.add(child);
        parent.setChildren(children);
        TestListUniChildEntity uniChild = new TestListUniChildEntity();
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
     * Try the following sequence of events:
     * Create a new parent object with null children and uni-children.
     * Un-prune the object.  Do we get an empty PersistentList?
     * Save the object.  It should succeed. This simulates a new record 
     * coming in from a Web Service.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void insertUnprunedParentNullChildren() throws Exception {
        parent.setChildren(null);
        parent.setUniChildren(null);
        parent.setPruned(true);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                pruner.unprune(parent);
            };
        });
        assertNull("unpruning should not have created a child set",
                parent.getChildren());
        assertNull("unpruning should not have created a uniChild set",
                parent.getUniChildren());
        assertNull("unpruning should not have created a transChild set",
                parent.getTransChildren());
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parentRows = countRowsInTable(PARENT_TABLE);
                childRows = countRowsInTable(CHILD_TABLE);
                uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
                parent = parentDao.save(parent);
                assertEquals("Failed to insert parent", 
                        parentRows+1, countRowsInTable(PARENT_TABLE));
                assertEquals("Shouldn't have changed children", 
                        childRows, countRowsInTable(CHILD_TABLE));
                assertEquals("Shouldn't have changed the uniChild table.",
                        uniChildRows,
                        countRowsInTable(UNI_CHILD_TABLE));
            }
        });
    }

    /**
     * Try inserting a new parent with new bidirectional children after
     * un-pruning.  The save should succeed.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void insertUnprunedParentAndChildren() throws Exception {
        parent.setUniChildren(null);
        parent.setPruned(true);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                pruner.unprune(parent);
            };
        });
        assertNotNull("unpruning should have kept the a child set",
                parent.getChildren());
        assertNull("unpruning should not have created a uniChild set",
                parent.getUniChildren());
        assertNull("unpruning should not have created a transChild set",
                parent.getTransChildren());
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parentRows = countRowsInTable(PARENT_TABLE);
                childRows = countRowsInTable(CHILD_TABLE);
                uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
                parent = parentDao.save(parent);
                assertEquals("Failed to insert parent", 
                        parentRows+1, countRowsInTable(PARENT_TABLE));
                assertEquals("Failed to insert children", 
                        childRows+parent.getChildren().size(),
                        countRowsInTable(CHILD_TABLE));
                assertEquals("Shouldn't have changed the uniChild table.",
                        uniChildRows,
                        countRowsInTable(UNI_CHILD_TABLE));
            }
        });
    }

    /** 
     * Try inserting a new parent with new unidirectional children after
     * re-hydrating.  This should fail because Hibernate won't resolve the ID
     * in the child records, but the unpruning should work.
     * <p>
     * The actual exception is misleading, but as we discovered in the 
     * BaseDaoJpaTest, this is just something JPA does.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void insertUnprunedParentAndUniChildren() throws Exception {
        parent.setChildren(null);
        parent.setPruned(true);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                pruner.unprune(parent);
            };
        });
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                assertNull("unpruning should not have created  a child set",
                        parent.getChildren());
                assertNotNull("unpruning should have kept a uniChild set",
                        parent.getUniChildren());
                assertNull("unpruning should not have created a transChild set",
                        parent.getTransChildren());
                assertEjbThrows(EntityExistsException.class, parentDao, "save", parent);
            }
        });
    }

    /** 
     * Try inserting a new parent with new transient children after 
     * un-pruning. This should have no problems.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void insertRehydratedParentAndTransChildren() throws Exception {
        // put the children in the transient collection.
        parent.setTransChildren(parent.getChildren());
        parent.setChildren(null);
        parent.setUniChildren(null);
        parent.setPruned(true);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                pruner.unprune(parent);
            };
        });
        assertNull("unpruning should not have created the a child set",
                parent.getChildren());
        assertNull("unpruning should not have created a uniChild set",
                parent.getUniChildren());
        assertNotNull("unpruning should have kept a transChild set",
                parent.getTransChildren());
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parentRows = countRowsInTable(PARENT_TABLE);
                childRows = countRowsInTable(CHILD_TABLE);
                uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
                parent = parentDao.save(parent);
                assertEquals("Failed to insert parent", 
                        parentRows+1, countRowsInTable(PARENT_TABLE));
                assertEquals("Shouldn't have changed children", 
                        childRows, countRowsInTable(CHILD_TABLE));
                assertEquals("Shouldn't have changed the uniChild table.",
                        uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
            }
        });
    }

    /**
     * Try fetching a parent with lazy children (both-types).  Add Transient
     * children and prune the object. Make sure the collections come back
     * null. Make a change to the parent and make sure we can still save the
     * un-pruned object.  This tests the savability of pruned/un-pruned
     * objects, and also tests the collection types of children.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void updateParentWithoutFetchingChildren() throws Exception {
        // This test spans transactions, so we need to commit in this test.
        setDefaultRollback(false);
        // Get children BEFORE fetching the object. Then fetch the parent
        // then set the transient children in the parent. Mess this up
        // And you'll wind up fetching the children from the DB. We 
        // do this inside a transaction because of Java scoping 
        transChildren = new ArrayList<TestListChildEntity>(parent.getChildren().size());
        transChildren.addAll(parent.getChildren());
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parent = parentDao.findById(TEST_ID);
                parent.setTransChildren(transChildren);
                parentRows = countRowsInTable(PARENT_TABLE);
                childRows = countRowsInTable(CHILD_TABLE);
                uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
                numTransChild = parent.getTransChildren().size();
            }
        });
        // prune the object outside the transaction.  Change description and
        // un-prune.
        pruner.prune(parent);
        assertNull("Prune should have nulled out the unitialized child set",
                parent.getChildren());
        assertNull("Prune should have nulled out the unitialized uniChild set",
                parent.getUniChildren());
        assertNotNull("Prune should not have nulled out the transChildren",
                parent.getTransChildren());
        parent.setDescription("Updated by updateParentWithoutFetchingChildren");
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                pruner.unprune(parent);
            };
        });
        // This time, our null sets should be replaced with PersistentLists.
        children = parent.getChildren();
        assertNotNull("Unprune should have created a PersistentList for children",
                children);
        assertTrue("The child set is the wrong type",
                PersistentList.class.isAssignableFrom(children.getClass()));
        uniChildren = parent.getUniChildren();
        assertNotNull("Unprune should have created a PersistentList for uniChildren",
                uniChildren);
        assertTrue("The uniChild set is the wrong type",
                PersistentList.class.isAssignableFrom(uniChildren.getClass()));
        // Save the changed object in a new transaction.
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                parent = parentDao.save(parent);
                assertEquals("Shouldn't have changed parent count", 
                        parentRows, countRowsInTable(PARENT_TABLE));
                assertEquals("Shouldn't have changed child count", 
                        childRows, countRowsInTable(CHILD_TABLE));
                assertEquals("Shouldn't have changed uniChild count", 
                        uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
                assertEquals("Shouldn't have changed the transient child count",
                        numTransChild, parent.getTransChildren().size());
                // Don't forget to clean up.
                deleteData();
            }
        });
    }

    /**
     * Try fetching a parent with lazy children (both-types), and fetch the
     * children. Add Transient children and Prune the object. Make sure
     * the collections come back non-null. Make a change to the parent and make
     * sure we can still save the un-pruned object.  This tests the savability 
     * of pruned/un-pruned objects, and also tests the collection types of 
     * children. This test also makes a change to a child record.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void updateParentFetchingChildren() throws Exception {
        // This test spans transactions, so set rollback to false.
        setDefaultRollback(false);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parent = parentDao.findById(TEST_ID);
                assertTrue("Failed to fetch Children", parent.getChildren().size() > 0);
                assertTrue("Failed to fetch uniChildren", parent.getUniChildren().size() > 0);
                transChildren = new ArrayList<TestListChildEntity>(parent.getChildren().size());
                transChildren.addAll(parent.getChildren());
                parent.setTransChildren(transChildren);
                parentRows = countRowsInTable(PARENT_TABLE);
                childRows = countRowsInTable(CHILD_TABLE);
                uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
                numTransChild = parent.getTransChildren().size();
            }
        });
        // prune the parent outside a transaction and make sure we get what 
        // we were expecting.
        pruner.prune(parent);
        children = parent.getChildren();
        assertNotNull("Prune should not null fetched children",
                children);
        assertFalse("The Pruned child set shouldn't be a PersistentList",
                PersistentList.class.isAssignableFrom(children.getClass()));
        uniChildren = parent.getUniChildren();
        assertNotNull("Prune should not null fetched uniChildren",
                uniChildren);
        assertFalse("The pruned uniChild set shouldn't be a PersistentList",
                PersistentList.class.isAssignableFrom(uniChildren.getClass()));
        String newDesc = "Updated by updateParentFetchingChildren";
        parent.setDescription(newDesc);
        for ( TestListChildEntity c : parent.getChildren() ) {
            c.setDescription(newDesc);
        }
        for ( TestListUniChildEntity u : parent.getUniChildren() ) {
            u.setDescription(newDesc);
        }
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                pruner.unprune(parent);
            };
        });
        // This time, our null sets should be replaced with PersistentLists.
        children = parent.getChildren();
        assertNotNull("Unprune should have created a List for children",
                children);
        uniChildren = parent.getUniChildren();
        assertNotNull("Unprune should have created a List for uniChildren",
                uniChildren);
        
        // Save the updated object in a new transaction
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                parent = parentDao.save(parent);
                assertEquals("Shouldn't have changed parent count", 
                        parentRows, countRowsInTable(PARENT_TABLE));
                assertEquals("Shouldn't have changed child count", 
                        childRows, countRowsInTable(CHILD_TABLE));
                assertEquals("Shouldn't have changed uniChild count", 
                        uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
                assertEquals("Shouldn't have changed the transient child count",
                        numTransChild, parent.getTransChildren().size());
                // don't forget to clean up
                deleteData();
            }
        });
    }

    /**
     * Try loading a parent with children.  Make sure the children load 
     * from the database and prune to a depth of 1.  Make sure the children
     * are now null.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void pruneToDepth() throws Exception {
        // we want to do a count between transactions, so we need to 
        // disable rollback.
        setDefaultRollback(false);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parent = parentDao.findById(TEST_ID);
                assertTrue("Failed to fetch Children", parent.getChildren().size() > 0);
                assertTrue("Failed to fetch uniChildren", parent.getUniChildren().size() > 0);
                transChildren = new ArrayList<TestListChildEntity>(parent.getChildren().size());
                transChildren.addAll(parent.getChildren());
                parent.setTransChildren(transChildren);
                parentRows = countRowsInTable(PARENT_TABLE);
                childRows = countRowsInTable(CHILD_TABLE);
                uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
                numTransChild = parent.getTransChildren().size();
            }
        });
        // Remember, pruning must happen outside a transaction.  First, 
        // make sure the children are still loaded.
        assertTrue("Failed to fetch Children", parent.getChildren().size() > 0);
        assertTrue("Failed to fetch uniChildren", parent.getUniChildren().size() > 0);
        pruner.prune(parent, 1);
        assertNull("We shouldn't have children anymore", parent.getChildren());
        assertNull("We shouldn't have uniChildren anymore", parent.getUniChildren());
        assertNull("We shouldn't have transient children anymore", parent.getTransChildren());
        // Make sure the counts aren't affected.
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                assertEquals("Shouldn't have changed parent count", 
                        parentRows, countRowsInTable(PARENT_TABLE));
                assertEquals("Shouldn't have changed child count", 
                        childRows, countRowsInTable(CHILD_TABLE));
                assertEquals("Shouldn't have changed uniChild count", 
                        uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
                // don't forget to clean up
                deleteData();
            }
        });
    }

    /**
     * Try loading a parent with no children.  Access the collections then 
     * prune.  Do we get an empty collection? This should not return null
     * because we actually tried to do a fetch on the children. Try on both 
     * collections.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void pruneChildlessParent() throws Exception {
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parent = parentDao.findById(TEST_CHILDLESS_ID);
                assertTrue("Shouldn't have Children", parent.getChildren().size() == 0);
                assertTrue("Shouldn't have uniChildren", parent.getUniChildren().size() ==0);
            }
        });
        // prune the entity outside a transaction.
        pruner.prune(parent);
        assertNotNull("Should still have child set", parent.getChildren());
        assertEquals("Should have empty child set",
                0, parent.getChildren().size());
        assertNotNull("Should still have uniChild set", parent.getUniChildren());
        assertEquals("Should have empty uniChild set",
                0, parent.getUniChildren().size());
    }

    /**
     * Try creating a child object with a valid (but pruned) parent (from
     * the db). Un-prune the child and try a child save. This simulates
     * creating a child from a web service client.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void insertUnprunedChild() throws Exception {
        // This test spans transactions, so set rollback to false.
        setDefaultRollback(false);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parentRows = countRowsInTable(PARENT_TABLE);
                childRows = countRowsInTable(CHILD_TABLE);
                uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
                parent = parentDao.findById(TEST_ID);
            }
        });
        // prune and add the child outside a transaction.  Then un-prune.
        pruner.prune(parent); // simulate giving to client
        child = new TestListChildEntity();
        child.setParent(parent);
        child.setCode("TESTINS");
        child.setDescription("Inserted by insertUnprunedChild");
        child.setCreateUser(USER);
        child.setUpdateUser(USER);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                pruner.unprune(child);
            };
        });
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                child = childDao.save(child);
                assertNotNull("Child should now have an ID", child.getId());
                assertEquals("Shouldn't have changed parent table", 
                        parentRows, countRowsInTable(PARENT_TABLE));
                assertEquals("Failed to insert child", 
                        childRows+1, countRowsInTable(CHILD_TABLE));
                assertEquals("Shouldn't have changed uniChild table", 
                        uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
                // Don't forget to clean up
                deleteData();
            }
        });
    }

    /**
     * Try creating a new child pointing to a new parent and un-prune the 
     * child.  We expect the save to fail because Hibernate wants the parent
     * to be saved first, but the un-prune should work. 
     * @throws Exception if anything goes badly.
     */
    @Test
    public void insertUnprunedChildNewParent() throws Exception {
        parent.setId(null);
        child = new TestListChildEntity();
        child.setParent(parent);
        child.setCode("TESTINS");
        child.setDescription("Inserted by insertUnprunedChildNewParent");
        child.setCreateUser(USER);
        child.setUpdateUser(USER);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                pruner.unprune(child);
            };
        });
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                assertEjbThrows(IllegalStateException.class, childDao, "save", child);
            }
        });
    }

    /**
     * Try adding a child to a parent while it's pruned, and saving it
     * when it is un-pruned. For this test the child points to its parent.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void addChildPointingToParent() throws Exception {
        // We need to span transactions
        setDefaultRollback(false);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parentRows = countRowsInTable(PARENT_TABLE);
                childRows = countRowsInTable(CHILD_TABLE);
                uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
                parent = parentDao.findById(TEST_ID);
                assertTrue("Need a parent that has children", parent.getChildren().size()>0);
            }
        });
        // prune and add child outside transaction.
        pruner.prune(parent);
        child = new TestListChildEntity();
        child.setParent(parent);
        child.setCode("TESTINS");
        child.setDescription("Inserted by addChildPointingToParent");
        child.setCreateUser(USER);
        child.setUpdateUser(USER);
        parent.getChildren().add(child);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                pruner.unprune(parent);
            };
        });

        // Save in new transaction.
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                parent = parentDao.save(parent);
                assertEquals("Shouldn't have changed parent table", 
                        parentRows, countRowsInTable(PARENT_TABLE));
                assertEquals("Failed to insert child", 
                        childRows+1, countRowsInTable(CHILD_TABLE));
                assertEquals("Shouldn't have changed uniChild table", 
                        uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
                // don't forget to clean up
                deleteData();
            }
        });
    }

    /**
     * Try adding a child to a parent while it's pruned, and saving it
     * when it is un-pruned. For this test the child does not point to its
     * parent, but un-pruned the parent should fix that.  If it doesn't,
     * we'll know because the child won't save with a null parent.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void addChildNotPointingToParent() throws Exception {
        // We need to span transactions
        setDefaultRollback(false);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parentRows = countRowsInTable(PARENT_TABLE);
                childRows = countRowsInTable(CHILD_TABLE);
                uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
                parent = parentDao.findById(TEST_ID);
                assertTrue("Need a parent that has children", parent.getChildren().size()>0);
            }
        });
        
        // prune and add child outside transaction.
        pruner.prune(parent);
        child = new TestListChildEntity();
        child.setCode("TESTINS");
        child.setDescription("Inserted by addChildNotPointingToParent");
        child.setCreateUser(USER);
        child.setUpdateUser(USER);
        parent.getChildren().add(child);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                pruner.unprune(parent);
            };
        });
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                parent = parentDao.save(parent);
                assertEquals("Shouldn't have changed parent table", 
                        parentRows, countRowsInTable(PARENT_TABLE));
                assertEquals("Failed to insert child", 
                        childRows+1, countRowsInTable(CHILD_TABLE));
                assertEquals("Shouldn't have changed uniChild table", 
                        uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
                // Don't forget to clean up
                deleteData();
            }
        });
    }

    /**
     * Try adding a uniChild to a parent while it's pruned, and saving it
     * when it is un-pruned. For this test the child points to its parent.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void addUniChildPointingToParent() throws Exception {
        // We need to span transactions
        setDefaultRollback(false);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parentRows = countRowsInTable(PARENT_TABLE);
                childRows = countRowsInTable(CHILD_TABLE);
                uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
                parent = parentDao.findById(TEST_ID);
                assertTrue("Need a parent that has uniChildren", parent.getUniChildren().size()>0);
            }
        });

        // prune, change entity, unprune outside transaction.
        pruner.prune(parent);
        TestListUniChildEntity uniChild = new TestListUniChildEntity();
        uniChild.setParentId(parent.getId());
        uniChild.setCode("TESTINS");
        uniChild.setDescription("Inserted by addUniChildPointingToParent");
        uniChild.setCreateUser(USER);
        uniChild.setUpdateUser(USER);
        parent.getUniChildren().add(uniChild);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                pruner.unprune(parent);
            };
        });

        // save in new transaction
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                parent = parentDao.save(parent);
                assertEquals("Shouldn't have changed parent table", 
                        parentRows, countRowsInTable(PARENT_TABLE));
                assertEquals("Shouldn't have changed child table", 
                        childRows, countRowsInTable(CHILD_TABLE));
                assertEquals("Failed to insert uniChild", 
                        uniChildRows+1, countRowsInTable(UNI_CHILD_TABLE));
                // clean up.
                deleteData();
            }
        });
    }

    /**
     * Try adding a child to a parent while it's pruned, and saving it
     * when it is un-pruned. For this test the child does not point to its
     * parent. This save should fail because the child was not set up 
     * correctly, and the de-pruner doesn't have any way to know how to 
     * get the ID.
     * <p>
     * The actual exception is misleading, but as we discovered in the 
     * BaseDaoJpaTest, this is just something JPA does.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void addUniChildNotPointingToParent() throws Exception {
        // we need to span transactions
        setDefaultRollback(false);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parent = parentDao.findById(TEST_ID);
                assertTrue("Need a parent that has uniChildren", parent.getUniChildren().size()>0);
            }
        });
        pruner.prune(parent);
        TestListUniChildEntity uniChild = new TestListUniChildEntity();
        uniChild.setCode("TESTINS");
        uniChild.setDescription("Inserted by addChildNotPointingToParent");
        uniChild.setCreateUser(USER);
        uniChild.setUpdateUser(USER);
        parent.getUniChildren().add(uniChild);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                pruner.unprune(parent);
            };
        });
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                assertEjbThrows(EntityExistsException.class, parentDao, "save", parent);
            }
        });
        // Clean up in a new transaction because the exception above kills
        // the transaction.
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData();
            }
        });
    }

    /**
     * Try adding a child to a parent that never fetched children.  We expect
     * hibernate to keep this sane by only inserting the new item without
     * deleting the old ones.  This only works without the delete-orphan option,
     * which is one of the reasons we don't use that option.<br>
     * If this works, there really isn't a need to test the other collections.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void addChildPointingToParentUnfetched() throws Exception {
        // we need to span transactions
        setDefaultRollback(false);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parentRows = countRowsInTable(PARENT_TABLE);
                childRows = countRowsInTable(CHILD_TABLE);
                uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
                parent = parentDao.findById(TEST_ID);
            }
        });
        pruner.prune(parent);
        assertNull("We should have a null child set in the pruned parent",
                parent.getChildren());
        child = new TestListChildEntity();
        child.setParent(parent);
        child.setCode("TESTINS");
        child.setDescription("Inserted by addChildPointingToParent");
        child.setCreateUser(USER);
        child.setUpdateUser(USER);
        children = new ArrayList<TestListChildEntity>();
        children.add(child);
        parent.setChildren(children);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                pruner.unprune(parent);
            };
        });
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                parent = parentDao.save(parent);
                assertEquals("Shouldn't have changed parent table", 
                        parentRows, countRowsInTable(PARENT_TABLE));
                assertEquals("Failed to insert child", 
                        childRows+1, countRowsInTable(CHILD_TABLE));
                assertEquals("Shouldn't have changed uniChild table", 
                        uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
                // don't forget to clean up
                deleteData();
            }
        });
    }

    /**
     * Try loading a parent that only has bidirectional children.  Don't fetch
     * the collection and try a delete. 
     * <p>
     * Note that this test requires multiple sessions.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void deleteParentWithUnfetchedChildren() throws Exception {
        // we need to span transactions
        setDefaultRollback(false);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parentRows = countRowsInTable(PARENT_TABLE);
                childRows = countRowsInTable(CHILD_TABLE);
                uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
                parent = parentDao.findById(TEST_UNICHILDLESS_ID);
            }
        });
        // prune and un-prune outside the transaction.
        pruner.prune(parent);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                pruner.unprune(parent);
            };
        });
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                parentDao.delete(parent);
                assertEquals("Failed to delete parent", 
                        parentRows-1, countRowsInTable(PARENT_TABLE));
                assertTrue("Failed to delete children", 
                        childRows > countRowsInTable(CHILD_TABLE));
                assertEquals("Shouldn't have changed uniChild table", 
                        uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
                // don't forget to clean up
                deleteData();
            }
        });
    }

    /**
     * Try loading a parent that has unidirectional children.  Don't fetch
     * the collection and try a delete.  We expect the delete fail because we
     * can't disassociate  the uniChildren from it's parent.  This is a 
     * feature of how Hibernate deals with unidirectional joins.
     * <p>
     * Note that this test requires multiple sessions.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void deleteParentWithUnfetchedUniChildren() throws Exception {
        // we need to span transactions
        setDefaultRollback(false);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parent = parentDao.findById(TEST_ID);
            }
        });
        pruner.prune(parent);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                pruner.unprune(parent);
            };
        });
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                assertEjbThrows(PersistenceException.class, parentDao, "delete", parent);
            }
        });
        // Clean up in a new transaction because the exception above kills
        // the transaction.
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData();
            }
        });
    }

    /**
     * Try loading a parent that only has bidirectional children.  Fetch
     * the collection and try a delete.  We expect the delete to cascade.
     * <p>
     * Note that this test requires multiple sessions.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void deleteParentWithFetchedChildren() throws Exception {
        // We need to span transactions
        setDefaultRollback(false);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parentRows = countRowsInTable(PARENT_TABLE);
                childRows = countRowsInTable(CHILD_TABLE);
                uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
                parent = parentDao.findById(TEST_UNICHILDLESS_ID);
                assertTrue("This test needs child records", parent.getChildren().size()>0);
            }
        });
        // prune and un-prune outside transaction.
        pruner.prune(parent);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                pruner.unprune(parent);
            };
        });
        // delete in new transaction.
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                parentDao.delete(parent);
                assertEquals("Failed to delete parent", 
                        parentRows-1, countRowsInTable(PARENT_TABLE));
                assertTrue("Failed to delete children", 
                        childRows > countRowsInTable(CHILD_TABLE));
                assertEquals("Shouldn't have changed uniChild table", 
                        uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
                // clean up
                deleteData();
            }
        });
    }

    /**
     * Try loading a parent that has unidirectional children.  Fetch
     * the collection and try a delete.  We expect the delete fail because we
     * can't disassociate  the uniChildren from it's parent.  This is a 
     * feature of how Hibernate deals with unidirectional joins.
     * <p>
     * Note that this test requires multiple sessions.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void deleteParentWithFetchedUniChildren() throws Exception {
        // We need to span transactions
        setDefaultRollback(false);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                parent = parentDao.findById(TEST_ID);
                assertTrue("This test needs uniChild records", parent.getUniChildren().size()>0);
            }
        });
        pruner.prune(parent);
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                pruner.unprune(parent);
            };
        });
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                assertEjbThrows(PersistenceException.class, parentDao, "delete", parent);
            }
        });
        // Clean up in a new transaction because the exception above kills
        // the transaction.
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData();
            }
        });
    }

    /**
     * This test was designed to test observed behavior in hibernate.
     * In Hibernate, if 2 children refer to the same parent, the parent
     * will only be fetched once, and the same reference will be in both
     * children.  Whatever code we put in to prevent circular reference
     * issues cannot interfere with multiple children trying to prune
     * the same parent.  We'll test this by fetching more than one child, 
     * then making sure the parents don't have Hibernate data types for
     * their child collections.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void fetchChildrenSingleParent() throws Exception {
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                childList = childDao.findByParentId(TEST_ID);
                // This test doesn't work without at least 2 items.
                assertNotNull("Couldn't load any children", childList);
                assertTrue("Need at least 2 children", childList.size() > 1 );
            }
        });
        for ( TestListChildEntity c : childList ) {
            pruner.prune(c);
            parent = c.getParent();
            // we the parent's children are lazy loaded, so we should not get
            // children
            assertNull("A parent's children should be lazy-loaded, therefore null. Parent NOT pruned",
                    parent.getChildren());
        }
    }

    /**
     * Try fetching a child by it's id.  We want to have an uninitialized
     * proxy for a parent.  Then try pruning.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void fetchChildAndPrune() throws Exception {
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                deleteData(); // in case some other test did a commit.
                createData();
                child = childDao.findById(new BigInteger("-1"));
                // This test doesn't work without at least 2 items.
                assertNotNull("Couldn't load child", child);
                assertEquals("Got wrong child", new BigInteger("-1"), child.getId());
            }
        });
        pruner.prune(child);
        parent = child.getParent();
        // we the parent's children are lazy loaded, so we should not get
        // children
        assertNull("A parent's children should be lazy-loaded, therefore null. Parent NOT pruned",
                parent.getChildren());
    }
}
