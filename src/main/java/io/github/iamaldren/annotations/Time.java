package io.github.iamaldren.annotations;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Inherited
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Time {

    String name() default "micrometer.handler.annotation.timer";

    String[] tags() default {};

    boolean longTask() default false;

    double[] percentiles() default {};

    boolean publishPercentiles() default false;

    boolean enableCustomLogging() default false;

    TimeUnit timerUnit() default TimeUnit.MILLISECONDS;

    String loggingMessage() default "Total execution time is {}";

}
