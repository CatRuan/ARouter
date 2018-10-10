package com.alibaba.android.arouter.exception;

public class CallMethodException extends Exception {

    private int exceptionType;

    public CallMethodException(int exceptionType, String detailException) {
        super(detailException);
        this.exceptionType = exceptionType;
    }

}
