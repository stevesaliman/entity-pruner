package net.saliman.entitypruner.testhelper.junit;

import net.saliman.entitypruner.testhelper.DatabaseType;
import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.glassfish.embeddable.archive.ScatteredArchive;
import org.glassfish.embeddable.archive.ScatteredEnterpriseArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.assertNotNull;

/**
 * This class is a base class that can be used to run a unit test inside an
 * embedded GlassFish v3 container.  There is a fair amount of setup that 
 * must be done before this class will work properly.
 * <p>
 * First, any services in the project with <code>@WebService</code> 
 * annotations will need to have <code>@WebMethod(exclude=true)</code>
 * annotations on any method that needs to be ignored by GlassFIsh.
 * <p>
 * Secondly, the project will need to have access to a GlassFish v3 container,
 * preferably within in the project, which is basically a freshly unzipped 
 * GlassFish distribution. 
 * <p>
 * Next, the glassfish-embedded-static-shell.jar from the GlassFIsh 
 * lib/embedded directory will need to be in the classpath, and it must be 
 * the only GlassFish jar file in the classpath used for unit tests. This 
 * will cause a couple of issues for developers.  Eclipse users will find 
 * that Eclipse has created the project with a GlassFish library.  Users will
 * need to remove the library and add the glassfish-embedded-static-shell.jar.
 * Ant build files will need to have a separate class path for using
 * GlassFish tools in the build, such as wsgen.
 * <p>
 * Next, you need a template domain.xml file with templates for port numbers 
 * and database connections, and an application-test.properties template that
 * defines the properties that are needed to deploy the application to the
 * embedded GlassFish.  The exact properties will depend on the type of 
 * deployment being done.
 * <p>
 * The deployment mode must be specified by the 
 * <code>embedded.deployment.mode</code> property.  There are currently 4 
 * supported deployment modes:
 * <ul>
 * <li>EAR - Deploys an enterprise application from an ear file.  This is
 * the preferred deployment mode for JavaEE 5 enterprise applications being
 * tested by Ant because it tests against the exact file that will be deployed
 * to production</li>
 * <li>SCATTERED_EAR - Deploys an enterprise application assembled from various
 * directories.  This is the deployment mode for running a JavaEE 5 test from
 * Eclipse.</li> 
 * <li>WAR - Deploys a web application from a war file.  This is the preferred
 * deployment mode for JavaEE 6 applications because of the simpler deployment
 * vs. an ear file.</li>
 * <li>SCATTERED_WAR - Deploys a web application assembled from various 
 * directories.  This is the deployment mode for running a JavaEE 6 test from
 * Eclipse.</li>
 * </ul>
 * The rest of the properties that this class uses are:
 * <ul>
 * <li>embedded.server.dir - The location of the embedded GlassFish server</li>
 * <li>embedded.domain.dir - The location of the embedded GlassFish's 
 * domain</li>
 * <li>application.module.classes - Defines the location of the application's 
 * classes.  For EAR and WAR deployments, this will be the name of the ear or
 * war file.  For the other deployments, this is the directory containing the
 * application or modules class files.</li>
 * <li>application.lib.dir (optional) - For scattered deployments, each jar or zip
 * file in the given directory will be added to the deployed application. For
 * scattered ear deployments, the name of the library in the ear will be the
 * name of the jar file with "lib/" at the beginning because we assume that
 * jar files will be deployed in production in the "lib" directory of the ear.
 * This property is ignored in EAR and WAR deployments</li>
 * <li>application.resource.files (optional) - For scattered ear deployments, 
 * this property will hold the name of the application.xml file.  For 
 * scattered war deployments, this file will hold the name of any resources 
 * that need to be added such as ejb-jar.xml files.  web.xml files will 
 * generally not need to be added because we don't need to have servlets 
 * running.  Multiple resources can be specified, separated by a comma.  This
 * property is ignored by EAR and WAR deployments</li>
 * <li>application.module.name (optionsl)- Used by ear and scattered ear 
 * deployments to specify the name of the module under test.  This is not the
 * same as the name of the application.  It will probably be something like 
 * myapp-ejb.</li>
 * </ul>
 * <p>
 * Unit tests extending this class will need to encapsulate its test code 
 * in a {@link Transactable}, and use 
 * the {@link #runInTransaction(Transactable)} method to run tests in a 
 * transaction.  this makes it easy to simulate service calls where parts are
 * run inside a transaction, and parts are outside the transaction.  By default
 * the transaction is associated with the <b>default</b> transaction in the
 * persistence.xml file.  If you need to use a different transaction, you must
 * extend the {@link JpaTransactionEjb} class.  This new class can have an 
 * EntityManager injected from a different persistence unit.  The EntityManager
 * in {@link JpaTransactionEjb} can then be set to the new one from the sub
 * class.  Tests will then need to set the <code>transactionBeanName</code>
 * in their constructor to get the right transaction.  Tests and classes under
 * test must use the same persistence unit, or strange things will happen.
 * You've been warned.
 * <p>
 * Tests will need to know how beans are named in GlassFish. The format of a 
 * GlassFish bean name is typically 
 * <code>java:global/<i>project</i>/<i>InterfaceName</i></code>. This class 
 * deploys all applications under the name "test".  As a convenience, the
 * {@link #getBean(String)} method can take just the interfaceName. Unit 
 * tests will also have to set up their data differently from most unit tests.
 * Usually, the setup methods are used to create test data, but in container
 * tests, the test data will need to be set up in the same transaction as the
 * test itself, which means each test will need to set up its own data.  As a
 * practical matter, this probably means having a separate, private, 
 * un-annotated setup method to create data, that tests can call from inside
 * their transactions.
 * <p>
 * This class also contains convenience methods to count rows in a table and
 * execute SQL to set up tests.  Calls to these methods also need to be in 
 * a unit test's {@link Transactable} so they get run in the same transaction
 * as the test itself. Your tests will look something like this:<br>
 * <pre>
    @Test 
    public void myTest() throws Exception {
        runInTransaction(new Transactable() {
            public void run() throws Exception {
                // All test code goes here, including data setup, tests, and
                // asserts.
            }
        });
    }
 * </pre><p>
 * By default, all transactions will roll back when they are finished so that
 * tests have no lasting impact on the database.  If, for some reason, you
 * want to see the effects of the test in the database after the test is
 * finished, you can call <code>setDefaultRollback(false)</code> to cause 
 * transactions to commit.  Doing this will only impact the current test 
 * method, since default rollback is reset to <code>false</code> before each
 * test.  An exception thrown by the test method will force a rollback 
 * regardless of the state of the defaultRollback property.  Also Keep in mind
 * that unchecked exceptions thrown by a bean get wrapped in an 
 * <code>EJBException</code> before it is received by the caller.
 * <p>
 * The most expensive part of an in-container test is the container startup 
 * and application deployment.  To help with this, the container startup
 * happens once per JVM.  What this means to the developer is that "forkmode"
 * must be set in the build.xml if ant is forking tests in another JVM.  
 * Jvmargs should also be set to make sure that the forked JVMs have enough
 * memory and PermGenSpace to run all the tests.
 * @author Steven C. Saliman
 */
