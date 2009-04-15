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
import java.util.List;
import java.util.Set;

import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;

import org.apache.log4j.PropertyConfigurator;
import org.hibernate.collection.PersistentSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.transaction.annotation.Transactional;

import com.saliman.entitypruner.testhelper.TestChildDao;
import com.saliman.entitypruner.testhelper.TestChildDaoJpa;
import com.saliman.entitypruner.testhelper.TestChildEntity;
import com.saliman.entitypruner.testhelper.TestParentDao;
import com.saliman.entitypruner.testhelper.TestParentDaoJpa;
import com.saliman.entitypruner.testhelper.TestParentEntity;
import com.saliman.entitypruner.testhelper.TestUniChildDao;
import com.saliman.entitypruner.testhelper.TestUniChildDaoJpa;
import com.saliman.entitypruner.testhelper.TestUniChildEntity;

/**
 * This class tests to see how a DAO operates when dealing with pruning and 
 * un-pruning.
 * <p>
 * This class uses the TestParentDaoJpa, TestChildDaoJpa, and
 * TestUniChildDaoJpa helper DAOs to test loading, saving, and deleting 
 * entities after pruning and un-pruning.
 * <p>
 * It isn't generally a good idea to have a unit test that tests 2 things, 
 * but we really can't test the prune/unprune methods of the 
 * EntityPruner class without seeing how they save to the database.
 * <p>
 * From time to time, you may want to see results of this test in the database.
 * To do this, you will need to do 2 things:<br>
 * 1: change the value of the <code>ROLLBACK<code> constant to 
 * <code>false</code><br>
 * 2: Isolate the test you want to see by adding the <code>@Ignore </code> 
 * annotation to the rest of the tests. <b>Do not forget to remove them when
 * you are done!</b><br>
 * If you do choose to commit transactions, you will need to restore the 
 * database to its original state.
 * <p>
 * This class won't always reflect reality, since it runs outside a container.
 * EJB containers handle transactions a little differently.
 * 
 * @author Steven C. Saliman
 */
