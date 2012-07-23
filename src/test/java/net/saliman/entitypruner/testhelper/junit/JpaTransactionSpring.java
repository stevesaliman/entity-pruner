package net.saliman.entitypruner.testhelper.junit;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation for a Spring bean that provides a transactional context
 * for Out of container unit tests, since unit tests can't obtain transactions 
 * directly.  The default behavior of a transaction is for it to always 
 * rollback.  If, for some reason, a transaction should commit, use the
 * {@link #setDefaultRollback(boolean)} method.
 * 
 * @author Steven C. Saliman
 */
@Component("JpaTransaction")
@Transactional(propagation=Propagation.NEVER)
public class JpaTransactionSpring extends AbstractJpaTransactionSpring
        implements JpaTransaction {
    @PersistenceContext(unitName="default")
    protected EntityManager em;

    /**
	 * Gets the EntityManager for this instance. 
	 * @return The entity manager in use
	 */
    @Override
	public EntityManager getEntityManager() {
	    return em;
	}
}
