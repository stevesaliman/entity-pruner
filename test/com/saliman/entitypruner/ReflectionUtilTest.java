package com.saliman.entitypruner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import javax.persistence.Transient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.saliman.entitypruner.testhelper.TestClass;

/**
 * Tests that we can load attributes from a class correctly.  We'll use the 
 * same classes here as we use for the JUnit utility tests.
 * 
 * @author Steven C. Saliman
 */
public class ReflectionUtilTest {
    
    private List<Field> list;
    
    /**
     * Default constructor.
     */
    public ReflectionUtilTest() {
    }

    /**
     * Set up for each test.
     */
    @Before
    public void setUp() {
    }
    
    /**
     * Clean up from each test.
     */
    @After
    public void tearDown() {
        if ( list != null ) {
            list.clear();
            list = null;
        }
    }
    
    /**
     * Try loading attributes from the hierarchy of classes used in the JUnit 
     * utility tests. We care about the order.  The correct order is:
     * String - from an abstract great grandparent
     * TypedSet - From the parent
     * I - a primitive, first declared in the base class.
     * Bool - a boolean (with an "is"), declared next in the base class.
     * UntypedSet - an untyped set declared in the base class.
     */
    @Test
    public void loadBeanFieldsGood() {
        list = ReflectionUtil.loadBeanFields(TestClass.class);
        assertNotNull("Should have gotten attributes", list);
        assertEquals("Loaded the wrong number of attributes", 5, list.size());
        // Check that we got the right things at the right places.
        // The String attribute from the great grandparent
        Field field = list.get(0);
        assertEquals("The 'string' attribute should be first", "string", field.getName());
        assertEquals("The 'string' attribute is the wrong type", String.class, field.getType());
        // The Typed set attribute from the parent
        field = list.get(1);
        assertEquals("The 'typedSet' attribute should be second", "typedSet", field.getName());
        assertEquals("The 'typedSet' attribute is the wrong type", Set.class, field.getType());
        // The int attribute
        field = list.get(2);
        assertEquals("The 'i' attribute should be thrid", "i", field.getName());
        assertEquals("The 'i' attribute is the wrong type", int.class, field.getType());
        // The boolean attribute
        field = list.get(3);
        assertEquals("The 'bool' attribute should be fourth", "bool", field.getName());
        assertEquals("The 'bool' attribute is the wrong type", boolean.class, field.getType());
        // The untyped set attribute
        field = list.get(4);
        assertEquals("The 'untypedSet' attribute should be fifth", "untypedSet", field.getName());
        assertEquals("The 'untypedSet' attribute is the wrong type", Set.class, field.getType());
    }

    /**
     * Try loading attributes from the hierarchy of classes used in the JUnit 
     * utility tests. We care about the order.  The correct order is:
     * String - from an abstract great grandparent
     * TypedSet - From the parent
     * I - a primitive, first declared in the base class.
     * Bool - a boolean (with an "is"), declared next in the base class.
     * UntypedSet - an untyped set declared in the base class.
     */
    @Test
    public void loadBeanFieldsReadOnly() {
        list = ReflectionUtil.loadBeanFields(TestClass.class, true);
        assertNotNull("Should have gotten attributes", list);
        assertEquals("Loaded the wrong number of attributes", 6, list.size());
        // Check that we got the right things at the right places.
        // The String attribute from the great grandparent
        Field field = list.get(0);
        assertEquals("The 'string' attribute should be first", "string", field.getName());
        assertEquals("The 'string' attribute is the wrong type", String.class, field.getType());
        // The Typed set attribute from the parent
        field = list.get(1);
        assertEquals("The 'typedSet' attribute should be second", "typedSet", field.getName());
        assertEquals("The 'typedSet' attribute is the wrong type", Set.class, field.getType());
        // The int attribute
        field = list.get(2);
        assertEquals("The 'i' attribute should be thrid", "i", field.getName());
        assertEquals("The 'i' attribute is the wrong type", int.class, field.getType());
        // The boolean attribute
        field = list.get(3);
        assertEquals("The 'bool' attribute should be fourth", "bool", field.getName());
        assertEquals("The 'bool' attribute is the wrong type", boolean.class, field.getType());
        // The untyped set attribute
        field = list.get(4);
        assertEquals("The 'untypedSet' attribute should be fifth", "untypedSet", field.getName());
        assertEquals("The 'untypedSet' attribute is the wrong type", Set.class, field.getType());
        // The noSetter attribute should be present when we pass "true"
        field = list.get(5);
        assertEquals("The 'noSetter' attribute should be sixth", "noSetter", field.getName());
        assertEquals("The 'noSetter' attribute is the wrong type", long.class, field.getType());       
    }
    
    /**
     * Try getting a field from a class directly.
     */
    @Test
    public void getFieldDirect() {
    	Field f = ReflectionUtil.getField(TestClass.class, "bool");
    	assertNotNull("Failed to get field", f);
    	assertEquals("Got wrong field", "bool", f.getName());
    }
    
    /**
     * Try getting a field from the middle of a hierarchy.  This is also good
     * because this one is private and has no accessors.
     */
    @Test
    public void getFieldMiddle() {
    	Field f = ReflectionUtil.getField(TestClass.class, "noAccessors");
    	assertNotNull("Failed to get field", f);
    	assertEquals("Got wrong field", "noAccessors", f.getName());
    }
    
    /**
     * Try getting a field from the top of a hierarchy.  This also tests 
     * getting a field from an abstract class.
     */
    @Test
    public void getFieldTop() {
    	Field f = ReflectionUtil.getField(TestClass.class, "string");
    	assertNotNull("Failed to get field", f);
    	assertEquals("Got wrong field", "string", f.getName());
    }
    
    /**
     * Try getting a field that is in more than one class. The field is only
     * annotated in one of them, so we'll see if reflectionUtil gave us the
     * one we wanted. "multi"
     */
    @Test
    public void getFieldMulti() {
    	Field f = ReflectionUtil.getField(TestClass.class, "multi");
    	assertNotNull("Failed to get field", f);
    	assertEquals("Got wrong field", "multi", f.getName());
    	// check the annotation to see if we got the right one.
    	Annotation a = f.getAnnotation(Transient.class);
    	assertNotNull("Got wrong field - missing annotation", a);
    }
    
    /**
     * Try getting a field with no class
     */
    @Test
    public void getFieldNoClass() {
    	Field f = ReflectionUtil.getField(null, "bool");
    	assertNull("Shouldn't have gotten a field", f);
    }
    
    /**
     * Try getting a field with no name.
     */
    @Test
    public void getFieldNoName() {
    	Field f = ReflectionUtil.getField(TestClass.class, null);
    	assertNull("Shouldn't have gotten a field", f);
    }
}
