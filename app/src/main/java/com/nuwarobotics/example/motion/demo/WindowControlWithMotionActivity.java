package com.nuwarobotics.example.motion.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.nuwarobotics.example.R;
import com.nuwarobotics.example.motion.base.BaseAppCompatActivity;
import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.RobotEventCallback;

public class WindowControlWithMotionActivity extends BaseAppCompatActivity {
    private NuwaRobotAPI mRobotAPI;
    private IClientId mClientId;

    private final String MOTION_SAMPLE = "667_P4_Answer"; //一個指定的 Motion
    private TextView mTexPlayStatus;
    Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainHandler = new Handler(Looper.getMainLooper());   //幫 main thread創造一個 message looper ，然後用 Handler處理訊息
        //當 thread只有一個 Runable(任務)要跑，他不需要 Looper，但如果是多次執行，就要用上 Looper

        mTexPlayStatus = findViewById(R.id.play_status);

        //Step 1 : Initial Nuwa API Object
        mClientId = new IClientId(this.getPackageName());
        mRobotAPI = new NuwaRobotAPI(this, mClientId);
        mRobotAPI.registerRobotEventListener(robotEventCallback); //listen callback of robot service event

        Button btn = findViewById(R.id.btn_playmotion);
        btn.setOnClickListener(v->{
            mTexPlayStatus.setText("");

            //Step 2 : Execute "Play motion" without window
            //Notice: If motionPlay with paramter "auto_fadin = true", the displayed window should not be hidden.
            //        Because the method hideWindow() will execute motionStop to stop the current motion.
            mRobotAPI.motionPlay(MOTION_SAMPLE, false);  //"auto_fadin" is false. The initial window is hidden.   //若沒更改 Motion剛開始幾秒會以沒有 window的形式執行

        });

    }

    Runnable showWindowRunable = ()->{  //把凱比的臉給開起來
        if(mRobotAPI != null){
            showEventMsgOnTextView("[Handler]Show Window...");

            //Step 3:  Setup the timing of showing window by user's requirement
            mRobotAPI.showWindow(true);
        }
    };

    private RobotEventCallback robotEventCallback = new RobotEventCallback() {
        @Override
        public void onStartOfMotionPlay(String s) {
            showEventMsgOnTextView("[Event]Start Playing Motion...");

            if(mainHandler == null){
                return;
            }

            mainHandler.postDelayed(showWindowRunable,6000); //這邊的延遲送出
        }

        @Override
        public void onStopOfMotionPlay(String s) {
            showEventMsgOnTextView("[Event]Stop Playing Motion...");
        }

        @Override
        public void onCompleteOfMotionPlay(String s) {  //執行完成>>關掉 Window
            showEventMsgOnTextView("[Event]Play Motion Complete!!!");

            //Step 3 : Hide window for the displayed window
            // the transparent view must be closed after motion is complete, error or other case.
            if(mRobotAPI != null){
                mRobotAPI.hideWindow(true);
            }
        }

        @Override
        public void onPlayBackOfMotionPlay(String s) {
            showEventMsgOnTextView("[Event]Playing Motion...");
        }

        @Override
        public void onErrorOfMotionPlay(int i) {
            showEventMsgOnTextView("[Event]When playing Motion, error happen!!! error code: " + i);

            if(mRobotAPI != null){
                mRobotAPI.hideWindow(true);
            }
        }
    };

    private void showEventMsg(TextView tv, String status){ //更新 TextView跟 Log
        if(tv != null){
            runOnUiThread(()->tv.append(status + "\n"));
        }

        Log.d(TAG, status);
    }

    private void showEventMsgOnTextView(String status){
        showEventMsg(mTexPlayStatus, status);
    }

    @Override
    protected void onStop(){
        super.onStop();

        //Step 4 : Release robotAPI before closing activity
        if(mRobotAPI != null){
            mRobotAPI.release();
        }
    }

    @Override
    protected int getLayoutRes(){
        return R.layout.activity_controlwindow;
    }

    @Override
    protected int getToolBarTitleRes(){
        return R.string.lbl_example_4;
    }
}
