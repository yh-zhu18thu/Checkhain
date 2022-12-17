package com.cybepunk.checkhain;

import android.util.Log;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class BlockChainManagerImpl {

    private static BlockChainManagerImpl INSTANCE;

    private String mAddress = "0x9c14333eCbcd67329279E085929a8BBa11c990C3";
    private String mPrivateKey = "d919f5061f042e6e32709513e480ea7a59158329b266bfe6e901fe14187a2d83";

    //private String mAddress = "0x2f24Ac6c8429a58A96619414eBf543f56051078B";
    //private String mPrivateKey = "755d07ecece16c03bd7b6b02a24c0aea4c56c354d98bb60682c00b5a3ba580d8";

    //private String mAddress = "0xb05cF693cADB853FB21DBE4e4C0ED4891539B618";
    //private String mPrivateKey = "d2220403a0da8aa88d1e2220054517d93144d591c34d900df53ba8c136edb0fb";

    //private String mAddress = "0x875d6Dca67DdAC7af880e07C1AFBFf51176354A5";
    //private String mPrivateKey = "d1c0a3fdb14cbb3a01208de165306c3ce72dce53f02b4865a4e80b85a69b0e9d";

    private String dualMessage;

    private BlockChainManagerImpl() {
    }

    public static BlockChainManagerImpl getInstance(){
        if(INSTANCE==null){
            INSTANCE = new BlockChainManagerImpl();
        }
        return INSTANCE;
    }

    public void askForDualMessage(){
        String address= "http://43.138.68.127:5000/generate/";
        String req = address+ INSTANCE.mPrivateKey;
        Log.e("zyh-bc","req: "+req);
        HTTPUtil.sendOkHttpRequest(req, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                dualMessage = response.body().string();
                Log.e("zyh-bc","recv dual msg: "+dualMessage);
                BluetoothManagerImpl.getInstance().sendLongBroadCast(dualMessage);
            }
        });
    }

    public void reportReceivedMessage(String msg){
        String address= "http://43.138.68.127:5000/validate/";
        String req = address+mAddress+"/"+msg;
        HTTPUtil.sendOkHttpRequest(req, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.e("zyh-bc","report succeed "+req);

            }
        });
    }

    public void checkForSignInStatus(Callback callback){
        String address = "http://43.138.68.127:5000/check/";
        String req = address+mAddress;
        HTTPUtil.sendOkHttpRequest(req,callback);
    }














}
