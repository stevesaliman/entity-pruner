package com.saliman.entitypruner.testhelper;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import javax.ejb.Local;

import com.saliman.entitypruner.EntityUtil;
import com.saliman.entitypruner.PrunableEntity;

/**
 * This is a base Data Access Object that defines basic interactions with the
 * database.  It defines methods that can:<br>
 * load all entities<br>
 * find an entity by ID<br>
 * save an entity<br>
 * save a <code>Collection</code> of entities<br>
 * save an array of entities<br>
 * delete an entity<br>
 * delete a <code>Collection</code> of entities<br>
 * delete an array of entities<br>
 * <p>
 * The actual behavior on these methods on child objects is heavily dependent
 * on the JPA provider (the framework assumes Hibernate), and the configuration
 * of the Entity beans themselves. Some of the issues are discussed in the
 * descriptions of each method in this class, but this is not intended to be a
 * Hibernate or JPA tutorial. The tests for this class assume lazy loading, and
 * cascading set to all. There are tests for both both entities that use the
 * delete-orphan option and entities that don't.  In general, the delete-orphan
 * option creates more issues than it resolves, because we constantly change
 * the collection references when we dehydrate/rehydrate entities, so it is not
 * a recommended option in our environment, but we test it anyway.  If 
 * extending DAOs use entities with different options the extending DAO's tests
 * should re-test the save and delete methods to make sure the desired behavior
 * happens.
 * <p>
 * None of these methods do any transaction management.  Transaction management
 * is typically handled at a higher layer.
 * <p>
 * Application DAOs should provide their own interfaces that extend this one.
 * Typically, an application DAO will only need to add find methods to look for
 * objects by differing criteria, for example, findByName.
 * <p>
 * Note that none of the DAO methods deal with populating to any given depth.
 * This is because it is not up to the DAO to know how deep an object needs
 * to be populated, that is a business logic concern.  Workflow Tasks can 
 * use {@link EntityUtil#populateToDepth(PrunableEntity, int)} to populate objects
 * if needed.  Doing so here defeats the benefits of lazy loading for server
 * based applications.
 * 
 * @author Steven C. Saliman
 * @param <T> The specific {@link BaseEntity} that this DAO uses.
 */
@Local
public interface BaseDao<T extends BaseEntity> {
    /**
     * Finds all the instances of an entity in the database.
     * @return a <code>List</code> of all the entities in the database.
     */
    public List<T> findAll();
    
    /**
     * Finds a {@link BaseEntity} by its database ID.
     * @param id the database id of the entity we want.
     * @return the instance of the entity from the database with the given id.
     */
    public T findById(BigInteger id);
    
    /**
     * Saves a transient or persistent {@link BaseEntity} to the database.
     * When a new entity is saved, the ID well be set.  For existing entities,
     * the Version will be incremented.<br>
     * This method returns the persistent version of the entity, which 
     * may or may not be the same instance that was given, but it will have
     * all the data from the database after the save completes, including 
     * database populated columns.<br>
     * Whether or not saves will be cascaded into the children depends on how
     * the Entity is set up.  If the BaseEntity is set up to cascade 
     * saves, then this save method will take care of it.  It will also take
     * care of inserting new child record if a new member is added to a child
     * Set. If you want to delete a child from a parent collection and have
     * Hibernate delete the record from the database when calling 
     * <code>save</code> on the parent, you'll need to add the following 
     * annotation to the collection in the BaseEntity:<br>
     * <code> 
     * Cascade({org.hibernate.annotations.CascadeType.ALL,
     * org.hibernate.annotations.CascadeType.DELETE_ORPHAN})
     * </code>
     * <p>
     * That said, the DELETE_ORPHAN annotation tends to create more issues than
     * it solves, especially when dealing with lazy-cloned entities, so it is 
     * not recommended.
     * <p>
     * If you save an entity that has a parent, the parent will not see the 
     * new item unless it is refreshed from another session.  In general, it is
     * not a good idea to create children independent of parents if the parent
     * is in the same session.
     * @param entity the entity to save.
     * @return a persistent copy of the entity.
     */
    public T save(T entity);
    
    /**
     * Saves a <code>Collection</code> of {@link BaseEntity} instances to the 
     * database. It saves the entities one at a time, so if one entity fails
     * to save, it will leave more than one unsaved.  Note that the elements
     * of the collection could change, since the {@link #save(BaseEntity)}
     * method returns persistent objects.
     * @param entities the <code>Collection</code> to save.
     */
    public void save(Collection<T> entities);
    
    /**
     * Saves an array of {@link BaseEntity} instances to the database. It saves
     * the entities one at a time, so if one entity fails to save, it will
     * leave more than one unsaved.  Note that the elements of the collection
     * could change, since the {@link #save(BaseEntity)} method returns
     * persistent objects.
     * @param entities the array to save.
     * @see BaseDao#save(BaseEntity)
     */
    public void save(T[] entities);

    /**
     * Deletes a {@link BaseEntity} from the database. If the delete
     * was successful, the entity's ID will be null.
     * @param entity the entity to delete.
     * @see BaseDao#save(BaseEntity)
     */
    public void delete(T entity);

    /**
     * Deletes a <code>Collection</code> of {@link BaseEntity} instances from
     * the database. It deletes the entities one at a time, so if one entity 
     * fails to delete, it will leave more than one undeleted. 
     * @param entities the <code>Collection</code> to delete.
     */
    public void delete(Collection<T> entities);
    
    /**
     * Deletes an array of {@link BaseEntity} instances from the database. It
     * deletes the entities one at a time, so if one entity fails to delete, 
     * it will leave more than one undeleted. 
     * @param entities the array to delete.
     */
    public void delete(T[] entities);
}
