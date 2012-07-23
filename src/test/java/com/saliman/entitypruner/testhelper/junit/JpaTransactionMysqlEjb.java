package com.saliman.entitypruner.testhelper.junit;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * This is a little helper class that allows us to use the mysql persistence
 * unit in unit tests.
 * 
 * @author Steven C. Saliman
 */
@Stateless(name="JpaTransactionMysql")
public class JpaTransactionMysqlEjb extends AbstractJpaTransactionEjb 
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
