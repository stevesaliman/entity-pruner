package com.saliman.entitypruner.testhelper;

import java.io.Serializable;
import java.math.BigInteger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OptimisticLockType;


/** 
 * This class only exists to test the Framework code. It tests the
 * {@link BaseEntity} and {@link BaseDaoJpa} code. 
 * This object needs to have a only a parent object id.  It tests to make 
 * sure we can dehydrate/rehydrate objects destined for XML serialization
 * without lazy loading issues.
 *  
 * @author Steven C. Saliman
 * @see TestParentEntity
 */
@Entity
@Table(name="test_uni_child")
@org.hibernate.annotations.Entity(mutable=true, 
		                          dynamicInsert=true,
		                          dynamicUpdate=true,
		                          optimisticLock=OptimisticLockType.VERSION)
@Cache(usage=CacheConcurrencyStrategy.NONE)
public class TestUniChildEntity extends AuditableEntity
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
    public TestUniChildEntity() {
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
        return new ToStringBuilder(this).append("id", getId())
                                       .toString();
    }

    public boolean equals(Object other) {
        if ( other == null ) {
            return false;
        }

        if ( other == this ) {
            return true;
        }

        if ( !(other instanceof TestUniChildEntity) ) {
            return false;
        }

        TestUniChildEntity castOther = (TestUniChildEntity) other;
        return new EqualsBuilder().append(this.getParentId(), castOther.getParentId())
                                  .append(this.getCode(), castOther.getCode())
                                  .isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder().append(parentId)
                                    .append(code)
                                    .toHashCode();
    }
}
