package com.saliman.entitypruner;

/**
 * This interface defines the valid pruning states for an entity.  They
 * are implemented as strings instead of an enum because we want it to be 
 * simple for clients to understand as the value gets passed back and forth.
 * <p>
 * Clients should be careful not to change the value of the entity's 
 * pruning state, and new values should be set to PRUNED_COMPLETE.
 * <p>
 * Server code should not allow updates to an entity in an UNPRUNED_PARTIAL
 * state because it would result in a lot of fields being set incorrectly to
 * null in the database.
 *  
 * @author Steven C. Saliman
 * @see EntityPruner for more details about pruning entities.
 */
public interface PruningState {
	/** 
	 * Constant representing a pruned entity that contains all its attributes.
	 * A COMPLETE_PRUNED entity can be unpruned and safely saved to a database.
	 */
	public static final String PRUNED_COMPLETE = "PRUNED_COMPLETE";
	/**
	 * Constant representing an unpruned entity thas all its attributes. This
	 * is the state of an entity when it comes from the database.
	 */
	public static final String UNPRUNED_COMPLETE = "UNPRUNED_COMPLETE";
	/**
	 * Constant representing a pruned entity that has had some of its 
	 * attributes stripped out.  A PRUNED_PARTIAL entity is useful when clients
	 * only need a small subset of information about an entity.
	 */
	public static final String PRUNED_PARTIAL = "PRUNED_PARTIAL";
	/**
	 * Constant representing an entity that has been unpruned, but does not 
	 * have all its attributes.  Applications probably don't want to allow
	 * an UNPRUNED_PARTIAL entity to be saved to the database because we have
	 * no way to tell the difference between an attribute that was never sent
	 * to the client, and one where the attribute was intentionally set to 
	 * null.  Pruning an entity with a &quot;select&quot; argument will 
	 * cause an entity to be set to this state.
	 */
	public static final String UNPRUNED_PARTIAL = "UNPRUNED_PARTIAL";
}
