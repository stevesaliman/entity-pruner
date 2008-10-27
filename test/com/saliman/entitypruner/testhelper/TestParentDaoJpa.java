package com.saliman.entitypruner.testhelper;

import org.apache.log4j.Logger;

/**
 * This class only exists to test the <code>OracleXml</code> Data Type. It
 * serves as a DAO to a test table that has an Oracle XMLTYPE column. 
 *
 * @author Steven C. Saliman
 */
public class TestParentDaoJpa extends BaseDaoJpa<TestParentEntity>
                                    implements TestParentDao {
    /** the class variable used for logging */
    private static Logger LOG = Logger.getLogger(TestParentDaoJpa.class);

    /**
	 * Default Constructor
	 */
	public TestParentDaoJpa() {
		super();
        LOG.debug("TestParentDaoJpa()");
	}
}
