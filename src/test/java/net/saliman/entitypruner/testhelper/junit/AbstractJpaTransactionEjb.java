package net.saliman.entitypruner.testhelper.junit;

import net.saliman.entitypruner.testhelper.DatabaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 * Implementation for a Stateless EJB bean that provides a transactional 
 * context for out of container unit tests, since unit tests can't obtain 
 * transactions directly.  The default behavior of a transaction is for it to
 * always rollback.  If, for some reason, a transaction should commit, use 
 * the {@link #setDefaultRollback(boolean)} method.
 * 
 * @author Steven C. Saliman
 */
public abstract class AbstractJpaTransactionEjb implements JpaTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractJpaTransactionEjb.class);
    @Resource
    private SessionContext ejbContext;
    private DatabaseType databaseType = DatabaseType.GENERIC;
    private boolean defaultRollback = true;

    /**
	 * Gets the EntityManager for this instance. This abstract class won't get
	 * an injected one, so require the child implementations to provide one
	 * back to the parent.
	 * @return The entity manager in use
	 */
	protected abstract EntityManager getEntityManager();

	/**
	 * @return the database type in use.
	 */
	@Override
	public DatabaseType getDatabaseType() {
		return databaseType;
	}

	/**
	 * Set the database type for this transaction
	 * @param databaseType the database type to use.
	 */
	@Override
	public void setDatabaseType(DatabaseType databaseType) {
		this.databaseType = databaseType;
	}

	/**
     * @return the defaultRollback
     */
	@Override
    public boolean isDefaultRollback() {
        return defaultRollback;
    }

    /**
     * Sets whether or not transactions should be rolled back after each
     * test.  The default is <code>true</code>, meaning that transactions will
     * rollback after each test.
     * @param defaultRollback the defaultRollback to set
     */
	@Override
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
	@Override
    public int executeUpdate(String sql, Object... args) throws Exception {
        LOG.trace("executeUpdate(String, Object...)");
        Query q = getEntityManager().createNativeQuery(sql);
        LOG.trace("Query created");
        
        if ( args != null && args.length > 0 ) {
            for ( int i = 0; i < args.length; i++ ) {
                // Oracle has a problem with null arguments, so if we want to
                // set a value to null, we'll use an empty string instead, 
                // which Oracle will treat as null.  Don't forget, query 
                // params are 1 based!
                if ( args[i] != null ) {
                    q.setParameter(i+1, args[i]);
                } else {
                	if ( databaseType == DatabaseType.ORACLE ) {
                		q.setParameter(i+1, "");
                	} else {
                		q.setParameter(i+1, null);
                	}
                }
            }
        }
        int retval = -1;
        try { 
            LOG.trace("Executing...");
            retval = q.executeUpdate();
            LOG.trace("Execution complete");
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
    @Override
    public int countRowsInTable(String tableName) {
        Query q = getEntityManager().createNativeQuery("select count(0) from " + tableName); 
        // This is done as a String because Oracle and Mysql can't agree on
        // what data type this should really be.
        String count = q.getSingleResult().toString();
        // It's pretty unlikely that you'd have more than MAXINT rows in 
        // a table.
        return Integer.parseInt(count);
    }
    
    /**
     * Creates a new JPA transaction and Runs the code in the given
     * {@link Transactable}. 
     * @param transactable the code to run
     * @throws Exception whatever exception the Transactable throws. This 
     * method will rollback even checked exceptions.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
   	@Override
    public void runInTransaction(Transactable transactable) throws Exception {
        LOG.trace("runInTransaction(Transactable)");
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
