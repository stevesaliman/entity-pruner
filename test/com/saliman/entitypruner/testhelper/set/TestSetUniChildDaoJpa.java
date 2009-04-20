package com.saliman.entitypruner.testhelper.set;

import javax.ejb.Stateless;

import com.saliman.entitypruner.testhelper.BaseDaoJpa;

/**
 * This class only exists to test the Framework code.
 *
 * @author Steven C. Saliman
 */
@Stateless(name="TestSetUniChildDao")
public class TestSetUniChildDaoJpa extends BaseDaoJpa<TestSetUniChildEntity>
                                implements TestSetUniChildDao {
    /**
	 * Default Constructor
	 */
	public TestSetUniChildDaoJpa() {
		super();
	}
}
