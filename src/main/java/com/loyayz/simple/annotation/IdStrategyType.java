package com.loyayz.simple.annotation;

import com.loyayz.simple.helper.Snowflake;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
public enum IdStrategyType {
    /**
     * 雪花算法
     *
     * @see Snowflake
     */
    SNOWFLAKE,
    /**
     * UUID.replace("-", "")
     */
    UUID,
    /**
     * 数据库自增主键
     * 此时数据库表需要定义主键自增
     */
    AUTO
}
