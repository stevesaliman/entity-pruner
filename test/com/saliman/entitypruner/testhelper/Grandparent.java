package com.saliman.entitypruner.testhelper;

import java.util.Date;

import javax.swing.JDialog;

/**
 * This class is used to test the getMethod method of the TestBase class.
 * It is the grandparent of a 3 class hierarchy
 * @author Steven C. Saliman
 */
public class Grandparent extends AbstractGreatGrandparent {
    @SuppressWarnings("unused")
    private String noAccessors; // shouldn't be loaded by the beanFieldLoader.


    /** 
     * This method will be used to determine if we can invoke a private
     * method from our test. This particular method is meant to cause
     * an ambiguous method exception when the first arg is null. 
     */
    @SuppressWarnings("unused")
    private int method(Integer i, String s1, String s2) {
        return INTEGER_STRING_STRING;
    }

    /**
     * This method is used to determine if we can execute a private method
     */
    @SuppressWarnings("unused")
    private int method(JDialog j, String s) {
        return JDIALOG_STRING;
    }
    
    /**
     * This method is here to try to confuse things when we look for
     * the Date, String method.  It has too many args and should not be a 
     * factor.
     * @param d 
     * @param s1 
     * @param s2 
     * @return a constant
     */
    public int method(Date d, String s1, String s2) {
        return DATE_STRING_STRING;
    }
    /**
     * This method is used to determine if we can handle implementations
     * of abstract methods.
     */
    @Override
    public int method(StringBuffer b, String s) {
        return BUFFER_STRING;
    }
}
