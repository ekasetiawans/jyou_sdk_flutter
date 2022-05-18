package id.kakzaki.jyou_sdk;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.sxr.sdk.ble.keepfit.service.BluetoothLeService;

public class SampleBleService extends BluetoothLeService {
    public static String ECG_SWITCH = "ECG_SWITCH";
    public static String ECG_VALUE = "ECG_VALUE";

    @Override
    public void onCreate() {
        Log.i("info"," onCreate SampleBleService");
        super.onCreate();
    }
}
