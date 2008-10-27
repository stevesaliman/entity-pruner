package com.saliman.entitypruner.testhelper;

import java.math.BigInteger;
import java.util.List;


/**
 * This class only exists to test the Framework code. 
 *
 * @author Steven C. Saliman
 */
public interface TestChildDao extends BaseDao<TestChildEntity> {
    /**
     * Find children with the specified parent id.
     * @param parentId
     * @return a list of children.
     */
    public List<TestChildEntity> findByParentId(BigInteger parentId);
}
