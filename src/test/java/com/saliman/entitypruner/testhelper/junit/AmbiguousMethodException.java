package com.saliman.entitypruner.testhelper.junit;
/**
 * Used by the devutil JUnit class to show that we have more than one method
 * that matches the given parameters.
 * 
 * @author Steven C. Saliman
 */
public class AmbiguousMethodException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new <code>AmbiguousMethodException</code>
     * @param string the message to use in the new exception.
     */
    public AmbiguousMethodException(String string) {
        super(string);
    }
}
