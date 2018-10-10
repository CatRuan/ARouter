package com.alibaba.android.arouter.facade.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a page can be route by router.
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/8/15 下午9:29
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface Route {

    /**
     * 路由地址
     */
    String path();

    /**
     * 将路由节点进行分组，可以实现按组动态加载
     */
    String group() default "";

    /**
     * 路由名称，用于生成文档
     */
    String name() default "undefined";

    /**
     * 用32位int类型标示，可用于页面的一些配置
     */
    int extras() default Integer.MIN_VALUE;

    /**
     * 路由的优先级
     */
    int priority() default -1;
}
