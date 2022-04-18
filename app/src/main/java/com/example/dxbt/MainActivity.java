package com.example.dxbt;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements BLESPPUtils.OnBluetoothAction, View.OnClickListener, SensorEventListener {
    // 蓝牙工具
    private BLESPPUtils mBLESPPUtils;
    // 保存搜索到的设备，避免重复
    private ArrayList<BluetoothDevice> mDevicesList = new ArrayList<>();
    // 对话框控制
    private DeviceDialogCtrl mDeviceDialogCtrl;
    // log 视图
    private TextView mLogTv,mLogStr;
    // 输入的 ET
    private EditText mInputET;

    private SensorManager sManager;
    private Sensor mSensorOrientation;

    private int lock =0;
    private float fx=0;
    int line = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //传感器
        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorOrientation = sManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sManager.registerListener(this, mSensorOrientation, SensorManager.SENSOR_DELAY_UI);





        // 申请权限
        initPermissions();
        // 绑定视图
        findViewById(R.id.btn_send).setOnClickListener(this);
        mLogTv = findViewById(R.id.tv_log);
        mLogStr = findViewById(R.id.tv_logString);
        mLogTv.setMovementMethod(ScrollingMovementMethod.getInstance());
        mInputET = findViewById(R.id.ed_input);

        // 初始化
        mBLESPPUtils = new BLESPPUtils(this, this);
        // 启用日志输出
        mBLESPPUtils.enableBluetooth();
        // 设置接收停止标志位字符串
        mBLESPPUtils.setStopString("\r\n");
        // 用户没有开启蓝牙的话打开蓝牙
        if (!mBLESPPUtils.isBluetoothEnable()) mBLESPPUtils.enableBluetooth();
        // 启动工具类
        mBLESPPUtils.onCreate();

        mDeviceDialogCtrl = new DeviceDialogCtrl(this);
        mDeviceDialogCtrl.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBLESPPUtils.onDestroy();
    }

    /**
     * 申请运行时权限，不授予会搜索不到设备
     */
    private void initPermissions() {
        if (ContextCompat.checkSelfPermission(this, "android.permission-group.LOCATION") != 0) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            "android.permission.ACCESS_FINE_LOCATION",
                            "android.permission.ACCESS_COARSE_LOCATION",
                            "android.permission.ACCESS_WIFI_STATE"},
                    1
            );
        }
    }

    /**
     * 当发现新设备
     *
     * @param device 设备
     */
    @Override
    public void onFoundDevice(BluetoothDevice device) {
        Log.d("BLE", "发现设备 " + device.getName() + device.getAddress());
        // 判断是不是重复的
        for (int i = 0; i < mDevicesList.size(); i++) {
            if (mDevicesList.get(i).getAddress().equals(device.getAddress())) return;
        }
        // 添加，下次有就不显示了
        mDevicesList.add(device);
        // 添加条目到 UI 并设置点击事件
        if (device.getName()!=null){
        mDeviceDialogCtrl.addDevice(device, new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                BluetoothDevice clickDevice = (BluetoothDevice) v.getTag();
                postShowToast("开始连接:" + clickDevice.getName());
                mLogTv.setText(mLogTv.getText() + "" + "    开始连接:" + clickDevice.getName());
                mBLESPPUtils.connect(clickDevice);
            }
        });}
    }

    /**
     * 当连接成功
     *
     * @param device 设备
     */
    @Override
    public void onConnectSuccess(final BluetoothDevice device) {
        postShowToast("连接成功", new DoSthAfterPost() {
            @SuppressLint("SetTextI18n")
            @Override
            public void doIt() {
                mLogTv.setText(
                        mLogTv.getText() + "\n    连接成功:" + device.getName() + "\n    " + device.getAddress()
                                + " \n" );
                mDeviceDialogCtrl.dismiss();
            }
        });
    }

    /**
     * 当连接失败
     *
     * @param msg 失败信息
     */
    @Override
    public void onConnectFailed(final String msg) {
        postShowToast("连接失败:" + msg, new DoSthAfterPost() {
            @SuppressLint("SetTextI18n")
            @Override
            public void doIt() {
                mLogTv.setText(mLogTv.getText() + "\n    连接失败:" + msg);
            }
        });
    }

    /**
     * 当接收到 byte 数组
     *
     * @param bytes 内容
     */
    @Override
    public void onReceiveBytes(final byte[] bytes) {

        changeText(mLogTv.getText() + "\n    --->收到数据:    " + new String(bytes));

//        postShowToast("收到数据:" + new String(bytes),
//                new DoSthAfterPost() {
//            @SuppressLint("SetTextI18n")
//            @Override
//            public void doIt() {
//                mLogTv.setText(mLogTv.getText() + "------>收到数据:" + new String(bytes));
//            }
//        });
    }

    /**
     * 当调用接口发送 byte 数组
     *
     * @param bytes 内容
     */
    @Override
    public void onSendBytes(final byte[] bytes) {

        changeText(mLogTv.getText() + "\n      发送数据:    " + new String(bytes));


//        postShowToast("发送数据:" + new String(bytes), new DoSthAfterPost() {
//            @SuppressLint("SetTextI18n")
//            @Override
//            public void doIt() {
//                mLogTv.setText(mLogTv.getText() + "发送数据:" + new String(bytes));
//            }
//        });
    }

    /**
     * 当结束搜索设备
     */
    @Override
    public void onFinishFoundDevice() { }

    /**
     * 按钮的点击事件
     *
     * @param v 点击的按钮
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_send) {
            mBLESPPUtils.send((mInputET.getText().toString() + "\r\n").getBytes());
        }
    }

    /**
     * 设备选择对话框控制
     */
    private class DeviceDialogCtrl {
        private LinearLayout mDialogRootView;
        private ProgressBar mProgressBar;
        private AlertDialog mConnectDeviceDialog;

        DeviceDialogCtrl(Context context) {
            // 搜索进度条
            mProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
            mProgressBar.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            50
                    )
            );

            // 根布局
            mDialogRootView = new LinearLayout(context);
            mDialogRootView.setOrientation(LinearLayout.VERTICAL);
            mDialogRootView.addView(mProgressBar);
            mDialogRootView.setMinimumHeight(700);

            // 容器布局
            ScrollView scrollView = new ScrollView(context);
            scrollView.addView(mDialogRootView,
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            700
                    )
            );

            // 构建对话框
            mConnectDeviceDialog = new AlertDialog
                    .Builder(context)
                    .setNegativeButton("刷新", null)
                    .setPositiveButton("退出", null)
                    .create();
            mConnectDeviceDialog.setTitle("选择连接的蓝牙设备");
            mConnectDeviceDialog.setView(scrollView);
            mConnectDeviceDialog.setCancelable(false);
        }

        /**
         * 显示并开始搜索设备
         */
        void show() {
            mBLESPPUtils.startDiscovery();
            mConnectDeviceDialog.show();
            mConnectDeviceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mConnectDeviceDialog.dismiss();
                    return false;
                }
            });
            mConnectDeviceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mConnectDeviceDialog.dismiss();
                    finish();
                }
            });
            mConnectDeviceDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDialogRootView.removeAllViews();
                    mDialogRootView.addView(mProgressBar);
                    mDevicesList.clear();
                    mBLESPPUtils.startDiscovery();
                }
            });
        }

        /**
         * 取消对话框
         */
        void dismiss() {
            mConnectDeviceDialog.dismiss();
        }

        /**
         * 添加一个设备到列表
         * @param device 设备
         * @param onClickListener 点击回调
         */
        private void addDevice(final BluetoothDevice device, final View.OnClickListener onClickListener) {
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    TextView devTag = new TextView(MainActivity.this);
                    devTag.setClickable(true);
                    devTag.setPadding(20,20,20,20);
                    devTag.setBackgroundResource(R.drawable.rect_round_button_ripple);
                    devTag.setText(device.getName() + "\nMAC:" + device.getAddress());
                    devTag.setTextColor(Color.WHITE);
                    devTag.setOnClickListener(onClickListener);
                    devTag.setTag(device);
                    devTag.setLayoutParams(
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                    );
                    ((LinearLayout.LayoutParams) devTag.getLayoutParams()).setMargins(
                            20, 20, 20, 20);
                    mDialogRootView.addView(devTag);
                }
            });
        }
    }

    /**
     * 在主线程弹出 Toast
     *
     * @param msg 信息
     */
    private void postShowToast(final String msg) {
        postShowToast(msg, null);
    }

    /**
     * 在主线程弹出 Toast
     *
     * @param msg 信息
     * @param doSthAfterPost 在弹出后做点什么
     */
    private void postShowToast(final String msg, final DoSthAfterPost doSthAfterPost) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                if (doSthAfterPost != null) doSthAfterPost.doIt();
            }
        });
    }

    private interface DoSthAfterPost {
        void doIt();
    }





    @Override
    public void onSensorChanged(SensorEvent event) {

        fx=(int) (Math.round(event.values[2] * 100)) / 100;
       if (fx>33&lock!=1){
           mBLESPPUtils.send(("1\r\n").getBytes());
           mLogStr.setText("<-");
           lock=1;
       }
        if (fx<-33&lock!=2){
            mBLESPPUtils.send(("2\r\n").getBytes());
            mLogStr.setText("->");
            lock=2;
        }
        if (fx<20&fx>-20&lock!=3){
            mBLESPPUtils.send(("0\r\n").getBytes());
            mLogStr.setText("∧");
            lock=3;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    public void changeText(String s){

        mLogTv.setText(s);

        int line = mLogTv.getLineCount();
        if (line > 12) {//超出屏幕自动滚动显示(11是当前页面显示的最大行数)
            int offset = mLogTv.getLineCount() * mLogTv.getLineHeight();
            mLogTv.scrollTo(0, offset - mLogTv.getHeight() + mLogTv.getLineHeight());
        }
    }


}