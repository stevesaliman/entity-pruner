package com.saliman.entitypruner.testhelper.set;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.saliman.entitypruner.testhelper.BaseDaoJpa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class only exists to test Framework in a MySql database.
 *
 * @author Steven C. Saliman
 */
@Stateless(name="TestSetParentDaoMysql")
public class TestSetParentDaoJpaMysql extends BaseDaoJpa<TestSetParentEntity>
                                    implements TestSetParentDao {
    /** the class variable used for logging */
    private static Logger LOG = LoggerFactory.getLogger(TestSetParentDaoJpaMysql.class);

    @PersistenceContext(unitName="mysql")
    protected EntityManager mysqlEm;

    /**
	 * Default Constructor
	 */
	public TestSetParentDaoJpaMysql() {
		super();
        LOG.debug("TestSetParentDaoJpa()");
	}

	/**
	 * Replace the default entityManager with the MySql one. 
	 */
	@PostConstruct
	public void replaceEm() {
		entityManager = mysqlEm;
	}
}
