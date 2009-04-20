package com.saliman.entitypruner.testhelper.set;

import javax.ejb.Local;

import com.saliman.entitypruner.testhelper.BaseDao;


/**
 * This class only exists to test the Framework code. 
 *
 * @author Steven C. Saliman
 */
@Local
public interface TestSetParentDao extends BaseDao<TestSetParentEntity> {
    // no new methods.
}
