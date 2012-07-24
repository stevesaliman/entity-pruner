package net.saliman.entitypruner;

import java.util.Map;

/**
 * Classes implement this interface to mark it as being prunable by the 
 * {@link EntityPruner}.  Entities must be pruned before they can be Serialized
 * or Marshalled for sending to a remote client.
 * <p> 
 * Implementing this interface means that entities will implement
 * accessors for &quot;prunedState&quot; and &quot;fieldIdMap&quot; 
 * attributes, as well as a method to determine if the Entity is persistent or
 * not.
 * <p>
 * The pruningState attribute is used by the {@link EntityPruner} to determine
 * if a given entity has already been pruned, and how. The default value of 
 * the pruned attribute <b>needs</b> to be <code>UNPRUNED_COMPLETE</code>, so
 * that JPA entities are created correctly on the server during a query. If an
 * RMI client needs to create an entity in the client, it should be OK to 
 * leave it null, since an incoming new entity will look the same to JPA as 
 * one that was created inside server code (as far as nulls and ordinary 
 * Collections are concerned).  Server code that tries to update an entity
 * should probably look at the prunedState attribute and only allow an update
 * if the state is <code>UNPRUNED_COMPLETE</code>
 * <p>
 * The fieldIdMap attribute is used by the {@link EntityPruner} to store
 * the field name and ID of proxied parent entities when the parent entity 
 * hasn't been loaded yet.
 * <p>
 * A class wishing to be prunable must have as it's ID a class that has a 
 * <code>toString</code> method and a constructor that takes creates an 
 * instance from that string. All Java number classes currently fit that
 * description.
 * <p> 
 * The reason IDs need to be convertible to Strings has to do with the 
 * fieldIdMap.  Different languages treat objects (particularly numbers)
 * differently and that causes problems when we cross language boundaries.  
 * For example, if a Java entity has a Long id of 987654321, and if the object 
 * is sent to ActionScript, the number will come back as a double with the 
 * value "9.87654321E8", which the Long constructor has a problem with.  The 
 * problem gets worse as numbers get very large.  An 18 digit Long will lose
 * precision during the round trip, with obvious bad side effects.  All the 
 * Java number classes have constructors that take a String.  If the ID needs 
 * to be something else, it is up to the developer to make sure that class 
 * can convert back and forth to a String.  To use something like a date, 
 * the class would need to be sub-classed to be used. (but you don't want to
 * use a date as the primary key, do you?)
 * <p>
 * It is also strongly recommended, though not required, that Entities also
 * implement a <code>toString()</code> method to make the log entries more
 * meaningful.
 * @author Steven C. Saliman
 * @see EntityPruner for more details about pruning entities.
 */
public interface PrunableEntity {
	/**
     * @return The current pruning state of the entity.  The EntityPruner
     * considers a null state to be UNPRUNED_PARTIAL.
     */
    public String getPruningState();
    
    /**
     * @param pruningState The state to set
     */
    public void setPruningState(String pruningState);
    
    /**
     * @return the map of field to ID mappings.
     */
    public Map<String, String> getFieldIdMap();
    
    /**
     * @param fieldIdMap the map of field to ID mappings to use.
     */
    public void setFieldIdMap(Map<String, String> fieldIdMap);
    
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
