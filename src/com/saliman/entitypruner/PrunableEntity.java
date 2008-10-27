package com.saliman.entitypruner;


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
 * @see EntityPruner
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
    public Object getId();
}
