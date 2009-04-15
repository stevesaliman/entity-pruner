package com.saliman.entitypruner;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * Classes implement this interface to mark it as being prunable by the 
 * {@link EntityPruner}.  Entities must be pruned before they can be Serialized
 * or Marshalled for sending to a remote client.
 * <p> 
 * Implementing this interface means that entities will implement
 * accessors for a &quot;id&quot; and an &quot;pruned&quot; attributes.
 * <p>
 * The id attribute is the one that JPA uses for entity identity.  It doesn't 
 * matter what the data type is, because the {@link EntityPruner} only needs to
 * know if the entity has an id or not.  The default value in an entity should
 * be null, to indicate no database identity.
 * <p>
 * The pruned attribute is used by the {@link EntityPruner} to determine if 
 * a given entity has already been pruned. The default value of the pruned
 * attribute <b>needs</b> to be <code>false</code>, so that JPA entities are 
 * created correctly on the server during a query. If an RMI client needs to
 * create an entity in the client, it should be OK to leave it false, since an
 * incoming new entity will look the same to JPA as one that was created 
 * inside server code (as far as nulls and ordinary Collections are concerned).
 * 
 * @author Steven C. Saliman
 * @see EntityPruner for more details about pruning entities.
 */
public interface PrunableEntity {
    /**
     * @return whether or not this entity has already been pruned.
     */
    public boolean isPruned();
    
    /**
     * @param pruned whether or not this entity should be marked as pruned.
     */
    public void setPruned(boolean pruned);
    
    /**
     * Get the Id from the entity.  We don't really care what it is, only 
     * whether or not it is <code>null</code>, which is why we can get away
     * with returning an <code>Object</code>.  If an entity's ID column is not
     * named &quot;id&quot;, then this method will probably be implemented as 
     * a pass-through to the method that gets the actual ID.
     * @return the ID of the entity.
     */
    public BigInteger getId();
    
    /**
     * Sets the ID.  The Entity Pruner doesn't actually set the ID, but pruned
     * entities are often serialized to a client (which is why we pruned the
     * entity in the first place).  Some serializers, like BlazeDS, use
     * code in the java.beans package, which doesn't see the set method in a
     * class if it isn't also in the interfaces it implements.  Since Entities
     * may have different classes have different types of IDs, we use generics
     * here, since using Object here and a concrete class in the implementing
     * entity was not enough to get the java.beans.Introspector to see the ID
     * field. 
     * @param id
     */
    public void setId(BigInteger id);
    
    /**
     * When a field has (or had) a proxy for an entity, get that entitiy's ID.
     * @param field the name of the field who's proxy entity's ID we want.
     * @return the ID of the proxy entity, or null if the field didn't have 
     * a proxy.
     */
    public Serializable getProxyEntityId(String field);
    
    /**
     * Add a proxy entity ID to the entity.  When the {@link EntityPruner} 
     * prunes an uninitialized proxy entity from a field, it uses this method
     * to store the ID from that proxy so the unprune method can restore it
     * later.
     * @param field the name of the field with the proxy.
     * @param proxyEntityId the ID of the proxied entity.
     */
    public void addProxyEntityId(String field, Serializable proxyEntityId);
    
}
