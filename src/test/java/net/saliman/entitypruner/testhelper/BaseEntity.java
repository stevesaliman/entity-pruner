package net.saliman.entitypruner.testhelper;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import net.saliman.entitypruner.EntityPruner;
import net.saliman.entitypruner.EntityUtil;
import net.saliman.entitypruner.PrunableEntity;

/**
 * BaseEntity is the base class for all JPA Entities. An Entity represents a 
 * single entity in the application, along with its relationships with other
 * entities in the application.  An instance of an Entity will represent one 
 * row in the database. It is intended for each kind of Entity to map to a 
 * single table in the database, and it is the entity classes we annotate for 
 * use with JPA.  At the moment, the Framework assumes that Hibernate will be 
 * the JPA provider.  Using another vendor, such as TopLink, can cause things 
 * to fail.
 * <p>
 * This abstract class provides for the two data values that must be in every
 * database table: id and version. The id is the primary key of the table,
 * and must be called <b>id</b>.  This value should be null in new Entity 
 * instances.  The version column must be named <b>version</b>, and is used
 * for optimistic locking.  This class also defines the sequence generator
 * that will be used to generate IDs when records are inserted to the 
 * database.  If an Entity needs to keep track of the create and update user,
 * they should extend {@link AuditableEntity} to get the 4 audit columns as
 * well.
 * <p>
 * To use an Entity in applications, 3 things are required.<br>
 * The application will need to provide a set of Entity classes that extend 
 * this class (preferably in a package named entity). Extending classes 
 * will be required to implement <code>Serializable</code> and provide 
 * implementations of <code>Entity</code>'s  {@link BaseEntity#toString()}, 
 * {@link BaseEntity#equals(Object)}, {@link BaseEntity#hashCode()}, methods. 
 * The <code>toString</code> is required to keep logs clean, 
 * <code>equals</code> and <code>hashCode</code> are needed to keep Hibernate
 * sane. Finally, a sequence named <b>ID_GENERATOR_SEQ</b> needs to be created
 * in the database.
 * <p>
 * If an Entity needs to be sent to a remote client, either through a Web
 * Service, or RMI, the Entity will need to be pruned first to avoid Lazy 
 * Loading and circular reference issues. Objects coming in from a remote
 * client will need to be un-pruned before use. This abstract class implements
 * the {@link PrunableEntity} interface and adds the methods needed so that 
 * the {@link EntityPruner} can be used to perform this work. At the moment, 
 * the {@link EntityPruner}  assumes that collections of children are a Set, 
 * not a List.
 * <p>
 * Care should be taken when considering attributes of a primitive type. It's
 * fine if the attribute is transient, but if it is mapped to a database
 * column, there will be problems.  First and foremost, primitives can't be
 * null, so if a primitive is mapped to a database column that allows null 
 * values, there will be exceptions trying to map the row to an object.
 * primitives also cause trouble with the <code>findByExample</code> methods 
 * in DAOs.  The issue is that all non-null attributes will be used to build
 * search criteria, and primitives can't be null, so they'll always be
 * considered, using java default values for the primitive attributes.  This
 * probably won't be what you want, especially since you can't provide a 
 * wildcard for a primitive. Use of the Java wrapper classes is recommended
 * instead.
 * <p>
 * One also need to be aware of a current limitation of the 
 * {@link java.beans.Introspector} class.  It expects a reader method to 
 * start with &quot;get&quot; for all types except the primitive 
 * <code>boolean</code> type, whose readers start with &quot;is&quot;.  This
 * is important because code that tries to serialize a bean to send to a 
 * client, like BlazeDS, uses the Introspector to get the properties of a bean.
 * There is some debate on whether this is a bug or a feature, but until that
 * gets sorted out, Boolean fields should have, both a &quot;get&quot; and an
 * &quot;is&quot; method.
 * 
 * @author Steven C. Saliman
 * @see AuditableEntity
 * @see EntityUtil
 */
@MappedSuperclass
public abstract class BaseEntity implements Serializable, PrunableEntity {
    /** serial version UID */
    private static final long serialVersionUID = 4L;

    /** the default version id value for data object instances */
    private static final Long DEFAULT_VERSION = new Long(0);

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO, generator="ID_GENERATOR")
    @SequenceGenerator(name="ID_GENERATOR", sequenceName="id_generator_seq", allocationSize=1)
