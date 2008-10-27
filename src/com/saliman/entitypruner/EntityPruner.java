package com.saliman.entitypruner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.collection.PersistentSet;
import org.hibernate.proxy.HibernateProxy;

/**
 * This class &quot;prunes&quot; entities so they can be serialized or 
 * Marshalled for use in Web Service and RMI calls. 
 * <p>
 * Entities must implement the {@link PrunableEntity} interface to be pruned
 * with this class.  The <code>EntityPruner</code> is also designed to work
 * with entities that annotated at a field level.  It will still try to prune
 * entities annotated at a method level, but you probably won't be very 
 * happy with the results.
 * <p>
 * This class is heavily dependent on the JPA provider and the types of
 * collections Entities have.  At the moment, the methods in this class only
 * work with Hibernate, and child collections must be a <code>Set</code>.  In
 * Addition, the entities must use field annotations and not method 
 * annotations.
 * 
 * @author Steven C. Saliman
 */
public class EntityPruner {
    /** logger for the class */
    private static final Logger LOG = Logger.getLogger(EntityUtil.class);

    /**
     * Prune the given entity to prepare it for serializing for RMI, or
     * Marshalling to XML for SOAP or REST. It is very important that this 
     * happens outside a transaction, otherwise the JPA provider will assume
     * that changes made during pruning need to be saved.  This is not a 
     * problem in a Spring based application, because Spring will only create 
     * the transaction when it is told, but EJB containers, such as GlassFish, will
     * create a default transaction when the service endpoint is invoked, 
     * unless the 
     * <code>TransactionAttribute(TransactionAttributeType.NEVER)<code> 
     * annotation is present in the endpoint class.<br>
     * In unit tests, the EntityManager.clear method should be called to make
     * sure we are dealing with detached entities.
     * Pruning basically means 3 things:<br>
     * 1) replacing uninitialized collections with <code>null</code> 
     *    and replacing all initialized proxy lists with regular Lists to
     *    avoid <code>LazyInitializationException</code> issues during
     *    serialization.  If a collection has been initialized, then this 
     *    method should iterate through the child collection and prune each 
     *    entity in the collection.  This method should also prune any parent 
     *    entities.<br>
     * 2) Replacement of any Hibernate proxy objects with the actual entity.
     *    This could mean replacing Hibernate's proxy collections with actual
     *    Set or List collections, or it could mean replacing CGLib enhanced
     *    classes with the original entity classes they were wrapping.<br>
     * 3) Removal of circular references. This method tries to detect 
     *    bidirectional associations, and when found, the child's parent
     *    reference is set to null to prevent XML serialization problems.
     *    This method uses the persistence annotations to detect these 
     *    bidirectional associations, which means that if an entity contains
     *    a <code>Transient</code> collection of entities that refer back to
     *    the parent, the circular reference will remain.  It is up to the
     *    caller to make sure we don't try to prune these kinds of entities.
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
    public static void prune(PrunableEntity entity) {
        LOG.trace("prune(PrunableEntity)");
        // First things first.  See if we've already started this one
        if ( entity == null || entity.isPruned() ) {
            return;
        }
        // When we prune children, they may cause recursive calls to prune.
        // Mark this as pruned so those recursive calls don't attempt to do it
        // again.
        entity.setPruned(true);
        
        String msg = "Error pruning " + entity + ": ";
        try {
            List<Field> fields = loadFields(entity.getClass());
            for ( Field field : fields ) {
                field.setAccessible(true);
                Object value = getValue(field, entity);
                if ( value != null ) {
                    if ( field.getName().equals("pruned") ) {
                        // we're always pruned, this is a no-op
                        entity.setPruned(true);
                    } else if ( PrunableEntity.class.isAssignableFrom(value.getClass()) ) {
                        // If this is another Entity, de-proxy it, dehydrate 
                        // it, then set the field's value to the de-proxied
                        // value
                        value = deproxy(value, field.getType());
                        field.set(entity, value);
                        prune((PrunableEntity)value);
                    } else if ( Collection.class.isAssignableFrom(field.getType()) ) {
                        // Handle Collections. We already know it's not null,
                        // but we need to replace proxy collections with
                        // non proxy collections.
                        pruneCollection(entity, (Collection<?>)value, field);
                    }
                    // the implied else from above is that the value is an 
                    // object that doesn't need dehydration, which could be
                    // other entities if they don't implement PrunableEntity
                }
            }
        } catch (IllegalAccessException e) {
            msg = msg + e.getMessage();
            throw new IllegalStateException(msg, e);
        } catch (InvocationTargetException e) {
            msg = msg + e.getMessage();
            throw new IllegalStateException(msg, e);
        } catch (SecurityException e) {
            msg = msg + e.getMessage();
            throw new IllegalStateException(msg, e);
        } catch (IllegalArgumentException e) {
            msg = msg + e.getMessage();
            throw new IllegalStateException(msg, e);
        } catch (NoSuchMethodException e) {
            msg = msg + e.getMessage();
            throw new IllegalStateException(msg, e);
        }
    }

    /**
     * Un-prune the given entity so it can be saved by Hibernate.  Basically
     * this means replacing <code>null</code> collections with new 
     * <code>PersistentBag</code> objects so that hibernate doesn't try to 
     * cascade saves to children. If the entity has an initialized collection
     * of children, this method will attempt to detect if it is a bidirectional
     * relationship and set the parent in each child before un-pruning each
     * child.
     * <p>
     * This works well enough to save an entity, but not well enough to 
     * use the un-pruned entity to get previously uninitialized collections.
     * @param entity the {@link PrunableEntity} to un-prune
     * @throws IllegalStateException if something goes wrong
     */
    public static void unprune(PrunableEntity entity) {
        LOG.trace("unprune(PrunableEntity)");
        // bail if we're already un-pruned.  This avoids loops.
        if ( entity == null || !entity.isPruned() ) {
            return;
        }
        entity.setPruned(false);
        String msg = "Error un-pruning " + entity + ": ";
        try {
            List<Field> fields = loadFields(entity.getClass());
            for ( Field field : fields ) {
                field.setAccessible(true);
                Object value = getValue(field, entity);
                if ( field.getName().equals("pruned") ) {
                    // always set to be un-pruned
                    entity.setPruned(false);
                } else if ( PrunableEntity.class.isAssignableFrom(field.getType()) ) {
                    // If this is another Entity, un-prune it.
                    if ( value != null ) {
                        unprune((PrunableEntity)value);
                    }
                } else if ( Collection.class.isAssignableFrom(field.getType()) ) {
                    // un-pruning may result in a new collection.
                    unpruneCollection(entity, (Collection<?>)value, field);
                }
                // The implied else block is for objects that don't need
                // un-pruning.  Nothing needs to be done in that case
            }
        } catch (InvocationTargetException e) {
            msg = msg + e.getMessage();
            throw new IllegalStateException(msg, e);
        } catch (IllegalAccessException e) {
            msg = msg + e.getMessage();
            throw new IllegalStateException(msg, e);
        } catch (SecurityException e) {
            msg = msg + e.getMessage();
            throw new IllegalStateException(msg, e);
        } catch (NoSuchMethodException e) {
            msg = msg + e.getMessage();
            throw new IllegalStateException(msg, e);
        }
    }

