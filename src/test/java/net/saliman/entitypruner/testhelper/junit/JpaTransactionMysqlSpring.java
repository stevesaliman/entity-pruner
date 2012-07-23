package net.saliman.entitypruner.testhelper.junit;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * This is a little helper class that allows us to use the mysql persistence
 * unit in unit tests.
 * 
 * @author Steven C. Saliman
 */
@Component("JpaTransactionMysql")
@Transactional(propagation=Propagation.NEVER)
public class JpaTransactionMysqlSpring extends AbstractJpaTransactionSpring
                                       implements JpaTransaction {
	
    @PersistenceContext(unitName="mysql")
	private EntityManager em;
	
    /**
	 * Gets the EntityManager for this instance. 
	 * @return The entity manager in use
	 */
    @Override
	public EntityManager getEntityManager() {
	    return em;
	}
}
