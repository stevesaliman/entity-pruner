package net.saliman.entitypruner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Transient;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(EntityUtil.class);

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
     * {@link BaseDao#save(PrunableEntity)} returns a copy of the original entity,
     * refreshed from the database, which can cause the transient attributes to
     * be lost.
     * @param source the source entity
     * @param dest the destination entity.
     * @throws IllegalStateException if we can't get one of the values.
     */
    public static void copyTransientData(PrunableEntity source, PrunableEntity dest) {
        List<Field> fields = ReflectionUtil.loadBeanFields(source.getClass(), true);
        for ( Field f : fields ) {
            Annotation a = f.getAnnotation(Transient.class);
            if ( a != null ) {
                try {
                    f.setAccessible(true);
                    setValue(f, dest, getValue(f, source));
                } catch (IllegalAccessException e) {
                    String msg = null;
                    msg = "Entity " + source + " has an inaccessable " +
                       	 "or mismatched transient attribute: " + f.getName();
                    LOG.warn(msg);
                    throw new IllegalStateException(msg);
                } catch (SecurityException e) {
                    String msg = null;
                    msg = "Entity " + source + " has an inaccessable " +
                       	 "or mismatched transient attribute: " + f.getName();
                    LOG.warn(msg);
                    throw new IllegalStateException(msg);
				} catch (IllegalArgumentException e) {
                    String msg = null;
                    msg = "Entity " + source + " has an inaccessable " +
                       	 "or mismatched transient attribute: " + f.getName();
                    LOG.warn(msg);
                    throw new IllegalStateException(msg);
				} catch (InvocationTargetException e) {
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
     * Populate the given entity based on the given options.  Options are 
     * patterned after Ruby on Rails options when querying things.  This
     * method is designed to be used with the {@link EntityPruner} to provide
     * whatever a client needs from an object graph.
     * <p>
     * Supported options are:
     * <code>include</code> a comma separated list of child collections to
     * populate.  This option does not cascade into children.<br>
     * <code>select</code> a comma separated list of attributes to populate.
     * This list only has an effect on attributes that are entities
     * themselves. This option does not cascade into children.<br>
     * <code>depth</code> the minimum number of levels we want in the populated
     * entity.  Use 1 for the entity itself with all it's parents, 2 for the 
     * entity and its children, etc. If a select option is present, a depth of 0
     * can be used to avoid loading attributes that aren't wanted.
     * <p>
     * This method also makes sure that any field that is a proxy is 
     * initialized.
     * <p>
     * This method can only be called within a session, or we'll get lazy 
     * loading errors.
     * @param entity the {@link PrunableEntity} entity to populate
     * @param options the map of options that should be used.
     */
    public static void populateEntity(PrunableEntity entity, Map<String, String> options) {
        if ( entity == null ) {
            return;
        }
        // Convert the options into sets of unique keys.
        String []split = {};
        Set<String> includeSet = null;
        if ( options != null && options.containsKey(Options.INCLUDE) ) {
        	includeSet = new HashSet<String>();
        	split = options.get(Options.INCLUDE).split(",");
            for ( String i : split ) {
            	includeSet.add(i.trim());
            }
        }
        
        Set<String> selectSet = null;
        if ( options != null && options.containsKey(Options.SELECT) ) {
        	selectSet = new HashSet<String>();
        	split = options.get(Options.SELECT).split(",");
            for ( String i : split ) {
            	selectSet.add(i.trim());
            }
        }
        // If no depth was given, use a depth of one.
        int depth = 1;
        if ( options != null && options.containsKey(Options.DEPTH) ) {
        	String depthStr = options.get(Options.DEPTH);
        	try {
        		depth = Integer.parseInt(depthStr);
        	} catch(NumberFormatException nfe) {
        		throw new IllegalArgumentException(depthStr +
        				" is not a valid depth");
        	}
        }
        
        // set up the options for recursive calls.  We need to do this with 
        // a copy of the original options in case the caller wants to doo 
        // something else with the original options.
        // At the moment, only the depth is needed.  Includes and selects 
        // don't cascade.
        Map<String, String> newOptions = new HashMap<String, String>();
        if ( options != null ) {
        	newOptions.put(Options.DEPTH, Integer.toString(depth-1));
        }
        
        // Loop through fields.  Make sure proxies are initialized, and if 
        // the value is a collection, and we're interested in a collection or
        // persistable, fetch it from the database.
        // We're interested if the field is in the include or select list, or
        // if we don't have a list and the depth is > 1 for collections, > 0 
        // for persistables.
        List<Field> fields = ReflectionUtil.loadBeanFields(entity.getClass(), true);
        for ( Field f : fields ) {
            try {
            	if ( PrunableEntity.class.isAssignableFrom(f.getType())) {
            		boolean selected = selectSet != null && selectSet.contains(f.getName());
            		if ( selected || (selectSet == null && depth > 0) ) {
            			Object value = getValue(f, entity);
            			if ( value instanceof HibernateProxy ) {
            				LazyInitializer initializer = ((HibernateProxy) value).getHibernateLazyInitializer();
            				initializer.initialize();
            			}
            		}
                } else if ( Collection.class.isAssignableFrom(f.getType()) ) {
                	boolean included = includeSet != null &&  includeSet.contains(f.getName());
                	if ( included || (includeSet == null && depth > 1) ) {
                		f.setAccessible(true);
                		Collection<?> collection;
                		collection = (Collection<?>)getValue(f, entity);
                		if ( collection != null ) {
                			for ( Object value : collection ) {
                				// the iterator causes the children to be loaded. 
                				// We need the recursive call to de-proxy.
                				if ( PrunableEntity.class.isAssignableFrom(value.getClass()) ) {
                					// child needs one less than parent
                					populateEntity((PrunableEntity)value, newOptions);
                				}
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
     * Replace proxy objects with actual classes. This is needed because Flex
     * won't know how to map a proxy class to a Flex entity, and even if it 
     * did, there would be a lot of extra data that Flex doesn't need.
     * <p> 
     * The expectation is that this method, like others in this class, will 
     * be called inside of a session, so that uninitialized proxies can be
     * initialized later.
     * <p>
     * Most of the time, the class returned by a hibernate query will be 
     * correct.  The most common use case for this method is when business
     * logic wants to return entities that were obtained from other entities,
     * rather than queried directly.  For example, we query the database for
     * a department, then return the department's children.
     * @param entity the entity containing the value we are de-proxying.
     * @return the original object, cast to the entity class.
     * @throws ClassCastException If we can't make the cast.
     */
    public static PrunableEntity deproxy(PrunableEntity entity) throws ClassCastException {
        if ( entity instanceof HibernateProxy ) {
        	LazyInitializer initializer = ((HibernateProxy) entity).getHibernateLazyInitializer();
        	return (PrunableEntity)initializer.getImplementation();
        }
        return entity;
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
     * @see #getValue(Field, PrunableEntity)
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
}
