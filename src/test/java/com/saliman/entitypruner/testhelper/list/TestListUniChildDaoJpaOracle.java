package com.saliman.entitypruner.testhelper.list;

import com.saliman.entitypruner.testhelper.BaseDaoJpa;

import javax.ejb.Stateless;

/**
 * This class only exists to test the Framework code in an Oracle database.
 *
 * @author Steven C. Saliman
 */
@Stateless(name="TestListUniChildDaoOracle")
public class TestListUniChildDaoJpaOracle extends BaseDaoJpa<TestListUniChildEntity>
                                implements TestListUniChildDao {
    /**
	 * Default Constructor
	 */
	public TestListUniChildDaoJpaOracle() {
		super();
	}
}
