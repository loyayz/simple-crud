package com.loyayz.simple.annotation;

import com.loyayz.simple.BaseMapper;
import com.loyayz.simple.helper.ModelHelper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
@SuppressWarnings("all")
public class ModelScannerRegistrar implements ImportBeanDefinitionRegistrar {
    private static final String MAPPER_BEAN_NAME_PREFIX = "baseMapper#";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes mapperScanAttrs =
                AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(ModelScan.class.getName()));
        if (mapperScanAttrs != null) {
            String defaultBasePackage = getDefaultBasePackage(importingClassMetadata);
            this.registerBeanDefinitions(registry, mapperScanAttrs,defaultBasePackage);
        }
    }

    @SneakyThrows
    void registerBeanDefinitions( BeanDefinitionRegistry registry, AnnotationAttributes attrs,String defaultBasePackage) {
        List<String> basePackages = new ArrayList<>();
        basePackages.addAll(
                Arrays.stream(attrs.getStringArray("value")).filter(StringUtils::hasText).collect(Collectors.toList())
        );
        basePackages.addAll(
                Arrays.stream(attrs.getStringArray("basePackages")).filter(StringUtils::hasText).collect(Collectors.toList())
        );
        if (basePackages.isEmpty()) {
            basePackages.add(defaultBasePackage);
        }
        Class<?> superClass = attrs.getClass("superClass");
        Class[] annotationClass = attrs.getClassArray("annotationClass");
        ClassPathModelScanner scanner = new ClassPathModelScanner(basePackages, superClass, annotationClass);
        Set<String> modelClassNames = scanner.scan();
        for (String modelClassName : modelClassNames) {
            Class<?> modelClass = ClassUtils.forName(modelClassName, ModelScannerRegistrar.class.getClassLoader());
            if (ModelHelper.exist(modelClass)) {
                continue;
            }
            BaseMapper<?> mapper = ModelHelper.init(modelClass);
            RootBeanDefinition bean = new RootBeanDefinition(BaseMapper.class, () -> mapper);
            bean.setTargetType(ResolvableType.forClassWithGenerics(BaseMapper.class, modelClass));
            registry.registerBeanDefinition(beanName(modelClass), bean);
        }
    }

    private static String beanName(Class<?> modelClass) {
        return MAPPER_BEAN_NAME_PREFIX + modelClass.getName();
    }

    private static String getDefaultBasePackage(AnnotationMetadata importingClassMetadata) {
        return ClassUtils.getPackageName(importingClassMetadata.getClassName());
    }

    static class RepeatingRegistrar extends ModelScannerRegistrar {
        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            AnnotationAttributes mapperScansAttrs =
                    AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(ModelScans.class.getName()));
            String defaultBasePackage = getDefaultBasePackage(importingClassMetadata);
            if (mapperScansAttrs != null) {
                AnnotationAttributes[] annotations = mapperScansAttrs.getAnnotationArray("value");
                for (AnnotationAttributes annotation : annotations) {
                    super.registerBeanDefinitions(registry, annotation,defaultBasePackage);
                }
            }
        }
    }

    static class ClassPathModelScanner extends ClassPathScanningCandidateComponentProvider {
        private final String[] basePackages;

        ClassPathModelScanner(List<String> basePackages,
                              Class<?> superClass,
                              Class<? extends Annotation>[] annotationClass) {
            super(false);
            super.addIncludeFilter(new AssignableTypeFilter(superClass));
            for (Class<? extends Annotation> clazz : annotationClass) {
                super.addIncludeFilter(new AnnotationTypeFilter(clazz));
            }
            String basePackage = StringUtils.collectionToCommaDelimitedString(basePackages);
            this.basePackages = StringUtils.tokenizeToStringArray(basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
        }

        Set<String> scan() {
            Set<BeanDefinition> beans = new HashSet<>();
            for (String basePackage : this.basePackages) {
                beans.addAll(super.findCandidateComponents(basePackage));
            }
            return beans.stream().map(BeanDefinition::getBeanClassName).collect(Collectors.toSet());
        }
    }

}
