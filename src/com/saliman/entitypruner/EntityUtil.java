package com.saliman.entitypruner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

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

        if ( collection == null ) {
            // null is treated as uninitialized
            init = false;
        } else if (!PersistentCollection.class.isAssignableFrom(collection.getClass())) {
            // if it's not a Hibernate PersistentBag, it's initialized.
            init = true;
        } else if ( !((PersistentCollection)collection).wasInitialized() ) {
            // If Hibernate hasn't loaded the collection, it't not initialized
            init = false;
        } else {
            // Assume Positive Intent.
            init = true;
        }
        return init;
    }
    
    /**
     * Copies the transient attributes from one entity to another.  This 
     * is needed when we save an Entity, because the 
     * {@link BaseDao#save(Persistable)} returns a copy of the original entity,
     * refreshed from the database, which can cause the transient attributes to
     * be lost.
     * @param source the source entity
     * @param dest the destination entity.
     * @throws IllegalStateException if we can't get one of the values.
     */
    public static void copyTransientData(PrunableEntity source, PrunableEntity dest) {
        List<Field> fields = obtainFields(source.getClass());
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
     * loaded, etc.
     * <p>
     * This method also makes sure that any field that is a proxy is 
     * initialized.
     * <p>
     * This method can only be called within a session, or we'll get lazy 
     * loading errors.
     * @param entity the {@link PrunableEntity} entity to populate
     * @param depth the depth to populate to.  1 for just the entity, 2 for
     *        children, etc.
     */
    public static void populateToDepth(PrunableEntity entity, int depth) {
        if ( entity == null || depth < 1 ) {
            return;
        }
        // Loop through fields.  Make sure proxies are initialized, and if the
        // value is a collection, and depth > 1, also fetch children.
        List<Field> fields = obtainFields(entity.getClass());
        for ( Field f : fields ) {
            try {
                if ( PrunableEntity.class.isAssignableFrom(f.getType()) ) {
                    Object value = getValue(f, entity);
                    if ( value instanceof HibernateProxy ) {
                        LazyInitializer initializer = ((HibernateProxy) value).getHibernateLazyInitializer();
                        initializer.initialize();
                    }
                } else if ( depth > 1 && Collection.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Collection<?> collection;
                    collection = (Collection<?>)f.get(entity);
                    if ( collection != null ) {
                        for ( Object value : collection ) {
                            // the iterator causes the children to be loaded. 
                            // We need the recursive call to de-proxy.
                            if ( PrunableEntity.class.isAssignableFrom(value.getClass()) ) {
                                // child needs one less than parent
                                populateToDepth((PrunableEntity)value, depth-1);
                            }
                        }
                    }
                }
            } catch (IllegalStateException e) {
                String msg = null;
                msg = "Entity " + entity + " has an inaccessable " +
                "attribute: " + f.getName();
                LOG.warn(msg);
                throw new IllegalStateException(msg);
            } catch (IllegalAccessException e) {
                String msg = null;
                msg = "Entity " + entity + " has an inaccessable " +
                "attribute: " + f.getName();
                LOG.warn(msg);
                throw new IllegalStateException(msg);
            } catch (SecurityException e) {
                String msg = null;
                msg = "Entity " + entity + " has an inaccessable " +
                "attribute: " + f.getName();
                LOG.warn(msg);
                throw new IllegalStateException(msg);
            } catch (IllegalArgumentException e) {
                String msg = null;
                msg = "Entity " + entity + " has an inaccessable " +
                "attribute: " + f.getName();
                LOG.warn(msg);
                throw new IllegalStateException(msg);
            } catch (InvocationTargetException e) {
                String msg = null;
                msg = "Entity " + entity + " has an inaccessable " +
                "attribute: " + f.getName();
                LOG.warn(msg);
                throw new IllegalStateException(msg);
            }
        }
    }

    /**
     * Helper method that gets all the fields of a class and it's super-classes.
     * @param clazz The class whose fields we want.
     * @return a List of fields from the given class and it's parents, up to 
     *         the Object class.
     */
    private static List<Field> obtainFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<Field>();
        if ( !clazz.equals(Object.class) ) {
            fields.addAll(obtainFields(clazz.getSuperclass()));
        }
        Field[] arr = clazz.getDeclaredFields();
        for ( int i=0 ; i < arr.length; i++ ) {
            fields.add(arr[i]);
        }
        return fields;
    }
    
    /**
     * Gets the value from the given field.  We can't just use field.get 
     * because Hibernate doesn't always store the value in the field. It seems
     * to store it in some CGLIB field, using proxy methods to get to it. We,
     * therefore, need to use those same proxies, or we won't be getting the 
     * correct value.
     * @param field the field object representing the field with the value
     *        we want.
     * @param entity the entity with the value we want
     * @return the value for the given field in the given entity.
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private static Object getValue(Field field, PrunableEntity entity)
                   throws SecurityException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        // we need to call the method to get the proxy...
        String name = field.getName();
        name = name.substring(0,1).toUpperCase() + name.substring(1);
        Class<?> type = field.getType();
        if ( boolean.class.isAssignableFrom(type)
                || Boolean.class.isAssignableFrom(type) ) {
            name = "is" + name;
        } else {
            name = "get" + name;
        }
        Object value = null;
        // Get the value.  We need to try using the "get" method first, but
        // not all fields will have a method (SERIAL_VERSION_UID for example),
        // so we need to get the actual field value if the method doesn't 
        // exist.
        try {
            Method method = field.getDeclaringClass().getMethod(name);
            value = method.invoke(entity);
        } catch (NoSuchMethodException e) {
            // not all fields will have a getter, so just get it from the
            // field directly
            value = field.get(entity);
        }
        return value;
    }
}
