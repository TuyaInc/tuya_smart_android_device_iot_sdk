package com.tuya.smartai.demo;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONObject;
import com.tuya.smartai.iot_sdk.DPEvent;
import com.tuya.smartai.iot_sdk.IoTSDKManager;

import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import io.reactivex.disposables.Disposable;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = "MainActivity";

    private final int PERMISSION_CODE = 123;

    Disposable httpdisposable;

    private String[] requiredPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    IoTSDKManager ioTSDKManager;

    ImageView qrCode;
    TextView shortUrl;
    TextView console;

    Switch boolView;
    EditText intView;
    RadioGroup enumView;
    EditText strView;
    EditText rawView;
    EditText bitmapView;

    AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.reset).setOnClickListener(this::onClick);
        qrCode = findViewById(R.id.qrcode);
        console = findViewById(R.id.console);
        console.setOnLongClickListener(v -> {
            clear();
            return false;
        });
        shortUrl = findViewById(R.id.shorturl);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        dialog = builder
                .setCancelable(false)
                .setTitle("提示")
                .setMessage("重启APP完成解绑")
                .setPositiveButton("确认", (dialog1, which) -> {
                    Intent mStartActivity = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    if (mStartActivity != null) {
                        int mPendingIntentId = 123456;
                        PendingIntent mPendingIntent = PendingIntent.getActivity(MainActivity.this, mPendingIntentId
                                , mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, mPendingIntent);
                        Runtime.getRuntime().exit(0);
                    }
                })
                .create();

        initDPViews();

        if (!EasyPermissions.hasPermissions(this, requiredPermissions)) {
            EasyPermissions.requestPermissions(this, "需要授予权限以使用设备", PERMISSION_CODE, requiredPermissions);
        } else {
            initSDK();
        }
    }

    private void initDPViews() {

        findViewById(R.id.bool_send).setOnClickListener(this::onClick);
        findViewById(R.id.int_send).setOnClickListener(this::onClick);
        findViewById(R.id.enum_send).setOnClickListener(this::onClick);
        findViewById(R.id.string_send).setOnClickListener(this::onClick);
        findViewById(R.id.bitmap_send).setOnClickListener(this::onClick);
        findViewById(R.id.raw_send).setOnClickListener(this::onClick);
        findViewById(R.id.combo_send).setOnClickListener(this::onClick);

        boolView = findViewById(R.id.bool_val);
        intView = findViewById(R.id.int_val);
        enumView = findViewById(R.id.enum_val);
        strView = findViewById(R.id.string_val);
        rawView = findViewById(R.id.raw_val);
        bitmapView = findViewById(R.id.bitmap_val);
    }

    private void initSDK() {

        ioTSDKManager = new IoTSDKManager(this) {
            @Override
            protected boolean isOffline() {
                //实现自定义网络监测
                return super.isOffline();
            }
        };

        //注意：这里的pid等配置读取自local.properties文件，不能直接使用。请填写你自己的配置！
        ioTSDKManager.initSDK("/sdcard/", "xqcwrcjnq6smfygq"
                , BuildConfig.UUID, BuildConfig.AUTHOR_KEY, new IoTSDKManager.IoTCallback() {

                    @Override
                    public void onDpEvent(DPEvent event) {
                        if (event != null) {
                            output("收到 dp: " + event);
                        }
                    }

                    @Override
                    public void onReset() {

                        runOnUiThread(() -> dialog.show());
                    }

                    @Override
                    public void onShorturl(String urlJson) {
                        output("shorturl: " + urlJson);

                        String url = (String) JSONObject.parseObject(urlJson).get("shortUrl");

                        runOnUiThread(() -> {
                            shortUrl.setText("用涂鸦智能APP扫码激活");
                            qrCode.setImageBitmap(QRCodeUtil.createQRCodeBitmap(url, 400, 400));
                        });
                    }

                    @Override
                    public void onActive() {
                        output("onActive: devId-> " + ioTSDKManager.getDeviceId());

                        runOnUiThread(() -> {
                            shortUrl.setText("激活成功了");
                        });
                    }

                    @Override
                    public void onFirstActive() {
                        output("onFirstActive");
                    }

                    @Override
                    public void onMQTTStatusChanged(int status) {
                        output("onMQTTStatusChanged: " + status);

                        switch (status) {
                            case IoTSDKManager.STATUS_OFFLINE:
                                // 设备网络离线
                                break;
                            case IoTSDKManager.STATUS_MQTT_OFFLINE:
                                // 网络在线MQTT离线
                                break;
                            case IoTSDKManager.STATUS_MQTT_ONLINE:
                                // 网络在线MQTT在线
                                break;
                        }
                    }
                });

    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        initSDK();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    public void onClick(View v) {
        if (ioTSDKManager == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.reset:
                ioTSDKManager.reset();
                break;
//            case R.id.http:
//                JSONObject params = new JSONObject();
//                params.put("uuid", "1234");
//                params.put("appId", "10000");
//
//                if (httpdisposable != null) {
//                    httpdisposable.dispose();
//                }
//                httpdisposable = Observable.just(0)
//                        .subscribeOn(Schedulers.io())
//                        .subscribe(integer -> {
//                            HttpResponse response = ioTSDKManager.httpRequest("tuya.device.qrcode.info.get", "1.0", params.toJSONString());
//                            Log.w(TAG, response.toString());
//                        });
//
//                break;
            case R.id.bool_send:
                ioTSDKManager.sendDP(101, DPEvent.Type.PROP_BOOL, boolView.isChecked());
                break;
            case R.id.int_send:
                int intVal = Integer.parseInt(intView.getText().toString());
                ioTSDKManager.sendDP(102, DPEvent.Type.PROP_VALUE, intVal);
                break;
            case R.id.string_send:
                ioTSDKManager.sendDP(104, DPEvent.Type.PROP_STR, strView.getText().toString());
                break;
            case R.id.enum_send:
                int checked = 0;
                int radioButtonId = enumView.getCheckedRadioButtonId();
                switch (radioButtonId) {
                    case R.id.enum_0:
                        checked = 0;
                        break;
                    case R.id.enum_1:
                        checked = 1;
                        break;
                    case R.id.enum_2:
                        checked = 2;
                        break;
                }
                ioTSDKManager.sendDP(103, DPEvent.Type.PROP_ENUM, checked);
                break;
            case R.id.raw_send:
                ioTSDKManager.sendDP(105, DPEvent.Type.PROP_RAW, rawView.getText().toString().getBytes(Charset.forName("UTF-8")));
                break;
            case R.id.bitmap_send:
                ioTSDKManager.sendDP(106, DPEvent.Type.PROP_BITMAP, Integer.parseInt(bitmapView.getText().toString()));
                break;
            case R.id.combo_send:
                DPEvent event0 = new DPEvent(101, (byte) DPEvent.Type.PROP_BOOL, boolView.isChecked(), System.currentTimeMillis());
                DPEvent event1 = new DPEvent(102, (byte) DPEvent.Type.PROP_VALUE, Integer.parseInt(intView.getText().toString()), System.currentTimeMillis());
                DPEvent event2 = new DPEvent(105, (byte) DPEvent.Type.PROP_RAW, rawView.getText().toString().getBytes(Charset.forName("UTF-8")), System.currentTimeMillis());
                DPEvent[] events = {event0, event1, event2};
                ioTSDKManager.sendDP(events);
                break;
        }
    }

    private void output(String text) {
        runOnUiThread(() -> console.append(text + "\n"));
        Log.d(TAG, text);
    }

    private void clear() {
        runOnUiThread(() -> console.setText("接收日志在这里输出(长按清除): \n"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (httpdisposable != null) {
            httpdisposable.dispose();
        }

        if (ioTSDKManager != null) {
            ioTSDKManager.destroy();
        }
    }
}
