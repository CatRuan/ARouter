package com.alibaba.android.arouter.facade.model;

import java.util.List;


public class MethodModel {
    String methodName;
    List<ParamModel> params;//param types
    // 是否为同步方法
    boolean isSynchronous;

    public MethodModel(String methodName, List<ParamModel> params) {
        this.methodName = methodName;
        this.params = params;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public boolean isSynchronous() {
        return isSynchronous;
    }

    public void setSynchronous(boolean synchronous) {
        isSynchronous = synchronous;
    }

    public List<ParamModel> getParams() {
        return params;
    }

    public void setParams(List<ParamModel> params) {
        this.params = params;
    }

    public Class<?>[] getParamsClass() {
        if (null != params) {
            Class<?>[] cls = new Class[params.size()];
            for (int i = 0; i < params.size(); i++) {
                cls[i] = params.get(i).paramClass;
            }
            return cls;
        }
        return null;
    }
}
