package io.github.iamaldren.annotations;

import java.lang.annotation.*;

@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Count {

    String name() default "micrometer.handler.annotation.count";

    String description() default "count metric";

    String[] tags() default {};

    boolean countFailuresOnly() default false;

}
