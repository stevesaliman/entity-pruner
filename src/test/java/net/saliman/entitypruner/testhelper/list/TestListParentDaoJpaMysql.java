package net.saliman.entitypruner.testhelper.list;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import net.saliman.entitypruner.testhelper.BaseDaoJpa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class only exists to test the Framework in a MySql database. 
 *
 * @author Steven C. Saliman
 */
@Stateless(name="TestListParentDaoMysql")
public class TestListParentDaoJpaMysql extends BaseDaoJpa<TestListParentEntity>
                                    implements TestListParentDao {
    /** the class variable used for logging */
    private static Logger LOG = LoggerFactory.getLogger(TestListParentDaoJpaMysql.class);

    @PersistenceContext(unitName="mysql")
    protected EntityManager mysqlEm;

    /**
	 * Default Constructor
	 */
	public TestListParentDaoJpaMysql() {
		super();
        LOG.debug("TestListParentDaoJpa()");
	}

	/**
	 * Replace the default entityManager with the MySql one. 
	 */
	@PostConstruct
	public void replaceEm() {
		entityManager = mysqlEm;
	}

	@Override
	public List<TestListParentEntity> executeSql(String sql,
			Map<String, Object> bindings, Map<String, String> options) {
		Query query = createQuery(sql, options);
		if ( bindings != null ) {
	        for ( String key : bindings.keySet() ) {
	            Object val = bindings.get(key);
	            if ( Date.class.isAssignableFrom(val.getClass()) ) {
	                query.setParameter(key, (Date)val, TemporalType.TIMESTAMP);
	            } else {
	                query.setParameter(key, val);
	            }
	        }
		}
		return query.getResultList();
	}
}
