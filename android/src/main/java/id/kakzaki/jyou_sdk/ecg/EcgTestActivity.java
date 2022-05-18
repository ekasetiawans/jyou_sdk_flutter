package id.kakzaki.jyou_sdk.ecg;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.gaoxin.ndk.EcgInfo;
import com.gaoxin.ndk.ReportData;
import id.kakzaki.jyou_sdk.R;
import id.kakzaki.jyou_sdk.SampleBleService;
import com.sxr.sdk.ble.keepfit.ecg.EcgGrid;
import com.sxr.sdk.ble.keepfit.ecg.EcgUtil;
import com.sxr.sdk.ble.keepfit.ecg.EcgView;

import java.util.ArrayList;
import java.util.Map;

public class EcgTestActivity extends Activity {
    private String TAG = getClass().getSimpleName();
    private boolean bAuto = false;
    private EcgView evTest;
    private EditText etName;
    private TextView tvHeart;
    private Switch sDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ecg_test);
        initView();

        initData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        ecgUtil.stop(evTest);
    }

    private void initData() {
        IntentFilter intentFilter = new IntentFilter(SampleBleService.ECG_VALUE);
        registerReceiver(broadcastReceiver, intentFilter);

        if(getIntent().hasExtra("auto")){
            bAuto = getIntent().getBooleanExtra("auto", false);
            if(bAuto){
                evTest.post(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.bOpen).performClick();
                    }
                });
            }
        }
    }

    private EcgUtil ecgUtil = EcgUtil.getInstance();
    private void initView() {
        etName = (EditText) findViewById(R.id.etName);
        tvHeart = (TextView) findViewById(R.id.tvHeart);
        evTest = (EcgView) findViewById(R.id.evTest);
        sDevice = (Switch) findViewById(R.id.sDevice);
        evTest.speed = 256;

        final Handler updateUIHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                //super.handleMessage(msg);
                Bundle data = msg.getData();
                int heartRate = data.getInt("heartRate");

                if(heartRate > 0){
                    tvHeart.setText(String.valueOf(heartRate));
                }else{
                    tvHeart.setText("--");
                }
                return true;
            }
        });

        ecgUtil.setEcgCallback(new EcgUtil.EcgCallback() {
            @Override
            public void receiveEcgInfo(EcgInfo ecgInfo) {
                int ecgSample = ecgInfo.ecgSample; // 原始心电数据
                int heartRate = ecgInfo.heartRate; // 实时心率
                int qrsResult = ecgInfo.qrsResult; // 不为0则有qrs波
                double baseFilterData = ecgInfo.baseFilterData; //基础滤波数据,用于画图

                Message msg = Message.obtain();
                Bundle bundle = new Bundle();
                bundle.putInt("heartRate",heartRate);
                msg.setData(bundle);
                updateUIHandler.sendMessage(msg);
            }
        });

        String name = (String) ShareUtil.getValue("etName", "");
        etName.setText(name);

        sDevice.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ShareUtil.setValue("sDevice", isChecked);
            }
        });
        boolean isChecked = (boolean) ShareUtil.getValue("sDevice", false);
        sDevice.setChecked(isChecked);
    }

    private ArrayList<Integer> alData = new ArrayList<>();
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action == null)
                return;
            if(action.equals(SampleBleService.ECG_VALUE)){
                int[] values = intent.getIntArrayExtra("values");
                ecgUtil.parseEcgRawDataByInt(values);
                for (int value: values) {
                    alData.add(value);
                }
            }
        }
    };

    public void openEcg(View view) {
        Intent intent = new Intent(SampleBleService.ECG_SWITCH);
        intent.putExtra("state", true);
        boolean isChecked = (boolean) ShareUtil.getValue("sDevice", false);
        intent.putExtra("mode", isChecked ? 1 : 0);
        sendBroadcast(intent);

        alData.clear();
        ecgUtil.start(evTest);
    }

    public void closeEcg(View view) {
        Intent intent = new Intent(SampleBleService.ECG_SWITCH);
        intent.putExtra("state", false);
        boolean isChecked = (boolean) ShareUtil.getValue("sDevice", false);
        intent.putExtra("mode", isChecked ? 1 : 0);
        sendBroadcast(intent);

        ecgUtil.stop(evTest);
        ReportData reportData = ecgUtil.getReportData();
        int heartRate = reportData.heartRate; // 平均心率
        int healthScore = reportData.healthScore; // 健康指数

        String[] ecgResKey = new String[]{ // 心电症状说明
                "e1", // 心率不齐
                "e2", // 心动过速
                "e3", // 心动过缓
                "e4", // 停搏
                "e5", // 室性逸搏
                "e6" // 室性早搏
        };
        Map<String, String> ecgRes = reportData.ecgRes; // 心电症状, 数值不为0则有

        String[] hrvScoreKey = new String[]{ // 健康指标说明
                "hrvA", // 精神压力
                "hrvB", // 疲劳指数
                "hrvC", // 心脏年龄
                "hrvD", // 身心放松度
                "hrvE", // 心脏活力
                "hrvF" // 交感-副交感
        };
        Map<String, String> hrvScore = reportData.hrvScore; // // 健康指标数值

        String suggestKey = reportData.suggestKey; // 建议, S+三位数字
        /* 第一位数字说明
        1: 您的精神压力较低，身心状态正佳，快把秘诀告诉身边的小伙伴吧。
        2: 您的精神压力适中，生活和学习上都处于一个比较平稳的状态。希望您继续保持，再接再励。
        3: 您的精神压力较高，会导致您的注意力不能集中，还会对您的身体健康造成一定影响。建议您保持规律作息，保证充足睡眠，合理安排饮食，适当进行体育锻炼。
        */
        /* 第二位数字说明
        1: 您的疲劳指数良好，说明您真正做到了劳逸结合，现代社会竞争激烈，工作压力大，做到您这点可真不简单。
        2: 您的疲劳指数适中，身体状态也较好，说明您的自我调节能力不错，已经掌握了减轻疲劳程度的方法。
        3: 您的疲劳指数较大，身心感觉较不适，建议您规律饮食、营养均衡搭配。保持积极乐观的心态，多陪陪家人，感受家人带来的温暖。
        */
        /* 第三位数字说明
        1: 您的交感神经系统比较活跃，您可能会出现心悸，心慌，憋气的症状，建议您平常多注意休息，调整心态，进食时间要有规律，不要吃得过饱，不要过分摄取水分。
        2: 您的交感与副交感神经系统处于平衡状态。您的身体和心理处于一个较佳的状态。
        3: 您的副交感神经系统比较活跃，您可能会出现身体倦怠，站立时头晕目眩，容易疲劳等症状。建议您要有适度的睡眠时间，不足或过度均不好。要有适度的运动，做一点深呼吸和简单的体操，也会收到较好的效果。
        */
    }

    public void backEcg(View view){
        evTest.speed = 5;
        evTest.setOnLoadingListener(new EcgView.OnLoadingListener() {
            @Override
            public void onLoading(int position, double value, int size) {
                // position 当前绘制的X轴位置
                // value 当前绘制的数值
                // size 绘制队列中的剩余数量
                if(size == 0){
                    ecgUtil.stop(evTest);
                }
            }
        });
        ecgUtil.start(evTest);
        int[] values = new int[alData.size()];
        for (int i = 0; i < alData.size(); i++) {
            values[i] = alData.get(i);
        }
        ecgUtil.parseEcgRawDataByInt(values);
    }

    public void reportEcg(View view){
        // 获取原始数据
        int[] values = new int[alData.size()];
        for (int i = 0; i < alData.size(); i++) {
            values[i] = alData.get(i);
        }
        EcgView[] ecgViews = new EcgView[]{
                (EcgView) findViewById(R.id.evTest0),
                (EcgView) findViewById(R.id.evTest1),
                (EcgView) findViewById(R.id.evTest2)
        };
        EcgGrid ecgGrid = (EcgGrid) findViewById(R.id.egBack);
        ecgUtil.drawReport(values, ecgViews, ecgGrid);
    }

    public void autoPlay(View view){
        String name = etName.getText().toString();
        ShareUtil.setValue("etName", name);
        closeEcg(view);
        setResult(RESULT_OK);
        finish();
    }

}
