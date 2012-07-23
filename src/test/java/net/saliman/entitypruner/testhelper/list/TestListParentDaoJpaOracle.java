package net.saliman.entitypruner.testhelper.list;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import net.saliman.entitypruner.testhelper.BaseDaoJpa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class only exists to test the Framework in an Oracle database.  It also
 * serves as a DAO to a test table that has an Oracle XMLTYPE column. 
 *
 * @author Steven C. Saliman
 */
@Stateless(name="TestListParentDaoOracle")
public class TestListParentDaoJpaOracle extends BaseDaoJpa<TestListParentEntity>
                                    implements TestListParentDao {
    /** the class variable used for logging */
    private static Logger LOG = LoggerFactory.getLogger(TestListParentDaoJpaOracle.class);

    /**
	 * Default Constructor
	 */
	public TestListParentDaoJpaOracle() {
		super();
        LOG.debug("TestListParentDaoJpa()");
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
