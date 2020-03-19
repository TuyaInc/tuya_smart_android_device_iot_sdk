## Android IoT SDK

### 接入
* 权限要求

```java
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

* 初始化

```java
IoTSDKManager ioTSDKManager = new IoTSDKManager(context);

/**
     * 初始化SDK
     * @param basePath  存储路径
     * @param productId 产品id
     * @param uuid  用户id
     * @param authKey 认证key
     * @param mCallback SDK回调方法
     * @return
     */
ioTSDKManager.initSDK(String basePath, String productId, String uuid, String authorKey, IoTCallback mCallback);


public interface IoTCallback {

        //dp事件接收
        void onDpEvent(DPEvent event);

        //解绑设备
        void onReset();

        //收到配网二维码短链 若为null则获取失败
        void onShorturl(String url);
        
        //设备上线
        void onActive();
        
        /**
         * MQTT状态变化
         * @param status STATUS_OFFLINE 网络离线; 
         *               STATUS_MQTT_OFFLINE 网络在线MQTT离线; 
         *               STATUS_MQTT_ONLINE 网络在线MQTT在线
         */
        void onMQTTStatusChanged(int status);
    }

```

* 销毁  

```java
//会进行广播注销等销毁操作
ioTSDKManager.destroy();
```

### API
```java
//本地解绑
IoTSDKManager.reset();

/**
     * 发送dp事件
     * @param id dp id
     * @param type 类型 DPEvent.Type
     * @param val 值
     * @return
     */
IoTSDKManager.sendDP(int id, int type, Object val)

/**
     * 发送http请求
     * @param apiName 请求api
     * @param apiVersion 版本号
     * @param jsonMsg   参数json
     * @return
     */
IoTSDKManager.httpRequest(String apiName, String apiVersion, String jsonMsg)


//获取设备id
IoTSDKManager.getDeviceId()

//自定义实现网络状态监测，返回值为网络是否离线。SDK已提供默认实现，如无需要不必扩展此方法。
ioTSDKManager = new IoTSDKManager(this) {

            @Override
            protected boolean isOffline() {
                //实现自己的网络状态监测
                return super.isOffline();
            }
        }

```