@ContextConfiguration(locations={"classpath:applicationContext-test.xml"})
@Transactional
public class EntityPrunerTest
       extends AbstractTransactionalJUnit4SpringContextTests  {
    /** the class variable used for logging */
    private static final String PARENT_DAO_NAME = "testParentDaoJpa";
    private static final String CHILD_DAO_NAME = "testChildDaoJpa";
    private static final String UNI_CHILD_DAO_NAME = "testUniChildDaoJpa";
    private static final String PRUNER_NAME = "entityPruner";
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
    
    
    private TestParentDao parentDao;
    private TestChildDao childDao;
    private TestUniChildDao uniChildDao;
    private EntityPruner pruner;
    private TestParentEntity parent;
    private Set<TestChildEntity> children;
    private Set<TestUniChildEntity> uniChildren;

    /**
     * Default Constructor, initializes logging.
     */
    public EntityPrunerTest() {
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
     * Private helper method to purge all the DAOs of any managed entities.
     * This simulates multiple transactions by forcing each subsequent DAO
     * call to re-associate given entities.
     */
    private void purgeDaos() {
        if ( parentDao != null ) {
            ((TestParentDaoJpa)parentDao).getEntityManager().clear();
        }
        if ( childDao != null ) {
            ((TestChildDaoJpa)childDao).getEntityManager().clear();
        }
        if ( uniChildDao != null ) {
            ((TestUniChildDaoJpa)uniChildDao).getEntityManager().clear();
        }
    }
    
    /**
     * Lists up the test by getting what we need from Spring.
     * @throws Exception 
     */
    @Before
    public void setUp() throws Exception {
        deleteData(); // in case some other test did a commit.
        createData();
        parentDao = (TestParentDao)applicationContext.getBean(PARENT_DAO_NAME);
        assertNotNull("Unable to get TestParentDao", parentDao);
        childDao = (TestChildDao)applicationContext.getBean(CHILD_DAO_NAME);
        assertNotNull("Unable to get TestChildDao", childDao);
        uniChildDao = (TestUniChildDao)applicationContext.getBean(UNI_CHILD_DAO_NAME);
        assertNotNull("Unable to get TestUniChildDao", uniChildDao);
        pruner = (EntityPruner)applicationContext.getBean(PRUNER_NAME);
        assertNotNull("Unable to get EntityPruner", pruner);
        parent = new TestParentEntity();
        children = new HashSet<TestChildEntity>(2);
        uniChildren = new HashSet<TestUniChildEntity>(2);
        parent.setCode("JPARENT1");
        parent.setDescription("Parent 1");
        parent.setAffirmative(Boolean.TRUE); // can't be null;
        parent.setCreateUser(USER);
        parent.setUpdateUser(USER);
        TestChildEntity child = new TestChildEntity();
        child.setParent(parent);
        child.setCode("JCHILD1");
        child.setDescription("Child 1");
        child.setCreateUser(USER);
        child.setUpdateUser(USER);
        children.add(child);
        parent.setChildren(children);
        TestUniChildEntity uniChild = new TestUniChildEntity();
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
        pruner = null;
    }

    /** 
     * Try the following sequence of events:
     * Create a new parent object with null children and uni-children.
     * Un-prune the object.  Do we get an empty PersistentList?
     * Save the object.  It should succeed. This simulates a new record 
     * coming in from a Web Service.
     */
    @Test
    public void insertUnprunedParentNullChildren() {
        int parentRows = countRowsInTable(PARENT_TABLE);
        int childRows = countRowsInTable(CHILD_TABLE);
        int uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
        parent.setChildren(null);
        parent.setUniChildren(null);
        parent.setPruned(true);
        pruner.unprune(parent);
        assertNull("unpruning should not have created a child set",
                      parent.getChildren());
        assertNull("unpruning should not have created a uniChild set",
                   parent.getUniChildren());
        assertNull("unpruning should not have created a transChild set",
                   parent.getTransChildren());
        parent = parentDao.save(parent);
        assertEquals("Failed to insert parent", 
                     parentRows+1, countRowsInTable(PARENT_TABLE));
        assertEquals("Shouldn't have changed children", 
                     childRows, countRowsInTable(CHILD_TABLE));
        assertEquals("Shouldn't have changed the uniChild table.",
                     uniChildRows,
                     countRowsInTable(UNI_CHILD_TABLE));
    }
    
    /**
     * Try inserting a new parent with new bidirectional children after
     * un-pruning.  The save should succeed.
     */
    @Test
    public void insertUnprunedParentAndChildren() {
        int parentRows = countRowsInTable(PARENT_TABLE);
        int childRows = countRowsInTable(CHILD_TABLE);
        int uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
        parent.setUniChildren(null);
        parent.setPruned(true);
        pruner.unprune(parent);
        assertNotNull("unpruning should have kept the a child set",
                      parent.getChildren());
        assertNull("unpruning should not have created a uniChild set",
                   parent.getUniChildren());
        assertNull("unpruning should not have created a transChild set",
                   parent.getTransChildren());
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

    /** 
     * Try inserting a new parent with new unidirectional children after
     * re-hydrating.  This should fail because Hibernate won't resolve the ID
     * in the child records, but the unpruning should work.
     * <p>
     * The actual exception is misleading, but as we discovered in the 
     * BaseDaoJpaTest, this is just something JPA does.
     */
    @Test
    public void insertUnprunedParentAndUniChildren() {
        parent.setChildren(null);
        parent.setPruned(true);
        pruner.unprune(parent);
        assertNull("unpruning should not have created  a child set",
                      parent.getChildren());
        assertNotNull("unpruning should have kept a uniChild set",
                   parent.getUniChildren());
        assertNull("unpruning should not have created a transChild set",
                   parent.getTransChildren());
        try {
            parentDao.save(parent);
            fail("Should have thrown an EntityExistsException");
        } catch (EntityExistsException e) {
            // this is expected.
        }
    }
    
    /** 
     * Try inserting a new parent with new transient children after 
     * un-pruning. This should have no problems.
     */
    @Test
    public void insertRehydratedParentAndTransChildren() {
        int parentRows = countRowsInTable(PARENT_TABLE);
        int childRows = countRowsInTable(CHILD_TABLE);
        int uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
        // put the children in the transient collection.
        parent.setTransChildren(parent.getChildren());
        parent.setChildren(null);
        parent.setUniChildren(null);
        parent.setPruned(true);
        pruner.unprune(parent);
        assertNull("unpruning should not have created the a child set",
                      parent.getChildren());
        assertNull("unpruning should not have created a uniChild set",
                   parent.getUniChildren());
        assertNotNull("unpruning should have kept a transChild set",
                   parent.getTransChildren());
        parent = parentDao.save(parent);
        assertEquals("Failed to insert parent", 
                     parentRows+1, countRowsInTable(PARENT_TABLE));
        assertEquals("Shouldn't have changed children", 
                     childRows, countRowsInTable(CHILD_TABLE));
        assertEquals("Shouldn't have changed the uniChild table.",
                     uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
    }

    /**
     * Try fetching a parent with lazy children (both-types).  Add Transient
     * children and prune the object. Make sure the collections come back
     * null. Make a change to the parent and make sure we can still save the
     * un-pruned object.  This tests the savability of pruned/un-pruned
     * objects, and also tests the collection types of children.
     */
    @Test
    public void updateParentWithoutFetchingChildren() {
        // Get children BEFORE fetching the object. Then fetch the parent
        // then set the transient children in the parent. Mess this up
        // And you'll wind up fetching the children from the DB
        Set<TestChildEntity> transChildren = new HashSet<TestChildEntity>(parent.getChildren().size());
        transChildren.addAll(parent.getChildren());
        parent = parentDao.findById(TEST_ID);
        parent.setTransChildren(transChildren);
        int parentRows = countRowsInTable(PARENT_TABLE);
        int childRows = countRowsInTable(CHILD_TABLE);
        int uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
        int numTransChild = parent.getTransChildren().size();
        purgeDaos();
        pruner.prune(parent);
        assertNull("Prune should have nulled out the unitialized child set",
                   parent.getChildren());
        assertNull("Prune should have nulled out the unitialized uniChild set",
                   parent.getUniChildren());
        assertNotNull("Prune should not have nulled out the transChildren",
                      parent.getTransChildren());
        parent.setDescription("Updated by updateParentWithoutFetchingChildren");
        pruner.unprune(parent);
        // This time, our null sets should be replaced with PersistentLists.
        Set<TestChildEntity> childList = parent.getChildren();
        assertNotNull("Unprune should have created a PersistentSet for children",
                      childList);
        assertTrue("The child set is the wrong type",
                PersistentSet.class.isAssignableFrom(childList.getClass()));
        Set<TestUniChildEntity> uniChildList = parent.getUniChildren();
        assertNotNull("Unprune should have created a PersistentSet for uniChildren",
                      uniChildList);
        assertTrue("The uniChild set is the wrong type",
                   PersistentSet.class.isAssignableFrom(uniChildList.getClass()));
        // I'd love to get a count here, but that would create bad side effects.
        parent = parentDao.save(parent);
        assertEquals("Shouldn't have changed parent count", 
                     parentRows, countRowsInTable(PARENT_TABLE));
        assertEquals("Shouldn't have changed child count", 
                     childRows, countRowsInTable(CHILD_TABLE));
        assertEquals("Shouldn't have changed uniChild count", 
                uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
        assertEquals("Shouldn't have changed the transient child count",
                     numTransChild, parent.getTransChildren().size());
    }

    /**
     * Try fetching a parent with lazy children (both-types), and fetch the
     * children. Add Transient children and Prune the object. Make sure
     * the collections come back non-null. Make a change to the parent and make
     * sure we can still save the un-pruned object.  This tests the savability 
     * of pruned/un-pruned objects, and also tests the collection types of 
     * children. This test also makes a change to a child record.
     */
    @Test
    public void updateParentFetchingChildren() {
        parent = parentDao.findById(TEST_ID);
        assertTrue("Failed to fetch Children", parent.getChildren().size() > 0);
        assertTrue("Failed to fetch uniChildren", parent.getUniChildren().size() > 0);
        Set<TestChildEntity> transChildren = new HashSet<TestChildEntity>(parent.getChildren().size());
        transChildren.addAll(parent.getChildren());
        parent.setTransChildren(transChildren);
        int parentRows = countRowsInTable(PARENT_TABLE);
        int childRows = countRowsInTable(CHILD_TABLE);
        int uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
        int numTransChild = parent.getTransChildren().size();
        purgeDaos();
        pruner.prune(parent);
        Set<TestChildEntity> childList = parent.getChildren();
        assertNotNull("Prune should not null fetched the children set",
                      childList);
        assertFalse("The Pruned child set shouldn't be a PersistentSet",
                PersistentSet.class.isAssignableFrom(childList.getClass()));
        Set<TestUniChildEntity> uniChildList = parent.getUniChildren();
        assertNotNull("Prune should not null fetched the uniChildren set",
                      uniChildList);
        assertFalse("The pruned uniChild set shouldn't be a PersistentSet",
                PersistentSet.class.isAssignableFrom(uniChildList.getClass()));
        String newDesc = "Updated by updateParentFetchingChildren";
        parent.setDescription(newDesc);
        for ( TestChildEntity c : parent.getChildren() ) {
            c.setDescription(newDesc);
        }
        for ( TestUniChildEntity u : parent.getUniChildren() ) {
            u.setDescription(newDesc);
        }
        pruner.unprune(parent);
        // This time, our null sets should be replaced with PersistentSets.
        childList = parent.getChildren();
        assertNotNull("Unprune should have created a List for children",
                childList);
        uniChildList = parent.getUniChildren();
        assertNotNull("Unprune should have created a List for uniChildren",
                uniChildList);
        parent = parentDao.save(parent);
        assertEquals("Shouldn't have changed parent count", 
                parentRows, countRowsInTable(PARENT_TABLE));
        assertEquals("Shouldn't have changed child count", 
                childRows, countRowsInTable(CHILD_TABLE));
        assertEquals("Shouldn't have changed uniChild count", 
                uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
        assertEquals("Shouldn't have changed the transient child count",
                numTransChild, parent.getTransChildren().size());
    }
    
    /**
     * Try loading a parent with no children.  Access the collections then 
     * prune.  Do we get an empty collection? This should not return null
     * because we actually tried to do a fetch on the children. Try on both 
     * collections.
     */
    @Test
    public void pruneChildlessParent() {
        parent = parentDao.findById(TEST_CHILDLESS_ID);
        assertTrue("Shouldn't have Children", parent.getChildren().size() == 0);
        assertTrue("Shouldn't have uniChildren", parent.getUniChildren().size() ==0);
        purgeDaos();
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
     */
    @Test
    public void insertUnprunedChild() {
        int parentRows = countRowsInTable(PARENT_TABLE);
        int childRows = countRowsInTable(CHILD_TABLE);
        int uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
        parent = parentDao.findById(TEST_ID);
        purgeDaos();
        pruner.prune(parent); // simulate giving to client
        TestChildEntity child = new TestChildEntity();
        child.setParent(parent);
        child.setCode("TESTINS");
        child.setDescription("Inserted by insertUnprunedChild");
        child.setCreateUser(USER);
        child.setUpdateUser(USER);
        pruner.unprune(child);
        child = childDao.save(child);
        assertNotNull("Child should now have an ID", child.getId());
        assertEquals("Shouldn't have changed parent table", 
                     parentRows, countRowsInTable(PARENT_TABLE));
        assertEquals("Failed to insert child", 
                childRows+1, countRowsInTable(CHILD_TABLE));
        assertEquals("Shouldn't have changed uniChild table", 
                uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
    }

    /**
     * Try creating a new child pointing to a new parent and un-prune the 
     * child.  We expect the save to fail because Hibernate wants the parent
     * to be saved first, but the un-prune should work. 
     */
    @Test
    public void insertUnprunedChildNewParent() {
        parent.setId(null);
        TestChildEntity child = new TestChildEntity();
        child.setParent(parent);
        child.setCode("TESTINS");
        child.setDescription("Inserted by insertUnprunedChildNewParent");
        child.setCreateUser(USER);
        child.setUpdateUser(USER);
        pruner.unprune(child);
        try {
            childDao.save(child);
            fail("Should have thrown an IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }
    
    /**
     * Try adding a child to a parent while it's pruned, and saving it
     * when it is un-pruned. For this test the child points to its parent.
     */
    @Test
    public void addChildPointingToParent() {
        int parentRows = countRowsInTable(PARENT_TABLE);
        int childRows = countRowsInTable(CHILD_TABLE);
        int uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
        parent = parentDao.findById(TEST_ID);
        assertTrue("Need a parent that has children", parent.getChildren().size()>0);
        purgeDaos();
        pruner.prune(parent);
        TestChildEntity child = new TestChildEntity();
        child.setParent(parent);
        child.setCode("TESTINS");
        child.setDescription("Inserted by addChildPointingToParent");
        child.setCreateUser(USER);
        child.setUpdateUser(USER);
        parent.getChildren().add(child);
        pruner.unprune(parent);
        parent = parentDao.save(parent);
        assertEquals("Shouldn't have changed parent table", 
                parentRows, countRowsInTable(PARENT_TABLE));
        assertEquals("Failed to insert child", 
                childRows+1, countRowsInTable(CHILD_TABLE));
        assertEquals("Shouldn't have changed uniChild table", 
                uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
    }
    
    /**
     * Try adding a child to a parent while it's pruned, and saving it
     * when it is un-pruned. For this test the child does not point to its
     * parent, but un-pruned the parent should fix that.  If it doesn't,
     * we'll know because the child won't save with a null parent.
     */
    @Test
    public void addChildNotPointingToParent() {
        int parentRows = countRowsInTable(PARENT_TABLE);
        int childRows = countRowsInTable(CHILD_TABLE);
        int uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
        parent = parentDao.findById(TEST_ID);
        assertTrue("Need a parent that has children", parent.getChildren().size()>0);
        purgeDaos();
        pruner.prune(parent);
        TestChildEntity child = new TestChildEntity();
        child.setCode("TESTINS");
        child.setDescription("Inserted by addChildNotPointingToParent");
        child.setCreateUser(USER);
        child.setUpdateUser(USER);
        parent.getChildren().add(child);
        pruner.unprune(parent);
        parent = parentDao.save(parent);
        assertEquals("Shouldn't have changed parent table", 
                parentRows, countRowsInTable(PARENT_TABLE));
        assertEquals("Failed to insert child", 
                childRows+1, countRowsInTable(CHILD_TABLE));
        assertEquals("Shouldn't have changed uniChild table", 
                uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
    }
    
    /**
     * Try adding a uniChild to a parent while it's pruned, and saving it
     * when it is un-pruned. For this test the child points to its parent.
     */
    @Test
    public void addUniChildPointingToParent() {
        int parentRows = countRowsInTable(PARENT_TABLE);
        int childRows = countRowsInTable(CHILD_TABLE);
        int uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
        parent = parentDao.findById(TEST_ID);
        assertTrue("Need a parent that has uniChildren", parent.getUniChildren().size()>0);
        purgeDaos();
        pruner.prune(parent);
        TestUniChildEntity child = new TestUniChildEntity();
        child.setParentId(parent.getId());
        child.setCode("TESTINS");
        child.setDescription("Inserted by addUniChildPointingToParent");
        child.setCreateUser(USER);
        child.setUpdateUser(USER);
        parent.getUniChildren().add(child);
        pruner.unprune(parent);
        parent = parentDao.save(parent);
        assertEquals("Shouldn't have changed parent table", 
                parentRows, countRowsInTable(PARENT_TABLE));
        assertEquals("Shouldn't have changed child table", 
                childRows, countRowsInTable(CHILD_TABLE));
        assertEquals("Failed to insert uniChild", 
                uniChildRows+1, countRowsInTable(UNI_CHILD_TABLE));
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
     */
    @Test
    public void addUniChildNotPointingToParent() {
        parent = parentDao.findById(TEST_ID);
        assertTrue("Need a parent that has uniChildren", parent.getUniChildren().size()>0);
        purgeDaos();
        pruner.prune(parent);
        TestUniChildEntity child = new TestUniChildEntity();
        child.setCode("TESTINS");
        child.setDescription("Inserted by addChildNotPointingToParent");
        child.setCreateUser(USER);
        child.setUpdateUser(USER);
        parent.getUniChildren().add(child);
        pruner.unprune(parent);
        try {
            parentDao.save(parent);
            fail("Should have thrown EntityExistsException");
        } catch (EntityExistsException e) {
            // expected.
        }
    }
    
    /**
     * Try adding a child to a parent that never fetched children.  We expect
     * hibernate to keep this sane by only inserting the new item without
     * deleting the old ones.  This only works without the delete-orphan option,
     * which is one of the reasons we don't use that option.<br>
     * If this works, there really isn't a need to test the other collections.
     */
    @Test
    public void addChildPointingToParentUnfetched() {
        int parentRows = countRowsInTable(PARENT_TABLE);
        int childRows = countRowsInTable(CHILD_TABLE);
        int uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
        parent = parentDao.findById(TEST_ID);
        purgeDaos();
        pruner.prune(parent);
        assertNull("We should have a null child set in the pruned parent",
                   parent.getChildren());
        TestChildEntity child = new TestChildEntity();
        child.setParent(parent);
        child.setCode("TESTINS");
        child.setDescription("Inserted by addChildPointingToParent");
        child.setCreateUser(USER);
        child.setUpdateUser(USER);
        children = new HashSet<TestChildEntity>();
        children.add(child);
        parent.setChildren(children);
        pruner.unprune(parent);
        parent = parentDao.save(parent);
        assertEquals("Shouldn't have changed parent table", 
                parentRows, countRowsInTable(PARENT_TABLE));
        assertEquals("Failed to insert child", 
                childRows+1, countRowsInTable(CHILD_TABLE));
        assertEquals("Shouldn't have changed uniChild table", 
                uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
    }

    /**
     * Try loading a parent that only has bidirectional children.  Don't fetch
     * the collection and try a delete. 
     * <p>
     * Note that this test requires multiple sessions.
     */
    @Test
    public void deleteParentWithUnfetchedChildren() {
        int parentRows = countRowsInTable(PARENT_TABLE);
        int childRows = countRowsInTable(CHILD_TABLE);
        int uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
        parent = parentDao.findById(TEST_UNICHILDLESS_ID);
        purgeDaos();
        pruner.prune(parent);
        pruner.unprune(parent);
        parentDao.delete(parent);
        assertEquals("Failed to delete parent", 
                parentRows-1, countRowsInTable(PARENT_TABLE));
        assertTrue("Failed to delete children", 
                childRows > countRowsInTable(CHILD_TABLE));
        assertEquals("Shouldn't have changed uniChild table", 
                uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
    }
    
    /**
     * Try loading a parent that has unidirectional children.  Don't fetch
     * the collection and try a delete.  We expect the delete fail because we
     * can't disassociate  the uniChildren from it's parent.  This is a 
     * feature of how Hibernate deals with unidirectional joins.
     * <p>
     * Note that this test requires multiple sessions.
     */
    @Test
    public void deleteParentWithUnfetchedUniChildren() {
        parent = parentDao.findById(TEST_ID);
        purgeDaos();
        pruner.prune(parent);
        pruner.unprune(parent);
        try {
            parentDao.delete(parent);
            fail("should have thrown PersistenceException");
        } catch(PersistenceException e) {
            // expected.
        }
    }
    
    /**
     * Try loading a parent that only has bidirectional children.  Fetch
     * the collection and try a delete.  We expect the delete to cascade.
     * <p>
     * Note that this test requires multiple sessions.
     */
    //@Ignore("Still working on session issues.")
    @Test
    public void deleteParentWithFetchedChildren() {
        int parentRows = countRowsInTable(PARENT_TABLE);
        int childRows = countRowsInTable(CHILD_TABLE);
        int uniChildRows = countRowsInTable(UNI_CHILD_TABLE);
        parent = parentDao.findById(TEST_UNICHILDLESS_ID);
        assertTrue("This test needs child records", parent.getChildren().size()>0);
        // Isolate sessions AFTER loading children.
        //isolateSessions((TestParentDaoJpa)parentDao, true);
        //((TestParentDaoJpa)parentDao).getHibernateTemplate().evict(parent);
       // ((TestParentDaoJpa)parentDao).getEntityManager().clear();
        purgeDaos();
        pruner.prune(parent);
        pruner.unprune(parent);
        parentDao.delete(parent);
        assertEquals("Failed to delete parent", 
                parentRows-1, countRowsInTable(PARENT_TABLE));
        assertTrue("Failed to delete children", 
                childRows > countRowsInTable(CHILD_TABLE));
        assertEquals("Shouldn't have changed uniChild table", 
                uniChildRows, countRowsInTable(UNI_CHILD_TABLE));
    }
    
    /**
     * Try loading a parent that has unidirectional children.  Fetch
     * the collection and try a delete.  We expect the delete fail because we
     * can't disassociate  the uniChildren from it's parent.  This is a 
     * feature of how Hibernate deals with unidirectional joins.
     * <p>
     * Note that this test requires multiple sessions.
     */
    @Test
    public void deleteParentWithFetchedUniChildren() {
        parent = parentDao.findById(TEST_ID);
        assertTrue("This test needs uniChild records", parent.getUniChildren().size()>0);
        purgeDaos();
        pruner.prune(parent);
        pruner.unprune(parent);
        try {
            parentDao.delete(parent);
            fail("Should have thrown PersistenceException");
        } catch (PersistenceException e) {
            // expected.
        }
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
     */
    @Test
    public void fetchChildrenSingleParent() {
        List<TestChildEntity> childList = childDao.findByParentId(TEST_ID);
        // This test doesn't work without at least 2 items.
        assertNotNull("Couldn't load any children", childList);
        assertTrue("Need at least 2 children", childList.size() > 1 );
        purgeDaos();
        for ( TestChildEntity child : childList ) {
            pruner.prune(child);
            parent = child.getParent();
            // we the parent's children are lazy loaded, so we should not get
            // and children
            assertNull("A parent's children should be lazy-loaded, therefore null. Parent NOT pruned",
                        parent.getChildren());
        }
    }
}
