package net.saliman.entitypruner.testhelper.junit;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Implementation for a Stateless EJB bean that provides a transactional 
 * context for out of container unit tests, since unit tests can't obtain 
 * transactions directly.  The default behavior of a transaction is for it to
 * always rollback.  If, for some reason, a transaction should commit, use 
 * the {@link #setDefaultRollback(boolean)} method.
 * 
 * @author Steven C. Saliman
 */
@Stateless(name="JpaTransaction")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JpaTransactionEjb extends AbstractJpaTransactionEjb
       implements JpaTransaction {
    @PersistenceContext(unitName="default")
    protected EntityManager em;

    /**
	 * Gets the EntityManager for this instance. 
	 * @return The entity manager in use
	 */
    @Override
	protected EntityManager getEntityManager() {
	    return em;
	}
}
