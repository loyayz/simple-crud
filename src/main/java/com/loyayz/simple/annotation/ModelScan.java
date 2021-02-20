package com.loyayz.simple.annotation;

import com.loyayz.simple.BaseModel;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(ModelScannerRegistrar.class)
@Repeatable(ModelScans.class)
public @interface ModelScan {
    /**
     * Alias for basePackages
     */
    String[] value() default {};

    /**
     * Alias for value
     * 扫描指定包名下，继承 superClass() 或 annotationClass() 注解的类
     * 未指定包名时默认扫描添加 @ModelScan 注解的类所在的包
     * 例：
     * - package com.sample.simple;
     * - @ModelScan
     * - public class Test {}
     * 会扫描 com.sample.simple
     */
    String[] basePackages() default {};

    /**
     * 模型类的父类
     */
    Class<?> superClass() default BaseModel.class;

    /**
     * 模型类的注解
     */
    Class<? extends Annotation>[] annotationClass() default {};

    /**
     * 模型类型
     */
    ModelType type() default ModelType.RDBMS;

}
