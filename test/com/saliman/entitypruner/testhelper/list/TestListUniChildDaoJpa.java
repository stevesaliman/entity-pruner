package com.saliman.entitypruner.testhelper.list;

import javax.ejb.Stateless;

import com.saliman.entitypruner.testhelper.BaseDaoJpa;

/**
 * This class only exists to test the Framework code.
 *
 * @author Steven C. Saliman
 */
@Stateless(name="TestListUniChildDao")
public class TestListUniChildDaoJpa extends BaseDaoJpa<TestListUniChildEntity>
                                implements TestListUniChildDao {
    /**
	 * Default Constructor
	 */
	public TestListUniChildDaoJpa() {
		super();
	}
}
