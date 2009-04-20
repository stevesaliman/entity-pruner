package com.saliman.entitypruner.testhelper.list;

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
@Stateless(name="TestListChildDao")
public class TestListChildDaoJpa extends BaseDaoJpa<TestListChildEntity>
                                   implements TestListChildDao {
    private static final String FIND_BY_PARENT_QUERY = 
        "select c from TestListChildEntity c " +
        "where c.parent.id = :parentId";
    
    /**
	 * Default Constructor
	 */
	public TestListChildDaoJpa() {
		super();
	}

    public List<TestListChildEntity> findByParentId(BigInteger parentId) {
        Query query = entityManager.createQuery(FIND_BY_PARENT_QUERY);
        query.setParameter("parentId", parentId);
        return query.getResultList();
    }
}
