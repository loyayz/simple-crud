package com.loyayz.simple;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
public interface BaseMapper<T> {

    /**
     * 新增（非空字段）
     *
     * @param model 模型对象
     */
    boolean insert(T model);

    /**
     * 批量新增（所有字段）
     */
    boolean batchInsert(List<T> models);

    /**
     * 根据 ID 删除
     *
     * @param id 主键
     */
    boolean deleteById(Serializable id);

    /**
     * 根据主键批量删除
     *
     * @param ids 主键列表
     */
    boolean deleteByIds(Collection<? extends Serializable> ids);

    /**
     * 根据 id 修改（非空字段）
     */
    boolean updateById(T model);

    /**
     * 根据 id 修改（所有字段）
     */
    boolean updateByIdWithNull(T model);

    /**
     * 根据 id 查询
     */
    Optional<T> findById(Serializable id);

    /**
     * 根据 ids 查询
     */
    List<T> listByIds(List<? extends Serializable> ids);

    /**
     * 根据不为 null 的字段查询列表
     */
    List<T> listByCondition(T model, Sorter... sorters);

    /**
     * 根据不为 null 的字段查询分页
     */
    Page<T> pageByCondition(T model, int pageNum, int pageSize, Sorter... sorters);

    /**
     * 根据不为 null 的字段统计
     *
     * @param model 模型对象（可以为 null）
     */
    long countByCondition(T model);

    /**
     * 根据不为 null 的字段，查询是否存在记录
     *
     * @param model 模型对象（可以为 null）
     */
    default boolean existByCondition(T model) {
        long num = this.countByCondition(model);
        return num > 0;
    }

}
