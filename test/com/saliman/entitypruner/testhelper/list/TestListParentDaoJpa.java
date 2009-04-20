package com.saliman.entitypruner.testhelper.list;

import javax.ejb.Stateless;

import org.apache.log4j.Logger;

import com.saliman.entitypruner.testhelper.BaseDaoJpa;

/**
 * This class only exists to test the <code>OracleXml</code> Data Type. It
 * serves as a DAO to a test table that has an Oracle XMLTYPE column. 
 *
 * @author Steven C. Saliman
 */
@Stateless(name="TestListParentDao")
public class TestListParentDaoJpa extends BaseDaoJpa<TestListParentEntity>
                                    implements TestListParentDao {
    /** the class variable used for logging */
    private static Logger LOG = Logger.getLogger(TestListParentDaoJpa.class);

    /**
	 * Default Constructor
	 */
	public TestListParentDaoJpa() {
		super();
        LOG.debug("TestListParentDaoJpa()");
	}
}
