package net.saliman.entitypruner.testhelper;

import java.util.Set;

/**
 * This is a test class that is used to test both the jJUit utility and
 * attribute loading classes. This class makes sure we can deal with inherited
 * things.
 *
 * @author Steven C. Saliman
 */
public class Parent extends Grandparent {
    private Set<String> typedSet;

    //------------ Test JUnit utility functionality
    /**
     * Used to determine if we can run static methods.  Also used to test
     * inheritance.
     * @param i 
     * @param i2 
     * @return a constant
     */
    public static int method(int i, int i2) {
        return STATIC_INT_INT;
    }
    
    /**
     * Used to determine if we can run protected methods.  Also used
     * to see what happens with auto-boxing This method will be found even
     * if we pass an Integer.
     * @return a constant
     */
    protected int method(int i, String s) {
        return INT_STRING;
    }
    
    /**
     * Used to see what happens when a subclass overrides a method. Our
     * getMethod should find the right one.
     * @param s1 
     * @param s2 
     * @return a constant
     */
    public int method(StringBuffer s1, StringBuffer s2) {
        return BUFFER_BUFFER;
    }
    
    /**
     * Used to see what happens when we have a test class with a method that
     * takes the base class, and a parent class that has a method that takes
     * the parent class.  There should be no ambiguity.  Java treats the
     * child as an overriding method unless we specifically pass two
     * parents.
     * @param p1 
     * @param p2 
     * @return a constant
     */
    public int method(Parent p1, Parent p2) {
    	return PARENT_PARENT;
    }
    
    /**
     * Used to see what happens when we have a test class with Objects and
     * a parent with primitives.  Java would keep this straight, but because
     * of auto-boxing issues, our getMethod will return it as ambiguous.
     * @param l1 
     * @param l2 
     * @return a constant.
     */
    public int method(long l1, long l2) {
        return PRIMLONG_PRIMLONG;
    }
    
    //---------------- Test attribute loading functionality.
    /**
     * @return the typedSet
     */
    public Set<String> getTypedSet() {
        return typedSet;
    }

    /**
     * @param typedSet the typedSet to set
     */
    public void setTypedSet(Set<String> typedSet) {
        this.typedSet = typedSet;
    }
}
