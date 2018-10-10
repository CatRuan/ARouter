package com.alibaba.android.arouter.facade.template;

import com.alibaba.android.arouter.facade.model.MethodModel;

import java.util.List;

/**
 * Root element.
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/8/23 16:36
 */
public interface IRouteMethod {

    /**
     * Load routes to input
     *
     * @param methods input
     */
    void loadMethodInfo(List<MethodModel> methods);
}
