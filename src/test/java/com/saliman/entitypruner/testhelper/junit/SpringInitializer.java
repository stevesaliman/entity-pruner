package com.saliman.entitypruner.testhelper.junit;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * Web applications usually run inside a web container, which start up Spring 
 * when they start the web application.  Unit tests don't run in the container,
 * so unit tests for applications that use Spring may need to have Spring 
 * running before any meaningful testing can happen.<p>
 * In most cases, the unit test should just extend one of Sprint's test classes,
 * but there may be instances where we need the IoC container, but don't want
 * all the transaction management or other aspects of spring, and this class
 * handles that scenario.
 * <p>
 * This class is a little utility class that starts up Spring.  It is designed
 * to be used in the constructors of the unit test classes, and it contains two
 * initialize methods.  One takes the name of a spring configuration file, 
 * the other assumes a default name.
 * <p>
 * This class is designed to only fire up Spring once for each test run, so if 
 * there are different configuration files for different tests, only the first 
 * test's configuration will be loaded.
 * 
 * @author Steven C. Saliman
 */
public class SpringInitializer {
    private static final String DEFAULT_SPRING_CONFIG_FILE = "applicationContext-test.xml";
    private static ApplicationContext context;

    /**
     * Initialize spring using the default configuration file name.  The file
     * must be somewhere in the classpath, and must be named
     * applicationContext-test.xml.  To initialize spring with a different
     * file, use the <code>initialize(String)</code> version of this method.
     */
    public static void initialize() {
        if ( context == null ) {
            initialize(DEFAULT_SPRING_CONFIG_FILE);
        }
    }

    /**
     * Initialize spring using the given configuration file.  The file
     * must be somewhere in the classpath.
     * @param configFile the name of the Spring configuration file.  The
     *                   file must be in the classpath.
     */
    public static void initialize(String configFile) {
        if ( context == null ) {
            context = new ClassPathXmlApplicationContext(configFile);
        }
    }
}
