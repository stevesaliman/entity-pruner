package com.saliman.entitypruner;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
/**
 * This class contains utilities for working with reflection.
 * 
 * @author Steven C. Saliman
 */
public class ReflectionUtil {
    /** logger for the class */
    private static final Logger LOG = Logger.getLogger(ReflectionUtil.class);

    /**
     * Get all of the fields in a class that can be both read and written
     * using get and set methods.  This method will be used by serializers
     * and deserializers, and we don't want to expose and modify private
     * fields that don't have accessor methods.  If a class has such a field
     * it's value will be lost in the serialization/deserialization process,
     * which would be expected.
     * <p>
     * This method will attempt to load fields in the order they are declared
     * in the Java source file, starting with the parent.  It is worth noting
     * that the order of fields returned by the reflection API is not 
     * guaranteed.
     * @param clazz the class whose fields we want. 
     * @return a List of fields from the given class and its parents.
     */
    public static List<Field> loadBeanFields(Class<?> clazz) {
        return loadBeanFields(clazz, false);
    }

    /**
     * Get all of the fields in a class that can at least be read using get
     * methods.  This method will be used by serializers and deserializers, 
     * and we don't want to expose and modify private fields that don't have 
     * accessor methods.  If a class has such a field it's value will be lost
     * in the serialization/deserialization process, which would be expected.
     * <p>
     * This method will attempt to load fields in the order they are declared
     * in the Java source file, starting with the parent.  It is worth noting
     * that the order of fields returned by the reflection API is not 
     * guaranteed.
     * @param clazz the class whose fields we want. 
     * @param includeReadOnly if <code>true</code> include fields that have
     *        a getter but not a setter.
     * @return a List of fields from the given class and its parents.
     */
    public static List<Field> loadBeanFields(Class<?> clazz, 
    		boolean includeReadOnly ) {
        // The parent of Object is null, so this will be called once.
        // This is where we create the list that we will populate on the way
        // up the stack
        List<Field> fieldList = null;
        if ( clazz == null ) {
            return new ArrayList<Field>();
        }
        LOG.trace("loadBeanFields(" + clazz.getName() + ")");
        // start with the parent, then add this class' fields
        fieldList = loadBeanFields(clazz.getSuperclass());
        Field[] fields = clazz.getDeclaredFields();
        String suffix;
        for ( int i=0 ; i < fields.length ; i++ ) {
            Class<?> type = fields[i].getType();
            suffix = fields[i].getName();
            if ( suffix.length() == 1 ) {
                suffix = suffix.toUpperCase();
            } else {
                suffix = suffix.substring(0, 1).toUpperCase() +
                         suffix.substring(1);
            }
            Method getter = getMethod(clazz, "get" + suffix, null);
            Method iser = getMethod(clazz, "is" + suffix, null);
            Method setter = getMethod(clazz, "set" + suffix, type);
            // We need both a "set" and either a "get" or "is".
            if ( (getter != null || iser != null) && 
            		(setter != null || includeReadOnly) ) {
                fieldList.add(fields[i]);
            }
        }
        return fieldList;
    }
    
    /**
	 * Gets the value from the given field.  We can't just use field.get 
	 * because Hibernate doesn't always store the value in the field. It seems
	 * to store it in some CGLIB field, using proxy methods to get to it. We,
	 * therefore, need to use those same proxies, or we won't be getting the 
	 * correct value.
	 * @param field the field object representing the field with the value
	 *        we want.
	 * @param object the entity with the value we want
	 * @return the value for the given field in the given entity.
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static Object getFieldValue(Field field, Object object)
	               throws SecurityException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
	    // we need to call the method to get the proxy...
	    String name = field.getName();
	    name = name.substring(0,1).toUpperCase() + name.substring(1);
	    Class<?> type = field.getType();
	    if ( boolean.class.isAssignableFrom(type)
	            || Boolean.class.isAssignableFrom(type) ) {
	        name = "is" + name;
	    } else {
	        name = "get" + name;
	    }
	    Object value = null;
	    // Get the value.  We need to try using the "get" method first, but
	    // not all fields will have a method (SERIAL_VERSION_UID for example),
	    // so we need to get the actual field value if the method doesn't 
	    // exist.
	    try {
	        Method method = field.getDeclaringClass().getMethod(name);
	        value = method.invoke(object);
	    } catch (NoSuchMethodException e) {
	        // not all fields will have a getter, so just get it from the
	        // field directly
	        value = field.get(object);
	    }
	    return value;
	}

	/**
     * Helper method that wraps the <code>getMethod</code> method of the Java
     * reflection api.  It catches any exceptions and returns null.  This keeps
     * the calling code clean.
     * @param clazz the class to check
     * @param name the name of the method we want.
     * @param parameterType the parameter type for the method we want.  Since
     *        this class will only ever be called on to get accessors, we'll
     *        never need more than one type.
     */
    private static Method getMethod(Class<?> clazz, String name, 
                             Class<?> parameterType) {
        Method m = null;
        try {
            // An empty array is not the same as a null
            if ( parameterType == null ) {
                m = clazz.getMethod(name, new Class<?>[] {});
            } else {
                m = clazz.getMethod(name, parameterType);
            }
        } catch (SecurityException e) {
            // No need to do anything here.
        } catch (NoSuchMethodException e) {
            // no need to do anything here either.
        }
        return m;
    }

}
