package com.saliman.entitypruner.testhelper.junit;

import java.math.BigDecimal;
import java.security.ProviderException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This class serves as an Abstract base class for Unit Tests that need to 
 * test JPA code in a transaction. This class is annotated for execution in
 * a JUnit 4 environment.  It will not work with JUnit 3.
 * <p>
 * This class expects to find a persistence unit named &quot;JUNIT&quot; in
 * the persistence.xml file.  This class will take care of creating the 
 * <code>EntityManager</code>, concrete subclasses can then create an 
 * instance of an application DAO, and inject the entity manager using 
 * {@link #getEntityManager()}.
 * <p>
 * Concrete subclasses can also run setup and teardown SQL with the 
 * {@link #executeUpdate(String, Object...)} method.
 * <p>
 * By default, all transactions will rollback.  Subclasses can change this 
 * behavior with the {@link #setDefaultRollback(boolean)} method.
 * 
 * @author Steven C. Saliman
 */
public class AbstractTransactionalJunit4JpaTest {
    private static final Logger LOG = Logger.getLogger(AbstractTransactionalJunit4JpaTest.class);
    // static for performance.  We only need to get this once per test run.
    private static EntityManagerFactory emf;
    private static EntityManager em;
    private EntityTransaction tx;
    private boolean defaultRollback = true;

    /**
     * @return an <code>EntityManager</code> that can be injected into DAOs
     */
    public EntityManager getEntityManager() {
        return em;
    }

    /**
     * @return the defaultRollback
     */
    public boolean isDefaultRollback() {
        return defaultRollback;
    }

    /**
     * Sets whether or not transactions should be rolled back after each
     * test.  The default is <code>true</code>, meaning that transactions will
     * rollback after each test.
     * @param defaultRollback the defaultRollback to set
     */
    public void setDefaultRollback(boolean defaultRollback) {
        this.defaultRollback = defaultRollback;
    }

    /**
     * Initializes the EntityManager.  This method automatically runs one time
     * per class.  It depends on being able to find a persistence unit named
     * "JUNIT".
     * @throws ProviderException if the EntityManager can't be created.
     */
    @BeforeClass
    public static void initEntityManager() {
        LOG.trace("initEntityManager()");
        try {
            emf = Persistence.createEntityManagerFactory("JUNIT");
            em = emf.createEntityManager();
        } catch (PersistenceException pe) {
            LOG.error("Can't create EntityManager.  Is there a persistence " +
                    "unit named 'JUNIT'?");
            throw new ProviderException("Can't create EntityManager.  " +
            		"Is there a persistence unit named 'JUNIT'?", pe);
        }
    }

    /**
     * This method automatically runs once per class to close the 
     * EntityManager.
     */
    @AfterClass
    public static void closeEntityManager() {
        LOG.trace("closeEntityManager");
        if ( em != null ) {
            em.close();
            emf.close();
        }
    }

    /**
     * This method automatically runs before each test method to create a 
     * new transaction.  JUnit 4 will run this method before any subclass 
     * methods annotated with the <code>Before</code> annotation.
     */
    @Before
    public void initTransaction() {
        LOG.trace("initTransaction()");
        tx = em.getTransaction();
        tx.begin();
    }
    
    /**
     * This method automatically runs once after each test method to close
     * any outstanding transaction. JUnit4 will run this method after any
     * subclass methods annotated with the <code>After</code> annotation.
     */
    @After
    public void closeTransaction() {
        LOG.trace("closeTransaction()");
        if ( tx != null ) {
            if ( defaultRollback ) {
                tx.rollback();
            } else {
                tx.commit();
            }
            tx = null;
        }
    }
    
    /**
     * Convenience method that runs the given native SQL in the test's current
     * transaction.  This method is very handy for setting up the test data
     * a test will operate on.
     * @param sql The native query to run
     * @param args Any parameters to the query.
     * @return the number of rows affected by the SQL.
     */
    public int executeUpdate(String sql, Object... args) {
        Query q = em.createNativeQuery(sql);
        if ( args != null && args.length > 0 ) {
            for ( int i = 0; i < args.length; i++ ) {
                // Oracle has a problem with null arguments, so if we want to
                // set a value to null, we'll use an empty string instead, 
                // which Oracle will treat as null.  Don't forget, query 
                // params are 1 based!
                if ( args[i] != null ) {
                    q.setParameter(i+1, args[i]);
                } else {
                    q.setParameter(i+1, "");
                }
            }
        }
        int retval = -1;
        try { 
            q.executeUpdate(); 
        }
        catch (Exception e) {
            // Let's at least record the error so it doesn't get munched
            // silently if the caller eats the exception.
            LOG.error("Error executing query", e);
        }
        return retval;
    }
    
    /**
     * Convenience method to count the number of rows in a table.
     * @param tableName the name of the table to query
     * @return the number of rows in the given table.
     */
    public int countRowsInTable(String tableName) {
        Query q = em.createNativeQuery("select count(0) from " + tableName); 
        BigDecimal count = (BigDecimal)q.getSingleResult();
        // It's pretty unlikely that you'd have more than MAXINT rows in 
        // a table.
        return count.intValue();
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
