package net.saliman.entitypruner.testhelper.list;

import com.google.common.base.Objects;
import net.saliman.entitypruner.EntityPruner;
import net.saliman.entitypruner.PrunableEntity;
import net.saliman.entitypruner.testhelper.AuditableEntity;
import net.saliman.entitypruner.testhelper.BaseDaoJpa;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.Type;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import java.util.List;


/** 
 * This class only exists to test the Framework code. It tests the
 * {@link EntityPruner#prune(PrunableEntity)} and 
 * {@link EntityPruner#unprune(PrunableEntity)}
 * methods, because we really can't test those methods without seeing how
 * the {@link BaseDaoJpa} code deals with objects in various states.
 * Simply put, we aren't done with dehydrate and re-hydrate until the object
 * is saved.
 * <p>
 * It is also used to test the {@link BaseDaoJpa} functionality.
 * <p>
 * The main point here is that if a test fails it is important to know if it
 * is a problem with the DAO or with the Entity.
 * <p>
 * For the tests to work, this object needs 3 types of collections.  A 
 * Collection of objects with a Parent object, a Collection of objects with 
 * only the ID of the parent, and a Transient collection of objects.
 * <p>
 * This version of the test parent entity uses <code>List</code>s for its
 * child collections.
 * 
 * @author Steven C. Saliman
 */
@Entity
@Table(name="test_parent")
@org.hibernate.annotations.Entity(mutable=true, 
		                          dynamicInsert=true,
		                          dynamicUpdate=true,
		                          optimisticLock=OptimisticLockType.VERSION)
@Cache(usage=CacheConcurrencyStrategy.NONE)
public class TestListParentEntity extends AuditableEntity implements Serializable {
    /** Serial version ID */
    private static final long serialVersionUID = 1L;

    @Column(name="code")
    private String code;

    @Column(name="description")
    private String description;
    
    @Column(name="string_value")
    private String stringValue;
    
    @Column(name="int_value")
    private int intValue;
    
    @Column(name="double_value")
    private Double doubleValue;
    
    @Column(name="date_value")
    private Date dateValue;
   
    @Column(name="true_flag", nullable=false)
    @Type(type="yes_no")
    private Boolean affirmative;
    
    @Column(name="clob_value")
    private String clobValue;
    
    @Column(name="blob_value")
    private byte[] blobValue;
    
    @OneToMany(mappedBy="parent", fetch=FetchType.LAZY,
            cascade= {CascadeType.ALL })
    private List<TestListChildEntity> children;
    
    @OneToMany(fetch=FetchType.LAZY, cascade={CascadeType.ALL})
    @JoinColumn(name="test_parent_id")
    private List<TestListUniChildEntity> uniChildren;

    @Transient
    private List<TestListChildEntity> transChildren;

	/** default constructor */
    public TestListParentEntity() {
        super(); 
    }

    /**
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code the code to set
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

	/**
     * @return the stringValue
     */
    public String getStringValue() {
        return stringValue;
    }

    /**
     * @param stringValue the stringValue to set
     */
    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    /**
     * @return the intValue
     */
    public int getIntValue() {
        return intValue;
    }

    /**
     * @param intValue the intValue to set
     */
    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    /**
     * @return the doubleValue
     */
    public Double getDoubleValue() {
        return doubleValue;
    }

    /**
     * @param doubleValue the doubleValue to set
     */
    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    /**
     * @return the dateValue
     */
    public Date getDateValue() {
        return dateValue;
    }

    /**
     * @param dateValue the dateValue to set
     */
    public void setDateValue(Date dateValue) {
        this.dateValue = dateValue;
    }

    /**
     * @return the affirmative
     */
    public Boolean isAffirmative() {
        return affirmative;
    }

    /**
     * @param affirmative the affirmative to set
     */
    public void setAffirmative(Boolean affirmative) {
        this.affirmative = affirmative;
    }

    /**
     * @return the clobValue
     */
    public String getClobValue() {
        return clobValue;
    }

    /**
     * @param clobValue the clobValue to set
     */
    public void setClobValue(String clobValue) {
        this.clobValue = clobValue;
    }

    /**
     * @return the blobValue
     */
    public byte[] getBlobValue() {
        return blobValue;
    }

    /**
     * @param blobValue the blobValue to set
     */
    public void setBlobValue(byte[] blobValue) {
        this.blobValue = blobValue;
    }

    /**
     * @return the children
     */
    public List<TestListChildEntity> getChildren() {
        return children;
    }

    /**
     * @param children the children to set
     */
    public void setChildren(List<TestListChildEntity> children) {
        this.children = children;
    }

    /**
     * @return the uniChildren
     */
    public List<TestListUniChildEntity> getUniChildren() {
        return uniChildren;
    }

    /**
     * @param uniChildren the uniChildren to set
     */
    public void setUniChildren(List<TestListUniChildEntity> uniChildren) {
        this.uniChildren = uniChildren;
    }

    /**
     * @return the transChildren
     */
    public List<TestListChildEntity> getTransChildren() {
        return transChildren;
    }

    /**
     * @param transChildren the transChildren to set
     */
    public void setTransChildren(List<TestListChildEntity> transChildren) {
        this.transChildren = transChildren;
    }

    // Hibernate Required Code
	@Override
    public String toString() {
        return Objects.toStringHelper(this).add("id", getId()).toString();
    }

	@Override
	public boolean equals(Object other) {
        if ( other == null ) {
            return false;
        }

        if ( other == this ) {
            return true;
        }

        if ( !(other instanceof TestListParentEntity) ) {
            return false;
        }

        TestListParentEntity castOther = (TestListParentEntity) other;
        return Objects.equal(this.getCode(), castOther.getCode());
    }

	@Override
	public int hashCode() {
        return Objects.hashCode(getCode());
    }
}
