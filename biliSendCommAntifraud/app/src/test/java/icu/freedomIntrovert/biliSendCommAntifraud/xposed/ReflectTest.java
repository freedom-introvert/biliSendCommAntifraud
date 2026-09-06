package icu.freedomIntrovert.biliSendCommAntifraud.xposed;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReflectTest {
    private static class Parent {
        private final long id = 9007199254740993L;
        private String value(int index) { return "int:" + index; }
        private String value(String key) { return "string:" + key; }
        private void fail() { throw new IllegalStateException("original"); }
    }
    private static class Child extends Parent {}

    @Test public void readsInheritedPrivateLongWithoutPrecisionLoss() {
        assertEquals(9007199254740993L, Reflect.getLongField(new Child(), "id"));
    }
    @Test public void resolvesPrimitiveAndStringOverloadsInSuperclass() {
        assertEquals("int:3", Reflect.callMethod(new Child(), "value", 3));
        assertEquals("string:key", Reflect.callMethod(new Child(), "value", "key"));
    }
    @Test public void preservesOriginalInvocationCause() {
        try {
            Reflect.callMethod(new Child(), "fail");
            fail("Must propagate invocation failure");
        } catch (IllegalStateException e) {
            assertEquals("original", e.getCause().getMessage());
        }
    }
    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingField() { Reflect.getObjectField(new Child(), "missing"); }
    @Test public void reportsInheritedFieldAndMethodPresence() {
        assertTrue(Reflect.hasField(new Child(), "id"));
        assertTrue(Reflect.hasMethod(Child.class, "value", int.class));
        assertFalse(Reflect.hasField(new Child(), "missing"));
        assertFalse(Reflect.hasMethod(Child.class, "value", long.class));
    }

    @Test public void prefersDeclaredMethodThenInheritedFallback() throws Exception {
        assertEquals(Parent.class, Reflect.declaredOrInherited(
                Child.class, Parent.class, "value", int.class).getDeclaringClass());
        assertEquals(Object.class, Reflect.declaredOrInherited(
                Child.class, Object.class, "toString").getDeclaringClass());
    }
}
