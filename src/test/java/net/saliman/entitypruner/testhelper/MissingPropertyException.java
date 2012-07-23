package net.saliman.entitypruner.testhelper;

/**
 * This exception is used to indicate that the system was unable to retrieve a
 * property from the application.properties file that was expected to exist
 * <p>
 * This generally indicates one of two types of failures:<br>
 * 1) there is a problem accessing the property file (security, etc.)<br>
 * 2) there is a configuration problem within the file itself<br>
 * 
 * @author Steven C. Saliman
 */
public class MissingPropertyException extends RuntimeException {

    /** Serialization version id */
    private static final long serialVersionUID = 1L;

    /**
     * Default Constructor
     */
    public MissingPropertyException() {
        super();
    }

    /**
     * @param message
     */
    public MissingPropertyException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public MissingPropertyException(String message,
            Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public MissingPropertyException(Throwable cause) {
        super(cause);
    }

}
