package com.loyayz.simple.annotation;

import java.lang.annotation.*;

/**
 * 主键策略
 *
 * @author loyayz (loyayz@foxmail.com)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface IdStrategy {

    /**
     * 雪花算法默认起始时间（+8 2021-01-01 00:00:00）
     */
    long SNOWFLAKE_BEGIN_TIME = 1609430400000L;

    /**
     * 主键策略类型
     */
    IdStrategyType type() default IdStrategyType.SNOWFLAKE;

    /**
     * 雪花算法起始时间
     * 可通过系统参数设置全局起始时间，例 -Dsimple.snowflake=1609430400000
     * 优先级：系统参数 > 注解值
     */
    long beginTime() default SNOWFLAKE_BEGIN_TIME;

}
