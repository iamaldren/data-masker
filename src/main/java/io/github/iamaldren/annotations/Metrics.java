package io.github.iamaldren.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Metrics {

    String timer() default "micrometer.handler.annotation.timer";

    String gauge() default "micrometer.handler.annotation.gauge";

    String count() default "micrometer.handler.annotation.count";

    boolean enableTimer() default true;

    boolean enableGauge() default true;

    boolean enableCounter() default true;

    String[] tags() default {};

}
