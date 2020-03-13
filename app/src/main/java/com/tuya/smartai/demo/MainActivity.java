package com.tuya.smartai.demo;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONObject;
import com.tuya.smartai.iot_sdk.DPEvent;
import com.tuya.smartai.iot_sdk.HttpResponse;
import com.tuya.smartai.iot_sdk.IoTSDKManager;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = "MainActivity";

    private final int PERMISSION_CODE = 123;

    Disposable httpdisposable;

    private String[] requiredPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    IoTSDKManager ioTSDKManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.reset).setOnClickListener(this::onClick);
        findViewById(R.id.dp_send).setOnClickListener(this::onClick);
        findViewById(R.id.http).setOnClickListener(this::onClick);

        if (!EasyPermissions.hasPermissions(this, requiredPermissions)) {
            EasyPermissions.requestPermissions(this, "需要授予权限以使用设备", PERMISSION_CODE, requiredPermissions);
        } else {
            initSDK();
        }
    }

    private void initSDK() {

        ioTSDKManager = new IoTSDKManager(this);

        ioTSDKManager.initSDK("/sdcard/", "你的pid"
                , "你的uuid", "你的authkey", new IoTSDKManager.IoTCallback() {

                    @Override
                    public void onDpEvent(DPEvent event) {
                        if (event != null) {
                            Log.w(TAG, "rev dp: " + event);
                        }
                    }

                    @Override
                    public void onReset() {
                        Intent mStartActivity = getPackageManager().getLaunchIntentForPackage(getPackageName());
                        if (mStartActivity != null) {
                            int mPendingIntentId = 123456;
                            PendingIntent mPendingIntent = PendingIntent.getActivity(MainActivity.this, mPendingIntentId
                                    , mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                            AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, mPendingIntent);
                            Runtime.getRuntime().exit(0);
                        }
                    }

                    @Override
                    public void onShorturl(String url) {
                        Log.w(TAG, "shorturl: " + url);
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
            case R.id.http:
                JSONObject params = new JSONObject();
                params.put("uuid", "f2ef8b136911f4b0");
                params.put("appId", "U0fxNCEnZptKnQZy");

                if (httpdisposable != null) {
                    httpdisposable.dispose();
                }
                httpdisposable = Observable.just(0)
                        .subscribeOn(Schedulers.io())
                        .subscribe(integer -> {
                            HttpResponse response = ioTSDKManager.httpRequest("tuya.device.qrcode.info.get", "1.0", params.toJSONString());
                            Log.w(TAG, response.toString());
                        });

                break;
            case R.id.dp_send:
                ioTSDKManager.sendDP(1, DPEvent.Type.PROP_BOOL, false);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (httpdisposable != null) {
            httpdisposable.dispose();
        }
    }
}
