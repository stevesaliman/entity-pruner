package com.saliman.entitypruner.testhelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.ejb.EntityManagerImpl;

import com.saliman.entitypruner.EntityUtil;
import com.saliman.entitypruner.PrunableEntity;

/**
 * This is the JPA implementation of {@link BaseDao}.  Generally, one can
 * assume that the container will inject an EntityManager, though applications
 * running outside the container may need to worry about joining transactions.
 * <p>
 * DAOs should not be marked with transaction attributes themselves, but should
 * participate in transactions defined at a higher level, such as the Workflow. 
 * @see BaseDao
 * @param <T> The specific {@link BaseEntity} that this DAO uses.
 *  
 * @author Steven C. Saliman
 */
public class BaseDaoJpa<T extends BaseEntity> implements BaseDao<T> {
    private static final Logger LOG = Logger.getLogger(BaseDaoJpa.class);
    private Class<T> entityClass;
    // Entity Managers are not generally thread safe, but the Spring shared
    // EntityManager is, so this is safe if we're using Spring. It's also
    // thread safe in a JavaEE container like GlassFish.
    @PersistenceContext
    protected EntityManager entityManager;
    
    /**
	 * Default constructor.  It figures out what the BaseEntity class is so 
	 * that the find methods work.
	 */
	public BaseDaoJpa() {
	    // We'll typically get 2 cases here.
	    // 1. We've extended with an actual BaseEntity class, such as
	    //    Employee
	    // 2. We've extended with BaseEntity itself.
	    Type c = getClass().getGenericSuperclass();
	    Type type = ((ParameterizedType)c).getActualTypeArguments()[0];
	    if ( Class.class.isAssignableFrom(type.getClass()) ) {
	        entityClass = (Class<T>)type;
	    } else {
	        entityClass = (Class<T>)((ParameterizedType)type).getRawType();
	    }
	}

	/**
	 * Sets the EntityManager for this instance.  This is only here for 
	 * testing purposes, and should not be used in production.  In production,
	 * an entityManager will be injected via the PersistenceContext annotation.
	 * @return The entity manager in use
	 */
	public EntityManager getEntityManager() {
	    return entityManager;
	}
	
    /**
     * Sets the EntityManager for this instance.  This is only here for 
     * testing purposes, and should not be used in production.  In production,
     * an entityManager will be injected via the PersistenceContext annotation.
     * @param entityManager the entity manager to use
     */
	public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        LOG.info("Injected EM " + entityManager);
	}

	@Override
	public List<T> findAll() {
        LOG.trace("findAll()");
        List<T> results = null;
        Query query = entityManager.createQuery("select o from " + entityClass.getSimpleName() + " o");
        results = query.getResultList();
        return results;
	}

	@Override
	public T findById(BigInteger id) {
	    LOG.trace("findById(Long)");
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
     * This method sometimes throws surprising results.  It throws 
     * <code>EntityExistsException</code> for other situations, such as missing
     * foreign keys, or not-null violations.
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
                ((Session)entityManager.getDelegate()).refresh(tmp);
            } else if ( EntityManagerImpl.class.isAssignableFrom(delegate.getClass())){ 
                ((EntityManagerImpl)delegate).getSession().refresh(tmp);
            } else {
                LOG.warn("Can't refresh: " + delegate.getClass() +  " Is not a Session object");
            }
            EntityUtil.copyTransientData((PrunableEntity)entity, (PrunableEntity)tmp);
        } else {
            LOG.debug("     persist()");
            entityManager.persist(entity);
            entityManager.flush();
            entityManager.refresh(tmp);
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
     * Deletes a {@link BaseEntity} from the database. If the delete
     * was successful, the entity's ID will be null. This method will throw
     * exceptions if we can't save for some reason.  One thing in particular
     * that can happen is a persistence exception when saving an entity with
     * unidirectional children.  In that case, Hibernate will try to 
     * disassociate the children from the parent, rather than deleting them
     * like it would with bidirectional children.  This can cause constraint
     * violations on the children when the child's parent id is required.s
     * @param entity the object to delete.
     * @see BaseDao#save(BaseEntity)
     */
	@Override
    public void delete(T entity) {
        LOG.trace("delete(T)");
        // no need to delete the entity if it was never saved.
        if ( entity.getId() != null ) {
            // We may have been given a detached entity. If so, we need to
            // get a managed object before we can delete.  The best way to 
            // do this is to re-query and delete that instance.
            // Doing a refresh would re-query anyway, but findById avoids 
            // problems caused by dehydration/rehydration.
            T tmp = (T)entityManager.getReference(entity.getClass(), entity.getId());
            entityManager.remove(tmp);
            entityManager.flush();
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
     * Helper method that gets all the persisted fields of the
     * class we are querying. This is used by the findByExample method.
     * @param clazz The class whose fields we want.
     * @return a List of fields from the given class and it's parents, up to 
     *         the BaseEntity class.
     */
    private List<Field> loadFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<Field>();
        if ( !clazz.equals(BaseEntity.class) ) {
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
