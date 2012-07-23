package com.saliman.entitypruner.testhelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This gateway provides application property information to the application
 * by reading a property file. 
 * <p>
 * This implementation reads its information from a file called 
 * application.properties somewhere in the classpath.
 * <p>
 * Applications that don't need any application properties of their own can 
 * use this class as is, but if they need any values of their own, the 
 * application should provide its own (extended) implementation of the
 * {@link ApplicationPropertyGateway} interface.  That implementation should
 * extend this class to provide the base functionality and to get the 
 * Framework properties.
 * <p>
 * All accessors should eventually call {@link #getStringValue(String)}, which
 * assumes that properties are always required.  If for some reason a property
 * is not required, the accessor can choose to eat the 
 * {@link MissingPropertyException} that will get thrown.
 * <p>
 * This class can convert properties into Dates. At this time the following
 * formats are accepted:
 * <b>yyyy-MM-dd</b> For dates.<br>
 * <b>yyyy-MM-dd'T'HH:mm:ss</b> For Dates with a time.<br>
 * <b>HH:mm:ss</b> For times.
 * 
 * @author Steven C. Saliman
 */
@Stateless(name="ApplicationPropertyGateway")
public class ApplicationPropertyGatewayImpl 
       implements ApplicationPropertyGateway {
    /** Logger for the class */
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationPropertyGatewayImpl.class);
    
    /** Date format for date properties */
    private static final SimpleDateFormat DATE_FORMATTER = 
            new SimpleDateFormat("yyyy-MM-dd");
    /** Date format for date-time properties */
    private static final SimpleDateFormat DATETIME_FORMATTER = 
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    /** Date format for date properties */
    private static final SimpleDateFormat TIME_FORMATTER = 
            new SimpleDateFormat("HH:mm:ss");
    
    // The following strings define the property names we'll use to lookup
    // property names from the property file.
    private static final String APPLICATION_NAME = "application.name";
    private static final String APPLICATION_VERSION = "application.version";
    private static final String APPLICATION_CODE = "application.code";
    private static final String APPLICATION_ENVIRONMENT = "application.environment";
    private static final String SECURITY_PROVIDER_URL = "security.provider.url";
    private static final String MESSAGE_PROVIDER_BEANS = "message.provider.beans";
    private static final String SESSION_TIMEOUT_MINUTES = "session.timeout.minutes";
    private static final String SESSION_CACHE_CAPACITY = "session.cache.capacity";
    private static final String SESSION_CACHE_CONCURRENCY = "session.cache.concurrency";

    private String propertyFile = "application.properties";
    private Properties properties;
    
    /**
     * @return the name of the property file being read.
     */
    public String getPropertyFile() {
        return propertyFile;
    }

    /**
     * Use this method do override the default property file.  The default
     * is <b>application.properties</b>
     * @param propertyFile the name of the property file to read.
     */
    public void setPropertyFile(String propertyFile) {
        this.propertyFile = propertyFile;
    }

    /**
     * Returns all current properties.
     * @return a {@link Properties} object with all current properties.
     */
    @Override
    public Properties getProperties() {
        LOG.trace("getProperties");
        return properties;
    }

    /**
     * @return the name of the application
     * @throws MissingPropertyException if the value is not in the property
     *         file.
     */
    @Override
    public String getApplicationName() {
        return getStringValue(APPLICATION_NAME);
    }

    /**
     * @return the version of the application
     * @throws MissingPropertyException if the value is not in the property
     *         file.
     */
    @Override
    public String getApplicationVersion() {
        return getStringValue(APPLICATION_VERSION);
    }

    /**
     * @return the code used by the application when communicating with 
     * the security service.
     * @throws MissingPropertyException if the value is not in the property
     *         file.
     */
    @Override
    public String getApplicationCode() {
        return getStringValue(APPLICATION_CODE);
    }

    /**
     * @return the label for the application's current running environment.
     * @throws MissingPropertyException if the value is not in the property
     *         file.
     */
    @Override
    public String getApplicationEnvironment() {
        return getStringValue(APPLICATION_ENVIRONMENT);
    }

    /**
     * @return the comma separated list of beans to use for message processing.
     * Used by the MessageManager when running in an EJB container.
     */
    @Override
    public String getMessageProviderBeans() {
        return getStringValue(MESSAGE_PROVIDER_BEANS);
    }

    /**
	 * @return the base URL used to access security services.  Used by the 
	 * Framework's REST security provider.
	 * @throws MissingPropertyException if the value is not in the property
	 *         file.
	 */
	@Override
	public String getSecurityProviderUrl() {
	    return getStringValue(SECURITY_PROVIDER_URL);
	}

	/**
     * @return the initial size to use for the session cache
     */
	@Override
	public Integer getSessionCacheCapacity() {
    	// This one is optional.
    	Integer size = null;
    	try {
    		size = getIntegerValue(SESSION_CACHE_CAPACITY);
    	} catch (MissingPropertyException e) {
    		LOG.info("No value for " + SESSION_CACHE_CAPACITY);
    	}
    	return size;
	}

	/**
     * @return the level of concurrency for the session cache.
     */
	@Override
	public Integer getSessionCacheConcurrency() {
    	// This one is optional.
    	Integer concurrency = null;
    	try {
    		concurrency = getIntegerValue(SESSION_CACHE_CONCURRENCY);
    	} catch (MissingPropertyException e) {
    		LOG.info("No value for " + SESSION_CACHE_CONCURRENCY);
    	}
    	return concurrency;
	}

	/**
     * @return the number of minutes a session should be valid in the session
     * cache.
     */
    @Override
    public Integer getSessionTimeoutMinutes() {
        return getIntegerValue(SESSION_TIMEOUT_MINUTES);
    }

    /**
     * Gets a property from the property file.  This method assumes all 
     * properties are required.  Callers can choose to eat the exception if
     * a property is optional.  All properties start out as String values.
     * Other accessors can convert to a more strongly typed value if needed.
     * @param key The name of the property to get.
     * @return the property as a string value.
     * @throws MissingPropertyException if the value is not in the property
     *         file.
     */
    protected String getStringValue(String key) {
        if ( properties == null ) {
            initProperties();
        }
        String value = properties.getProperty(key);
        if ( value == null || value.length() < 1 ) {
            throw new MissingPropertyException("No value for the " + key +
                    " property!");
        }
        // TODO: provide for encrypted values.
        return value;
    }
    
    /**
     * Gets a property from the property file and converts it into an Integer.
     * @param key The name of the property to get.
     * @return the property as an Integer value.
     * @throws MissingPropertyException if the value is not in the property
     *         file.
     * @throws NumberFormatException if we can't create the Integer from the value
     */
    protected Integer getIntegerValue(String key) {
        String value = getStringValue(key);
        return new Integer(value);
    }

    /**
     * Gets a property from the property file and converts it into a Long.
     * @param key The name of the property to get.
     * @return the property as a Long value.
     * @throws MissingPropertyException if the value is not in the property
     *         file.
     * @throws NumberFormatException if we can't create the Long from the value
     */
    protected Long getLongValue(String key) {
        String value = getStringValue(key);
        return new Long(value);
    }
    
    /**
     * Gets a property from the property file and converts it into a Double.
     * @param key The name of the property to get.
     * @return the property as a Double value.
     * @throws MissingPropertyException if the value is not in the property
     *         file.
     * @throws NumberFormatException if we can't create the Long from the value
     */
    protected Double getDoubleValue(String key) {
        String value = getStringValue(key);
        return new Double(value);
    }

    
    /**
     * Gets a property from the property file and converts it into a Date.
     * This method will convert the string value into a date based on one of
     * the three supported formats, which can be distinguished by length.
     * @param key The name of the property to get.
     * @return the property as a Date value.
     * @throws MissingPropertyException if the value is not in the property
     *         file.
     * @throws IllegalStateException if the value does not contain a valid date. 
     */
    protected Date getDateValue(String key) {
        String value = getStringValue(key);
        Date dateValue = null;
        try {
            if ( value.length() == 8 ) {
                dateValue = TIME_FORMATTER.parse(value);
            } else if ( value.length() == 10 ) {
                dateValue = DATE_FORMATTER.parse(value);
            } else if ( value.length() == 19 ) {
                dateValue = DATETIME_FORMATTER.parse(value);
            } else {
                throw new IllegalStateException(value + " Is not a valid date");
            }
        } catch (ParseException e) {
            // Throw as an unchecked exception.
            throw new IllegalStateException(value + " Is not a valid date");
        }
        return dateValue;
    }
    
    /**
     * Helper method to load the properties file.
     */
    private void initProperties() {
        properties = new Properties();
        URL propFile = Thread.currentThread().getContextClassLoader().getResource(propertyFile);
        try {
            InputStream file = new FileInputStream(new File(propFile.toURI()));
            properties.load(file);
        } catch (Throwable e) {
            LOG.error("Property file " + propertyFile + 
                      " is missing! no properties will be available")
;            e.printStackTrace();
        }
    }
}
