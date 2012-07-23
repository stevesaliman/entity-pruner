package com.saliman.entitypruner.testhelper.set;

import com.saliman.entitypruner.testhelper.BaseDaoJpa;

import javax.ejb.Stateless;

/**
 * This class only exists to test the Framework code in an Oracle database.
 *
 * @author Steven C. Saliman
 */
@Stateless(name="TestSetUniChildDaoOracle")
public class TestSetUniChildDaoJpaOracle extends BaseDaoJpa<TestSetUniChildEntity>
                                implements TestSetUniChildDao {
    /**
	 * Default Constructor
	 */
	public TestSetUniChildDaoJpaOracle() {
		super();
	}
}
