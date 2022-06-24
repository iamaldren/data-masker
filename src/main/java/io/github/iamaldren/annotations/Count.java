package io.github.iamaldren.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Count {

    String name() default "micrometer.handler.annotation.count";

    String[] tags() default {};

    boolean countFailuresOnly() default false;

}
