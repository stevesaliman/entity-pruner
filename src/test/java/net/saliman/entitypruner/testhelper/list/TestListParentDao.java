package net.saliman.entitypruner.testhelper.list;

import net.saliman.entitypruner.testhelper.BaseDao;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;

/**
 * This class only exists to test the Framework code. 
 *
 * @author Steven C. Saliman
 */
@Local
public interface TestListParentDao extends BaseDao<TestListParentEntity> {
	/**
     * This method executes the given SQL.  It should be use the 
     * {@link BaseDaoJpa}'s createQuery method.
     * <p>
     * It's bad programming practice to let users of a DAO pass in SQL, but
     * there is no easy way to test the {@link BaseDaoJpa}'s executeQuery
     * method because the method is protected, and needs a transaction. Unit
     * tests can only do transactional work on methods that are publicly 
     * declared in a bean's interface.  We' can live with this because this
     * DAO only exists to test the Framework, and does not get compiled into
     * the Framework jar.
     * @param sql the SQL to execute.
     * @param bindings a Map of tokens and bindings for the SQL.
     * @param options an options map.
     * @return the results
     */
	public List<TestListParentEntity> executeSql(String sql, 
			Map<String, Object> bindings, Map<String, String> options);
}
