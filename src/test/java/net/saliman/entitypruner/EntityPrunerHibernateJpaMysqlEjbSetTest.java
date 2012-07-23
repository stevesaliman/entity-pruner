package net.saliman.entitypruner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.saliman.entitypruner.testhelper.DatabaseType;
import net.saliman.entitypruner.testhelper.junit.AbstractGlassFishContainerTest;
import net.saliman.entitypruner.testhelper.junit.Transactable;
import net.saliman.entitypruner.testhelper.set.TestSetChildDao;
import net.saliman.entitypruner.testhelper.set.TestSetChildEntity;
import net.saliman.entitypruner.testhelper.set.TestSetParentDao;
import net.saliman.entitypruner.testhelper.set.TestSetParentEntity;
import net.saliman.entitypruner.testhelper.set.TestSetUniChildDao;
import net.saliman.entitypruner.testhelper.set.TestSetUniChildEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This is a test class for the EntityPrunerHibernateJpa class.  This test only
 * tests that a pruned entity looks the way we expect it to.  It doesn't do
 * anything to test that a pruned entity can actually be used, or more 
 * importantly, that it can be unpruned later.  The pruning tests in the DAO
 * package do that.  This test depends on the correct operation of the Test
 * DAOs.  This version of the test uses an EJB container against a MySql 
 * database using Set based entities.
 * 
 * @author Steven C. Saliman
 */
public class EntityPrunerHibernateJpaMysqlEjbSetTest extends AbstractGlassFishContainerTest {
    // These constant names reflect OpenEjb naming conventions. We'll need to
    // change these to reflect embedded GlassFish when we switch.
    private static final String PARENT_DAO_NAME = "TestSetParentDaoMysql";
    private static final String CHILD_DAO_NAME = "TestSetChildDaoMysql";
    private static final String UNI_CHILD_DAO_NAME = "TestSetUniChildDaoMysql";
    private static final String PRUNER_NAME = "EntityPruner";
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
    Map<String, String> options;

    /**
     * Default Constructor, initializes logging.
     */
    public EntityPrunerHibernateJpaMysqlEjbSetTest() {
        super();
	    transactionBeanName = "JpaTransactionMysql";
	    setDatabaseType(DatabaseType.MYSQL);
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
        String sql = "delete from test_child where create_user = '" + USER + "'";
        executeUpdate(sql);
        sql = "delete from test_uni_child where create_user = '" + USER + "'";
        executeUpdate(sql);
        sql = "delete from test_parent where create_user = '" + USER + "'";
        executeUpdate(sql);
    }

    /**
     * Sets up the test by getting what we need from the container.
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
        if ( options != null ) {
        	options.clear();
        	options = null;
        }
    }

    /** 
     * This test tries to make sure we're talking to a MySql database by
     * executing a query on a MySql specific table.  This only really tests
     * that this class is using MySql for its transactions.  The class under
     * test may not, in which case we'll get other errors caused by the setup
     * and the class under test talking to 2 different databases.
     * @throws Exception if anything goes wrong.
     */
    @Test public void verifyMysql() throws Exception {
        runInTransaction(new Transactable() {
            @Override
            public void run() throws Exception {
            	// we don't care about the results, just that the query ran.
                @SuppressWarnings("unused")
				int parentRows = countRowsInTable("information_schema.tables");
            }
        });
    }

    /**
	 * Test fetching all children, then pruning with no args.  We should keep
	 * all children.  This will also test null include lists.
	 * @throws Exception if anything goes badly.
	 */
	@Test
	public void fetchAllPruneUnlimited() throws Exception {
	    runInTransaction(new Transactable() {
            @Override
	        public void run() throws Exception {
	                deleteData(); // in case some other test did a commit.
	                createData();
	                parent = parentDao.findById(TEST_ID);
	                parent.getChildren().size();
	                parent.getUniChildren().size();
	        };
	    });
	    assertNotNull("Test should start with a child set",
	            parent.getChildren());
	    assertNotNull("Test  should start with a uniChild set",
	            parent.getUniChildren());
	    pruner.prune(parent);
	    assertNotNull("pruner should NOT have pruned the child set",
	            parent.getChildren());
	    assertNotNull("pruner should NOT have pruned the uniChild set",
	            parent.getUniChildren());
	    assertEquals("Entity is in wrong pruning state", 
	    		PruningState.PRUNED_COMPLETE, parent.getPruningState());
	}