//    @GenericGenerator(name="ID_GENERATOR",
//                      strategy = "net.saliman.entitypruner.testhelper.BigIntegerSequenceGenerator",
//                      parameters = {
//                          @Parameter(name="sequence", value="id_generator_seq")
//                      })
    @Column(name="id", nullable=true)
    private BigInteger id;
    
    @Version
    @Column(name="version")
    private Long version;
    
    // These next 2 are for the EntityPruner
    @Transient
    private String pruningState;

    @Transient
    private Map<String, String> fieldIdMap;

    /**
     * The default constructor
     */
    public BaseEntity() {
        version = DEFAULT_VERSION;
    }

    /**
     * Returns the database id of the record This will be null until the record
     * is saved to the database
     * @return Long the database id of the record
     */
    public BigInteger getId() {
        return id;
    }

    /**
     * Set the BaseEntity's id to the given value.  This will be used
     * primarily by the JPA provider.
     * @param id The id to set.
     */
    public void setId(BigInteger id) {
        this.id = id;
    }

    /**
     * Gets the database version of this object.  This is used for optimistic
     * locking. 
     * @return Returns the version.
     */
    public Long getVersion() {
        return version;
    }

    /**
     * This method is provided for JPA so it can increment the version when
     * saving data.  It should not be used by JPA based applications, but
     * non-JPA applications must iterate this column in the database if it
     * doesn't want JPS applications overwriting its data.
     * @param version The version to set.
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Gets the pruned flag.  This flag is used by the {@link EntityPruner} to
     * determine if an object is currently pruned or not.  This will help 
     * avoid endless loops in bidirectional linked objects.
     * @return the pruned flag.
     */
    @Override
    public String getPruningState() {
        return pruningState;
    }

    /**
     * @param pruned the pruned to set
     */
    @Override
    public void setPruningState(String pruningState) {
        this.pruningState = pruningState;
    }

    /**
     * @return the fieldIdMap for the EntityPruner to use.
     */
    @Override
    public Map<String, String> getFieldIdMap() {
        return fieldIdMap;
    }

    /**
     * @param fieldIdMap the fieldIdMap to set from the EntityPruner
     */
    @Override
    public void setFieldIdMap(Map<String, String> fieldIdMap) {
        this.fieldIdMap = fieldIdMap;
    }

    /**
     * Determine if the Entity has been persisted.  For this class, and its
     * subclasses, this means it has an ID.
     * @return whether or not the Entity has been persisted.
     */
    @Override
    public boolean isPersistent() {
        return id != null;    
    }

    /**
     * Convert this Entity into a logical string representation. This makes the
     * log files easier to read.  The Commons Lang package provides a 
     * <code>ToStringBuilder<code> method that makes short work of this method.
     * Typically, the string representation includes the database id and the 
     * attributes that uniquely describe this object.  An example 
     * <code>toString()</code> method for a car might look like this:<pre>
    public String toString {
        return new ToStringBuilder(this)
                .append("id", getId())
                .append("make", make)
                .append("model", model)
                .toString();
    }
     * </pre>
     * In this example, "make" could be another Entity.
     * @return the string representation of this object.
     */
    @Override
    public abstract String toString();

    /**
     * Determine of two Entities are equal. The Commons Lang package provides
     * an <code>EqualsBuilder</code> that makes this method easier to write.
     * An example <code>equals(Object)</code> method for a car might look 
     * like this:<pre>
    public boolean equals(Object other) {
        if ( other == null ) {
            return false;
        }
        if ( other == this ) {
            return true;
        }
        if ( !other.instanceof Car ) {
            return false;
        }
        Car castOther = (Car)other;
        return new EqualsBuilder()
                .append(make, castOther.getMake())
                .append(model, castOther.getModel())
                .isEquals();
    }
     * </pre>
     * Note that the equals method does not look at the ID to make its
     * comparison.  It is a very bad idea to use the ID in the equals method
     * because it changes every time an entity is persisted for the first
     * time, making it impossible to find an item in a collection after it has
     * been saved.  You can get away with it in certain rare situations that 
     * deal with read-only objects. 
     * <p>
     * In addition to the <code>equals</code> method, Entities can have
     * a <code>KEYS</code> constant, defining an array of strings that hold
     * the names of the attributes used to determine equality. Code generators
     * will use this constant to generate <code>equals</code> methods 
     * appropriate to the target language in the generated classes using these
     * keys.  Care should be taken to make sure the attributes used in the 
     * Java <code>equals</code> implementation stay in sync with the keys
     * defined in the <code>KEYS</code> constant.
     * @param other the object to compare to this one.  Note that it is an 
     *        <code>Object</code> and not an instance of the Entity.  This
     *        is important.  If you don't use <code>Object</code> you are not
     *        overriding the <code>equals</code> method, you are 
     *        <b>Overloading</b> it, which is not the same thing.  You must 
     *        override the method for collections to behave correctly.
     * @return <code>true</code> if the objects are equal, <code>false</code>
     *         otherwise.
     */
    @Override
    public abstract boolean equals(Object other);

    /**
     * Determine the hash code for this entity.  The Commons Lang package 
     * provides a <code>HashCodeBuilder</code> that makes short work of this
     * method.  An example of a <code>HashCode</code> method for a car is:
     * <pre>
    public int hashCode() {
        return new HashCodeBuilder()
                .append(make)
                .append(model)
                .toHashCode();
    }
     * </pre> 
     * Note that the attributes used to compute the hash code are <b>exactly</b>
     * the same as the ones in <code>equals</code> This is because the general
     * contract of the equals and hashCode methods require that 
     * <code>a.hashCode() == b.hashCode()</code> is true for every a and b
     * where <code>a.equals(b)</code> is true.
     * @return a unique hash code for this class.
     */
    @Override
    public abstract int hashCode();
}
