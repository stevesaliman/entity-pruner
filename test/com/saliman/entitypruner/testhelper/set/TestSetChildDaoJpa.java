package com.saliman.entitypruner.testhelper.set;

import java.math.BigInteger;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.Query;

import com.saliman.entitypruner.testhelper.BaseDaoJpa;

/**
 * This class only exists to test the Framework. It
 * serves as a DAO to a test table that has an Oracle XMLTYPE column. 
 *
 * @author Steven C. Saliman
 */
@Stateless(name="TestSetChildDao")
public class TestSetChildDaoJpa extends BaseDaoJpa<TestSetChildEntity>
                                   implements TestSetChildDao {
    private static final String FIND_BY_PARENT_QUERY = 
        "select c from TestSetChildEntity c " +
        "where c.parent.id = :parentId";
    
    /**
	 * Default Constructor
	 */
	public TestSetChildDaoJpa() {
		super();
	}

    public List<TestSetChildEntity> findByParentId(BigInteger parentId) {
        Query query = entityManager.createQuery(FIND_BY_PARENT_QUERY);
        query.setParameter("parentId", parentId);
        return query.getResultList();
    }
}
