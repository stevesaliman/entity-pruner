package net.saliman.entitypruner.testhelper.set;

import net.saliman.entitypruner.testhelper.BaseDaoJpa;

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
@Stateless(name="TestSetChildDaoMysql")
public class TestSetChildDaoJpaMysql extends BaseDaoJpa<TestSetChildEntity>
                                   implements TestSetChildDao {
    private static final String FIND_BY_PARENT_QUERY = 
        "select c from TestSetChildEntity c " +
        "where c.parent.id = :parentId";
    
    @PersistenceContext(unitName="mysql")
    protected EntityManager mysqlEm;

    /**
	 * Default Constructor
	 */
	public TestSetChildDaoJpaMysql() {
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
    public List<TestSetChildEntity> findByParentId(BigInteger parentId) {
        Query query = entityManager.createQuery(FIND_BY_PARENT_QUERY);
        query.setParameter("parentId", parentId);
        return query.getResultList();
    }
}
