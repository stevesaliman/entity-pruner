package com.saliman.entitypruner.testhelper.junit;

/**
 * Spring doesn't really have an easy way to mark a transaction for rollback, 
 * like an EJB SessionContext does, so we need a way to signal for unit tests
 * to rollback transactions.   Since Spring can be told to rollback on certain
 * exceptions, this class will do just that.
 * <p>
 * When a caller gets a RollbackWarning, it should check the cause to see if 
 * there was some underlying problem.
 *
 * @author Steven C. Saliman
 */
public class RollbackWarning extends Exception {
	private static final long serialVersionUID = 1L;
	private Exception cause;
	
	/**
	 * Default Constructor.
	 */
	public RollbackWarning() {
		
	}
	
	/**
	 * Create a rollback warning with a root cause.
	 * @param cause the root cause
	 */
    public RollbackWarning(Exception cause) {
    	this.cause = cause;
    }

    /**
     * @return the root cause of this warning
     */
    @Override
	public Exception getCause() {
		return cause;
	}
}
