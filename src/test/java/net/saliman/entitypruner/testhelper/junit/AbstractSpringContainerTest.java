package net.saliman.entitypruner.testhelper.junit;

import net.saliman.entitypruner.testhelper.DatabaseType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Date;

import static org.junit.Assert.assertNotNull;

/**
 * This class is a base class that can be used to run a unit test inside a
 * Spring 3 container.
 * <p>
 * To use this class, there are a few beans that must be defined in an XML
 * file, which must be called <code>applicationContext-test.xml</code>, and
 * on the classpath.  There must be a treansaction manager, named 
 * <code>transactionManager</code>, which will probably need data source and 
 * entity manager factory beans.
 * <p>
 * Unit tests extending this class will need to encapsulate its transactional
 * test code in a {@link Transactable}, and it will need to know how the bean
 * is named in Spring. Unit tests will also have to set up their data 
 * differently from most unit tests. Usually, the setup methods are used to 
 * create test data, but in container tests, the test data will need to be 
 * set up in the same transaction as the test itself, which means each test 
 * will need to set up its own data.  As a practical matter, this probably 
 * means having a separate, private,  un-annotated setup method to create 
 * data, that tests can call from inside their transactions.
 * <p>
 * This class also contains convenience methods to count rows in a table and
 * execute SQL to set up tests.  Calls to these methods also need to be in 
 * a unit test's {@link Transactable} so they get run in the same transaction
 * as the test itself. Your tests will look something like this:<br>
 * <pre>
    @Test 
    public void myTest() throws Exception {
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                // All test code goes here, including data setup, tests, and
                // asserts.
            }
        });
    }
 * </pre><p>
 * By default, all transactions will roll back when they are finished so that
 * tests have no lasting impact on the database.  If, for some reason, you
 * want to see the effects of the test in the database after the test is
 * finished, you can call <code>setDefaultRollback(false)</code> to cause 
 * transactions to commit.  Doing this will only impact the current test 
 * method, since default rollback is reset to <code>false</code> before each
 * test.  An exception thrown by the test method will force a rollback 
 * regardless of the state of the defaultRollback property.
 * <p>
 * The most expensive part of an in-container test is the container startup 
 * and application deployment.  To help with this, the container startup
 * happens once per JVM.  What this means to the developer is that "forkmode"
 * must be set in the build.xml if ant is forking tests in another JVM.  
 * Jvmargs should also be set to make sure that the forked JVMs have enough
 * memory and PermGenSpace to run all the tests.
 * @author Steven C. Saliman
 */
public abstract class AbstractSpringContainerTest {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSpringContainerTest.class);
    private static ApplicationContext applicationContext;
    protected String transactionBeanName = "JpaTransaction";
    private JpaTransaction transaction;
    private DatabaseType databaseType = DatabaseType.GENERIC;

    static {
        // For performance, only need to start the container once per JVM
    	// Logging here is done with writes to System.output because
    	// we probably haven't initialized logging yet.
        System.out.println("Attempting to Start Spring at " + new Date());
    	applicationContext = new ClassPathXmlApplicationContext("applicationContext-test.xml");
    	System.out.println("Spring started");
        /// configure logging AFTER starting container.
    }

    /**
     * Default constructor
     */
    public AbstractSpringContainerTest() {
        
    }
    
    /**
     * @return the defaultRollback
     */
    protected boolean isDefaultRollback() {
        return transaction.isDefaultRollback();
    }

    /**
     * Sets whether or not transactions should be rolled back after each
     * test.  The default is <code>true</code>, meaning that transactions will
     * rollback after each test.
     * @param defaultRollback the defaultRollback to set
     */
    protected void setDefaultRollback(boolean defaultRollback) {
        transaction.setDefaultRollback(defaultRollback);
    }
    
    /**
     * Gets the named bean from Spring.  This method is static because some 
     * tests may want to only get the bean once in a BeforeClass method.
     * @param beanName the name of the bean to get from the container.
     * @return the requested bean.
     * @throws Exception if there is a problem getting the bean.
     */
    protected static Object getBean(String beanName) throws Exception {
        Object bean;
        bean = applicationContext.getBean(beanName);
        return bean;
    }
    
    /**
     * Set the database type for this test
     * @param databaseType the DatabaseType for this test.
     */
    public void setDatabaseType(DatabaseType databaseType) {
    	this.databaseType = databaseType;
    }

    /**
     * Initialize each test by getting a transaction bean from the container,
     * and making sure we're set to rollback.
     * <p> 
     * We always set rollback to false because individual tests may set it
     * to true, and we don't want one test to affect another test.
     * @throws Exception if we can't get the transaction bean
     */
    @Before
    public void initTransaction() throws Exception {
        transaction = (JpaTransaction)applicationContext.getBean(transactionBeanName);
        assertNotNull("Unable to get JpaTransaction bean.");
        transaction.setDatabaseType(databaseType);
        setDefaultRollback(true);
    }
    
    /**
     * Clean up from each test by releasing the Transaction Bean.
     */
    @After
    public void closeTransaction() {
        if ( transaction != null ) {
            transaction = null;
        }
    }
    
    /**
     * Convenience method that runs the given native SQL in the test's current
     * transaction.  This method is very handy for setting up the test data
     * a test will operate on.  This method won't work unless it is inside
     * a {@link Transactable} passed to {@link #runInTransaction(Transactable)}.
     * <p>
     * This test is just a pass through to 
     * {@link JpaTransaction#executeUpdate(String, Object...)}.  It is here 
     * because extending classes won't have access to the transaction bean.
     * @param sql The native query to run
     * @param args Any parameters to the query.
     * @return the number of rows affected by the SQL.
     * @throws Exception if anything goes wrong.
     */
    public int executeUpdate(String sql, Object... args) throws Exception {
        LOG.trace("executeUpdate(String Object...)");
        return transaction.executeUpdate(sql, args);
    }

    /**
     * Convenience method to count the number of rows in a table. This 
     * method won't work unless it is inside a {@link Transactable} passed to 
     * {@link #runInTransaction(Transactable)}.
     * <p>
     * This test is just a pass through to 
     * {@link JpaTransaction#countRowsInTable(String)}.  It is here 
     * because extending classes won't have access to the transaction bean.
     * @param tableName the name of the table to query
     * @return the number of rows in the given table.
     */
    public int countRowsInTable(String tableName) {
        return transaction.countRowsInTable(tableName);
    }

    /**
     * Gets a transaction bean from the container and uses it to run the
     * given code in an isolated transaction.
     * <p>
     * Be very careful not to eat exceptions inside the runnable.  This can
     * cause side effects if default rollback is set to false.
     * @param transactable the code to run.
     * @throws Exception if there is a problem, or if the test code throws
     * an exception, which may be what the test is looking for.
     */
    public void runInTransaction(Transactable transactable) throws Exception {
        LOG.trace("runInTransaction(Transactable)");
        try {
        	transaction.runInTransaction(transactable);
        } catch (RollbackWarning re) {
        	// Rember, the underlying JpaTransaction throws a RollbackWarning
        	// to signal to Spring that a rollback is needed.  This doesn't 
        	// mean we had an error, but we need to check.
        	if ( re.getCause() != null ) {
        		transaction = null;
        		initTransaction();
        		throw re.getCause();
        	}
        } catch (Exception e) {
            transaction = null;
            initTransaction();
            throw e;
        }
    }
    
    /**
	 * Because this class ends with the word "Test", Junit will attempt to 
	 * run tests against it.  This method makes sure we don't get falures
	 * because we don't have any tests.
	 */
	@Test
	public void testNothing() {
	    LOG.trace("testNothing()");
	}
}

