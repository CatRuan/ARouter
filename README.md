```
    本框架基于Arouter进行修改，添加跨模块调用方法的api，优化模块间通讯
```
#### 本框架继承Arouter原有的所有功能，具体可以参考[中文文档](https://github.com/alibaba/ARouter/blob/master/README_CN.md)  
本框架适用于组件化结构的项目

#### 除此之外添加如下新的api  
1. 跨模块调用同步方法，对比Arouter原有调用方式，不需要创建模块实例，即模块间没有强耦合
``` java
  // 同步方法调用（无参数/无返回）
  ARouter.getInstance().build("/module/service").call("test1");
  
  // 同步方法调用（带参数/无返回）
  ARouter.getInstance().build("/module/service").call("test2", 111, "小阮");
  
  // 同步方法调用（带参数/有返回）
  Object result = ARouter.getInstance().build("/module/service").call("test3", 1111, "小阮", new TestBean(-111, "down"));
  Log.i("ruan", "调用test3,返回值：" + result);
  
  // 同步方法调用（无参数/有返回）
  Object result2 = ARouter.getInstance().build("/module/service").call("test4");
  Log.i("ruan", "调用test4,返回值：" + result2.toString());
               
```  
2. 跨模块调用异步方法，根据route调用，不需要引入模块
``` java
// 同步方法调用（无参数/有返回）,对比Arouter原有调用方式，不需要创建模块实例，即模块间没有强耦合
ARouter.getInstance()
         .build("/module/service")
         .call("test5", "参数1", new MethodCallback<TestBean>() {

             @Override
             public void onNext(Object... object) {

             }

             @Override
             public void onError(String errorMsg) {

             }

             @Override
             public void onComplete(TestBean result) {
                 Log.i("ruan", "调用test5,返回值：" + result.toString());
             }
```  
3. 暴露模块方法（只允许在Activity或Iprovider中暴露）
``` java
Route(path = "/module/service")
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
```  
#### 缺陷
1、暂时只支持在本类中暴露方法，即子类不会继承父类暴露的方法
