package com.saliman.entitypruner.testhelper.junit;

import com.saliman.entitypruner.testhelper.MissingPropertyException;

/**
 * This is like the {@link com.saliman.entitypruner.testhelper.ApplicationPropertyGateway} but it is designed for
 * unit tests, and is used to load properties we don't need deployed with the
 * application, but do need for unit testing.  For simplicity, it extends the
 * ApplicationPropertyGatewayImpl, but most of the standard properties will 
 * not be present, and should not be loaded with this class.
 * <p>
 * By default, this class looks for a file in the classpath named
 * application-test.properties.
 * 
 * @author Steven C. Saliman
 */
public class TestPropertyGateway 
        extends com.saliman.entitypruner.testhelper.ApplicationPropertyGatewayImpl
         {
    // The following strings define the property names we'll use to lookup
    // property names from the property file.
    private static final String APPLICATION_LIB_DIR = "application.lib.dir";
    private static final String APPLICATION_MODILE_CLASSES = "application.module.classes";
	private static final String APPLICATION_MODULE_NAME = "application.module.name";
	private static final String APPLICATION_RESOURCE_FILES = "application.resource.files";
    private static final String EMBEDDED_DEPLOYMENT_MODE = "embedded.deployment.mode";
    private static final String EMBEDDED_DOMAIN_DIR = "embedded.domain.dir";
    private static final String EMBEDDED_SERVER_DIR = "embedded.server.dir";

	/** 
     * Default constructor, overrides the default property file.
     */
    public TestPropertyGateway() {
        setPropertyFile("application-test.properties");
    }
    
    /**
	 * @return the name of the directory where embedded GlassFish applications
	 *         can find their library jars.
	 */
	public String getApplicationLibDir() {
	    String retval = null;
	    try {
	        retval = getStringValue(APPLICATION_LIB_DIR);
	    } catch (MissingPropertyException e) {
	        // This is optional.
	    }
	    return retval;
	}

	/**
	 * @return the name of the file or directory where the class files for
	 *         an application to be embedded can be found.
	 */
	public String getApplicationModuleClasses() {
	    String retval = null;
	    retval = getStringValue(APPLICATION_MODILE_CLASSES);
	    return retval;
	}

	/**
	 * @return the name of the server module we are deploying to the embedded
	 *         GlassFish instance.
	 */
	public String getApplicationModuleName() {
	    String retval = null;
	    try {
	        retval = getStringValue(APPLICATION_MODULE_NAME);
	    } catch (MissingPropertyException e) {
	        // This is optional.
	    }
	    return retval;
	}

	/**
     * @return a comma separated list of the fully qualified names of files 
     *         that need to be added to an application being deployed to an
     *         embedded GlassFish instance.
     */
    public String getAplicationResourceFiles() {
        String retval = null;
        try {
            retval = getStringValue(APPLICATION_RESOURCE_FILES);
        } catch (MissingPropertyException e) {
            // This is optional.
        }
        return retval;
    }
    
    /**
     * @return the name of the directory that serves as the domain dir for the
     *         embedded GlassFish instance.
     */
    public String getEmbeddedDeploymentMode() {
        String retval = null;
        retval = getStringValue(EMBEDDED_DEPLOYMENT_MODE);
        return retval;
    }

    /**
     * @return the name of the directory that serves as the domain dir for the
     *         embedded GlassFish instance.
     */
    public String getEmbeddedDomainDir() {
        String retval = null;
        try {
            retval = getStringValue(EMBEDDED_DOMAIN_DIR);
        } catch (MissingPropertyException e) {
            // This is optional.
        }
        return retval;
    }

    /**
	 * @return the name of the root directory of the embedded GlassFish 
	 *         instance.
	 */
	public String getEmbeddedServerDir() {
	    String retval = null;
	    try {
	        retval = getStringValue(EMBEDDED_SERVER_DIR);
	    } catch (MissingPropertyException e) {
	        // This is optional.
	    }
	    return retval;
	}
}