	/** 
     * Try fetching all children, then pruning to a level of 1. We should 
     * lose all children.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void fetchAllPruneToDepth() throws Exception {
	    runInTransaction(new Transactable() {
            @Override
	        public void run() throws Exception {
	                deleteData(); // in case some other test did a commit.
	                createData();
	                parent = parentDao.findById(TEST_ID);
	                parent.getChildren().size();
	                parent.getUniChildren().size();
	        };
	    });
	    assertNotNull("Test should start with a child set",
	            parent.getChildren());
	    assertNotNull("Test  should start with a uniChild set",
	            parent.getUniChildren());
	    pruner.prune(parent, 1);
	    assertNull("pruner should have pruned the child set",
	            parent.getChildren());
	    assertNull("pruner should have pruned the uniChild set",
	            parent.getUniChildren());
	    assertEquals("Entity is in wrong pruning state", 
	    		PruningState.PRUNED_COMPLETE, parent.getPruningState());
    }

	/** 
     * Try fetching all children, then pruning to a level of 10. We should 
     * lose all children except for the one we requested.  Tests an include
     * list of one.
     * @throws Exception if anything goes badly.
     */
    @Test
    public void fetchAllPruneIncludeChild() throws Exception {
	    runInTransaction(new Transactable() {
            @Override
	        public void run() throws Exception {
	                deleteData(); // in case some other test did a commit.
	                createData();
	                parent = parentDao.findById(TEST_ID);
	                parent.getChildren().size();
	                parent.getUniChildren().size();
	        };
	    });
	    assertNotNull("Test should start with a child set",
	            parent.getChildren());
	    assertNotNull("Test  should start with a uniChild set",
	            parent.getUniChildren());
	    options = new HashMap<String, String>();
	    options.put(Options.INCLUDE, "uniChildren");
	    options.put(Options.DEPTH, "10");
	    pruner.prune(parent, options);
	    assertNull("pruner should have pruned the child set",
	            parent.getChildren());
	    assertNotNull("pruner should NOT have pruned the uniChild set",
	            parent.getUniChildren());
	    assertEquals("Entity is in wrong pruning state", 
	    		PruningState.PRUNED_COMPLETE, parent.getPruningState());
    }

	/** 
     * Try fetching all children, then pruning, including all children. Tests
     * the ability to parse the include list. 
     * @throws Exception if anything goes badly.
     */
    @Test
    public void fetchAllPruneIncludeAll() throws Exception {
	    runInTransaction(new Transactable() {
            @Override
	        public void run() throws Exception {
	                deleteData(); // in case some other test did a commit.
	                createData();
	                parent = parentDao.findById(TEST_ID);
	                parent.getChildren().size();
	                parent.getUniChildren().size();
	        };
	    });
	    assertNotNull("Test should start with a child set",
	            parent.getChildren());
	    assertNotNull("Test  should start with a uniChild set",
	            parent.getUniChildren());
	    options = new HashMap<String, String>();
	    options.put(Options.INCLUDE, "children, uniChildren");
	    options.put(Options.DEPTH, "10");
	    pruner.prune(parent, options);
	    assertNotNull("pruner should NOT have pruned the child set",
	            parent.getChildren());
	    assertNotNull("pruner should NOT have pruned the uniChild set",
	            parent.getUniChildren());
	    assertEquals("Entity is in wrong pruning state", 
	    		PruningState.PRUNED_COMPLETE, parent.getPruningState());
    }

