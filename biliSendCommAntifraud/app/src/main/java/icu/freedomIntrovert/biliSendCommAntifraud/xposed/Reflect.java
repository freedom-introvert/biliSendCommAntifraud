package icu.freedomIntrovert.biliSendCommAntifraud.xposed;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/** Java reflection only; declaring classes are resolved with the supplied host loader. */
public final class Reflect {
    private Reflect() {}

    public static Class<?> findClass(String name, ClassLoader loader) {
        try { return Class.forName(name, false, loader); }
        catch (ClassNotFoundException e) { throw new IllegalArgumentException("Class not found: " + name, e); }
    }

    public static Method method(Class<?> type, String name, Class<?>... args) throws NoSuchMethodException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Method method = c.getDeclaredMethod(name, args);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException(type.getName() + "." + name + Arrays.toString(args));
    }

    /** Prefer a method declared on {@code preferred}; otherwise walk {@code fallback}. */
    public static Method declaredOrInherited(Class<?> preferred, Class<?> fallback,
                                             String name, Class<?>... args) throws NoSuchMethodException {
        try {
            Method method = preferred.getDeclaredMethod(name, args);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ignored) {
            return method(fallback, name, args);
        }
    }

    public static boolean hasMethod(Class<?> type, String name, Class<?>... args) {
        try {
            method(type, name, args);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public static boolean hasField(Object object, String name) {
        if (object == null) return false;
        for (Class<?> c = object.getClass(); c != null; c = c.getSuperclass()) {
            try {
                c.getDeclaredField(name);
                return true;
            } catch (NoSuchFieldException ignored) {}
        }
        return false;
    }

    public static Object getObjectField(Object object, String name) {
        if (object == null) throw new IllegalArgumentException("Null receiver for field " + name);
        for (Class<?> c = object.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException ignored) {
            } catch (IllegalAccessException e) { throw new IllegalStateException(e); }
        }
        throw new IllegalArgumentException("Field not found: " + object.getClass().getName() + "." + name);
    }

    public static long getLongField(Object o, String name) { return ((Number) getObjectField(o, name)).longValue(); }
    public static int getIntField(Object o, String name) { return ((Number) getObjectField(o, name)).intValue(); }

    public static Object callMethod(Object object, String name, Object... args) {
        // Existing callers use zero args, an int index, or an exact String parameter.
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) throw new IllegalArgumentException("Use an explicit signature for null arguments");
            types[i] = args[i] instanceof Integer ? int.class : args[i].getClass();
        }
        try {
            return method(object.getClass(), name, types).invoke(object, args);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Invocation failed: " + name, e.getCause());
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
}
