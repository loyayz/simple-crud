package com.loyayz.simple;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
@Data
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Sorter {
    private Serializable func;
    private boolean orderByAsc;

    public SerializedLambda funcToSerializedLambda() {
        SerializedLambda lambda;
        try {
            Method method = func.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(Boolean.TRUE);
            lambda = (SerializedLambda) method.invoke(func);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return lambda;
    }

    public static <T, R> Sorter asc(SerializedGet<T, R> func) {
        return new Sorter().func(func).orderByAsc(true);
    }

    public static <T, R> Sorter asc(SerializedSet<T, R> func) {
        return new Sorter().func(func).orderByAsc(true);
    }

    public static <T, R> Sorter desc(SerializedGet<T, R> func) {
        return new Sorter().func(func).orderByAsc(false);
    }

    public static <T, R> Sorter desc(SerializedSet<T, R> func) {
        return new Sorter().func(func).orderByAsc(false);
    }

    public interface SerializedGet<T, R> extends Serializable {

        R apply(T t);

    }

    public interface SerializedSet<T, R> extends Serializable {

        void apply(T t, R r);

    }

}