	/** 
     * Try fetching all children, then pruning, including one child and an 
     * invalid attribute. Tests  to make sure an invalid attribute doesn't 
     * prevent the pruner from including the valid ones. 
     * @throws Exception if anything goes badly.
     */
    @Test
    public void fetchAllPruneIncludeOneWithInvalud() throws Exception {
	    runInTransaction(new Transactable() {
            @Override
	        public void run() throws Exception {
	                deleteData(); // in case some other test did a commit.
	                createData();
	                parent = parentDao.findById(TEST_ID);
	                parent.getChildren().size();
	                parent.getUniChildren().size();
	        };
	    });
	    assertNotNull("Test should start with a child set",
	            parent.getChildren());
	    assertNotNull("Test  should start with a uniChild set",
	            parent.getUniChildren());
	    options = new HashMap<String, String>();
	    options.put(Options.INCLUDE, "asdf,children ");
	    options.put(Options.DEPTH, "10");
	    pruner.prune(parent, options);
	    assertNotNull("pruner should NOT have pruned the child set",
	            parent.getChildren());
	    assertNull("pruner should have pruned the uniChild set",
	            parent.getUniChildren());
	    assertEquals("Entity is in wrong pruning state", 
	    		PruningState.PRUNED_COMPLETE, parent.getPruningState());
    }

    /**
	 * Test fetching all children, then pruning with invalid include.  We 
	 * should lose all children.
	 * @throws Exception if anything goes badly.
	 */
	@Test
	public void fetchAllPruneInvalidInclude() throws Exception {
	    runInTransaction(new Transactable() {
            @Override
	        public void run() throws Exception {
	                deleteData(); // in case some other test did a commit.
	                createData();
	                parent = parentDao.findById(TEST_ID);
	                parent.getChildren().size();
	                parent.getUniChildren().size();
	        };
	    });
	    assertNotNull("Test should start with a child set",
	            parent.getChildren());
	    assertNotNull("Test  should start with a uniChild set",
	            parent.getUniChildren());
	    options = new HashMap<String, String>();
	    options.put(Options.INCLUDE, "blahblahblah");
	    options.put(Options.DEPTH, "10");
	    pruner.prune(parent, options);
	    assertNull("pruner should have pruned the child set",
	            parent.getChildren());
	    assertNull("pruner should have pruned the uniChild set",
	            parent.getUniChildren());
	    assertEquals("Entity is in wrong pruning state", 
	    		PruningState.PRUNED_COMPLETE, parent.getPruningState());
	}

    /**
	 * Test fetching no children, then pruning.  We shouldn't have any 
	 * collections.
	 * @throws Exception if anything goes badly.
	 */
	@Test
	public void fetchNonePrune() throws Exception {
	    runInTransaction(new Transactable() {
            @Override
	        public void run() throws Exception {
	                deleteData(); // in case some other test did a commit.
	                createData();
	                parent = parentDao.findById(TEST_ID);
	        };
	    });
	    pruner.prune(parent, 10);
	    assertNull("pruner should have pruned the child set",
	            parent.getChildren());
	    assertNull("pruner should have pruned the uniChild set",
	            parent.getUniChildren());
	    assertEquals("Entity is in wrong pruning state", 
	    		PruningState.PRUNED_COMPLETE, parent.getPruningState());
	}

	/**
	 * Try finding a child, then prune the parent.  This should not do 
	 * do anything.
	 * @throws Exception if anything goes badly.
	 */
	@Test
	public void loadChildPrune() throws Exception {
	    runInTransaction(new Transactable() {
            @Override
	        public void run() throws Exception {
	                deleteData(); // in case some other test did a commit.
	                createData();
	                child = childDao.findById(TEST_ID);
	                parent = child.getParent();
	                parent.getChildren().size();
	                parent.getUniChildren().size();
	        };
	    });
	    assertNotNull("Test should start with a parent", child.getParent());
	    assertNotNull("Test should start with parent having children",
                      parent.getChildren());
	    assertTrue("Test should start with parent's children", 
	    		   parent.getChildren().size() > 0);
	    assertNotNull("Test should start with parent having uniChildren", 
	    		      parent.getUniChildren());
	    assertTrue("Test should start with parent having uniChildren", 
	    		   parent.getUniChildren().size() > 0);
	    pruner.prune(child, 2);
	    assertNotNull("Test should not have pruned parent", child.getParent());
	    assertNull("Test should have pruned parent's children",
                parent.getChildren());
	    assertNull("Test should have pruned parent's uniChildren",
                parent.getUniChildren());
	    assertEquals("Entity is in wrong pruning state", 
	    		PruningState.PRUNED_COMPLETE, child.getPruningState());
	}
	
