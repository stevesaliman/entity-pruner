package net.saliman.entitypruner.testhelper;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import net.saliman.entitypruner.EntityUtil;

/**
 * AuditableEntity is a base class for JPA Entities that need to be able to
 * keep track of who created and last modified an Entity. It extends 
 * {@link BaseEntity}, adding the 4 standard audit columns: create user, 
 * create date, update user, and update date. To be auditable, a table must
 * have <b>create_user</b>, <b>create_date</b>, <b>update_user</b>, and
 * <b>update_date</b> columns.
 * <p>
 * Since this class extends {@link BaseEntity}, Entities, can extend this
 * class for use with JPA, and extending classes will need to do all the 
 * same things as entities that extend {@link BaseEntity} directly.
 * <p>
 * 
 * @author Steven C. Saliman
 * @see BaseEntity
 * @see EntityUtil
 */
@MappedSuperclass
public abstract class AuditableEntity extends BaseEntity
                                      implements Serializable {
    /** serial version UID */
    private static final long serialVersionUID = 2L;

    @Column(name="create_user")
    private String createUser;
    
    @Column(name="create_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createDate;
    
    @Column(name="update_user")
    private String updateUser;

    @Column(name="update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;
    
    /**
     * The default constructor
     */
    public AuditableEntity() {
        super();
    }

    /**
     * Returns the user that created the record
     * @return String the user that created the record.
     */
    public String getCreateUser() {
        return createUser;
    }

    /**
     * Sets the user that created the record
     * @param createUser the user who created the record
     */
    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    /**
     * Returns the date/time the record was created This attribute is used to
     * identify when the record was created It should not be modified by any
     * code. The actual value is populated by database triggers.
     * @return Date the date/time the record was created
     */
    public Date getCreateDate() {
        return createDate;
    }

    /**
     * Sets the date/time the record was created This attribute is used to
     * identify when the record was created It should not be modified by any
     * code. The actual value is populated by database triggers.
     * @param createDate the date/time the record was created
     */
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    /**
     * Returns the user that last modified the record
     * @return String the user that modified the record last
     */
    public String getUpdateUser() {
        return updateUser;
    }

    /**
     * Sets the user that last modified the record
     * @param updateUser the user that modified the record last
     */
    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    /**
     * Returns the date/time the record was last modified This attribute is used
     * to identify when the record was modified It should not be modified by any
     * code. The actual value is populated by database triggers. This attribute
     * is used by Hibernate as a timestamp for record versioning.
     * 
     * @return Date the date/time the record was last modified
     */
    public Date getUpdateDate() {
        return updateDate;
    }

    /**
     * Returns the date/time the record was last modified This attribute is used
     * to identify when the record was modified It should not be modified by any
     * code. The actual value is populated by database triggers. This attribute
     * is used by Hibernate as a timestamp for record versioning.
     * 
     * @param updateDate the date/time the record was last modified
     */
    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }
}