public abstract class AbstractGlassFishContainerTest {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractGlassFishContainerTest.class);
    private static Context initialContext;
    private static String beanPrefix;
    protected String transactionBeanName = 
    		"JpaTransaction!net.saliman.entitypruner.testhelper.junit.JpaTransaction";
    private JpaTransaction transaction;
    private DatabaseType databaseType = DatabaseType.GENERIC;

    static {
        // For performance, only need to start the container once per JVM
    	// Logging here is done with writes to System.output because
    	// we probably haven't initialized logging yet.
        System.out.println("Attempting to Start GlassFish at " + new Date());
        // Create a Embedded GlassFish instance using a preconfigured domain.xml
        TestPropertyGateway gw = new TestPropertyGateway();
        String deploymentMode = gw.getEmbeddedDeploymentMode();
        String domainDir = gw.getEmbeddedDomainDir();
        String serverDir = gw.getEmbeddedServerDir();
        String resourceFiles = gw.getAplicationResourceFiles();
        String libDir = gw.getApplicationLibDir();
        String moduleName = gw.getApplicationModuleName();
        String moduleClasses = gw.getApplicationModuleClasses();

        GlassFish glassfish;
        BootstrapProperties bootstrapProperties;            
        try {
        	bootstrapProperties = new BootstrapProperties();
        	if ( serverDir != null ) {
        		bootstrapProperties.setInstallRoot(serverDir);
        	}
            GlassFishRuntime glassfishRuntime = GlassFishRuntime.bootstrap(bootstrapProperties);

            GlassFishProperties glassfishProperties = new GlassFishProperties();
            if ( domainDir != null ) {
            	glassfishProperties.setInstanceRoot(domainDir);
            }
            glassfish = glassfishRuntime.newGlassFish(glassfishProperties);
            // Start Embedded GlassFish
            glassfish.start();
            System.out.println("Embedded GlassFish started");

            // Deploy an application to the Embedded GlassFish
            if ( deploymentMode.equalsIgnoreCase("EAR") ) { 
            	deployFile(glassfish, moduleClasses);
        	    beanPrefix = "java:global/test/" + moduleName + "/";
            }
            else if ( deploymentMode.equalsIgnoreCase("WAR") ) {
            	deployFile(glassfish, moduleClasses);
        	    beanPrefix = "java:global/test/";
            } else if ( deploymentMode.equalsIgnoreCase("SCATTERED_EAR") ) {
            	deployScatteredEar(glassfish, moduleName, moduleClasses, resourceFiles, libDir);
        	    beanPrefix = "java:global/test/" + moduleName + "/";
            } else if ( deploymentMode.equalsIgnoreCase("SCATTERED_WAR") ) {
            	deployScatteredWar(glassfish, moduleClasses, resourceFiles, libDir);
        	    beanPrefix = "java:global/test/";
            } else {
            	throw new IllegalArgumentException(deploymentMode +
            			" is not a supported deployment mode");
            }
            initialContext = new InitialContext();
        } catch (Throwable e) {
        	// re-throw as a runtime exception
            e.printStackTrace();
            LOG.error("Can't start embedded GlassFish");
            throw new RuntimeException("Can't start embedded GlassFish", e);
        }
    }

    /**
     * Default constructor
     */
    public AbstractGlassFishContainerTest() {
        
    }
    
    /**
     * @return the defaultRollback
     */
    protected boolean isDefaultRollback() {
        return transaction.isDefaultRollback();
    }

    /**
     * Sets whether or not transactions should be rolled back after each
     * test.  The default is <code>true</code>, meaning that transactions will
     * rollback after each test.
     * @param defaultRollback the defaultRollback to set
     */
    protected void setDefaultRollback(boolean defaultRollback) {
        transaction.setDefaultRollback(defaultRollback);
    }
    
    /**
     * Gets the named bean from the EJB container.  This method is static 
     * because some tests may want to only get the bean once in a BeforeClass
     * method.
     * @param beanName the name of the bean to get from the container.  In 
     * GlassFish, the name of the bean has the name of the project in it, 
     * making it difficult for tests to know the full name of the bean. As a
     * convenience, this method can fill in the prefix of the bean for us.
     * For example, if the bean name is "java:global/myapp/MyBean", callers
     * can simply pass in "MyBean", and this method will do the rest.
     * @return the requested bean.
     * @throws Exception if we can't get the bean.
     */
    protected static Object getBean(String beanName) throws Exception {
        Object bean;
        if ( !beanName.startsWith("java:global") ) {
        	beanName = beanPrefix + beanName;
        }
        bean = initialContext.lookup(beanName);
        return bean;
    }
    
    /**
     * Gets the correct bean prefix for tests to use if the need to lookup
     * a bean outside this class.  This should really only be used by the
     * ApplicationEjbTest class, which has a getBean method of its own.
     */
    protected static String getBeanPrefix() {
    	return beanPrefix;
    }
    
    /**
     * Set the database type for this test
     * @param databaseType the DatabaseType for this test.
     */
    public void setDatabaseType(DatabaseType databaseType) {
    	this.databaseType = databaseType;
    }
    
    /**
     * Initialize each test by getting a transaction bean from the container,
     * and making sure we're set to rollback.
     * <p> 
     * We always set rollback to false because individual tests may set it
     * to true, and we don't want one test to affect another test.
     * @throws Exception if we can't get the transaction bean
     */
    @Before
    public void initTransaction() throws Exception {
        String lookupBeanName =  beanPrefix + transactionBeanName;

    	transaction = (JpaTransaction)initialContext.lookup(lookupBeanName);
        assertNotNull("Unable to get JpaTransaction bean.");
        transaction.setDatabaseType(databaseType);
        setDefaultRollback(true);
    }
    
    /**
     * Clean up from each test by releasing the Transaction Bean.
     */
    @After
    public void closeTransaction() {
        if ( transaction != null ) {
            transaction = null;
        }
    }
    
    /**
     * Convenience method that runs the given native SQL in the test's current
     * transaction.  This method is very handy for setting up the test data
     * a test will operate on.  This method won't work unless it is inside
     * a {@link Transactable} passed to {@link #runInTransaction(Transactable)}.
     * <p>
     * This test is just a pass through to 
     * {@link JpaTransaction#executeUpdate(String, Object...)}.  It is here 
     * because extending classes won't have access to the transaction bean.
     * @param sql The native query to run
     * @param args Any parameters to the query.
     * @return the number of rows affected by the SQL.
     * @throws Exception if anything goes wrong.
     */
    public int executeUpdate(String sql, Object... args) throws Exception {
        LOG.trace("executeUpdate(String Object...)");
        transaction.setDatabaseType(databaseType);
        return transaction.executeUpdate(sql, args);
    }

    /**
     * Convenience method to count the number of rows in a table. This 
     * method won't work unless it is inside a {@link Transactable} passed to 
     * {@link #runInTransaction(Transactable)}.
     * <p>
     * This test is just a pass through to 
     * {@link JpaTransaction#countRowsInTable(String)}.  It is here 
     * because extending classes won't have access to the transaction bean.
     * @param tableName the name of the table to query
     * @return the number of rows in the given table.
     */
    public int countRowsInTable(String tableName) {
        return transaction.countRowsInTable(tableName);
    }

    /**
     * Gets a transaction bean from the container and uses it to run the
     * given code in an isolated transaction. Unchecked Exceptions thrown from
     * this method will be wrapped in an <code>EJBException</code> from the 
     * container. 
     * <p>
     * Be very careful not to eat exceptions inside the runnable.  This can
     * cause side effects if default rollback is set to false.
     * @param transactable the code to run.
     * @throws Exception if there is a problem, or if the test code throws
     * an exception, which may be what the test is looking for.
     */
    public void runInTransaction(Transactable transactable) throws Exception {
        LOG.trace("runInTransaction(Transactable)");
        try {
        	transaction.runInTransaction(transactable);
        } catch (Exception e) {
            transaction = null;
            initTransaction();
            throw e;
        }
    }
    
    /**
	 * Because this class ends with the word "Test", Junit will attempt to 
	 * run tests against it.  This method makes sure we don't get falures
	 * because we don't have any tests.
	 */
	@Test
	public void testNothing() {
	    LOG.trace("testNothing()");
	}

	/**
	 * deploy an ear or war to the embedded GlassFish instance.
	 * @param glassfish the handle to the GlassFish instance.
	 * @param deployFileName the fully qualified name of the file to deploy.
	 * @throws GlassFishException if we have a problem with the deployment.
	 */
	private static void deployFile(GlassFish glassfish, String deployFileName) 
			            throws GlassFishException {
	    File deployFile = new File(deployFileName);
		if ( !deployFile.exists() ) {
	    	String msg = "Can't find " + deployFileName +
	    			" to deploy to embedded GlassFish";
	    	System.out.println(msg);
	    	throw new IllegalArgumentException(msg);
	    }

		System.out.println("Deploying file " + deployFileName +
		           " to embedded GlassFish");
	    Deployer deployer = glassfish.getDeployer();
	    deployer.deploy(deployFile, "--force=true", "--name=test",
                        "--contextroot=test");
		beanPrefix = "java:global/test/";
	}

	/**
	 * Create a scattered ear and deploy to the embedded GlassFish.
	 * @param glassfish the handle to the GlassFish instance.
	 * @param moduleName the name of the module being deployed.
	 * @param moduleClasses the name of the directory containing the module's
	 *        classes 
	 * @param applicationXml the location of the application.xml file.
	 * @param libDirName the name of the directory containing the modules 
	 *        libraries.
	 * @throws GlassFishException if we have a problem with the deployment.
	 * @throws IOException if we have issues dealing with any of the files. 
	 */
	private static void deployScatteredEar(GlassFish glassfish,
			String moduleName, String moduleClasses, String applicationXml,
			String libDirName) throws IOException, GlassFishException{
	    // I may not have a lib dir or application.xml, but I need to
	    // have the module class dir
	    File classDir = new File(moduleClasses);
	    if ( !classDir.exists() ) {
	    	String msg = "Can't find " + moduleClasses +
	    			" to deploy to embedded GlassFish";
	    	System.out.println(msg);
	    	throw new IllegalArgumentException(msg);
	    }
	    ScatteredEnterpriseArchive archive = new ScatteredEnterpriseArchive("test");
	    archive.addArchive(classDir, moduleName + ".jar");

	    if ( applicationXml != null ) {
	    	System.out.println("Adding metadata " + applicationXml +
	    			           " to scattered ear");
	    	archive.addMetadata(new File(applicationXml));
	    }
	    // Add all the jars from the lib directory.
	    if ( libDirName != null ) {
	    	File libDir = new File(libDirName);
	    	for ( File f : libDir.listFiles() ) {
	    		String filename = f.getName();
	    		if ( f.isFile() && (filename.endsWith(".jar") ||
	    				            filename.endsWith(".zip")) ) {
	                System.out.println("Adding library " + f.getName() +
	                		           " to scattered ear");
	    			archive.addArchive(f, "lib/" + f.getName());
	    		}
	    	}
	    }
	
		System.out.println("Deploying scattered ear " + moduleClasses +
		           " to embedded GlassFish");
	    Deployer deployer = glassfish.getDeployer();
	    deployer.deploy(archive.toURI(), "--force=true", "--name=test");
	}

	/**
	 * Create and deploy a scattered archive and deploy to the embedded 
	 * GlassFish instance
	 * @param glassfish the embedded GlassFish instance
	 * @param moduleClasses the directory with the application's classes
	 * @param resourceFiles additional resources to add
	 * @param libDirName the library directory for the application.
	 * @throws GlassFishException if we have a problem with the deployment.
	 * @throws IOException if we have issues dealing with any of the files. 
	 */
	private static void deployScatteredWar(GlassFish glassfish,
			String moduleClasses, String resourceFiles, String libDirName) throws IOException, GlassFishException {
		// Create a scattered web application.
		ScatteredArchive webmodule = new ScatteredArchive("test", ScatteredArchive.Type.WAR);

		// Add all the class dirs
		String [] classDirs = moduleClasses.split(",");
		for ( String dirName : classDirs ) {
			File classDir = new File(dirName);
			if ( !classDir.exists()  ) {
				String msg = "Can't find " + moduleClasses +
						" to deploy to embedded GlassFish";
				System.out.println(msg);
				throw new IllegalArgumentException(msg);
			}
			webmodule.addClassPath(classDir);
		}
		// Add libraries
		if ( libDirName != null ) {
			File libDir = new File(libDirName);
			for ( File f : libDir.listFiles() ) {
				String filename = f.getName();
				if ( f.isFile() && (filename.endsWith(".jar") ||
						filename.endsWith(".zip")) ) {
	                System.out.println("Adding library " + f.getName() +
         		                       " to scattered war class path");
					webmodule.addClassPath(f);
				}
			}
		}
		
		// Add resources.
		if ( resourceFiles != null ) {
			String [] resources = resourceFiles.split(",");
			for ( String r : resources ) {
				System.out.println("Adding resource " + r +
						           " to scattered war");
				webmodule.addMetadata(new File(r));
			}
		}

		System.out.println("Deploying scattered war " + moduleClasses +
		           " to embedded GlassFish");
		Deployer deployer = glassfish.getDeployer();
		deployer.deploy(webmodule.toURI(), "--force=true", "--name=test",
				        "--contextroot=test");
	}
}
