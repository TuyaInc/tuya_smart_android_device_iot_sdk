package com.tuya.smartai.demo;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSONObject;
import com.tuya.smartai.iot_sdk.DPEvent;
import com.tuya.smartai.iot_sdk.IoTSDKManager;
import com.tuya.smartai.iot_sdk.Log;
import com.tuya.smartai.iot_sdk.UpgradeEventCallback;
import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    TextView console;

    AlertDialog dialog;
    AlertDialog upgradeDialog;
    AlertDialog configDialog;

    String mPid;
    String mUid;
    String mAk;

    private RecyclerView dpList;
    private DPEventAdapter dpEventAdapter;
    private final int QR_REQUEST_CODE = 0x34;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ZXingLibrary.initDisplayOpinion(this);

        findViewById(R.id.reset).setOnClickListener(this::onClick);
        qrCode = findViewById(R.id.qrcode);
        console = findViewById(R.id.console);
        console.setOnLongClickListener(v -> {
            clear();
            return false;
        });

        dialog = new AlertDialog.Builder(MainActivity.this)
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

        upgradeDialog = new AlertDialog.Builder(MainActivity.this)
                .setCancelable(false)
                .setTitle("提示")
                .setMessage("有新版本，确认开始下载")
                .setPositiveButton("确认", (dialog1, which) -> ioTSDKManager.startUpgradeDownload())
                .create();

        View configView = LayoutInflater.from(this).inflate(R.layout.config_layout, null);
        EditText pid = configView.findViewById(R.id.et_pid);
        EditText uid = configView.findViewById(R.id.et_uid);
        EditText ak = configView.findViewById(R.id.et_ak);

        configDialog = new AlertDialog.Builder(MainActivity.this)
                .setCancelable(false)
                .setView(configView)
                .setTitle("配置")
                .setPositiveButton("确认", (dialog1, which) -> {
                    mPid = pid.getText().toString();
                    mUid = uid.getText().toString();
                    mAk = ak.getText().toString();

                    if (!EasyPermissions.hasPermissions(this, requiredPermissions)) {
                        EasyPermissions.requestPermissions(this, "需要授予权限以使用设备", PERMISSION_CODE, requiredPermissions);
                    } else {
                        initSDK();
                    }
                })
                .setNeutralButton("扫码导入", (dialog2, which) -> {
                    Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                    startActivityForResult(intent, QR_REQUEST_CODE);
                })
                .create();

        configDialog.setOnShowListener(dialog -> {
            pid.setText(!TextUtils.isEmpty(BuildConfig.PID) ? BuildConfig.PID.trim() : "");
            uid.setText(!TextUtils.isEmpty(BuildConfig.UUID) ? BuildConfig.UUID.trim() : "");
            ak.setText(!TextUtils.isEmpty(BuildConfig.AUTHOR_KEY) ? BuildConfig.AUTHOR_KEY.trim() : "");
        });

        initDPViews();

        configDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == QR_REQUEST_CODE) {
            configDialog.show();

            if (data != null) {
                Bundle extras = data.getExtras();
                if (extras != null && extras.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    String result = extras.getString(CodeUtils.RESULT_STRING);
                    JSONObject object = JSONObject.parseObject(result);
                    mUid = object.getString("UUID");
                    mPid = object.getString("PID");
                    mAk = object.getString("AUTHKEY");

                    if (TextUtils.isEmpty(mUid) || TextUtils.isEmpty(mPid) || TextUtils.isEmpty(mAk)) {
                        return;
                    }

                    mUid = mUid.trim();
                    mPid = mPid.trim();
                    mAk = mAk.trim();

                    if (configDialog.isShowing()) {
                        configDialog.hide();
                    }

                    if (!EasyPermissions.hasPermissions(this, requiredPermissions)) {
                        EasyPermissions.requestPermissions(this, "需要授予权限以使用设备", PERMISSION_CODE, requiredPermissions);
                    } else {
                        initSDK();
                    }
                }
            }
        }
    }

    private void initDPViews() {

        findViewById(R.id.send_dp).setOnClickListener(this::onClick);
        findViewById(R.id.send_time).setOnClickListener(this::onClick);

        dpList = findViewById(R.id.dp_list);

        dpList.setLayoutManager(new LinearLayoutManager(this));

    }

    private void initSDK() {

        Log.init(this, "/sdcard/tuya_log/iot_demo/", 3);

        ioTSDKManager = new IoTSDKManager(this) {
            @Override
            protected boolean isOffline() {
                //实现自定义网络监测
                return super.isOffline();
            }
        };

        output("固件版本：" + BuildConfig.VERSION_NAME);

        output("init sdk：" + mPid + "/" + mUid + "/" + mAk);

        //注意：这里的pid等配置读取自local.properties文件，不能直接使用。请填写你自己的配置！
        ioTSDKManager.initSDK("/sdcard/tuya_iot/", mPid
                , mUid, mAk, BuildConfig.VERSION_NAME, new IoTSDKManager.IoTCallback() {

                    @Override
                    public void onDpEvent(DPEvent event) {
                        if (event != null) {
                            output("收到 dp: " + event);

                            runOnUiThread(() -> dpEventAdapter.updateEvent(event));
                        }
                    }

                    @Override
                    public void onReset() {

                        getSharedPreferences("event_cache", MODE_PRIVATE).edit().clear().commit();

                        runOnUiThread(() -> dialog.show());
                    }

                    @Override
                    public void onShorturl(String urlJson) {
                        output("shorturl: " + urlJson);

                        String url = (String) JSONObject.parseObject(urlJson).get("shortUrl");

                        runOnUiThread(() -> {
                            qrCode.setVisibility(View.VISIBLE);
                            output("用涂鸦智能APP扫码激活");
                            qrCode.setImageBitmap(CodeUtils.createImage(url, 400, 400, null));
                        });
                    }

                    @Override
                    public void onActive() {
                        output("onActive: devId-> " + ioTSDKManager.getDeviceId());

                        runOnUiThread(() -> {
                            qrCode.setVisibility(View.GONE);
                            output("激活成功了");
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

                                SharedPreferences sp = getSharedPreferences("event_cache", MODE_PRIVATE);

                                DPEvent[] events = ioTSDKManager.getEvents();
                                dpEventAdapter = new DPEventAdapter(Arrays.stream(events)
                                        .peek(event -> {
                                            if (sp.contains(event.dpid + "")
                                                    && !TextUtils.isEmpty(sp.getString(event.dpid + "", ""))) {
                                                String valueStr = sp.getString(event.dpid + "", "");
                                                switch (event.type) {
                                                    case DPEvent.Type.PROP_STR:
                                                        event.value = valueStr;
                                                        break;
                                                    case DPEvent.Type.PROP_ENUM:
                                                    case DPEvent.Type.PROP_VALUE:
                                                    case DPEvent.Type.PROP_BITMAP:
                                                        event.value = Integer.parseInt(valueStr);
                                                        break;
                                                    case DPEvent.Type.PROP_BOOL:
                                                        event.value = Boolean.parseBoolean(valueStr);
                                                        break;
                                                    case DPEvent.Type.PROP_RAW:
                                                        event.value = valueStr.getBytes();
                                                        break;
                                                }
                                            }
                                        })
                                        .filter(Objects::nonNull).collect(Collectors.toList()));

                                runOnUiThread(() -> {
                                    dpList.setAdapter(dpEventAdapter);
                                    findViewById(R.id.send_dp).setEnabled(true);
                                    findViewById(R.id.send_time).setEnabled(true);
                                });

                                if (events != null) {
                                    for (DPEvent event : events) {
                                        if (event != null) {
                                            output(event.toString());
                                        }
                                    }
                                }
                                break;
                        }
                    }
                });

        ioTSDKManager.setUpgradeCallback(new UpgradeEventCallback() {
            @Override
            public void onUpgradeInfo(String s) {
                Log.w(TAG, "onUpgradeInfo: " + s);

                output("收到升级信息: " + s);

//                runOnUiThread(() -> upgradeDialog.show());

                ioTSDKManager.startUpgradeDownload();
            }

            @Override
            public void onUpgradeDownloadStart() {
                Log.w(TAG, "onUpgradeDownloadStart");

                output("开始升级下载");
            }

            @Override
            public void onUpgradeDownloadUpdate(int i) {
                Log.w(TAG, "onUpgradeDownloadUpdate: " + i);
            }

            @Override
            public void upgradeFileDownloadFinished(int result, String file) {
                Log.w(TAG, "upgradeFileDownloadFinished: " + result);

                output("下载完成：" + result + " / " + file);
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
        int timestamp = (int) (System.currentTimeMillis() / 1000L) - 60 * 10;
        switch (v.getId()) {
            case R.id.reset:
                ioTSDKManager.reset();
                break;
            case R.id.send_dp:
                ioTSDKManager.sendDP(dpEventAdapter.getCheckedList().toArray(new DPEvent[]{}));
                break;
            case R.id.send_time:
                DPEvent[] dpEvents = dpEventAdapter.getCheckedList().toArray(new DPEvent[]{});
                Arrays.stream(dpEvents).forEach(event -> event.timestamp = timestamp);
                ioTSDKManager.sendDPWithTimeStamp(dpEvents);
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
    public void onBackPressed() {
        super.onBackPressed();
        save();
        System.exit(0);
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

        Log.close();

        Log.w(TAG, "onDestroy");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        save();
        Log.w(TAG, "onStop");
    }

    private void save() {
        if (dpEventAdapter != null) {
            SharedPreferences sp = getSharedPreferences("event_cache", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            for (DPEvent event : dpEventAdapter.getData()) {
                if (event.type == DPEvent.Type.PROP_RAW) {
                    editor.putString(event.dpid + "", new String((byte[]) event.value));
                } else
                    editor.putString(event.dpid + "", event.value.toString());
            }
            editor.commit();
        }
    }
}
