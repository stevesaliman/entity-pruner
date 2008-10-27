package com.saliman.entitypruner.testhelper;

import java.math.BigInteger;
import java.util.List;

import javax.persistence.Query;

/**
 * This class only exists to test the Framework. It
 * serves as a DAO to a test table that has an Oracle XMLTYPE column. 
 *
 * @author Steven C. Saliman
 */
public class TestChildDaoJpa extends BaseDaoJpa<TestChildEntity>
                                   implements TestChildDao {
    private static final String FIND_BY_PARENT_QUERY = 
        "select c from TestChildEntity c " +
        "where c.parent.id = :parentId";
    
    /**
	 * Default Constructor
	 */
	public TestChildDaoJpa() {
		super();
	}

    public List<TestChildEntity> findByParentId(BigInteger parentId) {
        Query query = entityManager.createQuery(FIND_BY_PARENT_QUERY);
        query.setParameter("parentId", parentId);
        return query.getResultList();
    }
}
