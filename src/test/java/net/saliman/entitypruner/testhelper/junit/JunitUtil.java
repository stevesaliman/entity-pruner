package net.saliman.entitypruner.testhelper.junit;

import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.ejb.EJBException;

/**
 * This class has adds methods useful to unit testing.  The public methods
 * of this class can be used by unit tests in a similar manner to JUnit 4
 * <code>assert</code> methods, in that tests can do a static import of the
 * method, then call the methods.
 * 
 * @author Steven C. Saliman
 */
public class JunitUtil {
    /**
     * Asserts that a given method of an object throws a particular exception
     * when given certain arguments.  A JUnit failure will occur if the given
     * method doesn't throw the given exception.
     * <p>
     * It is important to note that varargs will  auto-box arguments.  This 
     * means that if we were to call <code>invoke(obj, "method", 1)</code>
     * the actual argument type as seen by <code>invoke</code> would be 
     * an Integer.  For this reason this method treats the 8 primitives and
     * their class equivalents as the same. This could cause unexpected
     * results.
     * <p>
     * For example, if we tried to call
     * <code>assertThrows(Exception.class, obj, "method", 1)</code>
     * and obj belongs to a class with a <code>method(Integer i)</code>
     * method, that is what will be run, which may or may not be what we 
     * want.
     * <p>
     * Junit 4 can do the same thing with the <code>expected=</code> parameter
     * to the <code>@Test </code>annotation, but unlike JUnit 4, this method
     * will report the actual exception. This method is also useful when we
     * want to test for an exception in a particular part of the test method.
     * JUnit 4 simply makes sure the exception is thrown somewhere in the test
     * method.
	 * @param exception the class of the exception to be thrown
	 * @param object the object with the method to run
	 * @param methodName the name of the method to run
	 * @param args the arguments to pass to the method. If there is only one 
	 *        argument to the method, and we want to pass a null value, use
	 *        <code>new Object[] { null }</code> instead of just null.  Passing
	 *        null itself indicates that the method has no arguments.
	 */
	public static void assertThrows(Class<? extends Throwable> exception, 
			                           Object object, String methodName,
	                                   Object... args) {
	    try {
	        Class<?>[] argTypes = getParameterTypes(args);
	        Method method = getMethod(object.getClass(), methodName, argTypes);
	        method.invoke(object, args);
            fail("No exception occured. Expected a " + exception.getName());
	    } catch (InvocationTargetException e) {
	        // did we get the right kind of exception?
	    	Throwable t = e.getTargetException();
	        if ( !exception.isAssignableFrom(t.getClass())) {
	            fail("Expected a " + exception.getName() + " but got a " +
                      t.getClass().getName()); 
	        }
        } catch (AmbiguousMethodException e) {
            fail("Ambiguous Method: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            fail("Invalid Method: " + e.getMessage());
        } catch (IllegalAccessException e) {
            fail("Unable to run Method: " + e);
	    }
	}

    /**
     * Asserts that a given static method of a class throws a particular 
     * exception when given certain arguments.  A JUnit failure will occur if 
     * the given method doesn't throw the given exception.
     * <p>
     * It is important to note that varargs will  auto-box arguments.  This 
     * means that if we were to call <code>invoke(obj, "method", 1)</code>
     * the actual argument type as seen by <code>invoke</code> would be 
     * an Integer.  For this reason this method treats the 8 primitives and
     * their class equivalents as the same. This could cause unexpected
     * results.
     * <p>
     * For example, if we tried to call
     * <code>assertThrows(Exception.class, obj, "method", 1)</code>
     * and obj belongs to a class with a <code>method(Integer i)</code>
     * method, that is what will be run, which may or may not be what we 
     * want.
     * <p>
     * Junit 4 can do the same thing with the <code>expected=</code> parameter
     * to the <code>@Test </code>annotation, but unlike JUnit 4, this method
     * will report the actual exception. This method is also useful when we
     * want to test for an exception in a particular part of the test method.
     * JUnit 4 simply makes sure the exception is thrown somewhere in the test
     * method.
     * @param exception the class of the exception to be thrown
     * @param clazz the class with the static method to run
     * @param methodName the name of the method to run
     * @param args the arguments to pass to the method. If there is only one 
     *        argument to the method, and we want to pass a null value, use
     *        <code>new Object[] { null }</code> instead of just null.  Passing
     *        null itself indicates that the method has no arguments.
     */
    public static void assertStaticThrows(Class<? extends Throwable> exception, 
                                       Class<?> clazz, String methodName,
                                       Object... args) {
        try {
            Class<?>[] argTypes = getParameterTypes(args);
            Method method = getMethod(clazz, methodName, argTypes);
            method.invoke(null, args);
            fail("No exception occured. Expected a " + exception.getName());
        } catch (InvocationTargetException e) {
            // did we get the right kind of exception?
            Throwable t = e.getTargetException();
            if ( !exception.isAssignableFrom(t.getClass())) {
                fail("Expected a " + exception.getName() + " but got a " +
                      t.getClass().getName()); 
            }
        } catch (AmbiguousMethodException e) {
            fail("Ambiguous Method: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            fail("Invalid Method: " + e.getMessage());
        } catch (IllegalAccessException e) {
            fail("Unable to run Method: " + e);
        }
    }

    /**
     * Asserts that a given method of an EJB throws a particular exception
     * when given certain arguments.  A JUnit failure will occur if the given
     * method doesn't throw the given exception.
     * <p>
     * When an EJB throws an unchecked exception, the container will wrap
     * the exception in an EJBException. This method will unwrap it and check
     * to see if the underlying cause is what we expected.
     * <p>
     * It is important to note that varargs will  auto-box arguments.  This 
     * means that if we were to call <code>invoke(obj, "method", 1)</code>
     * the actual argument type as seen by <code>invoke</code> would be 
     * an Integer.  For this reason this method treats the 8 primitives and
     * their class equivalents as the same. This could cause unexpected
     * results.
     * <p>
     * For example, if we tried to call
     * <code>assertEjbThrows(Exception.class, obj, "method", 1)</code>
     * and obj belongs to a class with a <code>method(Integer i)</code>
     * method, that is what will be run, which may or may not be what we 
     * want.
     * <p>
     * Junit 4 can do the same thing with the <code>expected=</code> parameter
     * to the <code>@Test </code>annotation, but unlike JUnit 4, this method
     * will report the actual exception. This method is also useful when we
     * want to test for an exception in a particular part of the test method.
     * JUnit 4 simply makes sure the exception is thrown somewhere in the test
     * method.
     * @param exception the class of the exception to be thrown
     * @param object the object with the method to run
     * @param methodName the name of the method to run
     * @param args the arguments to pass to the method. If there is only one 
     *        argument to the method, and we want to pass a null value, use
     *        <code>new Object[] { null }</code> instead of just null.  Passing
     *        null itself indicates that the method has no arguments.
     */
    public static void assertEjbThrows(Class<? extends Throwable> exception, 
                                       Object object, String methodName,
                                       Object... args) {
        try {
            Class<?>[] argTypes = getParameterTypes(args);
            Method method = getMethod(object.getClass(), methodName, argTypes);
            method.invoke(object, args);
            fail("No exception occured. Expected a " + exception.getName());
        } catch (InvocationTargetException e) {
            // did we get the right kind of exception?
            Throwable t = e.getTargetException();
            // Unwrap EJBExceptions.
            if ( EJBException.class.isAssignableFrom(t.getClass()) ) {
                t = t.getCause();
            }
            if ( !exception.isAssignableFrom(t.getClass())) {
                fail("Expected a " + exception.getName() + " but got a " +
                      t.getClass().getName()); 
            }
        } catch (AmbiguousMethodException e) {
            fail("Ambiguous Method: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            fail("Invalid Method: " + e.getMessage());
        } catch (IllegalAccessException e) {
            fail("Unable to run Method: " + e);
        }
    }
    
	/**
	 * This method runs a method and return's it's results. Why not just 
	 * run the method directly, you ask?  Because this <code>invoke</code>
	 * method can run private methods that cannot be run directly.  This
	 * allows testing of private "helper" methods.
     * <p>
     * It is important to note that varargs will  auto-box arguments.  This 
     * means that if we were to call <code>invoke(obj, "method", 1)</code>
     * the actual argument type as seen by <code>invoke</code> would be 
     * an Integer.  For this reason this method treats the 8 primitives and
     * their class equivalents as the same. This could cause unexpected
     * results.
     * <p>
     * For example, if we tried to call <code>invoke(obj, "method", 1)</code>
     * and obj belongs to a class with a <code>method(Integer i)</code>
     * method, that is what will be run, which may or may not be what we 
     * want.
	 * @param object the object we want to run the method against.
	 * @param methodName the name of the method to run
	 * @param args the arguments to pass to the method. If there is only one 
	 *        argument to the method, and we want to pass a null value, use
	 *        <code>new Object[] { null }</code> instead of just null.  Passing
	 *        null itself indicates that the method has no arguments. 
	 * @return Whatever the invoked method returns.
	 * @throws Throwable if the invoked method throws an exception.  The actual
	 *         type of the exception will be whatever the invoked method throws.
	 */
	public static Object invoke(Object object, String methodName, 
                                Object... args) throws Throwable {
        Class<?>[] argTypes = getParameterTypes(args);
        try {
	        Method method = getMethod(object.getClass(), methodName, argTypes);
	        return method.invoke(object, args);
        } catch (AmbiguousMethodException e) {
            fail("Ambiguous Method: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            fail("Invalid Method: " + e.getMessage());
        } catch (IllegalAccessException e) {
            fail("Unable to run Method: " + e);
        } catch (IllegalArgumentException e) {
            fail("Illegal Argument to Method: " + e);
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            throw t;
        }
        return null;
	}

    /**
     * This method runs a static method and return's it's results. Why not just 
     * run the method directly, you ask?  Because this <code>invoke</code>
     * method can run private methods that cannot be run directly.  This
     * allows testing of private "helper" methods.
     * <p>
     * It is important to note that varargs will  auto-box arguments.  This 
     * means that if we were to call <code>invoke(obj, "method", 1)</code>
     * the actual argument type as seen by <code>invoke</code> would be 
     * an Integer.  For this reason this method treats the 8 primitives and
     * their class equivalents as the same. This could cause unexpected
     * results.
     * <p>
     * For example, if we tried to call <code>invoke(obj, "method", 1)</code>
     * and obj belongs to a class with a <code>method(Integer i)</code>
     * method, that is what will be run, which may or may not be what we 
     * want.
     * @param clazz the class that contains the static method.
     * @param methodName the name of the method to run
     * @param args the arguments to pass to the method. If there is only one 
     *        argument to the method, and we want to pass a null value, use
     *        <code>new Object[] { null }</code> instead of just null.  Passing
     *        null itself indicates that the method has no arguments. 
     * @return Whatever the invoked method returns.
     * @throws Throwable if the invoked method throws an exception.  The actual
     *         type of the exception will be whatever the invoked method throws.
     */
    public static Object invokeStatic(Class<?> clazz, String methodName, 
                                Object... args) throws Throwable {
        Class<?>[] argTypes = getParameterTypes(args);
        try {
            Method method = getMethod(clazz, methodName, argTypes);
            return method.invoke(null, args);
        } catch (AmbiguousMethodException e) {
            fail("Ambiguous Method: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            fail("Invalid Method: " + e.getMessage());
        } catch (IllegalAccessException e) {
            fail("Unable to run Method: " + e);
        } catch (IllegalArgumentException e) {
            fail("Illegal Argument to Method: " + e);
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            throw t;
        }
        return null;
    }

    /**
	 * Helper method to retrieve a named method from a class.  It will look at
	 * all the methods in a class, and if the method found is not public,
	 * this method will make it accessible to the caller.
	 * <p>
	 * Note that if one of the arguments is null, we may wind up with an
	 * ambiguous signature.  For example, a class has<br>
	 * <code>someMeth(Double d, String s)</code><br>
	 * and<br>
	 * <code>someMeth(String s, String s)</code><br>
	 * If we call getMethod with arguments of {null, String}, getMethod will 
	 * not be able to tell which one to return, so it will throw an 
     * <code>AmbiguousMethodException</code>
     * <p>
	 * We Don't need to worry about the case where we have the same arguments
	 * but different return types, because this is not allowed by Java. 
	 * <p>
	 * We also don't need to worry about generics, since Java won't allow
	 * two methods with signatures that only differ in the generics.
	 * <p>
	 * This method is package protected so we can test it.  Also, other 
	 * things in the testing package may find it useful.
	 * There are some known issues with this method:<br>
	 * 1) I Haven't tried to deal with vararg methods yet.<br>
	 * 2) We can't tell the difference between a request for a no-arg method and a 
	 *    one arg method where we aren't passing any args. Requesting a one arg 
	 *    method with a null argument will return the no-arg method if it 
	 *    exists.  If you want to pass null to a one arg method, use 
	 *    <code>new Object[] { null }</code> instead.  It tells getMethod
	 *    that we want a one arg method.<br>
	 * 3) <code>volatile</code> methods will be ignored because the assumption
	 *    is made that they won't be tested.  They aren't used much, and one
	 *    situation was observed where the JVM created a volatile method that
	 *    corresponded to the actual method being tested in a Comparator, causing
	 *    the test to fail.<br>
     * 4) the calling method has varargs.  Varargs always auto-box their 
     *    elements. That means that if a class has a method that takes an
     *    Integer, and you request one that takes an int, you'll get the one 
     *    that takes the Integer.  Even worse, if a parent class actually has
     *    a method that did take an int, you'll still get the one from the
     *    child that takes an Integer. This means you may not always be testing
     *    the method you think you are. If we tried to fix this, we'd loose
     *    the ability to ever return a method that takes primitives, which
     *    happens a lot more than the duplicate method scenario described here.
	 * <p>
	 * This method is package visible to allow testing.
	 * @param cls the class whose method we want
	 * @param methodName the name of the method we want
	 * @param paramTypes the arguments to the method
	 * @return the requested <code>Method</code>
	 * @throws NoSuchMethodException If we can't do it
	 * @throws AmbiguousMethodException if more than one method fits.
	 */
	static Method getMethod(Class<?> cls, String methodName, Class<?>[] paramTypes) 
	               throws NoSuchMethodException, AmbiguousMethodException {
	    int methodCount = 0;
	    Method curMethod = null;
	    Method returnMethod = null;
	    Method[] methodArray = null;
	
	    // look for potential matches in the given class and all of it's parents
	    // We need to look at all the classes to make sure we don't have an 
	    // ambiguous signature, which can happen if one of the paramTypes
	    // is null. If none of the paramTypes are null, we can't be 
        // ambiguous - the child wins.
        boolean missingType = false;
        if ( paramTypes != null ) {
            for ( Class<?> c : paramTypes ) {
                if ( c == null ) {
                    missingType = true;
                }
            }
        }
        
	    while ( cls != null ) {
	        // First, see if this class has the method.
	        methodArray = cls.getDeclaredMethods();
	        for ( int i = 0; i < methodArray.length; i++ ) {
	            // If this method has the right name, see if it has the right arguments.
	            curMethod = methodArray[i];
	            if ( curMethod.getName().equals(methodName) ) {
	                if ( signatureMatches(curMethod, paramTypes) ) {
	                    methodCount++;
	                    // to save time, abort as soon as the count goes over 1.
	                    if ( methodCount > 1 ) {
	                        String message = buildMessage(methodName, paramTypes);
	                        throw new AmbiguousMethodException(message);
	                    } 
	                    // this is the first one, save it.
	                    returnMethod = curMethod;
	                }            
	            }
	        }
            // If we haven't found the method yet, try the parent. If we have
            // found the method, we still try the parent if we're missing a
            // type so we can see if the method is ambiguous.
            if ( methodCount < 1 || missingType ) {
	            cls = cls.getSuperclass();
            } else {
                // break out of this loop
                cls = null;
            }
	    }
	    // If we get here, we either have 0 or 1 methods found.  > 1 would have
	    // already caused an error.
	    if ( methodCount == 0 ) {
	        String message = buildMessage(methodName, paramTypes);
	        throw new NoSuchMethodException(message);
	    }
	    // by now, we can assume success, and method holds what we want.
	    returnMethod.setAccessible(true);
	    return returnMethod;
	}

	/**
     * helper method to get the types of the parameters that are passed to a
     * method.
     * @param parameters an array of method parameters
     * @return an array containing the <code>Class</code> of each parameter.
     */
    private static Class<?>[] getParameterTypes(Object[] parameters) {
        Class<?>[] paramTypes = null;
        if ( parameters != null ) {
            paramTypes = new Class[parameters.length];
            for ( int i = 0; i < parameters.length; i++ ) {
                if ( parameters[i] == null ) {
                    paramTypes[i]  = null;
                } else {
                    paramTypes[i] = parameters[i].getClass();
                }
            }
        }
        return paramTypes;
    }

    /**
     * Helper method to see if a given method matches the signature we are 
     * looking for.  It compares the parameter types of the method we are 
     * seeking to the parameter types of the method we have. If we have a 
     * <code>null</code> type, we will ignore that parameter and move on.
     * <p>
     * It is important to note that this method treats the 8 primitives and
     * their class equivalents as the same.  For example, a method with a
     * single <code>int</code> parameter will match if we are looking for 
     * an <code>Integer</code>. This is because varargs auto-box primitives.
     * <p>
     * This method ignores 2 kinds of methods<br>
     * It ignores abstract methods, because they can't be run.<br>
     * It also ignores volatile methods.  This is a bit risky, but we're 
     * assuming that we won't be trying to test volatile methods.  It has been
     * observed that the JVM created a volatile method when trying to test a
     * Comparator.
     * @param method the <code>Method</code> to check.
     * @param paramTypes an array of parameter types.
     * @return <code>true</code> if the given method has the same number and 
     *         types of parameters as the array of parameter types we are given,
     *         <code>false</code> otherwise.
     */
    private static boolean signatureMatches(Method method, Class<?>[] paramTypes) {
        // First things first:  If this is an abstract method, we can't run it,
        // return false.
        int modifiers = method.getModifiers();
        if ( Modifier.isAbstract(modifiers) ) {
            return false;
        }
        // this one is a bit risky, but I'm guessing we're not testing volatile
        // methods.  Added because the JVM created a volatile method that
        // wasn't in the code when testing a Comparator.
        if ( Modifier.isVolatile(modifiers) ) {
        	return false;
        }
        
        Class<?>[] methodParamTypes = method.getParameterTypes();
        // If the number of args doesn't match, we know it's not a match if we
        // don't have any params, assume we are looking for a 0 arg method. and
        // return true if we have one.
        if ( paramTypes == null ) {
            if ( methodParamTypes.length == 0 ) {
                return true;
            } 
            return false;
        }
    
        if ( paramTypes.length != methodParamTypes.length ) {
            return false;
        }
        
        
        // assume we're good until we learn otherwise.
        for ( int j = 0; j < paramTypes.length; j++ ) {
            if ( paramTypes[j] != null ) {
                if (!methodParamTypes[j].isAssignableFrom(paramTypes[j]) ) {
                    // if we can't get an exact match, and it's a primitive,
                	// try an inexact one. Convert to Object equivalent to 
                	// simulate auto-boxing and try again
                	methodParamTypes[j] = autoBox(methodParamTypes[j]);
                	paramTypes[j] = autoBox(paramTypes[j]);	
                    if (!methodParamTypes[j].isAssignableFrom(paramTypes[j]) ) {
                    	return false;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * This method simulates autoBoxing by converting a primitive class
     * (<code>boolean</code>, <code>byte</code>, <code>char</code>, 
     * <code>short</code>, <code>int</code>, <code>long</code>, 
     * <code>float</code>, and <code>double</code>) to their object 
     * equivalents.
	 * <p>
	 * This method is package visible to allow testing.
     * @param clazz the class to check
     * @return The object representation of <code>clazz</code> if it is
     *         a primitive, otherwise <code>clazz</code> is returned 
     *         unchanged.
     */
    static Class<?> autoBox(Class<?> clazz) {
	    Class<?> newClazz = null;
    	if ( boolean.class.equals(clazz) ) {
		   newClazz = Boolean.class;
    	} else if ( byte.class.equals(clazz) ) {
    		newClazz = Byte.class;
    	} else if ( char.class.equals(clazz) ) {
    		newClazz = Character.class;
    	} else if ( short.class.equals(clazz) ) {
    		newClazz = Short.class;
    	} else if ( int.class.equals(clazz) ) {
    		newClazz = Integer.class;
    	} else if ( long.class.equals(clazz) ) {
    		newClazz = Long.class;
    	} else if ( float.class.equals(clazz) ) {
    		newClazz = Float.class;
    	} else if ( double.class.equals(clazz) ) {
    		newClazz = Double.class;
    	} else {
    		newClazz = clazz;
    	}
    	return newClazz;
    	
	}


    /**
     * Little helper method to build error messages.
     * @param methodName the method name we were looking for
     * @param paramTypes the parameter types of the method we were looking for.
     * @return the error message.
     */
    private static String buildMessage(String methodName, Class<?>[] paramTypes) {
        StringBuffer message = new StringBuffer(methodName + "(");
        if ( paramTypes != null ) {
            for ( int i = 0; i < paramTypes.length; i++ ) {
                if ( i > 0 ) {
                    message.append(", ");
                }
                if ( paramTypes[i] == null ) {
                    message.append("<null>");
                } else {
                    message.append(paramTypes[i].getName());
                }
            }
        }
        message.append(")");
        return message.toString();
    }
}
