package com.saliman.entitypruner.testhelper.set;

import javax.ejb.Stateless;

import org.apache.log4j.Logger;

import com.saliman.entitypruner.testhelper.BaseDaoJpa;

/**
 * This class only exists to test the <code>OracleXml</code> Data Type. It
 * serves as a DAO to a test table that has an Oracle XMLTYPE column. 
 *
 * @author Steven C. Saliman
 */
@Stateless(name="TestSetParentDao")
public class TestSetParentDaoJpa extends BaseDaoJpa<TestSetParentEntity>
                                    implements TestSetParentDao {
    /** the class variable used for logging */
    private static Logger LOG = Logger.getLogger(TestSetParentDaoJpa.class);

    /**
	 * Default Constructor
	 */
	public TestSetParentDaoJpa() {
		super();
        LOG.debug("TestSetParentDaoJpa()");
	}
}
