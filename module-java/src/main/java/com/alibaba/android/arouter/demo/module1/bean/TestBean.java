package com.alibaba.android.arouter.demo.module1.bean;

public class TestBean {
    private int param1;
    private String param2;

    public int getParam1() {
        return param1;
    }

    public void setParam1(int param1) {
        this.param1 = param1;
    }

    public String getParam2() {
        return param2;
    }

    public void setParam2(String param2) {
        this.param2 = param2;
    }

    public TestBean(int param1, String param2) {
        this.param1 = param1;
        this.param2 = param2;
    }

    @Override
    public String toString() {
        return "TestBean{" +
                "param1=" + param1 +
                ", param2='" + param2 + '\'' +
                '}';
    }
}
