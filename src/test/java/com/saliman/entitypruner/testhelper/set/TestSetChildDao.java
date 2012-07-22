package com.saliman.entitypruner.testhelper.set;

import java.math.BigInteger;
import java.util.List;

import javax.ejb.Local;

import com.saliman.entitypruner.testhelper.BaseDao;


/**
 * This class only exists to test the Framework code. 
 *
 * @author Steven C. Saliman
 */
@Local
public interface TestSetChildDao extends BaseDao<TestSetChildEntity> {
    /**
     * Find children with the specified parent id.
     * @param parentId
     * @return a list of children.
     */
    public List<TestSetChildEntity> findByParentId(BigInteger parentId);
}
