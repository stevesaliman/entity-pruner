package com.saliman.entitypruner.testhelper.list;

import com.saliman.entitypruner.testhelper.BaseDaoJpa;

import java.math.BigInteger;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 * This class only exists to test the Framework in a MySql database. 
 *
 * @author Steven C. Saliman
 */
@Stateless(name="TestListChildDaoMysql")
public class TestListChildDaoJpaMysql extends BaseDaoJpa<TestListChildEntity>
                                   implements TestListChildDao {
    private static final String FIND_BY_PARENT_QUERY = 
        "select c from TestListChildEntity c " +
        "where c.parent.id = :parentId";
    
    @PersistenceContext(unitName="mysql")
    protected EntityManager mysqlEm;

    /**
	 * Default Constructor
	 */
	public TestListChildDaoJpaMysql() {
		super();
	}

	/**
	 * Replace the default entityManager with the MySql one. 
	 */
	@PostConstruct
	public void replaceEm() {
		entityManager = mysqlEm;
	}
	
	@Override
    public List<TestListChildEntity> findByParentId(BigInteger parentId) {
        Query query = entityManager.createQuery(FIND_BY_PARENT_QUERY);
        query.setParameter("parentId", parentId);
        return query.getResultList();
    }
}
