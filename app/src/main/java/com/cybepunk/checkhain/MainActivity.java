package com.cybepunk.checkhain;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private Button signInButton;
    private TextView stateText;
    private EditText logText;
    private Context context;
    private BluetoothManagerImpl mBluetoothManagerImpl;

    private final int REQ_PERMISSION_CODE = 100;

    protected void checkBluetoothPermission(){
        List<String> requestList = new ArrayList<>();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            requestList.add(Manifest.permission.BLUETOOTH_SCAN);
            requestList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            requestList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            requestList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            requestList.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if(requestList.size()!=0){
            ActivityCompat.requestPermissions(this,requestList.toArray(new String[0]),REQ_PERMISSION_CODE);
        }
    }

    public static final boolean isLocationEnable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean networkProvider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (networkProvider || gpsProvider) return true;
        return false;
    }

    private static final int REQUEST_CODE_LOCATION_SETTINGS = 2;
    private void setLocationService() {
        Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        this.startActivityForResult(locationIntent, REQUEST_CODE_LOCATION_SETTINGS);
    }


    public static boolean isSupportBle(Context context) {
        if (context == null || !context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false;
        }
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        return manager.getAdapter() != null;
    }
    //是否开启
    public static boolean isBleEnable(Context context) {
        if (!isSupportBle(context)) {
            return false;
        }
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        return manager.getAdapter().isEnabled();
    }
    //开启蓝牙
    public static void enableBluetooth(Activity activity, int requestCode) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        signInButton = findViewById(R.id.button);
        stateText = findViewById(R.id.textView);
        logText = findViewById(R.id.editTextTextMultiLine);
        logText.setFocusable(false);
        context = this;

        checkBluetoothPermission();
        if(!isLocationEnable(this)){
            setLocationService();
        }
        if(!isBleEnable(this)){
            Toast.makeText(this,"not support",Toast.LENGTH_SHORT).show();
        }
        BluetoothManagerImpl.getInstance().init(this);


        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInButton.setClickable(false);
                signInButton.setBackgroundColor(Color.GRAY);
                stateText.setText("正在签到");

                //发送请求，等到有回复时再开启广播
                BlockChainManagerImpl.getInstance().askForDualMessage();
                BluetoothManagerImpl.getInstance().listenToBroadCast();

                TimerTask updateTask = new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                logText.setText(BluetoothManagerImpl.getInstance().getBlueToothStatus());
                            }
                        });
                    }
                };
                Timer updateTimer = new Timer();
                updateTimer.schedule(updateTask,1000,500);

                Timer timer = new Timer();

                TimerTask timerTask =  new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                BlockChainManagerImpl.getInstance().checkForSignInStatus(new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {

                                    }

                                    @Override
                                    public void onResponse(Call call, Response response) throws IOException {
                                        String result = response.body().string();
                                        Log.e("zyh-bc-raw","result: "+result);
                                        if(!result.equals("False")){
                                            Log.e("zyh-bc","sign in!");
                                            stateText.setText("签到完成");
                                            signInButton.setText("已签到");
                                            timer.cancel();
                                        }else{
                                            Log.e("zyh-bc","not yet sign in");
                                        }
                                    }
                                });
                            }
                        });
                    }
                };
                timer.schedule(timerTask,1000,5000);
            }
        });

    }
}