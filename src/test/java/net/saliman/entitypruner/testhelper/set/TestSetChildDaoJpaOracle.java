package net.saliman.entitypruner.testhelper.set;

import net.saliman.entitypruner.testhelper.BaseDaoJpa;

import java.math.BigInteger;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.Query;

/**
 * This class only exists to test the Framework in an Oracle Database. It
 * serves as a DAO to a test table that has an Oracle XMLTYPE column. 
 *
 * @author Steven C. Saliman
 */
@Stateless(name="TestSetChildDaoOracle")
public class TestSetChildDaoJpaOracle extends BaseDaoJpa<TestSetChildEntity>
                                   implements TestSetChildDao {
    private static final String FIND_BY_PARENT_QUERY = 
        "select c from TestSetChildEntity c " +
        "where c.parent.id = :parentId";
    
    /**
	 * Default Constructor
	 */
	public TestSetChildDaoJpaOracle() {
		super();
	}

	@Override
    public List<TestSetChildEntity> findByParentId(BigInteger parentId) {
        Query query = entityManager.createQuery(FIND_BY_PARENT_QUERY);
        query.setParameter("parentId", parentId);
        return query.getResultList();
    }
}
