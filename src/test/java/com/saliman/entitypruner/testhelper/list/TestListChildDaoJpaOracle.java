package com.saliman.entitypruner.testhelper.list;

import com.saliman.entitypruner.testhelper.BaseDaoJpa;

import java.math.BigInteger;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.Query;

/**
 * This class only exists to test the Framework in an Oracle database. It
 * serves as a DAO to a test table that has an Oracle XMLTYPE column. 
 *
 * @author Steven C. Saliman
 */
@Stateless(name="TestListChildDaoOracle")
public class TestListChildDaoJpaOracle extends BaseDaoJpa<TestListChildEntity>
                                   implements TestListChildDao {
    private static final String FIND_BY_PARENT_QUERY = 
        "select c from TestListChildEntity c " +
        "where c.parent.id = :parentId";
    
    /**
	 * Default Constructor
	 */
	public TestListChildDaoJpaOracle() {
		super();
	}

	@Override
    public List<TestListChildEntity> findByParentId(BigInteger parentId) {
        Query query = entityManager.createQuery(FIND_BY_PARENT_QUERY);
        query.setParameter("parentId", parentId);
        return query.getResultList();
    }
}