	/**
	 * Try finding a record, fetching the children, then prune the parent, 
	 * selecting a couple of attributes from the parent, including a bogus one.
	 * Expect only the selected attributes plus both children.  This test also
	 * tests parsing the select string, and ignoring a bogus attribute.
	 * @throws Exception if anything goes badly.
	 */
	@Test
	public void fetchAllPruneSelect() throws Exception {
	    runInTransaction(new Transactable() {
            @Override
	        public void run() throws Exception {
	                deleteData(); // in case some other test did a commit.
	                createData();
	                parent = parentDao.findById(TEST_ID);
	                parent.getChildren().size();
	                parent.getUniChildren().size();
	        };
	    });
	    assertNotNull("Test should start with a child set",
	            parent.getChildren());
	    assertNotNull("Test should start with a uniChild set",
	            parent.getUniChildren());
	    assertNotNull("Test should start with an ID",
	    		parent.getId());
	    assertNotNull("Test should start with a version",
	    		parent.getVersion());
	    assertNotNull("Test should start with a code",
	    		parent.getCode());
	    assertNotNull("Test should start with a description",
	    		parent.getDescription());
	    options = new HashMap<String, String>();
	    options.put(Options.SELECT, "bogus,code,description");
	    options.put(Options.DEPTH, "10");
	    pruner.prune(parent, options);
	    assertNotNull("pruner should NOT have pruned the child set",
	            parent.getChildren());
	    assertNotNull("pruner should NOT have pruned the uniChild set",
	            parent.getUniChildren());
	    assertNull("pruner should have pruned the ID",
	    		parent.getId());
	    assertNull("pruner should have pruned the version",
	    		parent.getVersion());
	    assertNotNull("pruner should NOT have pruned the code",
	    		parent.getCode());
	    assertNotNull("pruner should NOT have pruned the description",
	    		parent.getDescription());
	    assertNotNull("Pruner should NOT have pruned the pruning state",
	    		parent.getPruningState());
	    assertEquals("Entity is in wrong state",
	    		PruningState.PRUNED_PARTIAL, parent.getPruningState());
	}
	
	/**
	 * Try finding a record, fetching the children, then prune the parent, 
	 * selecting an attribute from the parent, and including the child
	 * collection.
	 * @throws Exception if anything goes badly.
	 */
	@Test
	public void fetchAllPruneSelectInclude() throws Exception {
	    runInTransaction(new Transactable() {
            @Override
	        public void run() throws Exception {
	                deleteData(); // in case some other test did a commit.
	                createData();
	                parent = parentDao.findById(TEST_ID);
	                parent.getChildren().size();
	                parent.getUniChildren().size();
	        };
	    });
	    assertNotNull("Test should start with a child set",
	            parent.getChildren());
	    assertNotNull("Test should start with a uniChild set",
	            parent.getUniChildren());
	    assertNotNull("Test should start with an ID",
	    		parent.getId());
	    assertNotNull("Test should start with a version",
	    		parent.getVersion());
	    assertNotNull("Test should start with a code",
	    		parent.getCode());
	    assertNotNull("Test should start with a description",
	    		parent.getDescription());
	    options = new HashMap<String, String>();
	    options.put(Options.SELECT, "code,description");
	    options.put(Options.INCLUDE, "children");
	    pruner.prune(parent, options);
	    assertNotNull("pruner should NOT have pruned the child set",
	            parent.getChildren());
	    assertNull("pruner should have pruned the uniChild set",
	            parent.getUniChildren());
	    assertNull("pruner should have pruned the ID",
	    		parent.getId());
	    assertNull("pruner should have pruned the version",
	    		parent.getVersion());
	    assertNotNull("pruner should NOT have pruned the code",
	    		parent.getCode());
	    assertNotNull("pruner should NOT have pruned the description",
	    		parent.getDescription());
	    assertNotNull("Pruner should NOT have pruned the pruning state",
	    		parent.getPruningState());
	    assertEquals("Entity is in wrong state",
	    		PruningState.PRUNED_PARTIAL, parent.getPruningState());
	}

