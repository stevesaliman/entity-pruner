package com.saliman.entitypruner;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.collection.PersistentSet;
import org.hibernate.ejb.EntityManagerImpl;
import org.hibernate.impl.SessionImpl;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

/**
 * This class provides an implementation of the {@link EntityPruner} for 
 * applications using JPA and Hibernate.  It is intended to be used as a 
 * stateless session bean inside a Java EE container like GlassFish.
 * <p>
 * Entities must implement the {@link PrunableEntity} interface to be pruned
 * with this class.  This implementation is designed to work with entities 
 * that annotated at a field level.  It will still try to prune entities 
 * annotated at a method level, but you probably won't be very happy with the 
 * results.
 * <p>
 * This class is heavily dependent on the JPA provider and the types of
 * collections Entities have.  This implementation only work with Hibernate, 
 * and at the moment, child collections must be a <code>Set</code>.  In
 * Addition, the entities must use field annotations and not method 
 * annotations.
 * <p>
 * Since the EntityPruner logs its activity, we recommend Entities implement
 * a <code>toString()</code> method.
 * 
 * @see PrunableEntity
 * 
 * @author Steven C. Saliman
 */
// we need the stateless annotation to make the unit tests work.
@Stateless(name="EntityPruner")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class EntityPrunerHibernateJpa implements EntityPruner{
    /** logger for the class */
    private static final Logger LOG = Logger.getLogger(EntityPruner.class);
    
    @PersistenceContext
    private EntityManager entityManager;
    

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
     * 1) replacing Hibernate proxy objects with either their non proxy 
     *    equivalents (if they have been initialized), or <code>null</code> (if 
     *    they haven't)  This will prevent 
     *    <code>LazyInitializationException</code> when entities are serialized.<br> 
     * 2) Removal of circular references. This method tries to detect 
     *    bidirectional associations, and when found, the child's parent
     *    reference is set to null to prevent XML serialization problems.
     *    This method uses the persistence annotations to detect these 
     *    bidirectional associations, which means that if an entity contains
     *    a <code>Transient</code> collection of entities that refer back to
     *    the parent, the circular reference will remain.  It is up to the
     *    caller to make sure we don't try to prune these kinds of entities.
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
     * @param entity the {@link PrunableEntity} to pruned
     * @throws IllegalStateException if there is a problem.
     */
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void prune(PrunableEntity entity) {
        // If we have an object graph more than 999 levels deep, we've 
        // got much bigger problems than the obvious bug this hard coded
        // level will cause.
        prune(entity, 999);
    }

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
     * 1) replacing Hibernate proxy objects with either their non proxy 
     *    equivalents (if they have been initialized), or <code>null</code> (if 
     *    they haven't)  This will prevent 
     *    <code>LazyInitializationException</code> when entities are serialized.<br> 
     * 2) Removal of circular references. This method tries to detect 
     *    bidirectional associations, and when found, the child's parent
     *    reference is set to null to prevent XML serialization problems.
     *    This method uses the persistence annotations to detect these 
     *    bidirectional associations, which means that if an entity contains
     *    a <code>Transient</code> collection of entities that refer back to
     *    the parent, the circular reference will remain.  It is up to the
     *    caller to make sure we don't try to prune these kinds of entities.
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
     * This version of the <code>prune</code> method can be used to specify a 
     * maximum depth for the object.  This is handy in the case where the 
     * client only needs so many levels of an object, but more levels were 
     * populated by the server in the course of its actions inside the
     * transaction.  Note that only a collection constitutes a level, so
     * if an entity has an instance of another entity, both will be 
     * pruned and returned.
     * @param entity the {@link PrunableEntity} to pruned
     * @param depth the depth to populate to.  1 for just the entity, 2 for
     *        children, etc.
     * @throws IllegalStateException if there is a problem.
     */
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void prune(PrunableEntity entity, int depth) {
        LOG.trace("prune(PrunableEntity)");
        // First things first.  See if we've already started this one
        if ( entity == null || entity.isPruned() ) {
            return;
        }
        // When we prune children, they may cause recursive calls to prune.
        // Mark this as pruned so those recursive calls don't attempt to do it
        // again.
        entity.setPruned(true);
        
        // We can't use the entity's toString() because some entities use 
        // parent objects in their toString() methods, which could be 
        // uninitialized proxies.
        String msg = "Error pruning " + entity.getClass() + " " + 
                     entity + ": ";
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
                        // If this is another Prunable entity, de-proxy it, 
                        // then set the field's value to the de-proxied
                        // value and prune it.
                        value = deproxy(entity, value, field.getName(), field.getType());
                        field.set(entity, value);
                        prune((PrunableEntity)value, depth);
                    } else if ( Collection.class.isAssignableFrom(field.getType()) ) {
                        // Handle Collections. We already know it's not null,
                        // but we need to replace proxy collections with
                        // non proxy collections, or possibly prune out
                        // the collection.
                        pruneCollection(entity, depth, (Collection<?>)value, field);
                    }
                    // the implied else from above is that the value is an 
                    // object that doesn't need pruning, which could be
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
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void unprune(PrunableEntity entity) {
        LOG.trace("unprune(PrunableEntity)");
        // bail if we're already un-pruned.  This avoids loops.
        if ( entity == null || !entity.isPruned() ) {
            return;
        }
        
        // We're going to need to create new Hibernate proxy objects for 
        // uninitialized collections and parent entities.  When we do it for
        // parent entities, we need a SessionImpl, which we can get from the
        // EntitiyManager.
        SessionImpl session = null;
        Object delegate = entityManager.getDelegate();
        if ( SessionImpl.class.isAssignableFrom(delegate.getClass()) ) {
            session = ((SessionImpl)delegate);
        } else if ( EntityManagerImpl.class.isAssignableFrom(delegate.getClass())){ 
            session = (SessionImpl)((EntityManagerImpl)delegate).getSession();
        } else {
            LOG.warn("Can't refresh: " + delegate.getClass() +  " Is not a Session object");
        }

        entity.setPruned(false);
        // We can't use the entity's toString() because some entities use 
        // parent objects in their toString() methods, which could be 
        // uninitialized proxies.
        String msg = "Error pruning " + entity.getClass() + " " + 
                     entity + ": ";
        try {
            List<Field> fields = loadFields(entity.getClass());
            for ( Field field : fields ) {
                field.setAccessible(true);
                Object value = getValue(field, entity);
                if ( field.getName().equals("pruned") ) {
                    // always set to be un-pruned
                    entity.setPruned(false);
                } else if ( PrunableEntity.class.isAssignableFrom(field.getType()) ) {
                    // If this is another PrunableEntity entity, restore the 
                    // proxy class.  The helper method de-prunes it if 
                    // necessary.
                    reproxy(entity, (PrunableEntity)value, field, session);
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
    private List<Field> loadFields(Class<?> clazz) {
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
     * @param depth the depth to populate the entity to.  1 for just the 
     *        entity, 2 for children, etc.
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
    private void pruneCollection(PrunableEntity entity, 
                                        int depth,
                                        Collection<?> collection,
                                        Field field) 
                 throws IllegalAccessException, IllegalStateException,
                        InvocationTargetException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        Collection newValue = null;
        
        // TODO: Add support for lists.  We can do this by using the declared 
        // type.  Lists will be in a PersistentBag
        // We only need to deal with the collection if we want a depth > 1.
        // Otherwise, we don't want any children.
        if ( depth > 1 ) {
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
        }
        if ( newValue != null ) {
            // Prune the children.
            Field childsParent = null;
            boolean looked = false;
            for ( Object child : collection ) {
                // prune each child if the child is PrunableEntity
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
                    // prune each child, but since the child is one level
                    // down, prune it to 1 less depth.
                    prune((PrunableEntity)child, depth-1);
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
    private void unpruneCollection(PrunableEntity entity, 
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
            if ( (a == null) && (entity.isPersistent() ) ) {
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
    private Field loadChildsParentField(PrunableEntity entity, Field field,
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
    private Object getValue(Field field, PrunableEntity entity)
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
     * @see #getValue(Field, Persistable)
     */
    private void setValue(Field field, PrunableEntity entity, Object value)
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
     * <p>
     * This method also detects whether or not a proxy has been initialized.
     * if it hasn't it will return null after storing the proxy's ID for 
     * future unpruning.
     * @param <T> The class that we are casting to.
     * @param entity the entity containing the value we are de-proxying in
     * @param value The object to de-proxy
     * @param fieldName the name of the field we're looking at.
     * @param entityClass The class to cast to
     * @return the original object, cast to the entity class, or null if the 
     *         value represents an uninitialized proxy.
     * @throws ClassCastException If we can't make the cast.
     */
    private <T> T deproxy(PrunableEntity entity, Object value, String fieldName, 
            Class<T> entityClass) throws ClassCastException {
        if ( value instanceof HibernateProxy ) {
            LazyInitializer initializer = ((HibernateProxy) value).getHibernateLazyInitializer();
            if ( !initializer.isUninitialized() ) {
                return entityClass.cast(initializer.getImplementation());
            } 
            // This means we have an uninitialized proxy object.  We need
            // to prune it out to avoid lazy load problems, but we need to 
            // record the fact that it did have a value for unpruning later.
            Serializable proxyEntityId = initializer.getIdentifier();
            Map<String, Object> fieldIdMap = entity.getFieldIdMap();
            if ( fieldIdMap == null ) {
                fieldIdMap = new HashMap<String, Object>();
                entity.setFieldIdMap(fieldIdMap);
            }
            fieldIdMap.put(fieldName, proxyEntityId);
            return null;
        }
        return entityClass.cast(value);
     }

    /**
     * Helper method used to replace null values with uninitialized proxy 
     * instances if necessary.
     * @param entity The entity containing the value we are re-proxying.
     * @param value The object to re-proxy.
     * @param field the field we are looking at.
     * @param session a Hibernate SessionImpl, used to ask Hibernate for proxy
     * objects.
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws ClassCastException if the fieldIdMap contains a non-serializable
     * ID.
     */
    private void reproxy(PrunableEntity entity, PrunableEntity value,
            Field field, SessionImpl session) throws IllegalArgumentException, IllegalAccessException {
        field.setAccessible(true);
        // if value, we got good data, it means the client gave us real data,
        // unprune it.
        // if no value was sent, see if we have a parent id.  if we have one, 
        // it means an uninitialized proxy was stripped out on the way to the 
        // client, so don't allow changes - just restore the proxy.  If we 
        // don't have an ID it means we either never had data, or we had 
        // fetched data and the client deleted it.  In either case, null is the
        // correct new value.
        if ( value != null ) {
            unprune((PrunableEntity)value);
        } else {
            Serializable proxyEntityId = null;
            Map<String, Object> fieldIdMap = entity.getFieldIdMap();
            if ( fieldIdMap != null ) {
                proxyEntityId = (Serializable)fieldIdMap.get(field.getName());
            }
            if ( proxyEntityId != null ) {
                // We know it is lazy fetched because the pruner wouldn't have 
                // stored the id otherwise.  When I am in a less lazy mood, I'll 
                // look at annotations to determine nullability.
                Object newValue = null;
                newValue = session.internalLoad(field.getType().getName(), 
                                                proxyEntityId, false, true);
                field.set(entity, newValue);
            } else {
                field.set(entity, null);
            }
        }
    }
}
