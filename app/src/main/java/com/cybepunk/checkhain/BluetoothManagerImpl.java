package com.cybepunk.checkhain;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.beacon.Beacon;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.inuker.bluetooth.library.utils.BluetoothLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Vector;

public class BluetoothManagerImpl {
    private static BluetoothManagerImpl INSTANCE;

    private BluetoothManagerImpl() {
    }

    public static BluetoothManagerImpl getInstance(){
        if(INSTANCE==null){
            INSTANCE = new BluetoothManagerImpl();
        }
        return INSTANCE;
    }

    ScanCallback mLeScanCallback;
    BluetoothClient mClient;
    BluetoothAdapter mBluetoothAdapter;
    Context mContext;
    Map<String,TreeMap<Integer,String>> receiveMap = new HashMap<>();
    int cnt = 0;
    String wholeString;

    //UUID
    public static UUID UUID_SERVICE = UUID.fromString("db08662c-e9c8-4b10-b0d1-33c349bf9351");
    private static final int splitMaxSize  = 10;
    private static final int wholeContentLength = 205;
    private static final int packageNum = 21;

    public String getBlueToothStatus(){
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String,TreeMap<Integer,String>> entry : receiveMap.entrySet()){
            String address = entry.getKey();
            TreeMap<Integer,String> map = entry.getValue();
            sb.append("name: ").append(address).append(" react status: ").append((map.size() / (float) packageNum)*100).append("%\n");
        }
        return sb.toString();
    }

    public void init(Context context){
        mContext = context;
        mClient = new BluetoothClient(context);

//?????????????????????
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    public void listenToBroadCast(){
        BluetoothLeScanner bluetoothLeScanner=mBluetoothAdapter.getBluetoothLeScanner();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        List<ScanFilter> filter = new ArrayList<>();
        //builder.setDeviceName("AOSP on bramble");
        filter.add(builder.build());

        ScanSettings.Builder builderScanSettings = new ScanSettings.Builder();
        builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        ScanSettings settings = builderScanSettings.build();

        mLeScanCallback = new ScanCallback() {
            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                Log.e("zyh-recv","batch result");
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e("zyh-recv","scan failed "+errorCode);
            }

            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                //Log.e("zyh-recv","receive broadcast "+result.getDevice().getName());
                BluetoothDevice device = result.getDevice();
                if(result.getScanRecord().getServiceData()!=null){
                    for(Map.Entry<ParcelUuid, byte[]> entry : result.getScanRecord().getServiceData().entrySet()){
                        UUID resultUUID = entry.getKey().getUuid();
                        Log.e("zyh-recv",entry.getKey().getUuid().toString()+" : "+new String(entry.getValue()));
                        if(resultUUID.equals(UUID_SERVICE)){
                            if(!receiveMap.containsKey(device.getName())){
                                receiveMap.put(device.getName(),new TreeMap<>());
                            }
                            TreeMap<Integer,String> addressContent = receiveMap.get(device.getName());
                            byte[] serviceData = entry.getValue();
                            String userString = new String(serviceData);

                            int dashPos = userString.indexOf('-');
                            int splitRank = Integer.parseInt(userString.substring(0,dashPos));
                            String content = userString.substring(dashPos+1);
                            if(!addressContent.containsKey(splitRank)){
                                Log.e("zyh-confirm",splitRank+":"+content);
                                Log.e("zyh-confirm","size: "+addressContent.size());
                                addressContent.put(splitRank,content);
                                if(addressContent.size()==packageNum){
                                    StringBuilder sb = new StringBuilder();
                                    for(String subContent:addressContent.values()){
                                        sb.append(subContent);
                                    }
                                    Log.e("zyh-confirm","finish collecting: "+sb.toString());
                                    BlockChainManagerImpl.getInstance().reportReceivedMessage(sb.toString());
                                }
                            }
                            //?????????????????????????????????????????????
                        }
                    }
                }
            }
        };
        bluetoothLeScanner.startScan(filter, settings, mLeScanCallback);
    }

    public void sendLongBroadCast(String data){
        this.wholeString = data;
        String currentSubString = "0-"+data.substring((cnt++)*splitMaxSize,splitMaxSize);
        sendBroadCast(currentSubString);
    }

    private void sendBroadCast(String data){
        boolean result1 = mBluetoothAdapter.isMultipleAdvertisementSupported();
        if (result1){
            //??????BLE????????????????????????
            //?????????????????????????????????????????????LE??????????????????null???
            Log.e("zyh","sending: "+data+" substring num: "+cnt);

            AdvertiseSettings mAdvertiseSettings = new AdvertiseSettings.Builder()
                    //?????????????????????????????????????????????????????????
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    //??????????????????
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    //????????????180000???????????????0????????????????????????
                    .setTimeout(120000)
                    //????????????????????????
                    .setConnectable(false)
                    .build();

            AdvertiseData mAdvertiseData = new AdvertiseData.Builder()
                    //????????????????????????
                    .setIncludeDeviceName(true)
                    .build();

            AdvertiseData mScanResponseData = new AdvertiseData.Builder()
                            //????????????????????????
                            .setIncludeDeviceName(false)
                            //????????????????????????
                            .setIncludeDeviceName(false)
                            //?????????????????????UUID
                            //.addManufacturerData(0x11,data.getBytes())
                            .addServiceData(new ParcelUuid(UUID_SERVICE),data.getBytes())
                            .build();

            CheckAdvertiseCallback mAdvertiseCallback = new CheckAdvertiseCallback();

            BluetoothLeAdvertiser mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            if (mBluetoothLeAdvertiser != null){
                //????????????
                mBluetoothLeAdvertiser.startAdvertising(mAdvertiseSettings,
                        mAdvertiseData, mScanResponseData, mAdvertiseCallback);
            }else {
                Log.d("zyh","?????????????????????");
            }
        }else {
            Log.d("zyh","??????????????????????????????");
        }
    }

    private class CheckAdvertiseCallback extends AdvertiseCallback {
        Random random = new Random();
        //????????????????????????
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect){
            super.onStartSuccess(settingsInEffect);
            AdvertiseCallback currentCallBack = this;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.e("zyh","??????????????????");
                    try {
                        Thread.sleep(random.nextInt(100));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    BluetoothLeAdvertiser mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
                    mBluetoothLeAdvertiser.stopAdvertising(currentCallBack);
                    String subString;
                    if(cnt%packageNum==20){
                        subString = cnt % packageNum +"-"+wholeString.substring(200);
                    }else{
                        subString = cnt % packageNum +"-"+wholeString.substring((cnt%packageNum)*splitMaxSize,(cnt%packageNum)*splitMaxSize+splitMaxSize);
                    }
                    cnt++;
                    sendBroadCast(subString);
                }
            });
            thread.start();
        }

        //???????????????????????????
        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e("zyh","?????????????????????????????? = " + errorCode);
        }
    }

}
