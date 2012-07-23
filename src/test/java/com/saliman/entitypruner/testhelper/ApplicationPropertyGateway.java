package com.saliman.entitypruner.testhelper;

import java.util.Properties;

import javax.ejb.Local;

/**
 * This interface defines the methods that get properties used by the 
 * framework itself.  Applications that need their own properties should
 * extend this interface and add the methods to access the application
 * specific values.
 * <p>
 * The ApplicationPropertyGateway mechanism is designed for EJB applications,
 * where direct injection of strings and numbers is not so easy.  The 
 * preferred way to get properties in a Spring application is to declare a 
 * <code>PropertyPlaceholderConfigurer</code>, such as 
 * {@code DecrypterPropertyPlaceholderConfigurer}, add a property file to it's
 * location list, and have properties injected into classes using Spring's 
 * <code>@Value("${property.name}") annotation.
 * 
 * @author Steven C. Saliman
 */
@Local
public interface ApplicationPropertyGateway {
    /**
     * Returns all current properties.
     * @return a {@link Properties} object with all current properties.
     */
    public Properties getProperties();
    
    /**
     * @return the name of the application
     * @throws MissingPropertyException if the value is not in the property
     *         file.
     */
    public String getApplicationName();
    
    /**
     * @return the version of the application
     * @throws MissingPropertyException if the value is not in the property
     *         file.
     */
    public String getApplicationVersion();
    
    /**
     * @return the code used by the application when communicating with 
     * the security service.
     * @throws MissingPropertyException if the value is not in the property
     *         file.
     */
    public String getApplicationCode();
    
    /**
     * @return the label for the application's current running environment.
     * @throws MissingPropertyException if the value is not in the property
     *         file.
     */
    public String getApplicationEnvironment();
    
    /**
     * @return the comma separated list of beans to use for message processing.
     * Used by the MessageManager when running in an EJB container.
     */
    public String getMessageProviderBeans();
    
    /**
	 * @return the base URL used to access security services.  Used by the 
	 * Framework's REST security provider.
	 * @throws MissingPropertyException if the value is not in the property
	 *         file.
	 */
	public String getSecurityProviderUrl();

	/**
     * @return the initial size to use for the session cache
     */
    public Integer getSessionCacheCapacity();

	/**
     * @return the level of concurrency for the session cache.
     */
    public Integer getSessionCacheConcurrency();

	/**
     * @return the number of minutes a session should be valid in the session
     * cache.
     */
    public Integer getSessionTimeoutMinutes();
}
