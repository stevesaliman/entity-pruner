package net.saliman.entitypruner.testhelper.list;

import net.saliman.entitypruner.testhelper.BaseDaoJpa;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * This class only exists to test the Framework code in a MySql database.
 *
 * @author Steven C. Saliman
 */
@Stateless(name="TestListUniChildDaoMysql")
public class TestListUniChildDaoJpaMysql extends BaseDaoJpa<TestListUniChildEntity>
                                implements TestListUniChildDao {
    @PersistenceContext(unitName="mysql")
    protected EntityManager mysqlEm;

    /**
	 * Default Constructor
	 */
	public TestListUniChildDaoJpaMysql() {
		super();
	}

	/**
	 * Replace the default entityManager with the MySql one. 
	 */
	@PostConstruct
	public void replaceEm() {
		entityManager = mysqlEm;
	}
}
