package com.loyayz.simple.helper;

import com.loyayz.simple.Sorter;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
public final class Utils {
    private static final List<String> PROXY_CLASSES = Arrays.asList(
            "net.sf.cglib.proxy.Factory",
            "org.springframework.cglib.proxy.Factory",
            "javassist.util.proxy.ProxyObject",
            "org.apache.ibatis.javassist.util.proxy.ProxyObject");
    private static final Map<Class<?>, WeakReference<SerializedLambda>> LAMBDA_CACHE = new ConcurrentHashMap<>();

    public static boolean jpaPresent;
    public static boolean pageHelperPresent;

    static {
        try {
            Class.forName("javax.persistence.Table");
            jpaPresent = true;
        } catch (Throwable e) {
            jpaPresent = false;
        }
        try {
            Class.forName("com.github.pagehelper.PageHelper");
            pageHelperPresent = true;
        } catch (Throwable e) {
            pageHelperPresent = false;
        }
    }

    /**
     * 获取模型类
     *
     * @param clazz 原类
     * @return 如果是代理类，返回父类，否则返回自身
     */
    public static Class<?> getTargetClass(Class<?> clazz) {
        boolean isProxy = false;
        for (Class<?> cls : clazz.getInterfaces()) {
            if (PROXY_CLASSES.contains(cls.getName())) {
                isProxy = true;
                break;
            }
        }
        return isProxy ? clazz.getSuperclass() : clazz;
    }

    /**
     * 字符串驼峰转下划线格式
     *
     * @param param 需要转换的字符串
     * @return 转换好的字符串
     */
    public static String camelToUnderline(String param) {
        int len = param.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append("_");
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    /**
     * 获取子类及父类的所有属性列表
     * 若覆写了父类属性，则取子类的属性
     *
     * @param clazz       类
     * @param fieldFilter 属性过滤器
     * @return 属性列表
     */
    public static List<Field> getFields(Class<?> clazz, Predicate<Field> fieldFilter) {
        List<Field> superFields = new ArrayList<>();
        Class<?> currentClass = clazz.getSuperclass();
        while (currentClass != null && currentClass != Object.class) {
            Field[] declaredFields = currentClass.getDeclaredFields();
            Collections.addAll(superFields, declaredFields);
            currentClass = currentClass.getSuperclass();
        }
        Map<String, Field> fields = Stream.of(clazz.getDeclaredFields())
                .collect(toMap(Field::getName, f -> f, (o1, o2) -> o1, LinkedHashMap::new));
        superFields.stream()
                .filter(field -> !fields.containsKey(field.getName()))
                .forEach(field -> fields.put(field.getName(), field));
        return fields.values().stream()
                .filter(fieldFilter)
                .collect(toList());
    }

    /**
     * 获取表达式方法的属性名
     */
    public static String getLambdaProperty(Sorter sorter) {
        Class<?> funcClass = sorter.func().getClass();
        SerializedLambda lambda = Optional.ofNullable(LAMBDA_CACHE.get(funcClass))
                .map(WeakReference::get)
                .orElseGet(() -> {
                    SerializedLambda temp = sorter.funcToSerializedLambda();
                    LAMBDA_CACHE.put(funcClass, new WeakReference<>(temp));
                    return temp;
                });
        String result = lambda.getImplMethodName();
        if (result.startsWith("is")) {
            result = result.substring(2);
        } else if (result.startsWith("get") || result.startsWith("set")) {
            result = result.substring(3);
        }
        if (result.length() == 1 || result.length() > 1 && !Character.isUpperCase(result.charAt(1))) {
            result = result.substring(0, 1).toLowerCase(Locale.ENGLISH) + result.substring(1);
        }
        return result;
    }

}