    /**
     * Helper method that gets all the fields of a class and it's super-classes.
     * @param clazz The class whose fields we want.
     * @return a List of fields from the given class and it's parents.
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

    /**
     * Helper method to prune collections.  If the collection is a non
     * initialized Hibernate collection, this method will replace it with 
     * a null.  If it is initialized, it will replace it with a non Hibernate
     * HashSet.  This method will then prune each child entity in the
     * collection after first asking the child to delete its reference to the
     * parent so that we don't have circular references.  This method is not
     * perfect, but it should take care of all of the most common data modeling
     * scenarios.
     * <p>
     * At the moment, this assumes that we are using Sets and not Lists for
     * our child collections. We are also assuming Hibernate as a JPA provider.
     * @param entity the entity containing the collection to prune
     * @param collection the original collection to prune
     * @param field the field that holds this collection.
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws InvocationTargetException 
     * @throws IllegalStateException 
     * @throws NoSuchMethodException 
     * @throws IllegalArgumentException 
     * @throws SecurityException 
     */
    @SuppressWarnings("unchecked")
    private static void pruneCollection(PrunableEntity entity, 
                                        Collection<?> collection,
                                        Field field) 
                 throws IllegalAccessException, IllegalStateException,
                        InvocationTargetException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        Collection newValue = null;

