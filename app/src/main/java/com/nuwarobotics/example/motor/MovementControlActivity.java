package com.nuwarobotics.example.motor;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import com.nuwarobotics.example.R;
import com.nuwarobotics.example.util.Logger;
import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.RobotEventCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MovementControlActivity extends AppCompatActivity {
    NuwaRobotAPI mRobotAPI;
    IClientId mClientId;

    //因為這介面有兩個 list，所以宣告了2個 lsit物件+2個Adapter +兩個存放 移動控制strings的 ArrayList
    ListView mMovementListLow;
    ListView mMovementListAdvance;
    ArrayAdapter<String> mMovementListLowAdapter;
    ArrayAdapter<String> mMovementListAdvanceAdapter;
    ArrayList<String> listLow;
    ArrayList<String> listAdvance;
    private Map<String, Moveable> lowLevelControlMap= new HashMap<>();   //兩個 hashmap，放移動指令與 Moveable??
    private Map<String, Moveable> advanceControlMap= new HashMap<>();

    private Switch mWheelSwitch;   //移動鎖定的開關(Switch)
    private CheckBox mDropDectionCheckBox;//如果勾了 就會去偵測 凱比是否被放在高處 ，掉落警告

    private HandlerThread handlerThread;   //子線程
    private MessageHandler mMessageHandler; //子線程與主線程溝通的 handler

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();  //初始化

        //Step 1 : Initial Nuwa API Object       使用API的前置作業
        mClientId = new IClientId(this.getPackageName());
        mRobotAPI = new NuwaRobotAPI(this, mClientId);  //this =現在的 Activity

        //Step 2 : Register to receive Robot Event
        Logger.d("register RobotEventCallback ") ;
        mRobotAPI.registerRobotEventListener(mRobotEventCallback);//listen callback of robot service event
    }

    @Override
    protected void onDestroy() {   //同樣將 API thread給停下來。
        super.onDestroy();
        handlerThread.quit();
        mRobotAPI.release();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {   //按到凱比頭上那顆鍵時
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initMovementHandler(){
        handlerThread = new HandlerThread("Message Handler");
        handlerThread.start();
        mMessageHandler = new MessageHandler(handlerThread.getLooper());
    }

    private void initMovementCb(){   //初始化 + 設定凱比移動距離的參數 !!!!!重要!!!!!
        runOnUiThread(()->{    //放在UI thread內執行
            //init low level control
            //有設定一秒的極限： 0.2* 1M =20 cm ，之後給 mRobotAPI.move 的參數應該都是 0.2，-0.2 不像這個的 Random
            lowLevelControlMap.put("move(float val) max: 0.2(Meter/sec) go forward, min: -0.2(Meter/sec) go back", ()->mRobotAPI.move(new Random().nextFloat() * 0.4f - 0.2f)); //-0.2f ~ 0.2f
            //最多正負30度 ，不過因為只能轉 30 度是比較細的操作 ，之後應該不會採用
            lowLevelControlMap.put("turn(float val) max: 30(Degree/sec) turn left, min: -30(Degree/sec) turn right", ()->mRobotAPI.turn(new Random().nextInt(30 - (-30) ) - 30)); //-30 ~ 30 degree   bound = 0~59 - 30
            //清空 ArrayList ，丟入兩個低階指令。
            listLow.clear();
            listLow.addAll(lowLevelControlMap.keySet());

            advanceControlMap.put("forwardInAccelerationEx()", mRobotAPI::forwardInAccelerationEx);  //是我測錯了嗎? 為什麼他的向前加速走的跟烏龜一樣?還有距離有夠短
            advanceControlMap.put("backInAccelerationEx()", mRobotAPI::backInAccelerationEx);    //向後也是
            advanceControlMap.put("stopInAccelerationEx()", mRobotAPI::stopInAccelerationEx);      //慢到我覺得 不用停止加速了:3
            advanceControlMap.put("turnLeftEx()", mRobotAPI::turnLeftEx);  //左轉30度
            advanceControlMap.put("turnRightEx()", mRobotAPI::turnRightEx); //右轉30度
            advanceControlMap.put("stopTurnEx()", mRobotAPI::stopTurnEx); //中斷旋轉
            //清空 ArrayList ，丟入高階指令。
            listAdvance.clear();
            listAdvance.addAll(advanceControlMap.keySet());

            //update list view 更新
            mMovementListLowAdapter.notifyDataSetChanged();
            mMovementListAdvanceAdapter.notifyDataSetChanged();
        });
    }

    private String getWheelSwitchText(boolean lock_unlock){  //設定Switch 狀態的function
        String sLocked = getResources().getString(R.string.movement_text_lock);
        String sUnlocked = getResources().getString(R.string.movement_text_unlock);
        if(lock_unlock)
            return getResources().getString(R.string.movement_text_wheel, sLocked);
        else
            return getResources().getString(R.string.movement_text_wheel, sUnlocked);
    }

    private void initSwitchState(){
        runOnUiThread(()->{   //放在UI thread內執行
            mWheelSwitch.setChecked(false);  //預設 false
            mWheelSwitch.setText(getWheelSwitchText(false));
            mWheelSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
                mWheelSwitch.setEnabled(false);
                if(isChecked)
                    mRobotAPI.lockWheel();
                else
                    mRobotAPI.unlockWheel();

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e){
                    //do nothing
                }
                mWheelSwitch.setText(getWheelSwitchText(isChecked));
                mWheelSwitch.setEnabled(true);
            });

            mDropDectionCheckBox.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> { //要求掉落偵測的
                if(isChecked)
                    mRobotAPI.requestSensor(NuwaRobotAPI.SENSOR_DROP);
                else
                    mRobotAPI.stopSensor(NuwaRobotAPI.SENSOR_DROP);
            });
        });
    }

    private void initView(){
        setContentView(R.layout.activity_movementcontrol);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {  //如果存在toolbar
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);   //左上角的返回會出現
            getSupportActionBar().setTitle(this.getClass().getCanonicalName()); //設定toolbar的標題為 package+Activity的名字
        }

        //init Switch  找到switch的對應 View
        mWheelSwitch = findViewById(R.id.lock_wheel);

        //init CheckBox  找到選單的對應 View
        mDropDectionCheckBox = findViewById(R.id.detect_drop);

        //init list view   初始化最開始宣告的變數們
        listLow = new ArrayList<>(lowLevelControlMap.keySet());    //拿到lowLevelControlMap所有的 key值。
        mMovementListLowAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, listLow); //Adapter參數說明：http://aiur3908.blogspot.com/2015/06/android-listview.html
        mMovementListLow = findViewById(R.id.movement_list_low);
        mMovementListLow.setAdapter(mMovementListLowAdapter);
        mMovementListLow.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) ->{   //設定被 click反應
                Logger.d( "mMovementListLow: " + mMovementListLowAdapter.getItem(position) + " is clicked!");
                if(mMessageHandler != null){  //確定 handler被設定好
                    mMessageHandler.obtainMessage(MessageHandler.MSG_START_MOVING, mMovementListLowAdapter.getItem(position)).sendToTarget();
                    //stop moving after 1s   示範只讓他跑一秒鐘 ，用延遲送出停止訊息的方式
                    mMessageHandler.sendMessageDelayed(mMessageHandler.obtainMessage(MessageHandler.MSG_STOP_MOVING), 5000);
                } else
                    Toast.makeText(getApplicationContext(), "Not Ready to Moving, Click after a while", Toast.LENGTH_SHORT).show();
        });

        listAdvance = new ArrayList<>(advanceControlMap.keySet());
        mMovementListAdvanceAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, listAdvance);   // ArrayAdapter 最基本的 adapter可以丟入string陣列 >>顯示string
        mMovementListAdvance = findViewById(R.id.movement_list_advance);
        mMovementListAdvance.setAdapter(mMovementListAdvanceAdapter);
        mMovementListAdvance.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) ->{
                Logger.d( "mMovementListAdvance: " + mMovementListAdvanceAdapter.getItem(position) + " is clicked!");
                if(mMessageHandler != null){
                    mMessageHandler.obtainMessage(MessageHandler.MSG_START_MOVING, mMovementListAdvanceAdapter.getItem(position)).sendToTarget();
                    //stop moving after 1s  也是執行一秒就結束
                    mMessageHandler.sendMessageDelayed(mMessageHandler.obtainMessage(MessageHandler.MSG_STOP_MOVING), 1000);
                } else
                    Toast.makeText(getApplicationContext(), "Not Ready to Moving, Click after a while", Toast.LENGTH_SHORT).show();
        });
    }

    private RobotEventCallback mRobotEventCallback = new RobotEventCallback() { //特定事件觸發時的回傳設定
        @Override
        public void onWindowSurfaceReady() {
            Logger.d("mRobotEventCallback.onWindowSurfaceReady()");
        }

        @Override
        public void onWikiServiceStop() {
            Logger.d("onWikiServiceStop");

        }

        @Override
        public void onWikiServiceStart() { //代表凱比的API已經可以使用 +初始化這個Activity上的物件
            Logger.d("onWikiServiceStart");
            //ready to moving
            initMovementHandler();   //初始化 Handler +子線程
            initMovementCb();
            initSwitchState();
        }

        @Override
        public void onWikiServiceCrash() {
            Logger.d("onWikiServiceCrash");

        }

        @Override
        public void onWikiServiceRecovery() {
            Logger.d("onWikiServiceRecovery");

        }

        @Override
        public void onDropSensorEvent(int value) {  //把凱比拿起來的時候出現的警示 ，我會怕高
            Toast.makeText(getApplicationContext(), "onDropSensorEvent(" + value + ") received", Toast.LENGTH_SHORT).show();
        }
    };


    private class MessageHandler extends Handler {
        private static final int MSG_START_MOVING = 1;
        private static final int MSG_STOP_MOVING = 2;

        MessageHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.d("handleMessage(" + msg + ")");
            switch (msg.what){
                case MSG_START_MOVING:
                    String key = (String)msg.obj;
                    Moveable moveable = null;
                    if(lowLevelControlMap != null && lowLevelControlMap.containsKey(key))
                        moveable = lowLevelControlMap.get(key);
                    else if(advanceControlMap !=null && advanceControlMap.containsKey(key))
                        moveable = advanceControlMap.get(key);

                    if(moveable != null)
                        moveable.moveCb();
                    else
                        Logger.d("Cannot find related moveable callback to execute for \"" + key + "\"");
                    break;
                case MSG_STOP_MOVING:
                    mRobotAPI.turn(0);
                    mRobotAPI.move(0);
                    mRobotAPI.stopInAccelerationEx();
                    mRobotAPI.stopTurnEx();
                    break;
            }
        }
    }

    interface Moveable{
        void moveCb();
    }
}
