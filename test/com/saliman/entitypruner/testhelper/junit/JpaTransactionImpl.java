package com.saliman.entitypruner.testhelper.junit;

import java.math.BigDecimal;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.log4j.Logger;

/**
 * Implementation for a Stateless bean that provides a transactional context
 * for Out of container unit tests, since unit tests can't obtain transactions 
 * directly.  The default behavior of a transaction is for it to always 
 * rollback.  If, for some reason, a transaction should commit, use the
 * {@link #setDefaultRollback(boolean)} method.
 * 
 * @author Steven C. Saliman
 */
@Stateless(name="JpaTransaction")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JpaTransactionImpl implements JpaTransaction {
    private static final Logger LOG = Logger.getLogger(JpaTransactionImpl.class);
    @PersistenceContext
    private EntityManager em;
    @Resource
    private SessionContext ejbContext;
    private boolean defaultRollback = true;

    /**
     * @return an <code>EntityManager</code> that can be injected into DAOs
     * or used by tests if needed.  The preferred way to configure a DAO is 
     * to simply request a DAO bean from an embedded container.
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
     * Convenience method that runs the given native SQL in the test's current
     * transaction.  This method is very handy for setting up the test data
     * a test will operate on.  This method won't work unless it is inside
     * a {@link Transactable} passed to {@link #runInTransaction(Transactable)}.
     * @param sql The native query to run
     * @param args Any parameters to the query.
     * @return the number of rows affected by the SQL.
     * @throws Exception if anything goes wrong.
     */
    // Join the current transaction, but DO NOT start another one.
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int executeUpdate(String sql, Object... args) throws Exception {
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
            throw e;
        }
        return retval;
    }
    
    /**
     * Convenience method to count the number of rows in a table. This 
     * method won't work unless it is inside a {@link Transactable} passed to 
     * {@link #runInTransaction(Transactable)}.
     * @param tableName the name of the table to query
     * @return the number of rows in the given table.
     */
    // Join the current transaction, but DO NOT start another one.
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
        public int countRowsInTable(String tableName) {
        Query q = em.createNativeQuery("select count(0) from " + tableName); 
        BigDecimal count = (BigDecimal)q.getSingleResult();
        // It's pretty unlikely that you'd have more than MAXINT rows in 
        // a table.
        return count.intValue();
    }
    
    /**
     * Creates a new JPA transaction and Runs the code in the given
     * {@link Transactable}. 
     * @param transactable the code to run
     * @throws Exception whatever exception the Transactable throws. This 
     * method will rollback even checked exceptions.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void runInTransaction(Transactable transactable) throws Exception {
        try { 
            transactable.run();
            // on success, see if we should commit or rollback.
            if ( defaultRollback ) {
                ejbContext.setRollbackOnly();
            }
        } catch (Exception e) {
            // We only need to worry about checked exceptions here - Unchecked
            // Exceptions will roll back automatically, per the EJB spec.
            LOG.error("Error running job", e);
            // if we have an error, rollback.
            ejbContext.setRollbackOnly();
            throw e;
        }
    }
}
