package net.saliman.entitypruner.testhelper.set;

import com.google.common.base.Objects;
import net.saliman.entitypruner.testhelper.AuditableEntity;
import net.saliman.entitypruner.testhelper.BaseDaoJpa;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OptimisticLockType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;


/** 
 * This class only exists to test the Framework code. It tests the
 * {@link Persistable} and {@link BaseDaoJpa} code.  This object needs to
 * have a parent object.  It tests to make sure we can dehydrate/rehydrate 
 * without endless loops.
 * 
 * @author Steven C. Saliman
 * @see net.saliman.entitypruner.testhelper.set.TestSetParentEntity
 */
@Entity
@Table(name="test_child")
@org.hibernate.annotations.Entity(mutable=true, 
		                          dynamicInsert=true,
		                          dynamicUpdate=true,
		                          optimisticLock=OptimisticLockType.VERSION)
@Cache(usage=CacheConcurrencyStrategy.NONE)
public class TestSetChildEntity extends AuditableEntity
                         implements Serializable {
    /** Serial version ID */
    private static final long serialVersionUID = 1L;

    @ManyToOne
    @JoinColumn(name="test_parent_id")
    private TestSetParentEntity parent;

    @Column(name="code")
    private String code;

    @Column(name="description")
    private String description;
    /** default constructor */
    public TestSetChildEntity() {
        super(); 
    }

    /**
     * Gets the parent
     * @return the parent
     */
    public TestSetParentEntity getParent() {
        return parent;
    }

    /**
     * Sets the parent
     * @param parent the message to use.
     */
    public void setParent(TestSetParentEntity parent) {
        this.parent = parent;
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

        if ( !(other instanceof TestSetChildEntity) ) {
            return false;
        }

        TestSetChildEntity castOther = (TestSetChildEntity) other;
        return Objects.equal(this.getParent(), castOther.getParent())
				&& Objects.equal(this.getCode(), castOther.getCode());
    }

    public int hashCode() {
        return Objects.hashCode(getParent(), getCode());
    }
}
