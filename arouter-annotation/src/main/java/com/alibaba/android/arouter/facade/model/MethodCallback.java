package com.alibaba.android.arouter.facade.model;

public interface MethodCallback<T> {

    void onNext(Object... object);

    void onError(String errorMsg);

    void onComplete(T result);
}
