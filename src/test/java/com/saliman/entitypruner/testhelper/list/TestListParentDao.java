package com.saliman.entitypruner.testhelper.list;

import javax.ejb.Local;

import com.saliman.entitypruner.testhelper.BaseDao;


/**
 * This class only exists to test the Framework code. 
 *
 * @author Steven C. Saliman
 */
@Local
public interface TestListParentDao extends BaseDao<TestListParentEntity> {
    // no new methods.
}
