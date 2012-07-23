package net.saliman.entitypruner;


/**
 * This interface defines the standard keys that can appear in an options
 * map.  These options effect things like pruning of objects, the rows per
 * page to return on a find, etc.
 * <p>
 * There are 3 pruning options that the framework itself looks at, and are
 * support by any application.
 * <p>
 * Options that relate to finding entities are standard options that 
 * applications should support, but whether or not a particular option works
 * is up to implementing applications.
 * <p>
 * The actual values of each constant is the exact string that needs to be
 * passed in by calling programs to use the option.
 * 
 * @author Steven C. Saliman
 */
public interface Options {
	/**
	 * A depth options tells the entity populator and pruner to use a numeric
	 * depth in pruning.  A depth of 1 means the object itself, 2 means the 
	 * object and all of its children, etc. 
	 * <p> 
	 * This option is one of the 3 pruning options. If no pruning options are
	 * specified, the default is to return whatever the server loaded in the
	 * course of doing business.
	 * 
	 * @see #INCLUDE
     * @see #SELECT
	 */
    public static final String DEPTH = "depth";
    
    /**
     * The include option tells the entity populator and pruner to include
     * specific named child collections and exclude the rest.  Invalid 
     * attributes are ignored.  This option has no effect on attributes that
     * are not child collections.
	 * <p> 
	 * This option is one of the 3 pruning options. If no pruning options are
	 * specified, the default is to return whatever the server loaded in the
	 * course of doing business.
	 * 
     * @see #DEPTH
     * @see #SELECT
     */
    public static final String INCLUDE = "include";
    
    /**
     * The select option tells the entity populator and pruner to include 
     * specific named attributes and exclude the rest.  Invalid attributes 
     * are ignored.  This option has no effect on attributes that are 
     * child collections.
	 * <p> 
	 * This option is one of the 3 pruning options. If no pruning options are
	 * specified, the default is to return whatever the server loaded in the
	 * course of doing business.
	 * 
     * @see #DEPTH
     * @see #INCLUDE
     */
    public static final String SELECT = "select";
    
    /**
     * The page option tells find operations to return a page other than the 
     * first one (the default).  Pages are 1 based.  
     */
    public static final String PAGE = "page";
    
    /**
     * The per_page option tells find operations to limit the number of rows
     * returned on each page.  The default should be all rows, but that 
     * behavior may vary in each application.
     */
    public static final String PER_PAGE = "per_page";
    
    /**
     * The order option tells find operations to sort the results by the given
     * attribute list.  To sort in descending order, add <code>:desc</code>
     * to the attribute name.  Passing invalid attributes will cause errors.
     */
    public static final String ORDER = "order";

	/**
	 * {@code OptionMap} constant used by some operations to set a lock on
	 * a record when it reads it from the database.  This is useful to avoid
	 * concurrent modification issues.  The values of this constant should
	 * match the valid constants of the {@code LockModeType} class.
	 */
	public static final String LOCK_MODE = "lockMode";

}
