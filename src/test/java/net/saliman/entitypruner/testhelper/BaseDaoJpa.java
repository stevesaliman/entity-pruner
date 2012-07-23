package net.saliman.entitypruner.testhelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.Transient;

import net.saliman.entitypruner.EntityUtil;
import net.saliman.entitypruner.Options;
import net.saliman.entitypruner.ReflectionUtil;
import org.hibernate.Session;
import org.hibernate.TransientObjectException;
import org.hibernate.ejb.EntityManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the JPA implementation of {@link BaseDao}.  Generally, one can
 * assume that the container will inject an EntityManager, though applications
 * running outside the container may need to worry about joining transactions.
 * <p>
 * DAOs should not be marked with transaction attributes themselves, but should
 * participate in transactions defined at a higher level, such as the Workflow.
 * <p>
 * This class assumes that there is a persistence unit in the persistence.xml
 * named <b>default</b>.  If that really needs to be overridden, child classes
 * can get a different EntityManager for a different persistence unit. The 
 * child class will need to set the EntityManager in this base class to the new
 * one for methods in this class to work properly.  We strongly recommend
 * creating applications with only one persistence unit named "default"  The 
 * only time you should need to change this is to add a second persistence
 * unit, in cases where it just can't be avoided.
 * 
 * @see BaseDao
 * @param <T> The specific {@link Persistable} that this DAO uses.
 *  
 * @author Steven C. Saliman
 */
public class BaseDaoJpa<T extends BaseEntity> implements BaseDao<T> {
    private static final Logger LOG = LoggerFactory.getLogger(BaseDaoJpa.class);
    private Class<T> entityClass;
    // Entity Managers are not generally thread safe, but the Spring shared
    // EntityManager is, so this is safe if we're using Spring. It's also
    // thread safe in a JavaEE container like GlassFish.
    @PersistenceContext(unitName="default")
    protected EntityManager entityManager;
    
    
    /**
	 * Default constructor.  It figures out what the actual Entity class is so 
	 * that the find methods work.
	 */
	public BaseDaoJpa() {
	    // Some containers, like GlassFish 3, create subclasses of our 
	    // classes when they create beans.  For this reason, we need to
	    // walk up the class hierarchy until we find the generic argument
	    // that tells us our entity class.
        // We'll typically get 2 cases here.
        // 1. We've extended with an actual Entity class, such as
        //    Employee
        // 2. We've extended with BaseEntity itself.
	    Class<?> currentClass = getClass();
		Class<?> parentClass = getClass().getSuperclass();
		while ( parentClass != null && entityClass == null ) {
			if ( parentClass.getTypeParameters().length > 0 ) {
				Type c = currentClass.getGenericSuperclass();
				Type type = ((ParameterizedType)c).getActualTypeArguments()[0];
				if ( Class.class.isAssignableFrom(type.getClass()) ) {
					entityClass = (Class<T>)type;
				} else {
					entityClass = (Class<T>)((ParameterizedType)type).getRawType();
				}	        
			}
			currentClass = parentClass;
			parentClass = parentClass.getSuperclass();
		}
	}

	@Override
	public Long countAll() {
		LOG.trace("countAll()");
		Long count = null;
		String sql = "select count(*) from " + entityClass.getSimpleName();
		Query query = entityManager.createQuery(sql);
		count = (Long)query.getSingleResult();
		return count;
	}

	@Override 
	public List<T> findAll() {
		return findAll(null);
	}
	
