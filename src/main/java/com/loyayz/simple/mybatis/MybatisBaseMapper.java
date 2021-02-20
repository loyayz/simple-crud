package com.loyayz.simple.mybatis;

import com.loyayz.simple.BaseMapper;
import com.loyayz.simple.Page;
import com.loyayz.simple.Sorter;
import com.loyayz.simple.helper.ModelField;
import com.loyayz.simple.helper.ModelInfo;
import com.loyayz.simple.helper.Utils;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.apache.ibatis.mapping.SqlCommandType.*;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
public class MybatisBaseMapper<T> implements BaseMapper<T> {
    private final Class<T> modelClass;
    private final ModelInfo modelInfo;
    private final String modelName;
    /**
     * 缓存查询片段
     * SELECT 字段列表 FROM 表名
     */
    private final String selectScript;
    /**
     * 缓存查询片段
     * SELECT 字段列表 FROM 表名 where 条件
     */
    private final String selectByConditionScript;

    @Lazy
    @Autowired
    private SqlSession sqlSession;

    public MybatisBaseMapper(Class<T> modelClass, ModelInfo modelInfo) {
        this.modelClass = modelClass;
        this.modelInfo = modelInfo;
        this.modelName = modelInfo.modelName();
        this.selectScript = String.format("SELECT %s FROM %s ",
                MybatisScripts.selectColumns(modelInfo.fields()), this.modelName);
        this.selectByConditionScript = this.selectScript + MybatisScripts.conditions(this.modelInfo);
    }

    @Override
    public boolean insert(T model) {
        this.modelInfo.fillIdIfNull(model);
        String msId = this.addMappedStatement("insert", INSERT, Integer.class,
                () -> {
                    List<ModelField> fields = this.modelInfo.getInsertFields();
                    String columns = MybatisScripts.insertColumns(fields, true);
                    String properties = MybatisScripts.insertProperties(fields, true, "");
                    return String.format("<script>INSERT INTO %s \n %s VALUES \n %s</script>",
                            this.modelName, columns, properties);
                });
        return sqlSession.insert(msId, model) == 1;
    }

    @Override
    public boolean batchInsert(List<T> models) {
        if (models.isEmpty()) {
            return false;
        }
        for (T model : models) {
            this.modelInfo.fillIdIfNull(model);
        }
        String msId = this.addMappedStatement("batchInsert", INSERT, Integer.class,
                () -> {
                    String itemPrefix = "ent";
                    List<ModelField> fields = this.modelInfo.getInsertFields();
                    String columns = MybatisScripts.insertColumns(fields, false);
                    String properties = MybatisScripts.insertProperties(fields, false, itemPrefix);
                    properties = String.format("<foreach collection=\"list\" item=\"%s\" separator=\",\">%s</foreach>",
                            itemPrefix, properties);
                    return String.format("<script>INSERT INTO %s \n %s VALUES \n %s</script>",
                            this.modelName, columns, properties);
                });
        Map<String, Object> param = new HashMap<>(3);
        param.put("list", models);
        return sqlSession.insert(msId, param) > 0;
    }

    @Override
    public boolean deleteById(Serializable id) {
        if (id == null) {
            return false;
        }
        String msId = this.addMappedStatement("deleteById", DELETE, Integer.class,
                () -> String.format("<script>DELETE FROM %s WHERE %s</script>",
                        this.modelName, MybatisScripts.idEqual(this.modelInfo))
        );
        return sqlSession.delete(msId, id) == 1;
    }

