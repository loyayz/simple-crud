package com.loyayz.simple.helper;

import com.loyayz.simple.annotation.IdStrategy;
import com.loyayz.simple.annotation.IdStrategyType;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
@Getter
@Accessors(fluent = true)
public class ModelInfo implements Serializable {
    private static final long serialVersionUID = -1L;

    /**
     * 默认表主键名称
     */
    private static final String DEFAULT_ID_NAME = "id";
    /**
     * 模型类
     */
    private final Class<?> modelClass;
    /**
     * 模型名（表名）
     */
    private final String modelName;
    /**
     * 字段列表
     */
    private final List<ModelField> fields;
    /**
     * 主键字段
     */
    private final ModelField idField;
    /**
     * 主键生成器
     */
    private final IdGenerator idGenerator;
    /**
     * 是否自增主键
     */
    private final boolean autoId;

    static ModelInfo of(Class<?> modelClass) {
        return new ModelInfo(modelClass);
    }

    @SneakyThrows
    public <T> Object idValue(T model) {
        Assert.notNull(idField, String.format("%s 未配置 id 字段", modelClass.getName()));
        return idField.field().get(model);
    }

    /**
     * 可新增的字段列表
     */
    public List<ModelField> getInsertFields() {
        return this.fields.stream()
                .filter(ModelField::insertable)
                .collect(Collectors.toList());
    }

    /**
     * 可修改的字段列表
     */
    public List<ModelField> getUpdateFields() {
        return this.fields.stream()
                .filter(ModelField::updatable)
                .collect(Collectors.toList());
    }

    @SneakyThrows
    public <T> void fillIdIfNull(T model) {
        if (idGenerator == null || idField == null) {
            return;
        }
        if (this.idValue(model) == null) {
            Object id = this.idGenerator.generate(this);
            if (id != null) {
                if (String.class.isAssignableFrom(idField.propertyType())) {
                    id = String.valueOf(id);
                }
                idField.field().set(model, id);
            }
        }
    }

    private ModelInfo(Class<?> modelClass) {
        this.modelClass = modelClass;
        this.modelName = obtainModelName(modelClass);
        this.fields = obtainFields(modelClass);
        this.idField = obtainIdField(this.fields);
        this.idGenerator = obtainIdGenerator(this.idField);
        this.autoId = this.idGenerator == null;
        if (this.idField != null) {
            // 主键字段不可修改
            this.idField.updatable(false);
            // 自增主键不可新增
            if (this.autoId) {
                this.idField.insertable(false);
            }
        }
    }

    /**
     * 获取模型名，默认为类名转为下划线
     * 当类有 @Table 注解时：
     * 若 name() 有值，则表名为 name()
     * 若 schema() 有值，则表名为 schema().表名
     */
    private static String obtainModelName(Class<?> clazz) {
        String modelName = Utils.camelToUnderline(clazz.getSimpleName());
        if (Utils.jpaPresent) {
            Table table = clazz.getAnnotation(Table.class);
            if (table != null) {
                if (StringUtils.hasText(table.name())) {
                    modelName = table.name();
                }
                if (StringUtils.hasText(table.schema())) {
                    modelName = table.schema() + "." + modelName;
                }
            }
        }
        return modelName;
    }

    /**
     * 获取表字段
     */
    private static List<ModelField> obtainFields(Class<?> clazz) {
        Predicate<Field> fieldFilter = field -> {
            int modifiers = field.getModifiers();
            // 过滤掉静态属性
            boolean isStatic = Modifier.isStatic(modifiers);
            // 过滤掉 transient关键字修饰的属性
            boolean isTransient = Modifier.isTransient(modifiers);
            if (!isTransient) {
                // 过滤掉 @Transient 注解的属性
                isTransient = Utils.jpaPresent && field.isAnnotationPresent(Transient.class);
            }
            return !isStatic && !isTransient;
        };
        return Utils.getFields(clazz, fieldFilter)
                .stream()
                .map(ModelField::of)
                .collect(Collectors.toList());
    }

    /**
     * 获取表主键字段
     */
    private static ModelField obtainIdField(List<ModelField> fields) {
        ModelField idField = null;
        for (ModelField field : fields) {
            if (Utils.jpaPresent && field.field().isAnnotationPresent(Id.class)) {
                return field;
            }
            if (DEFAULT_ID_NAME.equals(field.property())) {
                idField = field;
            }
        }
        return idField;
    }

    /**
     * 获取表主键策略，默认使用雪花算法
     */
    private static IdGenerator obtainIdGenerator(ModelField idField) {
        IdStrategy strategy = idField == null ?
                null : idField.field().getAnnotation(IdStrategy.class);
        long beginTime = 0;
        if (strategy != null) {
            if (strategy.type() == IdStrategyType.AUTO) {
                return null;
            } else if (strategy.type() == IdStrategyType.UUID) {
                return UUID_ID_GENERATOR;
            }
            beginTime = strategy.beginTime();
        }
        // 优先级：系统参数 > 注解值
        String beginTimeParam = System.getProperty("simple.snowflake", "");
        if (!"".equals(beginTimeParam)) {
            beginTime = Long.parseLong(beginTimeParam);
        }
        if (beginTime <= 0 || beginTime == IdStrategy.SNOWFLAKE_BEGIN_TIME) {
            return DEFAULT_SNOWFLAKE_ID_GENERATOR;
        } else {
            return new SnowflakeIdGenerator(beginTime);
        }
    }

    private interface IdGenerator {

        /**
         * 生成主键值
         *
         * @param info 表信息
         * @return 主键值
         */
        Object generate(ModelInfo info);

    }

    private static final IdGenerator UUID_ID_GENERATOR = info -> UUID.randomUUID().toString().replace("-", "");
    private static final IdGenerator DEFAULT_SNOWFLAKE_ID_GENERATOR = new SnowflakeIdGenerator(IdStrategy.SNOWFLAKE_BEGIN_TIME);

    private static class SnowflakeIdGenerator implements IdGenerator {
        private final Snowflake snowflake;

        @Override
        public Object generate(ModelInfo info) {
            return snowflake.nextId();
        }

        public SnowflakeIdGenerator(long beginTime) {
            this.snowflake = new Snowflake(beginTime);
        }
    }

}
