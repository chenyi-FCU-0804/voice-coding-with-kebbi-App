package com.nuwarobotics.example.sensor;

import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.nuwarobotics.example.R;
import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.RobotEventListener;

public class SensorExampleActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();
    NuwaRobotAPI mRobotAPI;
    IClientId mClientId;
    TextView mTextHead,mTextChest,mTextFaceLeft,mTextFaceRight,mTextHandLeft,mTextHandRight;
    TextView mTextPIR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        //Sensor觸摸顯示用 TextView做。
        mTextHead = (TextView)findViewById(R.id.textView_head);
        mTextChest = (TextView)findViewById(R.id.textViewChest);
        mTextHandRight = (TextView)findViewById(R.id.textViewRightHand);
        mTextHandLeft = (TextView)findViewById(R.id.textViewLeftHand);
        mTextFaceLeft = (TextView)findViewById(R.id.textViewLeftFace);
        mTextFaceRight = (TextView)findViewById(R.id.textViewRightFace);
        mTextPIR = (TextView)findViewById(R.id.textViewPIR);  //PIR=紅外線感應

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.title_example_sensor);

        //Step 1 : Initial Nuwa API Object
        mClientId = new IClientId(this.getPackageName());
        mRobotAPI = new NuwaRobotAPI(this,mClientId);

        //Step 2 : Register receive Robot Event
        Log.d(TAG,"register EventListener ") ;
        mRobotAPI.registerRobotEventListener(robotEventListener);//listen callback of robot service event





    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // release Nuwa Robot SDK resource
        mRobotAPI.release();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {  // home鍵被按到的時候
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    RobotEventListener robotEventListener = new RobotEventListener() {
        @Override
        public void onWikiServiceStart() {  //在 API 設定好時跟著 requsetSensor
            // Nuwa Robot SDK is ready now, you call call Nuwa SDK API now.
            Log.d(TAG,"onWikiServiceStart, robot ready to be control ") ;
            // request touch sensor event
            //NOTICE : PLEASE REQUEST ON SERVICE_START
            mRobotAPI.requestSensor(NuwaRobotAPI.SENSOR_TOUCH | NuwaRobotAPI.SENSOR_PIR | NuwaRobotAPI.SENSOR_DROP);
            //touch event will received by onTouchEvent
            //PIR event will received by onPIREvent
            //drop sensor event will received by onDropSensorEvent
        }

        @Override
        public void onWikiServiceStop() {

        }

        @Override
        public void onWikiServiceCrash() {

        }

        @Override
        public void onWikiServiceRecovery() {

        }

        @Override
        public void onStartOfMotionPlay(String s) {

        }

        @Override
        public void onPauseOfMotionPlay(String s) {

        }

        @Override
        public void onStopOfMotionPlay(String s) {

        }

        @Override
        public void onCompleteOfMotionPlay(String s) {

        }

        @Override
        public void onPlayBackOfMotionPlay(String s) {

        }

        @Override
        public void onErrorOfMotionPlay(int i) {

        }

        @Override
        public void onPrepareMotion(boolean b, String s, float v) {

        }

        @Override
        public void onCameraOfMotionPlay(String s) {

        }

        @Override
        public void onGetCameraPose(float v, float v1, float v2, float v3, float v4, float v5, float v6, float v7, float v8, float v9, float v10, float v11) {

        }

        @Override
        public void onTouchEvent(int type, int touch) {
            // type: head: 1, chest: 2, right hand: 3, left hand: 4, left face: 5,right face: 6.
            // touch: touched: 1, untouched: 0
            Log.d(TAG,"onTouchEvent type="+type+" touch="+touch);
            int color = (touch==1)?Color.GREEN:Color.GRAY;   //如果touch ==1 有被摸 >>變成綠色 ，不然就還是灰色。
            switch(type) {
                case 1 :
                    mTextHead.setBackgroundColor(color);
                    break;
                case 2:
                    mTextChest.setBackgroundColor(color);
                    break;
                case 3:
                    mTextHandRight.setBackgroundColor(color);
                    break;
                case 4:
                    mTextHandLeft.setBackgroundColor(color);
                    break;
                case 5:
                    mTextFaceLeft.setBackgroundColor(color);
                    break;
                case 6:
                    mTextFaceRight.setBackgroundColor(color);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onPIREvent(int val) {   //紅外線感應到>>亮綠燈
            Log.d(TAG,"onPIREvent val="+val);
            int color = (val==1)?Color.GREEN:Color.GRAY;
            mTextPIR.setBackgroundColor(color);
        }

        @Override
        public void onTap(int type){    //如果只有被Tap  就傳Log的文字
            Log.d(TAG,"onTap type="+type);
            // type: head: 1, chest: 2, right hand: 3, left hand: 4, left face: 5,right face: 6.

        }

        @Override
        public void onLongPress(int type) {
            Log.d(TAG,"onLongPress type="+type);
            // type: head: 1, chest: 2, right hand: 3, left hand: 4, left face: 5,right face 6.
            int color = Color.RED;
            switch(type) {
                case 1 :
                    mTextHead.setBackgroundColor(color);
                    break;
                case 2:
                    mTextChest.setBackgroundColor(color);
                    break;
                case 3:
                    mTextHandRight.setBackgroundColor(color);
                    break;
                case 4:
                    mTextHandLeft.setBackgroundColor(color);
                    break;
                case 5:
                    mTextFaceLeft.setBackgroundColor(color);
                    break;
                case 6:
                    mTextFaceRight.setBackgroundColor(color);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onWindowSurfaceReady() {

        }

        @Override
        public void onWindowSurfaceDestroy() {

        }

        @Override
        public void onTouchEyes(int i, int i1) {

        }

        @Override
        public void onRawTouch(int i, int i1, int i2) {

        }

        @Override
        public void onFaceSpeaker(float v) {

        }

        @Override
        public void onActionEvent(int i, int i1) {

        }

        @Override
        public void onDropSensorEvent(int value) {

        }

        @Override
        public void onMotorErrorEvent(int i, int i1) {

        }
    };
}
