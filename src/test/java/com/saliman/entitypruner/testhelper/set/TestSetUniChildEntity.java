package com.saliman.entitypruner.testhelper.set;

import com.google.common.base.Objects;
import com.saliman.entitypruner.testhelper.AuditableEntity;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OptimisticLockType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigInteger;


/** 
 * This class only exists to test the EntityPruner code. 
 * This object needs to have a only a parent object id.  It tests to make 
 * sure we can dehydrate/rehydrate objects destined for XML serialization
 * without lazy loading issues.
 *  
 * @author Steven C. Saliman
 * @see com.saliman.entitypruner.testhelper.set.TestSetParentEntity
 */
@Entity
@Table(name="test_uni_child")
@org.hibernate.annotations.Entity(mutable=true, 
		                          dynamicInsert=true,
		                          dynamicUpdate=true,
		                          optimisticLock=OptimisticLockType.VERSION)
@Cache(usage=CacheConcurrencyStrategy.NONE)
public class TestSetUniChildEntity extends AuditableEntity
                         implements Serializable {
    /** Serial version ID */
    private static final long serialVersionUID = 1L;

    @Column(name="test_parent_id")
    private BigInteger parentId;

    @Column(name="code")
    private String code;
    
    @Column(name="description")
    private String description;
    
    
    /** default constructor */
    public TestSetUniChildEntity() {
        super(); 
    }

    /**
     * Gets the parent
     * @return the parent
     */
    public BigInteger getParentId() {
        return parentId;
    }

    /**
     * Sets the parent
     * @param parentId the message to use.
     */
    public void setParentId(BigInteger parentId) {
        this.parentId = parentId;
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

    // Hibernate Required Code
    public String toString() {
	    return Objects.toStringHelper(this).add("id", getId()).toString();
    }

    public boolean equals(Object other) {
        if ( other == null ) {
            return false;
        }

        if ( other == this ) {
            return true;
        }

        if ( !(other instanceof TestSetUniChildEntity) ) {
            return false;
        }

        TestSetUniChildEntity castOther = (TestSetUniChildEntity) other;
        return Objects.equal(this.getParentId(), castOther.getParentId())
				&& Objects.equal(this.getCode(), castOther.getCode());
    }

    public int hashCode() {
        return Objects.hashCode(getParentId(), getCode());
    }
}
