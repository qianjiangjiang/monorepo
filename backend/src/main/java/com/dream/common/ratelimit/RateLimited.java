package com.dream.common.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    String keyPrefix();

    int limit();

    long windowSeconds();

    boolean includeIp() default true;

    boolean byUser() default false;
}