	@Override
	public List<T> findAll(Map<String, String> options) {
        LOG.trace("findAll(Map<String, String>)");
        List<T> results = null;
        
        int pageSize = initializePageSize(options);
        int firstRow = initializeFirstRow(options, pageSize);
        String orderClause = initializeOrder(options);

        String sql = "select o from " + entityClass.getSimpleName() + " o " +
                     orderClause;
        
        Query query = entityManager.createQuery(sql);
        // If we've got paging, use it.
        if ( pageSize > 0 && firstRow > -1 ) {
        	query.setFirstResult(firstRow);
        	query.setMaxResults(pageSize);
        }
        // I think it is safe to assume that findAll won't be called for large
        // tables.  It will probably be used for small relatively static tables.
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);
        results = query.getResultList();
        return results;
	}

	@Override
	public T findById(BigInteger id) {
	    LOG.trace("findById(Integer)");
	    T results = null;
	    results = entityManager.find(entityClass, id);
	    return results;
    }

	/**
     * This method will flush to the database in order for the database to 
     * populate default values and fire triggers.  We then need to refresh the
     * object so we have all the trigger-based data. This will be critical if
     * we need to re-save later.
     * <p>
     * Users of this method will want to be awar of a 
     * <a href=https://hibernate.onjira.com/browse/HHH-5855>Hibernate Bug</a>
     * that can cause double inserts under certain conditions.  If you are:<br>
     * <ol>
     * <li>Using MySql for your database</li>
     * <li>Using IDENTITY for id generation (as the framework does)</li>
     * <li>Saving a parent object with a new child entity</li>
     * <li>Using a {@code List} for the child collection instead of a 
     *     {@code Set}
     * <li>Using Lazy loading instead of Eager fetching</li>
     * </ol>
     * Then Hibernate will attempt to insert the child record into the database
     * twice.  Fortunately, there is a workaround.  If the child is added to
     * to the beginning of the collection instead of the end 
     * {@code parent.getChildren().add(0, child)} instead of 
     * {@code parent.getChildren().add(child)}), Hibernate doesn't seem to 
     * exhibit this behavior. This is known to be an issue as of Hibernate 
     * 3.6.9.  It is not known if this is still an issue in later releases.
     */
	@Override
    public T save(T entity) {
        LOG.trace("save(T)");
        T tmp = entity;
        // if we have an id, it's merge, else persist.
        if ( entity.getId() != null ) {
            // The entityManager is supposed to be smart enough to figure
            // out the object state on a merge call, so persist may become
            // unnecessary
            LOG.debug("     merge()");
            tmp = (T)entityManager.merge(entity);
            entityManager.flush();
            // Hibernate issue.  See:
            // http://forum.hibernate.org/viewtopic.php?p=2342895&sid=227fda0ec04d2e291e59d2d843cb0ba7
            Object delegate = entityManager.getDelegate();
            if ( Session.class.isAssignableFrom(delegate.getClass()) ) {
                ((Session)delegate).refresh(tmp);
            } else if ( EntityManagerImpl.class.isAssignableFrom(delegate.getClass())){ 
                ((EntityManagerImpl)delegate).getSession().refresh(tmp);
            } else {
                LOG.warn("Can't refresh: " + delegate.getClass() +  " Is not a Session object");
            }
            EntityUtil.copyTransientData(entity, tmp);
        } else {
            LOG.debug("     persist()");
            // When we try to save a new child pointing to a new parent, 
            // Hibernate has a valid issue.  MySql will throw a 
            // PersistenceException, Oracle will throw an IllegalStateException
            // MySql's idea is better.   Let's be consistent and re-wrap 
            // Oracle's TransientObjectException into a PersistenceException.
            try {
            entityManager.persist(entity);
            entityManager.flush();
            entityManager.refresh(tmp);
            } catch (IllegalStateException ise) {
            	// see if this is the specific example from above...
            	Throwable cause = ise.getCause();
            	if ( TransientObjectException.class.isAssignableFrom(cause.getClass()) ) {
            		throw new PersistenceException(cause.getMessage(), cause);
            	}
            	// otherwise, just throw what we got.
            	throw ise;
            }
        }
        return tmp;
    }

	@Override
    public void save(Collection<T> entites) {
        LOG.trace("save(Collection<T>");
        for ( T entity : entites ) {
            save(entity);
        }
    }

	@Override
    public void save(T[] entities) {
        LOG.trace("save(T[])");
        for ( int i = 0; i < entities.length; i++ ) {
            save(entities[i]);
        }
    }

    /**
     * Deletes a {@link Persistable} from the database. If the delete
     * was successful, the entity's ID will be null. This method will throw
     * exceptions if we can't delete for some reason.  One thing in particular
     * that can happen is a persistence exception when deleting an entity with
     * unidirectional children.  In that case, Hibernate will try to 
     * disassociate the children from the parent, rather than deleting them
     * like it would with bidirectional children.  This can cause constraint
     * violations on the children when the child's parent id is required.
     * <p>
     * This version of the delete method deletes the entity with the given
     * id from the table that corresponds to the entity class that we given 
     * to the BaseDaoJpa subclass.  Care must be taken when extending this 
     * class. If the class is extended with an Entity Class that doesn't map 
     * to a table, this method will throw exceptions because of a lack of a 
     * table.  Subclasses that are created for abstract entities should 
     * override this method.  They should also override the
     * {@link #delete(Persistable)} method to use a specific class, or perhaps
     * the class of whatever entity is passed in.
     * @param id the id of the Entity to delete.
     * @see BaseDao#save(Persistable)
     */
	@Override
    public void delete(BigInteger id) {
        LOG.trace("delete(Integer)");
        // no need to delete the entity if it was never saved.
        if ( id != null ) {
            // We may have been given a detached entity. If so, we need to
            // get a managed object before we can delete.  The best way to 
            // do this is to re-query and delete that instance.
            // Doing a refresh would re-query anyway, but findById avoids 
            // problems caused by dehydration/rehydration.
            T tmp = (T)entityManager.getReference(entityClass, id);
            entityManager.remove(tmp);
            entityManager.flush();
        }
    }

    /**
     * Deletes a {@link Persistable} from the database. If the delete
     * was successful, the entity's ID will be null. This method will throw
     * exceptions if we can't delete for some reason.  One thing in particular
     * that can happen is a persistence exception when deleting an entity with
     * unidirectional children.  In that case, Hibernate will try to 
     * disassociate the children from the parent, rather than deleting them
     * like it would with bidirectional children.  This can cause constraint
     * violations on the children when the child's parent id is required.s
     * @param entity the object to delete.
     * @see BaseDao#save(Persistable)
     */
    @Override
    public void delete(T entity) {
        LOG.trace("delete(T)");
        // no need to delete the entity if it was never saved.
        if ( entity.getId() != null ) {
            delete(entity.getId());
            entity.setId(null);
        }
    }

    @Override
    public void delete(Collection<T> entities) {
        LOG.trace("delete(Collection<T>");
        for ( T entity : entities ) {
            delete(entity);
        }
    }

	@Override
    public void delete(T[] entities) {
        LOG.trace("delete(T[])");
        for ( int i = 0; i < entities.length; i++ ) {
            delete(entities[i]);
        }
    }

    /**
     * Evicts a {@link Persistable} instance from the JPA session.  JPA will
     * try to persist any changes to an entity whether 
     * {@link #save(Persistable)} is called or not, so if changes are made
     * that you don't want saved, you'll have to use the evict method to purge
     * it from the session.
     * <p>
     * This method should be used with care, because evicted entities can
     * cause problems with future database calls which will result in 
     * "detached entity" type messages.
     * @param entity the entity to evict.
     */
	@Override
    public void evict(T entity) {
	    LOG.trace("evict(T)");
        Object delegate = entityManager.getDelegate();
        if ( Session.class.isAssignableFrom(delegate.getClass()) ) {
            ((Session)delegate).evict(entity);
        } else if ( EntityManagerImpl.class.isAssignableFrom(delegate.getClass())){ 
            ((EntityManagerImpl)delegate).getSession().evict(entity);
        } else {
            LOG.warn("Can't evict: " + delegate.getClass() +  " Is not a Session object");
        }

    }

    /**
	 * Helper method subclasses can use to create a JPA Query. By using this
	 * helper, subclasses don't need to know how to set up paging.
	 * @param sql the JPQA query to execute
	 * @param options a Map containing paging and ordering options.
	 * @return whatever results are found.
	 */
	protected Query createQuery(String sql, Map<String, String> options) {
    	LOG.trace("executeQuery(String, Map<String, String>)");
        int pageSize = initializePageSize(options);
        int firstRow = initializeFirstRow(options, pageSize);
        String orderClause = initializeOrder(options);
        Query query = entityManager.createQuery(sql + " " + orderClause);
        // If we've got paging, use it.
        if ( pageSize > 0 && firstRow > -1 ) {
        	query.setFirstResult(firstRow);
        	query.setMaxResults(pageSize);
        }
        return query;

	}
	
    /**
     * Helper method to initialize the PageSize parameter with the 
     * value from the <b>per_page</b> option.  If there is no per_page option
     * present, or the page size is invalid, we return -1 to indicate all 
     * rows should be returned.
     * <p>
     * Applications may override this method if they want subclasses to use
     * a different set of rules in their DAOs.
     * @param options The options map.
     * @return The number of rows per page.
     */
    protected int initializePageSize(Map<String, String> options) {
        int pageSize = -1; 
        if ( options != null && options.containsKey(Options.PER_PAGE)) {
        	try {
        		pageSize = Integer.parseInt(options.get(Options.PER_PAGE));
        	} catch (NumberFormatException e ) {
        		LOG.info(options.get(Options.PER_PAGE) +
        				             " is an invalid value for 'per_page'. " +
        				             "Using default");
        		pageSize = -1;
        	}
        }
        if ( pageSize < 1 ) {
        	pageSize = -1;
        }
        return pageSize;
    }
    
    /**
     * Helper method to calculate the firstRow offset based on the page
     * number in the options and the value of the pageSize parameter. If there
     * is no pageSize, then the page option doesn't make sense, and -1 
     * will be returned.  If there is a pageSize, but no page, then this 
     * method assumes the first row is 1.
     * @param options The options map.
     * @param pageSize the value of the pageSize parameter
     * @return The number of rows per page.
     */
    protected int initializeFirstRow(Map<String, String> options, int pageSize) {
        // let's figure out our fast returns... no per_page = no page.
    	if ( pageSize < 1 ) {
        	return -1;
        }
    	// per_page with no page = 0.
        if ( options == null || !options.containsKey(Options.PAGE)) {
        	return 0;
        }
    	
    	int firstRow = -1; 
    	try {
    		firstRow = Integer.parseInt(options.get(Options.PAGE));
    		firstRow--; // convert into an offset
    	} catch (NumberFormatException e ) {
    		LOG.info(options.get(Options.PAGE) +
    				" is an invalid value for 'page'. " +
    		"Using default");
    		firstRow = -1;
    	}
    	if ( firstRow < 0 ) {
        	firstRow = 0;
        }
        
        // Now translate that into a row number.  JPA is 0 based...
        firstRow = (firstRow * pageSize);
        return firstRow;
    }

    /**
     * Helper method to figure out the order by clause. Basically this just 
     * converts the syntax.
     * 
     * @param options The options map.
     * @return the correct order by clause for SQL.
     */
    protected String initializeOrder(Map<String, String> options) {
    	StringBuffer orderBy = new StringBuffer("order by ");
    	if ( options == null || !options.containsKey(Options.ORDER) ) {
    		return "";
    	}
    	
    	String attr = null;
    	String direction = null;
    	String[] attrAndDir;
    	String[] attributes = options.get(Options.ORDER).split(","); 
    	int count = 0;
    	for ( String e : attributes ) {
    		attrAndDir = e.trim().split(":");
    		attr = attrAndDir[0];
    		if ( attrAndDir.length > 1 ) {
    		    direction = attrAndDir[1];
    		} else {
    			direction = null;
    		}
    		if ( validateAttribute(attr) ) {
    			if ( attr.endsWith(".") ) {
    				attr = attr.substring(0, attr.length()-1);
    			}
    			orderBy.append(attr);
    			count++;
    			if ( direction != null && direction.equals("desc") ) {
    				orderBy.append(" desc");
    			}
    			orderBy.append(", ");
    		}
    	}  
    	// Sanity check = did we get anything?
    	if ( count == 0 ) {
    		return "";
    	}
    	// Strip out the last "," and the space
    	orderBy.deleteCharAt(orderBy.length()-1);
    	orderBy.deleteCharAt(orderBy.length()-1);

    	return orderBy.toString();
    	
	}

    /**
     * Helper for a helper.  This method takes a dot notated attribute name,
     * walks the hierarchy, and makes sure that 1) All parts of the name 
     * are valid, and 2) That they are tied to the database.  Since JPA 
     * assumes that an attribute with no annotations has a database column
     * of the same name, we'll make the same assumption here and only reject
     * attributes that are either invalid field names, or explicitly marked
     * as transient.
     * @param name the dot notated attribute, like documentType.code
     * @return whether or not the name is legit.
     */
    private boolean validateAttribute(String name) {
		// Determine if the attribute is valid by walking the attribute
		// Hierarchy and checking its annotations. JPA assumes that 
		// unannotated fields have a matching column in the database, so 
		// unless we've explicitly marked a field as transient, we'll 
		// assume an ordering if valid.
		String[] parts = name.split("\\.");
		// assume valid until we've proven otherwise...
		boolean valid = true;
		Class<?> curClass = entityClass;
		Field f = null;
		Annotation a = null;
		int i = 0;
		while ( valid && i < parts.length ) {
			f = ReflectionUtil.getField(curClass, parts[i]);
			if ( f == null ) {
				valid = false; // invalid attr.
			} else {
				a = f.getAnnotation(Transient.class);
				if ( a != null ) {
					valid = false;
				}
				curClass = f.getType();
			}
			i++;
		}
		return valid;
    }

    /**
     * Helper method that gets all the persisted fields of the
     * class we are querying. This is used by the findByExample method, and is
     * different than {@link ReflectionUtil#loadBeanFields(Class)}.
     * @param clazz The class whose fields we want.
     * @return a List of fields from the given class and it's parents, up to 
     *         the Object class.
     */
    private List<Field> loadFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<Field>();
        if ( !clazz.equals(Object.class) ) {
            fields.addAll(loadFields(clazz.getSuperclass()));
        }
        Field[] arr = clazz.getDeclaredFields();
        for ( int i=0 ; i < arr.length; i++ ) {
            Field f  = arr[i];
            int modifiers = f.getModifiers();
            Annotation a = f.getAnnotation(Transient.class);
            // Ignore it if it's transient or it's a constant, or if it's
            // static.  Why would we persist a static?
            if ( a == null && !Modifier.isFinal(modifiers) &&
                    !Modifier.isStatic(modifiers)) {
                fields.add(f);
            }
        }
        return fields;
    }
}
