package com.loyayz.simple.helper;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
public class ModelField implements Serializable {
    private static final long serialVersionUID = -1L;
    private final Field field;
    /**
     * 属性名
     */
    private final String property;
    /**
     * 属性类型
     */
    private final Class<?> propertyType;
    /**
     * 字段名
     */
    private final String column;
    /**
     * 是否可 insert
     */
    @Setter(AccessLevel.PACKAGE)
    private boolean insertable;
    /**
     * 是否可 update
     */
    @Setter(AccessLevel.PACKAGE)
    private boolean updatable;

    static ModelField of(Field field) {
        return new ModelField(field);
    }

    private ModelField(Field field) {
        field.setAccessible(true);
        this.field = field;
        this.property = field.getName();
        this.propertyType = field.getType();

        boolean insertable = true;
        boolean updatable = true;
        String columnName = null;
        if (Utils.jpaPresent) {
            Column column = this.field.getAnnotation(Column.class);
            if (column != null) {
                columnName = column.name();
                insertable = column.insertable();
                updatable = column.updatable();
            }
        }
        this.insertable = insertable;
        this.updatable = updatable;
        if (!StringUtils.hasText(columnName)) {
            columnName = Utils.camelToUnderline(this.property);
        }
        this.column = columnName;
    }

}
