package com.saliman.entitypruner.testhelper;

import java.util.Collection;
import java.util.Set;

import javax.persistence.Transient;

/**
 * This is a test class that is used to test both the JunitUtil and 
 * ReflectionUtil utility classes.
 * 
 * @author Steven C. Saliman
 */
public class TestClass extends Parent {
    // These should have both a getter and a setter.
    private int i; // Tests getting a primitive
    private boolean bool; // Tests getting a boolean with an "is" method
    @SuppressWarnings("rawtypes")
    private Set untypedSet; // Tests getting an untyped set.
    // These should have a getter or a setter, but not both.
    private long noSetter;
    private double noGetter;
    // In addition, we should have an attribute in a parent class, and one
    // in an abstract one.  One of these should be a typed set.

    // This one is used to test getting a field when the field appears
    // in both a class and a parent.  The annotation should prove that 
    // we got the one in the child, not the one in the parent. The particular
    // annotation is not importatnt, but shouild be unique.
    @SuppressWarnings("unused")
    @Transient
    private String multi;

    
    //------------------ Methods to test JUnit utility functionality
    /**
     * Tests a method that throws an exception.  The exception thrown has 
     * nothing to do with normal execution of the junit methods, and is a good
     * choice to make sure we got the right exception for the right reaons.
     */
    public static void throwSomething() {
        throw new NullPointerException("oops!");
    }
    
    /**
     * This method will be used to test if we can find the method when
     * passed java.util.date, and also java.sql.date.  It is also used
     * in a test that passes a date and null.
     * @param d 
     * @param s 
     * @return a constant
     */
    public int method(java.util.Date d, String s) {
        return DATE_STRING;
    }
    
    /**
     * This method is used to test when null is passed for argument types.
     * This is the only single argument method
     * @param d 
     * @return a constant
     */
    public int method(java.util.Date d) {
        return DATE;
    }
    
    /**
     * used to see if we can get a no-arg method.
     * @return a constant
     */
    public int method() {
        return NO_ARG;
    }
    
    
    /**
     * This method is used to test execution of a package-protected
     * method, and is used to test arguments that are interfaces.
     * It will be tested with Collections, Lists(sub-interface), and
     * ArrayLists(implementations).
    */
    int method(Collection<String> c, String s) {
        return COLLECTION_STRING;
    }
    
    /** 
     * This method is used to test ambiguity when we call it with 
     * null, and two strings.  There is another method with 2 strings
     * at the end in a different class. This is the only method in this
     * class with 2 strings.
     * @param d 
     * @param s1 
     * @param s2 
     * @return a constant
     */
    public int method(Double d, String s1, String s2) {
        return DOUBLE_STRING_STRING;
    }
    
    /**
     * This tests getting a method that overrides a method from another class.
     */
    @Override
    public int method(StringBuffer sb1, StringBuffer sb2) {
        return OVERRIDDEN_BUFFER_BUFFER;
    }
    
    /**
     * Used to see what happens when we have a test class with a method that
     * takes the test class, and a parent class that has a method that takes
     * the parent class.  Java should treat this as an overridden method, so
     * our test should as well.
     * @param p 
     * @param b 
     * @return a constant
     */
    public int method(Parent p, TestClass b) {
    	return PARENT_BASE;
    }
    
    /**
     * Used to see what happens when we have a test class with Object 
     * representations of a Long, and a parent class that takes primitives. 
     * Java shouldn't treat this as an overridden method, but because of 
     * auto-boxing issues, our getMethod will.
     * @param l1 
     * @param l2 
     * @return a constant
     */
    public int method(Long l1, Long l2) {
        return LONG_LONG;
    }
    
    /**
     * Used to test the assertThrows method, this method always throws
     * a NullPointerException.
     */
    public void throwNPE() {
        throw new NullPointerException("Null");
    }

    //------------- Methods to test attribute loading.

    /**
     * @return the i
     */
    public int getI() {
        return i;
    }

    /**
     * @param i the i to set
     */
    public void setI(int i) {
        this.i = i;
    }

    /**
     * @return the bool
     */
    public boolean isBool() {
        return bool;
    }

    /**
     * @param bool the bool to set
     */
    public void setBool(boolean bool) {
        this.bool = bool;
    }

    /**
     * @return the set
     */
    @SuppressWarnings("rawtypes")
	public Set getUntypedSet() {
        return untypedSet;
    }

    /**
     * @param untypedSet the set to untypedSet
     */
    @SuppressWarnings("rawtypes")
	public void setUntypedSet(Set untypedSet) {
        this.untypedSet = untypedSet;
    }

    /**
     * This is PRIVATE. We shouldn't get this attribute when we test attribute
     * loading
     * @return the noGetter
     */
    @SuppressWarnings("unused")
    private double getNoGetter() {
        return noGetter;
    }

    /**
     * @param noGetter the noGetter to set
     */
    public void setNoGetter(double noGetter) {
        this.noGetter = noGetter;
    }

    /**
     * @return the noSetter
     */
    public long getNoSetter() {
        return noSetter;
    }

    /**
     * This is PRIVATE. We shouldn't get this attribute when we test attribute
     * loading
     * @param noSetter the noSetter to set
     */
    @SuppressWarnings("unused")
    private void setNoSetter(long noSetter) {
        this.noSetter = noSetter;
    }
}
