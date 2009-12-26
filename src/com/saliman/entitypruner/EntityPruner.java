package com.saliman.entitypruner;

import javax.ejb.Local;

/**
 * This interface defines methods to &quot;prunes&quot; entities so they can be
 * serialized or Marshalled for use in Web Service and RMI calls. 
 * <p>
 * Entities must implement the {@link PrunableEntity} interface to be pruned
 * with implementations of this class.
 * <p>
 * implementations of this class will most likely be heavily dependent on
 * the internals of of the persistence mechanism used in an application.
 * 
 * @see PrunableEntity
 *
 * @author Steven C. Saliman
 */
@Local
public interface EntityPruner {
    /**
     * Prune the given entity to prepare it for serializing for RMI, or
     * Marshalling to XML for SOAP or REST. It is very important that this 
     * happens outside a transaction, otherwise the JPA provider will assume
     * that changes made during pruning need to be saved.  This is not a 
     * problem in a Spring based application, because Spring will only create 
     * the transaction when it is told, but EJB containers, such as GlassFish,
     * will create a default transaction when the service endpoint is invoked, 
     * unless the 
     * <code>TransactionAttribute(TransactionAttributeType.NEVER)<code> 
     * annotation is present in the endpoint class.<br>
     * In unit tests, the EntityManager.clear method should be called to make
     * sure we are dealing with detached entities.
     * Pruning basically means 2 things:<br>
     * 1) replacing proxy objects with either their non proxy equivalents if
     * they have been initialized, or <code>null</code> if they haven't<br>
     * 2) Removal of circular references. This method tries to detect 
     *    bidirectional associations, and when found, the child's parent
     *    reference is set to null to prevent XML serialization problems.
     * <p>
     * Note that once an entity is pruned, it is no longer possible to 
     * access lazy-loaded collections, even if the entity is un-pruned later.
     * <p>
     * Also note that pruning an entity does not save the entity's old 
     * collections in any way. It is up to the caller to make a copy of the
     * object's collections before calling <code>prune</code>, if access to 
     * the original collections is needed.
     * <p>
     * @param entity the {@link PrunableEntity} to pruned
     * @throws IllegalStateException if there is a problem.
     */
    public void prune(PrunableEntity entity);

    /**
     * Prune the given entity to prepare it for serializing for RMI, or
     * Marshalling to XML for SOAP or REST. It is very important that this 
     * happens outside a transaction, otherwise the JPA provider will assume
     * that changes made during pruning need to be saved.  This is not a 
     * problem in a Spring based application, because Spring will only create 
     * the transaction when it is told, but EJB containers, such as GlassFish,
     * will create a default transaction when the service endpoint is invoked, 
     * unless the 
     * <code>TransactionAttribute(TransactionAttributeType.NEVER)<code> 
     * annotation is present in the endpoint class.<br>
     * In unit tests, the EntityManager.clear method should be called to make
     * sure we are dealing with detached entities.
     * Pruning basically means 3 things:<br>
     * 1) replacing proxy objects with either their non proxy equivalents (f
     *    they have been initialized), or <code>null</code> (if they haven't)
     *    <br>
     * 2) Removal of circular references. This method tries to detect 
     *    bidirectional associations, and when found, the child's parent
     *    reference is set to null to prevent XML serialization problems.
     * 3) Recursive pruning of parent entities, and each entity in a collection.
     * <p>
     * Note that once an entity is pruned, it is no longer possible to 
     * access lazy-loaded collections, even if the entity is un-pruned later.
     * <p>
     * Also note that pruning an entity does not save the entity's old 
     * collections in any way. It is up to the caller to make a copy of the
     * object's collections before calling <code>prune</code>, if access to 
     * the original collections is needed.
     * <p>
     * This version of the <code>prune</code> method can also be used to
     * specify a maximum depth for the object.  This is handy in the case 
     * where the client only needs so many levels of an object, but more 
     * levels were populated by the server in the course of its actions inside
     * the transaction.  Note that only a collection constitutes a level, so
     * if an entity has an instance of another entity, both will be 
     * pruned and returned.
     * @param entity the {@link PrunableEntity} to pruned
     * @param depth the depth to populate to.  1 for just the entity, 2 for
     *        children, etc.
     * @throws IllegalStateException if there is a problem.
     */
    public void prune(PrunableEntity entity, int depth);
    
    /**
     * Prune the given entity to prepare it for serializing for RMI, or
     * Marshalling to XML for SOAP or REST. It is very important that this 
     * happens outside a transaction, otherwise the JPA provider will assume
     * that changes made during pruning need to be saved.  This is not a 
     * problem in a Spring based application, because Spring will only create 
     * the transaction when it is told, but EJB containers, such as GlassFish,
     * will create a default transaction when the service endpoint is invoked, 
     * unless the 
     * <code>TransactionAttribute(TransactionAttributeType.NEVER)<code> 
     * annotation is present in the endpoint class.<br>
     * In unit tests, the EntityManager.clear method should be called to make
     * sure we are dealing with detached entities.
     * Pruning basically means 3 things:<br>
     * 1) replacing proxy objects with either their non proxy equivalents (f
     *    they have been initialized), or <code>null</code> (if they haven't)
     *    <br>
     * 2) Removal of circular references. This method tries to detect 
     *    bidirectional associations, and when found, the child's parent
     *    reference is set to null to prevent XML serialization problems.
     * 3) Recursive pruning of parent entities, and each entity in a collection.
     * <p>
     * Note that once an entity is pruned, it is no longer possible to 
     * access lazy-loaded collections, even if the entity is un-pruned later.
     * <p>
     * Also note that pruning an entity does not save the entity's old 
     * collections in any way. It is up to the caller to make a copy of the
     * object's collections before calling <code>prune</code>, if access to 
     * the original collections is needed.
     * <p>
     * This version of the <code>prune</code> method can also be used to
     * specify a maximum depth for the object, or the names of specific 
     * attributes to prune out.  This is handy in the case where the client 
     * only needs so many levels of an object, or certain attributes, but more
     * levels were populated by the server in the course of its actions inside
     * the transaction.  Note that only a collection constitutes a level, so
     * if an entity has an instance of another entity, both will be 
     * pruned and returned.  Services will probably specify "include" 
     * attributes, but the pruner uses "exclude" lists because an "include" 
     * sets the expectation that all attributes in the list will be populated,
     * and the EntityPruner will not go out to the database to fetch missing
     * values.
     * <p>
     * When specifying both a depth and an exclude list, the 
     * <code>EntityPruner</code> will exclude collections in the list, and 
     * prune the rest to the given depth.
     * @param entity the {@link Prunable} to pruned
     * @param depth the depth to populate to.  1 for just the entity, 2 for
     *        children, etc.
     * @param exclude a comma separated list of attributes to exclude.
     * @throws IllegalStateException if there is a problem.
     */
    public void prune(PrunableEntity entity, int depth, String exclude);

    /**
     * Un-prune the given entity so it can be saved by an ORM.  Basically this
     * means restoring the bidirectional references and restoring the 
     * appropriate proxy object for the underlying ORM implementation.
     * <p>
     * This works well enough to save an entity, but not well enough to 
     * use the un-pruned entity to get previously uninitialized collections.
     * @param entity the {@link PrunableEntity} to un-prune
     * @throws IllegalStateException if something goes wrong
     */
    public void unprune(PrunableEntity entity);

}
