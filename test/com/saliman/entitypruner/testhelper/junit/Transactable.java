package com.saliman.entitypruner.testhelper.junit;

/**
 * Defines a runnable body of work.  Similar to {@link java.lang.Runnable}, 
 * except that This class' {@link #run()} method can throw an exception,
 * which is desirable in a unit test where we want to test for exceptions.
 * 
 * @author Steven C. Saliman
 */
public interface Transactable {
    /**
     * Runs the actual code in this Transactable.
     * @throws Exception
     */
    public void run() throws Exception;
}
