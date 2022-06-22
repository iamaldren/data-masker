package io.github.iamaldren.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Time {

    String name() default "micrometer.handler.annotation.timer";

    String[] tags() default {};

    boolean longTask() default false;

    double[] percentiles() default {};

    boolean publishPercentiles() default false;

}
