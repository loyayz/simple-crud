package com.loyayz.simple.helper;

import com.loyayz.simple.BaseMapper;
import com.loyayz.simple.mybatis.MybatisBaseMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
@SuppressWarnings("all")
public final class ModelHelper {
    private static final Map<Class<?>, BaseMapper> MAPPER_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, ModelInfo> INFO_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取模型类对应的 mapper
     *
     * @param clazz 模型类
     * @return mapper
     */
    public static <T> BaseMapper<T> mapper(Class<?> clazz) {
        clazz = Utils.getTargetClass(clazz);
        return MAPPER_CACHE.get(clazz);
    }

    /**
     * 获取模型类对应的表信息
     *
     * @param clazz 模型类
     * @return 表信息
     */
    public static ModelInfo modelInfo(Class<?> clazz) {
        clazz = Utils.getTargetClass(clazz);
        return INFO_CACHE.get(clazz);
    }

    /**
     * 初始化模型类对应的表信息
     *
     * @param clazz 模型类
     * @return mapper
     */
    public synchronized static BaseMapper<?> init(Class<?> clazz) {
        clazz = Utils.getTargetClass(clazz);
        ModelInfo info = ModelInfo.of(clazz);
        BaseMapper<?> mapper = new MybatisBaseMapper(clazz, info);
        MAPPER_CACHE.put(clazz, mapper);
        INFO_CACHE.put(clazz, info);
        return mapper;
    }

    /**
     * 是否已初始化
     *
     * @param clazz 模型类
     * @return 是否已初始化
     */
    public static boolean exist(Class<?> clazz) {
        clazz = Utils.getTargetClass(clazz);
        return MAPPER_CACHE.containsKey(clazz);
    }

}
