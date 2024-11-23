package icu.freedomIntrovert.biliSendCommAntifraud.xposed;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class AdvancedFieldPrinter {

    private static final Set<Object> visited = new HashSet<>();

    public static void printFields(Object obj) {
        printFields(obj, 0);
    }

    private static void printFields(Object obj, int depth) {
        if (obj == null) {
            XB.log(getIndent(depth) + "null");
            return;
        }

        Class<?> clazz = obj.getClass();

        // 防止无限递归：跳过已经处理过的对象
        if (visited.contains(obj)) {
            XB.log(getIndent(depth) + "Circular reference detected: " + clazz.getName());
            return;
        }

        // 记录已访问的对象
        visited.add(obj);

        // 打印对象的类型
        XB.log(getIndent(depth) + "Object Type: " + clazz.getName());

        // 遍历所有字段
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            try {
                // 设置字段可以访问，即使是private字段
                field.setAccessible(true);

                // 获取字段的类型
                Class<?> fieldType = field.getType();
                String typeName = fieldType.getName();

                // 获取字段的名字
                String fieldName = field.getName();

                // 获取字段的值
                Object value = field.get(obj);

                // 打印字段基本信息
                XB.log(getIndent(depth + 1) + "Field Name: " + fieldName + ", Type: " + typeName);

                // 如果字段是基本类型或包装类型，不递归
                if (isPrimitiveOrWrapper(fieldType) || fieldType == String.class) {
                    XB.log(getIndent(depth + 2) + "Value: " + value);
                } else if (value != null) { // 对非基本类型的对象递归打印
                    printFields(value, depth + 2);
                } else {
                    XB.log(getIndent(depth + 2) + "Value: null");
                }
            } catch (IllegalAccessException e) {
                XB.log(getIndent(depth + 1) + "Error accessing field: " + field.getName());
            }
        }

        // 移除当前对象引用，允许其他部分重新访问
        visited.remove(obj);
    }

    private static String getIndent(int depth) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("  "); // 每层两个空格
        }
        return indent.toString();
    }

    private static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == Integer.class ||
                clazz == Long.class ||
                clazz == Double.class ||
                clazz == Float.class ||
                clazz == Boolean.class ||
                clazz == Byte.class ||
                clazz == Short.class ||
                clazz == Character.class;
    }

    // 示例对象
    public static class Example {
        private int age = 25;
        private String name = "John";
        private Example self; // 用于测试循环引用
        private NestedExample nested = new NestedExample();

        public Example() {
            this.self = this; // 循环引用
        }
    }

    public static class NestedExample {
        private String description = "Nested Example";
        private int number = 42;
    }

    public static void main(String[] args) {
        Example example = new Example();
        printFields(example);
    }
}

