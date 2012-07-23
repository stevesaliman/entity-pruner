package com.saliman.entitypruner.testhelper.set;

import javax.ejb.Stateless;

import com.saliman.entitypruner.testhelper.BaseDaoJpa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class only exists to test the Framework in an Oracle database. It also
 * serves as a DAO to a test table that has an Oracle XMLTYPE column. 
 *
 * @author Steven C. Saliman
 */
@Stateless(name="TestSetParentDaoOracle")
public class TestSetParentDaoJpaOracle extends BaseDaoJpa<TestSetParentEntity>
                                    implements TestSetParentDao {
    /** the class variable used for logging */
    private static Logger LOG = LoggerFactory.getLogger(TestSetParentDaoJpaOracle.class);

    /**
	 * Default Constructor
	 */
	public TestSetParentDaoJpaOracle() {
		super();
        LOG.debug("TestSetParentDaoJpa()");
	}
}
