package com.saliman.entitypruner.testhelper.list;

import com.saliman.entitypruner.testhelper.BaseDao;

import java.math.BigInteger;
import java.util.List;

import javax.ejb.Local;

/**
 * This class only exists to test the Framework code. 
 *
 * @author Steven C. Saliman
 */
@Local
public interface TestListChildDao extends BaseDao<TestListChildEntity> {
	/**
     * Find children with the specified parent id.
     * @param parentId
     * @return a list of children.
     */
    public List<TestListChildEntity> findByParentId(BigInteger parentId);
}