    @Override
    public boolean deleteByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        String msId = this.addMappedStatement("deleteByIds", DELETE, Integer.class,
                () -> String.format("<script>DELETE FROM %s WHERE \n %s</script>",
                        this.modelName, MybatisScripts.idIn(this.modelInfo))
        );
        Map<String, Object> param = new HashMap<>(3);
        param.put("ids", ids);
        return sqlSession.delete(msId, param) > 0;
    }

    @Override
    public boolean updateById(T model) {
        return this.updateById(model, false);
    }

    @Override
    public boolean updateByIdWithNull(T model) {
        return this.updateById(model, true);
    }

    @Override
    public Optional<T> findById(Serializable id) {
        if (id == null) {
            return Optional.empty();
        }
        String msId = this.addMappedStatement("findById", SELECT, modelClass,
                () -> String.format("<script>%s WHERE %s</script>",
                        this.selectScript,
                        MybatisScripts.idEqual(this.modelInfo))
        );
        return Optional.ofNullable(sqlSession.selectOne(msId, id));
    }

    @Override
    public List<T> listByIds(List<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        String msId = this.addMappedStatement("listByIds", SELECT, modelClass,
                () -> String.format("<script>%s WHERE %s</script>",
                        this.selectScript,
                        MybatisScripts.idIn(this.modelInfo))
        );
        Map<String, Object> param = new HashMap<>(3);
        param.put("ids", ids);
        return sqlSession.selectList(msId, param);
    }

    @Override
    public List<T> listByCondition(T model, Sorter... sorters) {
        String script = this.selectByConditionScript + MybatisScripts.sortScript(this.modelInfo, sorters);
        String finalScript = "<script>" + script + "</script>";
        String msId = this.addMappedStatement("listByCondition_" + script.hashCode(),
                SELECT, modelClass, () -> finalScript);
        return sqlSession.selectList(msId, model);
    }

    @Override
    public Page<T> pageByCondition(T model, int pageNum, int pageSize, Sorter... sorters) {
        if (Utils.pageHelperPresent) {
            return Pages.doSelectPage(pageNum, pageSize,
                    () -> this.listByCondition(model, sorters),
                    () -> this.countByCondition(model)
            );
        }
        throw new IllegalArgumentException("查无 mybatis 分页插件，只支持 PageHelper");
    }

    @Override
    public long countByCondition(T model) {
        String msId = this.addMappedStatement("countByCondition", SELECT, Long.class,
                () -> String.format("<script>SELECT COUNT(*) FROM %s %s</script>",
                        this.modelName,
                        MybatisScripts.conditions(this.modelInfo))
        );
        Long result = sqlSession.selectOne(msId, model);
        return result == null ? 0 : result;
    }

    private boolean updateById(T model, boolean updateNull) {
        String methodName = updateNull ? "updateByIdWithNull" : "updateById";
        String msId = this.addMappedStatement(methodName, UPDATE, Integer.class,
                () -> {
                    ModelField idField = this.modelInfo.idField();
                    Assert.notNull(idField, String.format("%s 未配置 id 字段", modelInfo.modelClass().getName()));
                    String script = this.modelInfo.getUpdateFields().stream()
                            .map(field -> {
                                String set = MybatisScripts.columnEqual(field) + ",";
                                return updateNull ? set : MybatisScripts.wrapperIf(field, set);
                            })
                            .collect(Collectors.joining("\n"));
                    return String.format("<script>UPDATE %s \n <set>%s</set> \n WHERE %s</script>",
                            this.modelName, script, MybatisScripts.columnEqual(idField));
                });
        return sqlSession.update(msId, model) == 1;
    }


    private String addMappedStatement(String methodName, SqlCommandType commandType, Class<?> resultType,
                                      Supplier<String> sql) {
        String msId = String.format("%s.%s.%s", modelClass.getName(), methodName, commandType);
        Configuration configuration = sqlSession.getConfiguration();
        if (configuration.hasStatement(msId, false)) {
            return msId;
        }
        String keyColumn = null, keyProperty = null;
        KeyGenerator keyGenerator = new NoKeyGenerator();
        if (commandType == INSERT && this.modelInfo.autoId()) {
            ModelField idField = this.modelInfo.idField();
            keyColumn = idField == null ? null : idField.column();
            keyProperty = idField == null ? null : idField.property();
            keyGenerator = new Jdbc3KeyGenerator();
        }

        SqlSource sqlSource = configuration
                .getDefaultScriptingLanguageInstance()
                .createSqlSource(configuration, sql.get(), null);
        MappedStatement ms = new MappedStatement.Builder(configuration, msId, sqlSource, commandType)
                .resultMaps(Collections.singletonList(
                        new ResultMap.Builder(configuration, msId, resultType, new ArrayList<>()).build()
                ))
                .keyGenerator(keyGenerator)
                .keyColumn(keyColumn)
                .keyProperty(keyProperty)
                .build();
        configuration.addMappedStatement(ms);
        return msId;
    }

}
