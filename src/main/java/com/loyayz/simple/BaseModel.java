package com.loyayz.simple;

import com.loyayz.simple.helper.ModelHelper;
import com.loyayz.simple.helper.ModelInfo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
@SuppressWarnings("unchecked")
public interface BaseModel<T> {

    /**
     * 保存（新增或修改）
     * id 为 null 时：新增
     * id 不为 null 时：修改
     */
    default boolean save() {
        if (this.idValue() == null) {
            return this.insert();
        } else {
            return this.updateById();
        }
    }

    /**
     * 新增（非空字段）
     */
    default boolean insert() {
        return this.mapper().insert((T) this);
    }

    /**
     * 批量新增（所有字段）
     */
    default boolean batchInsert(List<T> entities) {
        return this.mapper().batchInsert(entities);
    }

    /**
     * 根据主键删除
     */
    default boolean deleteById(Serializable id) {
        return this.mapper().deleteById(id);
    }

    /**
     * 根据主键批量删除
     *
     * @param ids 主键列表
     */
    default boolean deleteByIds(Collection<? extends Serializable> ids) {
        return this.mapper().deleteByIds(ids);
    }

    /**
     * 根据 id 修改（非空字段）
     */
    default boolean updateById() {
        return this.mapper().updateById((T) this);
    }

    /**
     * 根据 id 修改（所有字段）
     */
    default boolean updateByIdWithNull() {
        return this.mapper().updateByIdWithNull((T) this);
    }

    /**
     * 根据 id 查询
     */
    default T findById(Serializable id) {
        return this.mapper().findById(id).orElse(null);
    }

    /**
     * 根据 ids 查询
     */
    default List<T> listByIds(List<? extends Serializable> ids) {
        return this.mapper().listByIds(ids);
    }

    /**
     * 根据不为 null 的字段查询列表
     */
    default List<T> listByCondition(Sorter... sorters) {
        return this.mapper().listByCondition((T) this, sorters);
    }

    /**
     * 根据不为 null 的字段查询分页
     */
    default Page<T> pageByCondition(int pageNum, int pageSize, Sorter... sorters) {
        return this.mapper().pageByCondition((T) this, pageNum, pageSize, sorters);
    }

    /**
     * 根据不为 null 的字段统计
     */
    default long countByCondition() {
        return this.mapper().countByCondition((T) this);
    }

    /**
     * 根据不为 null 的字段，查询是否存在记录
     */
    default boolean existByCondition() {
        return this.mapper().existByCondition((T) this);
    }

    /**
     * 主键值
     */
    default <E extends Serializable> E idValue() {
        ModelInfo info = this.modelInfo();
        if (info == null) {
            return null;
        }
        return (E) info.idValue(this);
    }

    default BaseMapper<T> mapper() {
        return ModelHelper.mapper(getClass());
    }

    default ModelInfo modelInfo() {
        return ModelHelper.modelInfo(getClass());
    }

}
