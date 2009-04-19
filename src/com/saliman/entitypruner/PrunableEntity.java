package com.saliman.entitypruner;

import java.util.Map;


/**
 * Classes implement this interface to mark it as being prunable by the 
 * {@link EntityPruner}.  Entities must be pruned before they can be Serialized
 * or Marshalled for sending to a remote client.
 * <p> 
 * Implementing this interface means that entities will implement
 * accessors for &quot;pruned&quot; and &quot;fieldIdMap&quot; attributes, as
 * well as a method to determine if the Entity is persistent or not.
 * <p>
 * The pruned attribute is used by the {@link EntityPruner} to determine if 
 * a given entity has already been pruned. The default value of the pruned
 * attribute <b>needs</b> to be <code>false</code>, so that JPA entities are 
 * created correctly on the server during a query. If an RMI client needs to
 * create an entity in the client, it should be OK to leave it false, since an
 * incoming new entity will look the same to JPA as one that was created 
 * inside server code (as far as nulls and ordinary Collections are concerned).
 * <p>
 * The fieldIdMap attribute is used by the {@link EntityPruner} to store
 * the field name and ID of proxied parent entities when the parent entity 
 * hasn't been loaded yet.
 * <p>
 * It is also strongly recommended, though not required, that Entities also
 * implement a <code>toString()</code> method to make the log entries more
 * meaningful.
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
     * @return the map of field to ID mappings.
     */
    public Map<String, Object> getFieldIdMap();
    
    /**
     * @param fieldIdMap the map of field to ID mappings to use.
     */
    public void setFieldIdMap(Map<String, Object> fieldIdMap);

    /**
     * Determine if the Entity has been saved to the database or not.  The
     * Entity Pruner needs to know this in order to know whether or not to 
     * create uninitialized proxy objects.  In most cases, it is enough to
     * return whether or not the primary key is null.  This approach can cause 
     * a problem when the physical key and the business key is the same, in 
     * which case the Entity will need some other way to make the determination.
     * @return <code>true</code> if this Entity represents an entity that 
     * has been persisted to the database.
     */
    public boolean isPersistent();
    
}
