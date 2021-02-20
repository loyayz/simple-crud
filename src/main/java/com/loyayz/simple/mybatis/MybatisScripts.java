package com.loyayz.simple.mybatis;

import com.loyayz.simple.Sorter;
import com.loyayz.simple.helper.ModelField;
import com.loyayz.simple.helper.ModelInfo;
import com.loyayz.simple.helper.Utils;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
final class MybatisScripts {

    /**
     * 转换成 if 标签的脚本片段
     *
     * @param script script
     * @return <if test="属性名 != null">script</if>
     */
    static String wrapperIf(ModelField field, String script) {
        Class<?> propertyType = field.propertyType();
        if (propertyType.isPrimitive()) {
            return script;
        }
        String property = String.format("%s != null", field.property());
        return String.format("<if test=\"%s\">%s</if>", property, script);
    }

    /**
     * 字段列表转为新增脚本
     * insert into table (字段) values (值)
     * 位于 "字段" 部位
     * <if test="...">字段名,</if>
     * <if test="...">字段名,</if>
     * ...
     */
    static String insertColumns(List<ModelField> fields, boolean needIf) {
        String script = fields.stream()
                .map(field -> {
                    if (needIf) {
                        return wrapperIf(field, field.column() + ",");
                    } else {
                        return field.column();
                    }
                })
                .collect(Collectors.joining(needIf ? "\n" : ","));
        if (needIf) {
            return String.format("<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">%s</trim>", script);
        } else {
            return "(" + script + ")";
        }
    }

    /**
     * 字段列表转为新增脚本
     * insert into table (字段) values (值)
     * 位于 "值" 部位
     *
     * @param prefix 非空字符串表示批量插入脚本，不需要 if 标签
     *               #{prefix.属性名},#{prefix.属性名},...
     *               ---
     *               单个插入脚本，需要 if 标签
     *               <if test="...">#{prefix.属性名},</if>
     *               <if test="...">#{prefix.属性名},</if>
     *               ...
     */
    static String insertProperties(List<ModelField> fields, boolean needIf, String prefix) {
        String script = fields.stream()
                .map(field -> {
                    String propertyScript = field.property();
                    if (prefix.isEmpty()) {
                        propertyScript = String.format("#{%s}", propertyScript);
                    } else {
                        propertyScript = String.format("#{%s.%s}", prefix, propertyScript);
                    }
                    if (needIf) {
                        return wrapperIf(field, propertyScript + ",");
                    } else {
                        return propertyScript;
                    }
                })
                .collect(Collectors.joining(needIf ? "\n" : ","));
        if (needIf) {
            return String.format("<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">%s</trim>", script);
        } else {
            return "(" + script + ")";
        }
    }

    /**
     * 字段列表转为查询字段
     * 字段名 AS 属性名,字段名 AS 属性名
     */
    static String selectColumns(List<ModelField> fields) {
        return fields.stream()
                .map(field -> {
                    String column = field.column();
                    String property = field.property();
                    return property.equals(column) ?
                            column : column + " AS " + property;
                })
                .collect(Collectors.joining(","));
    }

    /**
     * 字段列表转为条件脚本
     *
     * <where>
     * <if test="...">AND 字段名 = #{属性名}</if>
     * <if test="...">AND 字段名 = #{属性名}</if>
     * ...
     * </where>
     */
    static String conditions(ModelInfo info) {
        String result = info.fields().stream()
                .map(field -> {
                    String script = " AND " + columnEqual(field);
                    return wrapperIf(field, script);
                })
                .collect(Collectors.joining("\n"));
        return "<where>" + result + "</where>";
    }

    /**
     * 字段转为相等的脚本
     *
     * @return 字段名 = #{属性名}
     */
    static String columnEqual(ModelField field) {
        return String.format("%s = #{%s}", field.column(), field.property());
    }

    static String idEqual(ModelInfo info) {
        ModelField idField = info.idField();
        Assert.notNull(idField, String.format("%s 未配置 id 字段", info.modelClass().getName()));
        return String.format("%s=#{id}", idField.column());
    }

    static String idIn(ModelInfo info) {
        ModelField idField = info.idField();
        Assert.notNull(idField, String.format("%s 未配置 id 字段", info.modelClass().getName()));
        String foreachScript = "<foreach collection=\"ids\" item=\"id\" open=\"(\" separator=\",\" close=\")\">#{id}</foreach>";
        return String.format("%s IN \n %s", idField.column(), foreachScript);
    }

    /**
     * 排序脚本
     */
    static String sortScript(ModelInfo info, Sorter... sorters) {
        if (sorters == null || sorters.length == 0) {
            return "";
        }
        List<ModelField> fields = info.fields();
        String script = Arrays.stream(sorters)
                .map(sorter -> {
                    String property = Utils.getLambdaProperty(sorter);
                    String sortType = sorter.orderByAsc() ? "asc" : "desc";
                    for (ModelField field : fields) {
                        if (field.property().equals(property)) {
                            return field.column() + " " + sortType;
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));
        return " ORDER BY " + script;
    }
}