        // TODO: Add support for lists.  We can do this by using the declared 
        // type.  Lists will be in a PersistentBag 
        if ( PersistentSet.class.isAssignableFrom(collection.getClass()) ) {
            if ( !((PersistentSet)collection).wasInitialized() ) {
                // non-initialized, so prune with a null.
                newValue = null;
            } else { 
                // replace PersistentSet with HashSet.
                newValue = new HashSet();
                newValue.addAll(collection);
            }
        } else {
            // Just use the existing collection.
            newValue = collection;
        }
        if ( newValue != null ) {
            // Prune the children.
            Field childsParent = null;
            boolean looked = false;
            for ( Object child : collection ) {
                // prune each child of it is a BaseEntity.
                if ( PrunableEntity.class.isAssignableFrom(child.getClass()) ) {
                    // See if the child pointed to the parent.
                    // we only need to do this once...
                    if ( childsParent == null && !looked ) {
                        looked = true;
                        childsParent = loadChildsParentField(entity, field,
                                                             child.getClass());
                    }
                    // set the child's parent to null
                    if ( childsParent != null ) {
                        childsParent.set(child, null);
                    }
                    prune((PrunableEntity)child);
                }
            }
        }
        setValue(field,entity, newValue);
    }
    
    /**
     * Helper method to un-prune collections. It replaces nulls with new 
     * proxy collections so Hibernate doesn't try to disassociate children,
     * which Hibernate will try to do if it sees that the parent went from 
     * having a child collection to not having a child collection. Hibernate
     * will also try to disassociate children if it sees that the collection is
     * not a Hibernate proxy object such as PersistentBag or PersistentSet.
     * <p>
     * If the collection is not null, this method will detect bidirectional
     * associations and re-inject the parent into each child after un-pruning
     * the child.  It is done after so that we don't try to un-prune this
     * object twice - it is entirely possible that we hit a child collection
     * before we hit the pruned field.
     * <p>
     * This method assumes Hibernate as a provider, and that Lists are used
     * for child collections and not Sets
     * @param entity the entity containing the collection to un-prune
     * @param collection the child collection to un-prune
     * @param field the field that contains the collection.
     * @throws NoSuchMethodException 
     * @throws SecurityException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws IllegalStateException
     */
    private static void unpruneCollection(PrunableEntity entity, 
                                          Collection<?> collection,
                                          Field field) 
                 throws SecurityException, NoSuchMethodException, 
                        IllegalStateException, IllegalAccessException,
                        InvocationTargetException {
        // GlassFish does some strange things when a null comes in for a
        // collection attribute.  It makes a collection that contains a null.
        // Try to detect this and set the collection to null.
        if ( collection != null ) {
            boolean hasNull = false;
            for ( Object child: collection ) {
                if ( child == null ) {
                    hasNull = true;
                }
            }
            if ( hasNull ) {
                collection = null;
            }
        }
        
        // TODO: change this method so it can handle lists as well as sets
        // this can be done by using the original field definition and choosing
        // the correct object accordingly.
        if ( collection == null ) {
            // We only want to put in an empty PersistentSet if:
            // 1. The parent is persistent(it has an id) This is safe because
            //    IDs don't change during the un-pruning process.
            // 2. The collection is persistent (not Transient).
            Annotation a = field.getAnnotation(Transient.class);
            if ( (a == null) && (entity.getId() != null) ) {
                setValue(field, entity, new PersistentSet());
            }
        } else {
            // Note that in this case, we'll have a collection that isn't
            // a Hibernate PersistentSet.  This is OK.
            Field childsParent = null;
            boolean looked = false;
            for ( Object child : collection ) {
                if ( PrunableEntity.class.isAssignableFrom(child.getClass()) ) {
                    unprune((PrunableEntity)child);
                }
                // remember this needs to come last.
                // we only need to do this once...
                if ( childsParent == null && !looked ) {
                    looked = true;
                    childsParent = loadChildsParentField(entity, field,
                                                         child.getClass());
                }
                if ( childsParent != null ) {
                    childsParent.set(child, entity);
                }
            }
        }
    }

    /**
     * Helper to the helper that gets the child's parent field.
     * @param entity the entity containing the child
     * @param field the field containing the child
     * @param child the class of the child
     */
    private static Field loadChildsParentField(PrunableEntity entity, 
                                                 Field field,
                                                 Class<?> childClazz) {
        Field childsParent = null;
        String mappedBy = null;
        String msg = null;
        // See if the current method has a OneToMany or OneToOne
        // annotation with a "mappedBy" with indicates a 
        // Bidirectional association.
        Annotation a = field.getAnnotation(OneToMany.class);
        if ( a != null ) {
            mappedBy = ((OneToMany)a).mappedBy();
        } else {
            a = field.getAnnotation(OneToOne.class);
            if ( a != null ) {
                mappedBy = ((OneToOne)a).mappedBy();
            }
        }
        // if we got an annotation with a "mappedBy", process it
        if ( (mappedBy != null) && (mappedBy.length() > 0) ) {
            // convert case
            try {
                Class<?> currClazz = childClazz;
                // We can't use getField for a private field...
                while ( childsParent == null && !currClazz.equals(Object.class) ) {
                    Field[] fields = currClazz.getDeclaredFields();
                    for ( int i = 0; i < fields.length; i++ ) {
                        if  ( fields[i].getName().equals(mappedBy) ) {
                            childsParent = fields[i];
                            childsParent.setAccessible(true);
                            break;
                        }
                    }
                }
                if ( childsParent == null ) {
                    msg = "Entity " + entity + " has a child collecion " +
                          "marked as bidrectional, but the child's parent " +
                          "attribute (" + mappedBy + ") can't be found";
                    LOG.warn(msg);
                    throw new NullPointerException(msg);
                }
                if ( !entity.getClass().isAssignableFrom(childsParent.getType()) ) {
                    msg = "Entity " + entity + " has a child collecion " +
                          "marked as bidrectional, but the child's parent " +
                          "attribute (" + mappedBy + ") is the wrong type";
                    LOG.warn(msg);
                    throw new NullPointerException(msg);
                }
            } catch (SecurityException e) {
                LOG.warn("Entity " + entity + " has a child collecion " +
                         "marked as bidrectional, but the child doesn't " +
                         "have a public field for the " + mappedBy +
                         " attribute");
            }
        }
        return childsParent;
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
    
    /**
     * Sets the value of the given field in the given entity. We can't just 
     * use field.set because Hibernate doesn't always store the value in the 
     * field. It seems to store it in some CGLIB field, using proxy methods to
     * get to it. We, therefore, need to use those same proxies, or we won't 
     * be setting the correct value.
     * @param field the field we want to set.
     * @param entity the entity we want to change
     * @param value the value to set.
     * @throws SecurityException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     * @see #getValue(Field, BaseEntity)
     */
    private static void setValue(Field field, PrunableEntity entity, Object value)
                   throws SecurityException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        String name = field.getName();
        name = name.substring(0,1).toUpperCase() + name.substring(1);
        name = "set" + name;
        // Try to set the value using the "set" method first.
        try {
            Method method = field.getDeclaringClass().getMethod(name, field.getType());
            method.invoke(entity, new Object[] {value});
        } catch (NoSuchMethodException e) {
            field.set(entity, value);
        }
    }
    
    /**
     * Helper method to replace proxy objects with actual classes. This is
     * needed because Flex won't know how to map a proxy class to a Flex 
     * entity, and even if it did, there would be a lot of extra data that 
     * Flex doesn't need.
     * @param <T> The class that we are casting to.
     * @param maybeProxy The object to de-proxy
     * @param entityClass The class to cast to
     * @return the original object, cast to the entity class.
     * @throws ClassCastException If we can't make the cast.
     */
    private static <T> T deproxy(Object maybeProxy, Class<T> entityClass) throws ClassCastException {
        if ( maybeProxy instanceof HibernateProxy ) {
           return entityClass.cast(((HibernateProxy) maybeProxy).getHibernateLazyInitializer().getImplementation());
        }
        return entityClass.cast(maybeProxy);
     }

}
