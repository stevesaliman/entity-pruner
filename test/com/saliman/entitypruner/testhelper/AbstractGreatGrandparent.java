package com.saliman.entitypruner.testhelper;

import java.io.File;
/**
 * This is a test class that is used to test both the JUnit utility and 
 * attribute loading classes. This class makes sure we can deal with abstract 
 * classes. All constants are here because it is the root of the hierarchy.
 * Note that the constants in this class have no getters or setters, so they
 * should not be found by the attribute loader.
 * 
 * @author Steven C. Saliman
 */
public abstract class AbstractGreatGrandparent {
    /** Constant for method return value */
    public static final int DATE_STRING = 1;
    /** Constant for method return value */
    public static final int DATE = 2;
    /** Constant for method return value */
    public static final int NO_ARG = 3;
    /** Constant for method return value */
    public static final int COLLECTION_STRING = 4;
    /** Constant for method return value */
    public static final int DOUBLE_STRING_STRING = 5;
    /** Constant for method return value */
    public static final int OVERRIDDEN_BUFFER_BUFFER = 6;
    /** Constant for method return value */
    public static final int INT_STRING = 7;
    /** Constant for method return value */
    public static final int BUFFER_BUFFER = 8;
    /** Constant for method return value */
    public static final int INTEGER_STRING_STRING = 9;
    /** Constant for method return value */
    public static final int URL_STRING = 10;
    /** Constant for method return value */
    public static final int DATE_STRING_STRING = 11;
    /** Constant for method return value */
    public static final int BUFFER_STRING = 12;
    /** Constant for method return value */
    public static final int FILE_STRING = 13;
    /** Constant for method return value */
    public static final int PARENT_PARENT = 14;
    /** Constant for method return value */
    public static final int PARENT_BASE = 15;
    /** Constant for method return value */
    public static final int LONG_LONG = 16;
    /** Constant for method return value */
    public static final int PRIMLONG_PRIMLONG = 17;
    /** Constant for the method return value */
    public static final int STATIC_INT_INT = 18;
    /** Constant for method return value */
    private String string;

    /**
     * Abstract method def. Used to check if the impl of this method can be found.
     * @param b 
     * @param s 
     * @return a constant
     */
    public abstract int method(StringBuffer b, String s);
    
    /**
     * Used to see if we can load methods from an abstract class.
     * @param f 
     * @param s 
     * @return a constant
     */
    public int method(File f, String s) {
        return FILE_STRING;
    }
    
    /**
     * Used to see if we can load attributes from an abstract class.
     * @return a constant
     */
    public String getAbstract() {
        return null;
    }

    //---- Used to test the attribute loader.
    /**
     * @return the string
     */
    public String getString() {
        return string;
    }

    /**
     * @param string the string to set
     */
    public void setString(String string) {
        this.string = string;
    }
}
