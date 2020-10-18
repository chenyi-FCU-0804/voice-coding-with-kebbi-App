package com.nuwarobotics.example.motor;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.nuwarobotics.example.R;
import com.nuwarobotics.example.util.Logger;
import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.RobotEventCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MotorControlActivity extends AppCompatActivity {
    NuwaRobotAPI mRobotAPI; //要用到API都需要這兩個物件
    IClientId mClientId;    //要用到API都需要這兩個物件
    ListView mMotorListView;

    private HandlerThread handlerThread;
    private MessageHandler handler;

    enum Motor{
        neck_y(1),
        neck_z(2),
        right_shoulder_z(3),
        right_shoulder_y(4),
        right_shoulder_x(5),
        right_elbow_y(6),
        left_shoulder_z(7),
        left_shoulder_y(8),
        left_shoulder_x(9),
        left_elbow_y(10);

        private int motorId;  //Motor的被定義的常數 neck_y=1
        private static SparseArray<Motor> array = new SparseArray<>();  //稀疏陣列=SparseArray ，https://www.itread01.com/content/1548758538.html

        static {    //他對應的號碼+動作放進array   enum解釋：https://cms.35g.tw/coding/java-enum-example/
            for(Motor motor : Motor.values())   //values就像把motor的一個小物件拿出來 包含 名稱跟數字
                array.put(motor.motorId, motor);
        }

        Motor(int motorId) {   //Motor的建構子(初始化)
            this.motorId = motorId;
        }

        public int getMotorId() { //取得現在的 motorId
            return this.motorId;
        }

        public static Motor valueOf(int motorId){  //返回ID對應的motor
            return array.get(motorId);
        }

        public static String[] getStringArray(){   //整理動作指令的格式
            return Arrays.toString(Motor.values()).replaceAll("^.|.$", "").split(", ");
        }
    }

    private static Map<Motor, Integer> moveRecordMap = new HashMap<>();

    static { //建立一個放 motor 跟 int的 hashmap
        for(Motor motor : Motor.values())
            moveRecordMap.put(motor, 20); //default is 20 degree(改角度的地方)
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();

        //Step 1 : Initial Nuwa API Object          //要取用API服務前都需要step1 and 2
        mClientId = new IClientId(this.getPackageName());
        mRobotAPI = new NuwaRobotAPI(this, mClientId);

        //Step 2 : Register to receive Robot Event
        Logger.d("register RobotEventCallback ") ;
        mRobotAPI.registerRobotEventListener(mRobotEventCallback);//listen callback of robot service event

    }

    @Override
    protected void onDestroy() {    //把一開始叫的 API 跟 handler關掉
        super.onDestroy();
        mRobotAPI.release();
        handlerThread.quit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {  //用onOptionItemSelected來控制點擊事件 ，但是在SDK內沒有看到 Menu
        switch (item.getItemId()) {
            case android.R.id.home:      //這邊的 R.id.home在哪?是凱比頭上那個按鈕嗎?
                finish();       //跳出app回到凱比的臉 ，但還沒有釋放剛剛占用的資源。
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initMotionHandler(){    //設定handler的子線程開始運作 ，開始接收被click的動作
        handlerThread = new HandlerThread("Motion Handler");
        handlerThread.start();

        handler = new MessageHandler(handlerThread.getLooper());
    }

    private void initView(){
        setContentView(R.layout.activity_motorcontrol);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(this.getClass().getCanonicalName());
        }

        //init list view
        final ArrayList<String> listArray = new ArrayList<>(Arrays.asList(Motor.getStringArray()));   //就等於pyhton 的 list   ，asList=把 strings放到 list內
        mMotorListView = findViewById(R.id.motion_list);
        mMotorListView.setAdapter(new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, listArray));    //這邊的android.R.layout.simple_list_item_1是android內建的 layout
                //http://aiur3908.blogspot.com/2015/06/android-listview.html
        mMotorListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Logger.d( "mMotorListView: " + listArray.get(position) + " is clicked!");   //用 position來知道哪個動作被click了
                if(handler != null){    //handler的作用是? adapter 跟主線程溝通的管道
                    handler.obtainMessage(MessageHandler.MSG_START_MOTOR_MOVING, position + 1/*motorId start from 1*/, 0).sendToTarget();
                    //從handler拿到的參數： 1.開始 or結束的常數 2. 現在執行的指令 3.因為沒用到所以丟0??
                } else
                    Toast.makeText(getApplicationContext(), "Not Ready to Moving, Click after a while", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private RobotEventCallback mRobotEventCallback = new RobotEventCallback() {
        @Override
        public void onWindowSurfaceReady() {
            Logger.d("mRobotEventCallback.onWindowSurfaceReady()");
        }

        @Override
        public void onWikiServiceStop() {
            Logger.d("onWikiServiceStop");
        }

        @Override
        public void onWikiServiceStart() {    //這個要事先執行，才可以call API
            Logger.d("onWikiServiceStart");
            //ready to moving
            initMotionHandler();
        }

        @Override
        public void onWikiServiceCrash() {
            Logger.d("onWikiServiceCrash");

        }

        @Override
        public void onWikiServiceRecovery() {
            Logger.d("onWikiServiceRecovery");

        }
    };

    private void moveMotor(int motorId){
        if(moveRecordMap.containsKey(Motor.valueOf(motorId))){
            Integer current_degree = moveRecordMap.get(Motor.valueOf(motorId));
            if(current_degree != null) {    //如果有相應的動作指令 >>
                mRobotAPI.ctlMotor(motorId, 0, current_degree, 45); // default SpeedInDegreePerSec is 45
                //向API發出動作指令  參數： 動作部位，速度，移動角度，一秒轉幾度
                moveRecordMap.put(Motor.valueOf(motorId), (-1) * current_degree); // reverse the degree
                //執行第一次走20度，第二次會走反方向的20度，所以要將角度 *(-1)
                //put會直接將相同 key的值給覆蓋掉。
            } else
                Logger.d("No related degree parameter for " + Motor.valueOf(motorId));
        } else
            Logger.d("No related motor config for " + Motor.valueOf(motorId));

    }

    private class MessageHandler extends Handler {
        private static final int MSG_START_MOTOR_MOVING = 1;
        private static final int MSG_STOP_MOTOR_MOVING = 2;

        public MessageHandler(Looper looper){  //Looper會一直從thread拿訊息 ，handler則把訊息送出去
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {    //從obtainMessage拿到的訊息會送到這處理
            Logger.d("handleMessage(" + msg + ")");
            switch (msg.what){
                case MSG_START_MOTOR_MOVING:
                    moveMotor(msg.arg1);    //第一個參數傳的是要執行的動作
                    break;
                case MSG_STOP_MOTOR_MOVING:
                    break;
            }
        }
    }
}
