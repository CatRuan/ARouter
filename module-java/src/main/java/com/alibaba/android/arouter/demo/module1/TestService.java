package com.alibaba.android.arouter.demo.module1;

import android.content.Context;
import android.util.Log;

import com.alibaba.android.arouter.demo.module1.bean.TestBean;
import com.alibaba.android.arouter.facade.annotation.Method;
import com.alibaba.android.arouter.facade.annotation.MethodCallBack;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.model.MethodCallback;
import com.alibaba.android.arouter.facade.template.IProvider;

@Route(path = "/module/service")
public class TestService implements IProvider {

    @Override
    public void init(Context context) {

    }

    @Method
    public void test1() {
        Log.i("ruan", "test1");
    }

    @Method
    public void test2(int parma1, String param2) {
        Log.i("ruan", "test2-" + parma1 + "," + param2);
    }

    @Method
    public int test3(int parma1, String param2, TestBean testBean) {
        Log.i("ruan", "test3-" + parma1 + "," + param2 + "," + testBean);
        return 1;
    }

    @Method
    public TestBean test4() {
        Log.i("ruan", "test4");
        return new TestBean(1, "hahah");
    }

    @Method
    public void test5(String params1, @MethodCallBack MethodCallback<TestBean> callback) {
        Log.i("ruan", "test4");
        TestBean result = new TestBean(1, "result");
        callback.onComplete(result);
    }
}
