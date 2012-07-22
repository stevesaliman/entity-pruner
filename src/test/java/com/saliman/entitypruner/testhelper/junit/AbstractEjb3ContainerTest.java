package com.saliman.entitypruner.testhelper.junit;

import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This class is a base class that can be used to run a unit test inside an
 * embedded container.  At the moment, that container is the OpenEjb
 * container.
 * <p>
 * To use this test, you'll need to have the OpenEjb jars in the classpath,
 * as well as an openejb.xml file in the classpath.  The openejb.xml file 
 * will define the data source for the embedded EJB container.
 * <p>
 * Unit tests extending this class will need to encapsulate its test code 
 * in a {@link Transactable}, and it will need to know how bean is named in 
 * OpenEjb. They will also have to set up their data differently from most
 * unit tests.  Usually, the setup methods are used to create test data,
 * but in container tests, the test data will need to be set up in the same
 * transaction as the test itself, which means each test will need to set up
 * its own data.  As a practical matter, this probably means having a separate,
 * private, un-annotated setup method to create data, that tests can call from
 * inside their transactions.
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
 * regardless of the state of the defaultRollback property.  Also Keep in mind
 * that unchecked exceptions thrown by a bean get wrapped in an 
 * <code>EJBException</code> before it is received by the caller.
 * 
 * @author Steven C. Saliman
 */
public abstract class AbstractEjb3ContainerTest {
    private static final Logger LOG = Logger.getLogger(AbstractEjb3ContainerTest.class);
    private static InitialContext initialContext;
    private JpaTransaction transaction;

    static {
        // For performance, only need to start the container once per JVM
        LOG.debug("Starting OpenEjb");
        Properties properties = new Properties();
        properties.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.openejb.client.LocalInitialContextFactory");
        URL config = AbstractEjb3ContainerTest.class.getClassLoader().getResource("openejb.xml");
        properties.setProperty("openejb.configuration", config.toExternalForm());
        try {
            initialContext = new InitialContext(properties);
        } catch (NamingException e) {
            // can't throw a checked exception
            throw new IllegalStateException("Can't start OpenEjb", e);
        }
        /// configure logging AFTER starting container.
        //com.pinnacol.framework.Application.configureLogging(-1);
    }

    /**
     * Default constructor
     */
    public AbstractEjb3ContainerTest() {
        
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
     * Gets the named bean from the EJB container.  This method is static 
     * because some tests may want to only get the bean once in a BeforeClass
     * method.
     * @param beanName the name of the bean to get from the container.
     * @return the requested bean.
     * @throws Exception if we can't get the bean.
     */
    protected static Object getBean(String beanName) throws Exception {
        Object bean;
        bean = initialContext.lookup(beanName);
        return bean;
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
        transaction = (JpaTransaction)initialContext.lookup("JpaTransactionLocal");
        assertNotNull("Unable to get JpaTransaction bean.");
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
     * given code in an isolated transaction. Unchecked Exceptions thrown from
     * this method will be wrapped in an <code>EJBException</code> from the 
     * container. 
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
        } catch (Exception e) {
            transaction = null;
            initTransaction();
            throw e;
        }
    }
    
    /**
     * Because this clas ends with the word "Test", Junit will attempt to 
     * run tests against it.  This method makes sure we don't get falures
     * because we don't have any tests.
     */
    @Test
    public void testNothing() {
        LOG.trace("testNothing()");
    }
}
