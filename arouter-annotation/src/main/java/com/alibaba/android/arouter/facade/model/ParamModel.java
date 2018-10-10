package com.alibaba.android.arouter.facade.model;

public class ParamModel {
    Class<?> paramClass;
    Object paramMirror;

    public ParamModel(Class<?> paramClass, Object paramMirror) {
        this.paramClass = paramClass;
        this.paramMirror = paramMirror;
    }

    public Class<?> getParamClass() {
        return paramClass;
    }

    public void setParamClass(Class<?> paramClass) {
        this.paramClass = paramClass;
    }

    public Object getParamMirror() {
        return paramMirror;
    }

    public void setParamMirror(Object paramMirror) {
        this.paramMirror = paramMirror;
    }
}
