package com.saliman.entitypruner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.collection.PersistentSet;


/**
 * This class is a utility class for working with Entity instances and their
 * descendants.  It currently deals with 2 basic areas.
 * <p>
 * It handles populating an entity to a specified depth to prepare for pruning
 * calls.
 * <p>
 * It also handles transferring transient attributes from one object to 
 * another, since saving an entity can wipe out transient attributes.
 * <p>
 * This class is heavily dependent on the JPA provider and the types of
 * collections Entities have.  At the moment, the methods in this class only
 * work with Hibernate, and child collections must be a <code>Set</code>.  In
 * Addition, the entities must use field annotations and not method 
 * annotations.
 * 
 * @author Steven C. Saliman
 */
public class EntityUtil {
    /** logger for the class */
    private static final Logger LOG = Logger.getLogger(EntityUtil.class);

    /**
     * Determines if a collection has been initialized. This is most useful 
     * in hiding Hibernate from non-DAO tasks.  An initialized collection is
     * one we can safely access, such as an initialized Hibernate PersistentBag
     * or an ordinary Set.  This method treats <code>null</code> as an 
     * uninitialized set.
     * @param collection the collection to check.
     * @return <code>true</code> if the collection has been initialized. 
     */
    public static boolean initialized(Collection<?> collection) {
        boolean init = false;

        //TODO: expand this to support and PersistentLists
        if ( collection == null ) {
            // null is treated as uninitialized
            init = false;
        } else if (!PersistentSet.class.isAssignableFrom(collection.getClass())) {
            // if it's not a Hibernate PersistentBag, it's initialized.
            init = true;
        } else if ( !((PersistentSet)collection).wasInitialized() ) {
            // If Hibernate hasn't loaded the collection, it't not initialized
            init = false;
        } else {
            // Assume Positive Intent.
            init = true;
        }
        return init;
    }
    
    /**
     * Copies the transient attributes from one entity to another.  This can
     * is needed when we save an entity, because if we refresh after a merge,
     * we'll lose the transient attributes to.
     * @param source the source entity
     * @param dest the destination entity.
     * @throws IllegalStateException if we can't get one of the values.
     */
    public static void copyTransientData(PrunableEntity source, 
                                         PrunableEntity dest) {
        List<Field> fields = loadFields(source.getClass());
        for ( Field f : fields ) {
            Annotation a = f.getAnnotation(Transient.class);
            if ( a != null ) {
                try {
                    f.setAccessible(true);
                    f.set(dest, f.get(source));
                } catch (IllegalAccessException e) {
                    String msg = null;
                    msg = "Entity " + source + " has an inaccessable " +
                       	 "or mismatched transient attribute: " + f.getName();
                    LOG.warn(msg);
                    throw new IllegalStateException(msg);
                }
            }
        }
    }
    
    /**
     * Populate the given entity to the given depth.  A depth of 1 indicates
     * that only the entity itself needs to be populated.  A depth of 2 means
     * all the entity's children are loaded, 3 causes grandchildren to be 
     * loaded, etc. Child collections will only be recursively  populated only
     * if the collection contains {@link PrunableEntity} instances, because
     * we don't get lazy loading problems with non-entities.
     * <p>
     * This method can only be called within a session, or we'll get lazy 
     * loading errors.
     * @param entity the Entity to populate
     * @param depth the depth to populate to.  1 for just the entity, 2 for
     *        children, etc.
     */
    public static void populateToDepth(PrunableEntity entity, int depth) {
        if ( entity == null || depth < 2 ) {
            return;
        }
        // loop through fields. If a collection, populate each child.
        List<Field> fields = loadFields(entity.getClass());
        for ( Field f : fields ) {
            if ( Collection.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                Collection<?> collection;
                try {
                    collection = (Collection<?>)f.get(entity);
                    if ( collection != null ) {
                        for ( Object value : collection ) {
                            // the iterator causes the children to be loaded. 
                            // We only need the recursive call if we want 
                            // grandchildren.
                            if ( depth > 2 && 
                                    PrunableEntity.class.isAssignableFrom(value.getClass()) ) {
                                // child needs one less than parent
                                populateToDepth((PrunableEntity)value, depth-1);
                            }
                        }
                    }
                } catch (IllegalStateException e) {
                    String msg = null;
                    msg = "Data Object " + entity + " has an inaccessable " +
                         "or attribute: " + f.getName();
                    LOG.warn(msg);
                    throw new IllegalStateException(msg);
                } catch (IllegalAccessException e) {
                    String msg = null;
                    msg = "Data Object " + entity + " has an inaccessable " +
                         "or attribute: " + f.getName();
                    LOG.warn(msg);
                    throw new IllegalStateException(msg);
                }
            }
        }
    }

    /**
     * Helper method that gets all the fields of a class and it's super-classes.
     * 
     * @param clazz The class whose fields we want.
     * @return a List of fields from the given class and it's parents, up to 
     *         the BaseEntity class.
     */
    private static List<Field> loadFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<Field>();
        if ( !clazz.equals(Object.class) ) {
            fields.addAll(loadFields(clazz.getSuperclass()));
        }
        Field[] arr = clazz.getDeclaredFields();
        for ( int i=0 ; i < arr.length; i++ ) {
            fields.add(arr[i]);
        }
        return fields;
    }
}
