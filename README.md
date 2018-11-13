# 厂商通道otherpush demo
## AS自动集成指南
### 1.1 导入依赖

在app build.gradle文件下配置 以下内容
```java
    android {
        ......
        defaultConfig {
            //信鸽官网上注册的包名.注意application ID 和当前的应用包名以及 信鸽官网上注册应用的包名必须一致。
            applicationId "你的包名" 
            ......
            ndk {
                //根据需要 自行选择添加的对应cpu类型的.so库。 
                abiFilters 'armeabi', 'armeabi-v7a', 'arm64-v8a' 
                // 还可以添加 'x86', 'x86_64', 'mips', 'mips64'
            }
            manifestPlaceholders = [
                XG_ACCESS_ID:"注册应用的accessid",
                XG_ACCESS_KEY : "注册应用的accesskey",
                HW_APPID: "华为的APPID"//华为通道需要配置
                PACKAGE_NAME:"应用包名"//小米通道需要配置
            ]
            ......
        }
        ......
    }
    dependencies {
        ......   
    //信鸽jar，包含厂商通道
    compile 'com.tencent.xinge:xinge:4.2.0-otherpush-Beta'
    //jg包
    compile'com.tencent.jg:jg:1.1'
    //wup包
    compile 'com.tencent.wup:wup:1.0.0.E-release'
    //mid包
    compile 'com.tencent.mid:mid:4.0.7'
    }

```
### 1.2 注册以及部分日志输出
配置好信鸽过后，获取信鸽注册日志（接入过程中建议调用有回调的注册接口，开启信鸽的debug日志输出。AndroidStudio 建议采用jcenter自动接入，无需在配置文件中配置信鸽各个节点，全部由依赖导入）。


**开启debug日志数据**
```java
XGPushConfig.enableDebug(this,true);
```
**开启厂商通道初始化代码**
在你的Application的attachBaseContext函数里面增加
```java
StubAppUtils.attachBaseContext(context);
```
在初始化或者主页面onCreat函数里添加
```java
 XGPushConfig.enableOtherPush(getApplicationContext(), true);
 XGPushConfig.setHuaweiDebug(true);
 XGPushConfig.setMiPushAppId(getApplicationContext(), "APPID");
 XGPushConfig.setMiPushAppKey(getApplicationContext(), "APPKEY");
 XGPushConfig.setMzPushAppId(this, "APPID");
 XGPushConfig.setMzPushAppKey(this, "APPKEY");
 ```
 
**token注册**

```java
XGPushManager.registerPush(this, new XGIOperateCallback() {
@Override
public void onSuccess(Object data, int flag) {
//token在设备卸载重装的时候有可能会变
Log.d("TPush", "注册成功，设备token为：" + data);
}
@Override
public void onFail(Object data, int errCode, String msg) {
Log.d("TPush", "注册失败，错误码：" + errCode + ",错误信息：" + msg);
}
})
```

**厂商通道token注册**

1.开启厂商通道初始化，等待云控下载对应设备的厂商dex包 （首次开启注册才会有以下代码）。
以小米为例，下载成功的日志如下：
```xml
10-25 15:16:31.067 16551-16551/? D/XINGE: [DownloadService] onCreate()
10-25 15:16:31.073 16551-16757/? D/XINGE: [DownloadService] action:onHandleIntent
10-25 15:16:31.083 16551-16757/? V/XINGE: [CloudCtrDownload] Create downloadControl
10-25 15:16:31.089 16551-16757/? I/XINGE: [CloudCtrDownload] action:download - url:https://pingjs.qq.com/xg/Xg-Xm-plug-1.0.2.pack, saveFilePath:/data/user/0/com.qq.xgdemo1122/app_dex/XG/5/, fileName:Xg-Xm-plug-1.0.2.pack
10-25 15:16:31.097 16551-16757/? V/XINGE: [CloudCtrDownload] Download file: Xg-Xm-plug-1.0.2.pack
10-25 15:16:31.641 16551-16757/? D/XINGE: [DownloadService] download file Succeed
10-25 15:16:31.650 16551-16757/? D/XINGE: [CloudCtrDownload] Download succeed.
10-25 15:16:31.653 16551-16551/? D/XINGE: [CloudControlDownloadReceiver] onReceive
10-25 15:16:31.673 16551-16738/? I/test: Download file SuccessXg-Xm-plug-1.0.2.pack to /data/user/0/com.qq.xgdemo1122/app_dex/XG/5/
```
2.观察到下载完成的日志后杀死应用进程，再次启动应用即可完成注册：
```xml
10-25 15:34:26.423 18700-18700/? D/TPush: +++ register push sucess. token:22dc455f79d36dec1065418e1d284639bac776b4
10-25 15:34:26.432 18700-18731/? I/XINGE: [XGOtherPush] other push token is : lYDvOWispXGoVADhRyiVdw3krLIolEd21JqdmjqBqDISK+gwl/PBm3tA9U43jxfH other push type: xiaomi
```
