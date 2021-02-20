package com.loyayz.simple.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(ModelScannerRegistrar.RepeatingRegistrar.class)
public @interface ModelScans {

    ModelScan[] value();

}
