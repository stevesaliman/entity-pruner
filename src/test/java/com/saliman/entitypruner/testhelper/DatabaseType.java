package com.saliman.entitypruner.testhelper;

/**
 * This enum defines the valid database types the framework can work with. 
 * Classes can use this if they need to change they way they operate based
 * on the specific database they are talking to.  For example, bind variables
 * on a native query need to be handled differently in MySql than in Oracle.
 *  
 * @author Steven C. Saliman
 *
 */
public enum DatabaseType {
    /** 
     * Enum representing a generic database.  This is the default in framework
     *  classes.
     */
	GENERIC,
	
	/** Enum representing a MySql database */
	MYSQL,
	
	/** Enum representing an Oracle database */
	ORACLE
}