	/**
	 * Try finding a child, fetching the child, then pruning the parent.  
	 * @throws Exception if anything goes badly.
	 */
	@Test
	public void loadChildParentPruneParent() throws Exception {
	    runInTransaction(new Transactable() {
            @Override
	        public void run() throws Exception {
	                deleteData(); // in case some other test did a commit.
	                createData();
	                child = childDao.findById(TEST_ID);
	                parent.getChildren().size();
	                parent.getUniChildren().size();
	        };
	    });
	    assertNotNull("Test should start with a parent", child.getParent());
	    options = new HashMap<String, String>();
	    options.put(Options.SELECT, "bogus,code,description");
	    options.put(Options.DEPTH, "10");
	    pruner.prune(child, options);
	    assertNull("Test should have pruned parent", child.getParent());
	    assertEquals("Entity is in wrong pruning state",
	    		PruningState.PRUNED_PARTIAL, child.getPruningState());
	}

	/**
	 * Try unpruning an entity that is in a partial state to make sure we get
	 * the correct state.  We aren't worried about anything else here because
	 * the rest of the pruner's functionality is well tested elsewhere.
	 * @throws Exception if anything goes badly.
	 */
	@Test
	public void unprunePartialState() throws Exception {
	    runInTransaction(new Transactable() {
            @Override
	        public void run() throws Exception {
	                deleteData(); // in case some other test did a commit.
	                createData();
	                parent = parentDao.findById(TEST_ID);
	                parent.getChildren().size();
	                parent.getUniChildren().size();
	        };
	    });
	    // Nothing else should matter but the value of the state.
	    parent.setPruningState(PruningState.PRUNED_PARTIAL);
	    runInTransaction(new Transactable() {
            @Override
	        public void run() throws Exception {
	        	pruner.unprune(parent);
	        };
	    });
	    assertEquals("Entity is in wrong pruning state", 
	    		PruningState.UNPRUNED_PARTIAL, parent.getPruningState());
	    runInTransaction(new Transactable() {
            @Override
	        public void run() throws Exception {
	        	deleteData();
	        };
	    });
	}	

	/**
	 * Try unpruning an entity that doesn't have a pruning state.  This can 
	 * happen when clients send partial entities. (or new ones).  We aren't 
	 * worried about anything else here because the rest of the pruner's functionality is well tested elsewhere.
	 * @throws Exception if anything goes badly.
	 */
	@Test
	public void unpruneNullState() throws Exception {
	    runInTransaction(new Transactable() {
            @Override
	        public void run() throws Exception {
	                deleteData(); // in case some other test did a commit.
	                createData();
	                parent = parentDao.findById(TEST_ID);
	                parent.getChildren().size();
	                parent.getUniChildren().size();
	        };
	    });
	    // Nothing else should matter but the value of the state.
	    parent.setPruningState(null);
	    runInTransaction(new Transactable() {
            @Override
	        public void run() throws Exception {
	        	pruner.unprune(parent);
	        };
	    });
	    assertEquals("Entity is in wrong pruning state", 
	    		PruningState.UNPRUNED_PARTIAL, parent.getPruningState());
	    runInTransaction(new Transactable() {
            @Override
	        public void run() throws Exception {
	        	deleteData();
	        };
	    });
	}	
}
