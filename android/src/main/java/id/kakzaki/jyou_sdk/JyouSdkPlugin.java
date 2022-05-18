package id.kakzaki.jyou_sdk;

import static android.content.Context.BIND_AUTO_CREATE;


import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.embedding.engine.plugins.service.ServiceAware;
import io.flutter.embedding.engine.plugins.service.ServicePluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.Html;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.sxr.sdk.ble.keepfit.aidl.AlarmInfoItem;
import com.sxr.sdk.ble.keepfit.aidl.BleClientOption;
import com.sxr.sdk.ble.keepfit.aidl.DeviceProfile;
import com.sxr.sdk.ble.keepfit.aidl.IRemoteService;
import com.sxr.sdk.ble.keepfit.aidl.IServiceCallback;
import com.sxr.sdk.ble.keepfit.aidl.UserProfile;
import com.sxr.sdk.ble.keepfit.aidl.Weather;
import id.kakzaki.jyou_sdk.ecg.EcgTestActivity;
import id.kakzaki.jyou_sdk.ecg.ShareUtil;
import io.flutter.plugin.common.PluginRegistry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;


/** JyouSdkPlugin */
public class JyouSdkPlugin implements FlutterPlugin, ActivityAware,MethodCallHandler, ServiceAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private static final String TAG = JyouSdkPlugin.class.getSimpleName();

  private IRemoteService mService;
  private boolean mIsBound = false;
  private int countStep = 0;
  private String data = "";

  private FlutterPluginBinding pluginBinding;
  private ServicePluginBinding servicePluginBinding;
  private ActivityPluginBinding activityBinding;
  private Object initializationLock = new Object();
  private Activity mactivity;
  private Context mcontext;

  private ArrayList<BleDeviceItem> nearbyItemList;

  private listDeviceViewAdapter nearbyListAdapter;

  private String pathLog = "/jyClient/log/";
  private boolean bSave = true;

  public static void registerWith(PluginRegistry.Registrar registrar) {
    final JyouSdkPlugin instance = new JyouSdkPlugin();
    //registrar.addRequestPermissionsResultListener(instance);
    Activity activity = registrar.activity();
    Application application = null;
    instance.setup(registrar.messenger(), application, activity, registrar, null);
    Log.i("info","registerWith");
  }

  public JyouSdkPlugin() {
  }

  private ServiceConnection mServiceConnection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      Log.i("info","onServiceConnected ddd");

      mService = IRemoteService.Stub.asInterface(service);
      try {
        mService.registerCallback(mServiceCallback);
        mService.openSDKLog(bSave, pathLog, "blue.log");

        boolean isConnected = callRemoteIsConnected();

        if (isConnected == false) {
          Log.i("info","isConnected == false");
        } else {
          int authrize = callRemoteIsAuthrize();
          if (authrize == 200) {
            String curMac = callRemoteGetConnectedDevice();

          }
        }

      } catch (RemoteException e) {
        Log.i("gagal","onServiceConnected printStackTrace");
        e.printStackTrace();
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      Log.i("info","Service disconnected");
      mService = null;
    }
  };

  @Override
  public void onAttachedToEngine(FlutterPluginBinding flutterPluginBinding) {
    this.pluginBinding = flutterPluginBinding;
  }

  @Override
  public void onAttachedToActivity(ActivityPluginBinding binding) {
    this.activityBinding = binding;
    this.mactivity=binding.getActivity();
    setup(
            pluginBinding.getBinaryMessenger(),
            pluginBinding.getApplicationContext(),
            activityBinding.getActivity(),
            null,
            activityBinding);
    Log.i("info","onAttachedToActivity");
  }


  private void setup(
          final BinaryMessenger messenger,
          final Context contextApp,
          final Activity activityApp,
          final PluginRegistry.Registrar registrar,
          final ActivityPluginBinding activityBinding
  ) {
    synchronized (initializationLock) {
      this.mactivity = activityApp;
      this.mcontext = contextApp;
      channel = new MethodChannel(messenger, "jyou_sdk/method");
      channel.setMethodCallHandler(this);
      ShareUtil.init(mcontext);

//      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//        Log.i("info","Setup startForegroundService");
      // activity.startForegroundService(new Intent(mcontext, SampleBleService.class));
//      } else {
//        Log.i("info","Setup startService");
          Intent gattServiceIntent = new Intent(mcontext,SampleBleService.class);
          gattServiceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          mcontext.startService(gattServiceIntent);

          IntentFilter intentFilter = new IntentFilter(SampleBleService.ECG_SWITCH);
          mcontext.registerReceiver(broadcastReceiver, intentFilter);
//      }

      String[] permissions = new String[]{
              Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
              Manifest.permission.WRITE_EXTERNAL_STORAGE
      };
    //  ActivityCompat.requestPermissions(mcontext, permissions, 0);
      Log.i("info","Setup starting");
    }
  }


  private IServiceCallback mServiceCallback = new IServiceCallback.Stub() {
    @Override
    public void onConnectStateChanged(int state) throws RemoteException {
      Log.i("onConnectStateChanged", curMac + " state " + state);
      updateConnectState(state);
    }

    private boolean bScan = false;
    @Override
    public void onScanCallback(final String deviceName, final String deviceMacAddress, final int rssi)
            throws RemoteException {
      Log.i(TAG, String.format("onScanCallback <%1$s>[%2$s](%3$d)", deviceName, deviceMacAddress, rssi));

      if (nearbyItemList == null || !bScan)
        return;

      if(bAutoEcg && !sAutoName.isEmpty() && !deviceName.contains(sAutoName))
        return;

      Iterator<id.kakzaki.jyou_sdk.BleDeviceItem> iter = nearbyItemList.iterator();
      id.kakzaki.jyou_sdk.BleDeviceItem item = null;
      boolean bExist = false;
      while (iter.hasNext()) {
        item = (id.kakzaki.jyou_sdk.BleDeviceItem) iter.next();
        if (item.getBleDeviceAddress().equalsIgnoreCase(deviceMacAddress)) {
          bExist = true;
          item.setRssi(rssi);
          break;
        }
      }

      if (bExist == false) {
        mactivity.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            BleDeviceItem item = new BleDeviceItem(deviceName, deviceMacAddress, "", "", rssi, "");
            nearbyItemList.add(item);
            Collections.sort(nearbyItemList, new ComparatorBleDeviceItem());

            nearbyListAdapter.notifyDataSetChanged();
          }
        });
      }
    }


    @Override
    public void onSetNotify(int result) throws RemoteException {
      Log.i("onSetNotify", String.valueOf(result));
    }

    @Override
    public void onSetUserInfo(int result) throws RemoteException {
      Log.i("onSetUserInfo", "" + result);
    }

    @Override
    public void onAuthSdkResult(int errorCode) throws RemoteException {
      Log.i("onAuthSdkResult", errorCode + "");
    }

    @Override
    public void onGetDeviceTime(int result, String time) throws RemoteException {
      Log.i("onGetDeviceTime", String.valueOf(time));
    }

    @Override
    public void onSetDeviceTime(int arg0) throws RemoteException {
      Log.i("onSetDeviceTime", arg0 + "");
    }

    @Override
    public void onSetDeviceInfo(int arg0) throws RemoteException {
      Log.i("onSetDeviceInfo", arg0 + "");
    }


    @Override
    public void onAuthDeviceResult(int arg0) throws RemoteException {
      Log.i("onAuthDeviceResult", arg0 + "");
    }


    @Override
    public void onSetAlarm(int arg0) throws RemoteException {
      Log.i("onSetAlarm", arg0 + "");
    }

    @Override
    public void onSendVibrationSignal(int arg0) throws RemoteException {
      Log.i("onSendVibrationSignal", "result:" + arg0);
    }

    @Override
    public void onGetDeviceBatery(int arg0, int arg1)
            throws RemoteException {
      Log.i("onGetDeviceBatery", "batery:" + arg0 + ", statu " + arg1);
    }


    @Override
    public void onSetDeviceMode(int arg0) throws RemoteException {
      Log.i("onSetDeviceMode", "result:" + arg0);
    }

    @Override
    public void onSetHourFormat(int arg0) throws RemoteException {
      Log.i("onSetHourFormat ", "result:" + arg0);

    }

    @Override
    public void setAutoHeartMode(int arg0) throws RemoteException {
      Log.i("setAutoHeartMode ", "result:" + arg0);
    }


    @Override
    public void onGetCurSportData(int type, long timestamp, int step, int distance,
                                  int cal, int cursleeptime, int totalrunningtime, int steptime) throws RemoteException {
      Date date = new Date(timestamp * 1000);
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
      String time = sdf.format(date);
      Log.i("onGetCurSportData", "type : " + type + " , time :" + time + " , step: " + step + ", distance :" + distance + ", cal :" + cal + ", cursleeptime :" + cursleeptime + ", totalrunningtime:" + totalrunningtime);
    }

    @Override
    public void onGetSenserData(int result, long timestamp, int heartrate, int sleepstatu)
            throws RemoteException {
      Date date = new Date(timestamp * 1000);
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
      String time = sdf.format(date);
      Log.i("onGetSenserData", "result: " + result + ",time:" + time + ",heartrate:" + heartrate + ",sleepstatu:" + sleepstatu);

    }


    @Override
    public void onGetDataByDay(int type, long timestamp, int step, int heartrate)
            throws RemoteException {
      Date date = new Date(timestamp * 1000);
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
      String recorddate = sdf.format(date);
      Log.i("onGetDataByDay", "type:" + type + ",time::" + recorddate + ",step:" + step + ",heartrate:" + heartrate);
      if (type == 2) {
        sleepcount++;
      }
    }

    @Override
    public void onGetDataByDayEnd(int type, long timestamp) throws RemoteException {
      Date date = new Date(timestamp * 1000);
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
      String recorddate = sdf.format(date);
      Log.i("onGetDataByDayEnd", type + " time:" + recorddate + ",sleepcount:" + sleepcount);
      sleepcount = 0;
    }


    @Override
    public void onSetPhontMode(int arg0) throws RemoteException {
      Log.i("onSetPhontMode", "result:" + arg0);
    }


    @Override
    public void onSetSleepTime(int arg0) throws RemoteException {
      Log.i("onSetSleepTime", "result:" + arg0);
    }


    @Override
    public void onSetIdleTime(int arg0) throws RemoteException {
      Log.i("onSetIdleTime", "result:" + arg0);
    }


    @Override
    public void onGetDeviceInfo(int version, String macaddress, String vendorCode,
                                String productCode, int result) throws RemoteException {
      Log.i("onGetDeviceInfo", "version :" + version + ",macaddress : " + macaddress + ",vendorCode : " + vendorCode + ",productCode :" + productCode + " , CRCresult :" + result);

    }

    @Override
    public void onGetDeviceAction(int type) throws RemoteException {
      Log.i("onGetDeviceAction", "type:" + type);
      if(type == 5){
        if(bAutoEcg){
          bAutoEcg = false;
        }
      }
    }


    @Override
    public void onGetBandFunction(int result, boolean[] results) throws RemoteException {
      Log.i("onGetBandFunction", "result : " + result + ", results :" + results.length);

      String function = "";
      for (int i = 0; i < results.length; i++) {
        function += String.valueOf((i + 1) + "=" + results[i] + " ");
      }
      Log.i("onGetBandFunction", function);
    }

    @Override
    public void onSetLanguage(int arg0) throws RemoteException {
      Log.i("onSetLanguage", "result:" + arg0);

    }


    @Override
    public void onSendWeather(int arg0) throws RemoteException {
      Log.i("onSendWeather", "result:" + arg0);
    }


    @Override
    public void onSetAntiLost(int arg0) throws RemoteException {
      Log.i("onSetAntiLost", "result:" + arg0);

    }


    @Override
    public void onReceiveSensorData(int arg0, int arg1, int arg2, int arg3,
                                    int arg4) throws RemoteException {
      Log.i("onReceiveSensorData", "result:" + arg0 + " , " + arg1 + " , " + arg2 + " , " + arg3 + " , " + arg4);
    }


    @Override
    public void onSetBloodPressureMode(int arg0) throws RemoteException {
      Log.i("onSetBloodPressureMode", "result:" + arg0);
    }


    @Override
    public void onGetMultipleSportData(int flag, String recorddate, int mode, int value)
            throws RemoteException {
//            Date date = new Date(timestamp * 1000);
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
//            String recorddate = sdf.format(date);
      Log.i("onGetMultipleSportData", "flag:" + flag + " , mode :" + mode + " recorddate:" + recorddate + " , value :" + value);
    }


    @Override
    public void onSetGoalStep(int result) throws RemoteException {
      Log.i("onSetGoalStep", "result:" + result);
    }


    @Override
    public void onSetDeviceHeartRateArea(int result) throws RemoteException {
      Log.i("onSetDevHeartRateArea", "result:" + result);
    }


    @Override
    public void onSensorStateChange(int type, int state)
            throws RemoteException {

      Log.i("onSensorStateChange", "type:" + type + " , state : " + state);
    }

    @Override
    public void onReadCurrentSportData(int mode, String time, int step,
                                       int cal) throws RemoteException {

      Log.i("onReadCurrentSportData", "mode:" + mode + " , time : " + time + " , step : " + step + " cal :" + cal);
    }

    @Override
    public void onGetOtaInfo(boolean isUpdate, String version, String path) throws RemoteException {
      Log.i("onGetOtaInfo", "isUpdate " + isUpdate + " version " + version + " path " + path);
    }

    @Override
    public void onGetOtaUpdate(int step, int progress) throws RemoteException {
      Log.i("onGetOtaUpdate", "step " + step + " progress " + progress);
    }

    @Override
    public void onSetDeviceCode(int result) throws RemoteException {
      Log.i("onSetDeviceCode", "result " + result);
    }

    @Override
    public void onGetDeviceCode(byte[] bytes) throws RemoteException {
      Log.i("onGetDeviceCode", "bytes " + id.kakzaki.jyou_sdk.SysUtils.printHexString(bytes));
    }

    @Override
    public void onCharacteristicChanged(String uuid, byte[] bytes) throws RemoteException {
      Log.i("onCharacteristicChanged", uuid + " " + id.kakzaki.jyou_sdk.SysUtils.printHexString(bytes));
    }

    @Override
    public void onCharacteristicWrite(String uuid, byte[] bytes, int status) throws RemoteException {
      Log.i("onCharacteristicWrite", status + " " + uuid + " " + id.kakzaki.jyou_sdk.SysUtils.printHexString(bytes));
    }

    @Override
    public void onSetEcgMode(int result, int state) throws RemoteException {
      Log.i("onSetEcgMode", "result " + result + " state " + state);
    }

    @Override
    public void onGetEcgValue(int state, int[] values) throws RemoteException {
      Log.i("onGetEcgValue", "state " + state + " value " + values.length);
    }

    @Override
    public void onGetEcgHistory(long timestamp, int number) throws RemoteException {
      Log.i("onGetEcgHistory", "timestamp " + timestamp + " number " + number);

    }

    @Override
    public void onGetEcgStartEnd(int id, int state, long timestamp) throws RemoteException {
      Log.i("onGetEcgStartEnd", "id " + id + " state " + state + " timestamp " + timestamp);

    }

    @Override
    public void onGetEcgHistoryData(int id, int[] values) throws RemoteException {
      Log.i("onGetEcgHistoryData", "id " + id + " values " + values.length);

    }

    @Override
    public void onSetDeviceName(int result) throws RemoteException {
      Log.i("onSetDeviceName", "result " + result);
    }

    @Override
    public void onGetDeviceRssi(int rssi) throws RemoteException {
      Log.i("onGetDeviceRssi", "rssi " + rssi);

    }

    @Override
    public void onSetReminder(int result) throws RemoteException {
      Log.i("onSetReminder", "result " + result);

    }

    @Override
    public void onSetReminderText(int result) throws RemoteException {
      Log.i("onSetReminderText", "result " + result);

    }

    @Override
    public void onSetBPAdjust(int result) throws RemoteException {
      Log.i("onSetBPAdjust", "result " + result);

    }

    @Override
    public void onSetTemperatureMode(int result) throws RemoteException {
      Log.i("onSetTemperatureMode", "result " + result);

    }

    @Override
    public void onGetTemperatureData(int surfaceTemp,int bodyTemp) throws RemoteException {
      Log.i("onGetTemperatureData", "surfaceTemp " + surfaceTemp + ", bodyTemp" + bodyTemp);

    }

    @Override
    public void onTemperatureModeChange(int enable) throws RemoteException {
      Log.i("onTemperatureModeChange", "enable " + enable);
    }

  };

  private Boolean btBind;
  private Boolean btUnbind;
  private Boolean btScan;
  private Boolean btConnect;
  private Boolean btDisconnect;
  //	private Button btReadCurSteps;
  private Boolean btReadFw;
  private String tvSync;
  private Boolean btSyncPersonalInfo;
  private Boolean bNotify;
  private Boolean set_time, getcursportdata, set_parameters;
  private String data_text;
  private Boolean set_userinfo, set_vir, set_photo, set_idletime, set_sleep, read_batery, read_fw, set_alarm, send_msg, set_autoheart, set_fuzhu, set_showmode, openheart, closeheart, getdata;
  private String et_getdata, et_getday;
  private Boolean setLanguage, send_weather, bt_getmutipleSportData, bt_open_blood, bt_close_blood, bt_setgoalstep, bt_setHeartRateArea;

  @Override
  public void onAttachedToService( ServicePluginBinding binding) {
    this.servicePluginBinding=binding;
    Log.i("info", "servicePluginBinding!");
  }

  @Override
  public void onDetachedFromService() {
    this.servicePluginBinding=null;
    Log.i("info", "onDetachedFromService!");
  }

  class listDeviceViewAdapter extends BaseAdapter implements
          OnItemSelectedListener {

    private static final int DEVICE_NEARBY = 0;
    int count = 0;
    private LayoutInflater layoutInflater;
    Context local_context;
    float xDown = 0, yDown = 0, xUp = 0, yUp = 0;
    //        private List<BleDeviceItem> itemList;
    private int type;
    protected AnimationDrawable adCallBand;

    public listDeviceViewAdapter(Context context, List<BleDeviceItem> list) {
      layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      //layoutInflater = LayoutInflater.from(context);
      local_context = context;
//            itemList = list;
    }

    public int getCount() {
      return nearbyItemList.size();
    }

    public Object getItem(int pos) {
      return pos;
    }

    public long getItemId(int pos) {
      return pos;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      return null;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
                               long id) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public int getType() {
      return type;
    }

    public void setType(int type) {
      this.type = type;
    }

  }

  private int callRemoteSetUserInfo() {
    int result = 0;
    if (mService != null) {
      try {
        result = mService.setUserInfo();
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }

    return result;
  }

  private int callRemoteIsAuthrize() {
    int isAuthrize = 0;
    if (mService != null) {
      try {
        isAuthrize = mService.isAuthrize();
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }

    return isAuthrize;
  }

  private int callRemoteSetOption(BleClientOption opt) {
    int result = 0;
    if (mService != null) {
      try {
        result = mService.setOption(opt);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }

    return result;
  }

  private boolean callRemoteIsConnected() {
    boolean isConnected = false;
    if (mService != null) {
      try {
        isConnected = mService.isConnectBt();
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
    return isConnected;
  }

  private String callRemoteGetConnectedDevice() {
    String deviceMac = "";
    if (mService != null) {
      try {
        deviceMac = mService.getConnectedDevice();
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }

    return deviceMac;
  }

  private void callRemoteConnect(String name, String mac) {
    if (mac == null || mac.length() == 0) {
      Log.i("info", "ble device mac address is not correctly!");
      return;
    }

    if (mService != null) {
      try {
        mService.connectBt(name, mac);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteDisconnect() {
    if (mService != null) {
      try {
        mService.disconnectBt(true);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteScanDevice() {
    if (nearbyItemList != null)
      nearbyItemList.clear();

    if (mService != null) {
      try {
        bStart = !bStart;
        mService.scanDevice(bStart);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private ListView nearbyListView;
  private boolean bScan = false;

  private boolean bOpen = false;

  private void setUuid() {
    if (mService != null) {
      try {
        bOpen = !bOpen;
        mService.setUuid(new String[0], new String[0], bOpen);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void setDeviceName(String name) {
    if (mService != null) {
      try {
        mService.setDeviceName(name);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callGetDeviceCode() {
    if (mService != null) {
      try {
        mService.getDeviceCode();
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callSetDeviceCode(byte[] bytes) {
    if (mService != null) {
      try {
        Log.i("callSetDeviceCode", SysUtils.printHexString(bytes));
        mService.setDeviceCode(bytes);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callSetParameters() {
    int result;
    if (mService != null) {
      try {
        DeviceProfile deviceProfile = new DeviceProfile(false, true, false, 1, 2, 00, 00);
        BleClientOption opt2 = new BleClientOption(null, deviceProfile, null);
        int result2 = callRemoteSetOption(opt2);
        result = mService.setDeviceInfo();
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callSetAlarm() {
    boolean result;
    if (mService != null) {
      try {
        ArrayList<AlarmInfoItem> lAlarmInfo = new ArrayList<AlarmInfoItem>();
        AlarmInfoItem item = new AlarmInfoItem(1, 1, 1, 11, 1, 1, 1, 1, 1, 1, 1, "要睡觉le", false);
        lAlarmInfo.add(item);
        BleClientOption bco = new BleClientOption(null, null, lAlarmInfo);
        mService.setOption(bco);
        mService.setAlarm();
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callNotify() {
    boolean result;
    if (mService != null) {
      try {
        String type = "Type";
        String name = "name";
        String content = "content";
        result = mService.setNotify(System.currentTimeMillis() + "", Integer.parseInt(type), name, content);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callGetOtaInfo() {
    int result;
    if (mService != null) {
      try {
        result = mService.getOtaInfo(true);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callSetDeviceTime() {
    int result;
    if (mService != null) {
      try {
        result = mService.setDeviceTime();
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callgetCurSportData() {
    int result;
    if (mService != null) {
      try {
        result = mService.getCurSportData();
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callSet_vir() {
    int result;
    if (mService != null) {
      try {
        result = mService.sendVibrationSignal(4); //震动4次
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteSetphoto() {
    int result;
    if (mService != null) {
      try {
        result = mService.setPhontMode(true);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteSetIdletime() {
    int result;
    if (mService != null) {
      try {
        result = mService.setIdleTime(300, 14, 00, 18, 00);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteSetSleepTime() {
    int result;
    if (mService != null) {
      try {
        result = mService.setSleepTime(12, 00, 14, 00, 22, 00, 8, 00);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteGetDeviceBatery() {
    int result;
    if (mService != null) {
      try {
        result = mService.getDeviceBatery();
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteGetDeviceInfo() {
    int result;
    if (mService != null) {
      try {
        result = mService.getDeviceInfo();
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteSetDeviceMode() {
    int result;
    if (mService != null) {
      try {
        result = mService.setDeviceMode(3);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteSetHourFormat() {
    int result;
    if (mService != null) {
      try {
        boolean is24HourFormat = true;
        result = mService.setHourFormat(is24HourFormat == true ? 0 : 1);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteSetHeartRateMode(boolean enable) {
    int result;
    if (mService != null) {
      try {
        result = mService.setHeartRateMode(enable, 60);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteSetAutoHeartMode(boolean enable) {
    int result;
    if (mService != null) {
      try {
        result = mService.setAutoHeartMode(enable, 18, 00, 19, 00, 15, 2); //18:00 - 19:00  15min 2min
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteGetData(int type, int day) {
    Log.i(TAG, "callRemoteGetData");
    int result;
    if (mService != null) {
      try {
        result = mService.getDataByDay(type, day);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteGetFunction() {
    Log.i(TAG, "callRemoteGetFunction");
    int result;
    if (mService != null) {
      try {
        result = mService.getBandFunction();
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteSetLanguage() {
    Log.i(TAG, "callRemoteGetData");
    int result;
    if (mService != null) {
      try {
        result = mService.setLanguage();
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteSetWeather() {
    Log.i(TAG, "callRemoteGetData");
    int result;
    if (mService != null) {
      try {
        result = mService.sendWeather();
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteGetMutipleData(int day) {
    Log.i(TAG, "callRemoteGetMutipleData");
    int result;
    if (mService != null) {
      try {
        result = mService.getMultipleSportData(day);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteOpenBlood(boolean enable) {
    Log.i(TAG, "callRemoteGetMutipleData");
    int result;
    if (mService != null) {
      try {
        result = mService.setBloodPressureMode(enable);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteSetGoalStep(int step) {
    Log.i(TAG, "callRemoteOpenBlood");
    int result;
    if (mService != null) {
      try {
        result = mService.setGoalStep(step);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }

  private void callRemoteSetHeartRateArea(boolean enable, int max, int min) {
    Log.i(TAG, "callRemoteOpenBlood");
    int result;
    if (mService != null) {
      try {
        result = mService.setDeviceHeartRateArea(enable, max, min);
      } catch (RemoteException e) {
        e.printStackTrace();
        Log.i("info", "Remote call error!");
      }
    } else {
      Log.i("info", "Service is not available yet!");
    }
  }


  protected String curMac;

  protected void updateConnectState(int state) {

    Message msg = new Message();
    Bundle data = new Bundle();
    data.putInt("state", state);
    msg.setData(data);
   // updateConnectStateHandler.sendMessage(msg);
  }
  private int sleepcount = 0;
  private boolean bStart = false;

  private PopupWindow window;

  private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (action == null)
        return;
      if (action.equals(SampleBleService.ECG_SWITCH)) {
        boolean state = intent.getBooleanExtra("state", false);
        int mode = intent.getIntExtra("mode", 0);
        try {
          mService.setEcgMode(state, mode);
        } catch (RemoteException e) {
          e.printStackTrace();
        }
      }
    }
  };


  @Override
  public void onMethodCall( MethodCall call,  Result result) {
    switch (call.method) {
      case "bind":
        try {
          Intent intent = new Intent(mactivity, SampleBleService.class);
          boolean val = mactivity.bindService(intent, mServiceConnection, Service.BIND_AUTO_CREATE);
          mIsBound = true;
          Log.i("info", "bindService "+val +" "+intent);
        } catch (Exception e) {
          e.printStackTrace();
        }
        break;
      case "unbind":
        if (mIsBound) {
          try {
            mService.unregisterCallback(mServiceCallback);
          } catch (RemoteException e) {
            e.printStackTrace();
          }
          mcontext.unbindService(mServiceConnection);
          mIsBound = false;
        }
        break;
      case "scan":
        bScan = true;
        callRemoteScanDevice();
        break;
      case "disconnect":
        callRemoteDisconnect();
        break;
      case "bNotify":
        callNotify();
        break;
      case "set_parameters":
        callSetParameters();
        break;
      case "set_time":
        callSetDeviceTime();
        break;
      case "set_userinfo":
        UserProfile userProfile = new UserProfile(10000, 170, 60, 50, 0, 1, 24);
        BleClientOption opt = new BleClientOption(userProfile, null, null);
        int resulti = callRemoteSetOption(opt);
        callRemoteSetUserInfo();
        break;
      case "getcursportdata":
        callgetCurSportData();
        break;
      case "set_vir":
        callSet_vir();
        break;
      case "set_photo":
        callRemoteSetphoto();
        break;
      case "set_idletime":
        callRemoteSetIdletime();
        break;
      case "set_sleep":
        callRemoteSetSleepTime();
        break;
      case "read_batery":
        callRemoteGetDeviceBatery();
        break;
      case "read_fw":
        callRemoteGetDeviceInfo();
        break;
      case "set_alarm":
        callSetAlarm();
        break;
      case "send_msg":
        callGetOtaInfo();
        break;
      case "set_autoheart":
        callRemoteSetAutoHeartMode(true);
        break;
      case "set_fuzhu":
        DeviceProfile deviceProfile = new DeviceProfile(true, true, false, 18, 20, 00, 00);
        BleClientOption opt2 = new BleClientOption(null, deviceProfile, null);
        int result2 = callRemoteSetOption(opt2);
        callRemoteSetDeviceMode();
        break;
      case "set_showmode":
        callRemoteSetHourFormat();
        break;
      case "openheart":
        callRemoteSetHeartRateMode(true);
        break;
      case "closeheart":
        callRemoteSetHeartRateMode(false);
        break;
      case "getdata":
        int type = Integer.valueOf(et_getdata);
        int day = Integer.valueOf(et_getday);

        callRemoteGetData(type, day);
        break;
      case "setLanguage":
        callRemoteSetLanguage();
        break;
      case "send_weather":
//            	Weather weather = new Weather();
        ArrayList<Weather> lWeathers = new ArrayList<Weather>();
        Weather weather = new Weather((int) (System.currentTimeMillis() / 1000), 300, 400, 7, 28, 2, 0, 0, 0, -20); //时间 白天、晚上天气 、最低最高温 空气质量、PM2.5 UV AQI 当前温度
        lWeathers.add(weather);

        BleClientOption opt3 = new BleClientOption(null, null, null, lWeathers);
        callRemoteSetOption(opt3);
        callRemoteSetWeather();
        break;
      case "bt_getmutipleSportData":
        callRemoteGetMutipleData(2);
        break;
      case "bt_open_blood":
        callRemoteOpenBlood(true);
        break;
      case "bt_close_blood":
        callRemoteOpenBlood(false);
        break;
      case "bt_setgoalstep":
        callRemoteSetGoalStep(50);
        break;
      case "bt_setHeartRateArea":
        callRemoteSetHeartRateArea(true, 150, 80);
        break;
      case "bGetDeviceCode":
        callGetDeviceCode();
        break;
      case "bSetDeviceCode":
        String name = "Device";
        byte[] bytes = name.getBytes();
        callSetDeviceCode(bytes);
        break;
      case "bGetBandFunction":
        callRemoteGetFunction();
        break;
      case "bSetUuid":
        setUuid();
        break;
      case "bEcgSync":
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long time = calendar.getTime().getTime();
        Log.i(TAG, "getTime " + time);
        int offset = TimeZone.getDefault().getRawOffset();
        Log.i(TAG, "getRawOffset " + offset);
        long timestamp = (time - 0) / 1000;
        try {
          mService.getEcgHistory((int) timestamp);
        } catch (RemoteException e) {
          e.printStackTrace();
        }
      case "bTestEcg":
       // startActivityForResult(new Intent(this, EcgTestActivity.class), 0);
        break;
      case "bSetName":
        String name3 = "coba";
        setDeviceName(name3);
        break;
      case "bGetRssi":
        try {
          if(mService != null)
            mService.getDeviceRssi();
        } catch (RemoteException e) {
          e.printStackTrace();
        }
        break;
      case "bSetReminderTime":
        String name2 = "coba";
        int id = 1;
        int type1 = 1;
        if(name2.contains(" ")){
          id = Integer.parseInt(name2.split(" ")[0]);
          type1 = Integer.parseInt(name2.split(" ")[1]);
        }
        try {
          if(mService != null)
            mService.setReminder(60, 0, 0, 18, 0, id, type1);
        } catch (RemoteException e) {
          e.printStackTrace();
        }
        break;
      case "bSetReminderText":
        String names ="coba";
        int ids = 1;
        if(names.contains(" ")){
          ids = Integer.parseInt(names.split(" ")[0]);
          names = names.split(" ")[1];
        }
        try {
          if(mService != null)
            mService.setReminderText(ids, names);
        } catch (RemoteException e) {
          e.printStackTrace();
        }
        break;
      case "bSetBPAdjust":
        int nSBP = 120;
        int nDBP = 70;
        try {
          if(mService != null)
            mService.setBPAdjust(nSBP, nDBP);
        } catch (RemoteException e) {
          e.printStackTrace();
        }

        break;
    }
  }

  @Override
  public void onDetachedFromEngine( FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity();
  }

  private void detach() {
    Log.i(TAG, "detach");
    mcontext = null;
    activityBinding = null;
    channel.setMethodCallHandler(null);
    channel = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges( ActivityPluginBinding binding) {
    onAttachedToActivity(binding);
  }

  @Override
  public void onDetachedFromActivity() {
    detach();
  }

  private boolean bAutoEcg = false;
  private String sAutoName = "";


}
