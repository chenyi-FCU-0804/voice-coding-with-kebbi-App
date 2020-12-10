package com.nuwarobotics.example.motion.demo;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.nuwarobotics.example.R;
import com.nuwarobotics.example.motion.base.BaseAppCompatActivity;
import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.RobotEventCallback;

public class PlayMotionActivity extends BaseAppCompatActivity {
    private NuwaRobotAPI mRobotAPI;
    private IClientId mClientId;

    private final String MOTION_SAMPLE = "666_SA_Think";
    private TextView mTexPlayStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTexPlayStatus = findViewById(R.id.play_status);   // Button下面呈現 Motion狀況

        //Step 1 : Initial Nuwa API Object
        mClientId = new IClientId(this.getPackageName());
        mRobotAPI = new NuwaRobotAPI(this, mClientId);
        mRobotAPI.registerRobotEventListener(robotEventCallback); //listen callback of robot service event

        Button btn = findViewById(R.id.btn_playmotion);
        btn.setOnClickListener(v->{
            mTexPlayStatus.setText("");

            //Step 2 : Execute "Play motion"
            mRobotAPI.motionPlay(MOTION_SAMPLE, true);    // fadein true效果：會有凱比的表情跟著Motion一起呈現    // false效果：沒有凱比的表情
        });

    }

    private void showEventMsg(String status){
        runOnUiThread(()->{
            mTexPlayStatus.append(status);
            mTexPlayStatus.append("\n");
            Log.d(TAG, status);
        });

    }

    private RobotEventCallback robotEventCallback = new RobotEventCallback() {   //隨著凱比 Motion的執行 ，下面的Sring都會漸漸出現在 TextView裡。
        @Override
        public void onStartOfMotionPlay(String s) {
            showEventMsg("Start Playing Motion...");
        }

        @Override
        public void onStopOfMotionPlay(String s) {
            showEventMsg("Stop Playing Motion...");
        }

        @Override
        public void onCompleteOfMotionPlay(String s) {
            showEventMsg("Play Motion Complete!!!");

            //Step 3 : If (the parameter of motionPlay)auto_fadein is ture,   如果有設定凱比的表情呈現 ，就要在動作結束時記得關掉他的臉。
            // the transparent view must be closed after motion is complete, error or other case.
            if(mRobotAPI != null){
                mRobotAPI.hideWindow(false);
            }
        }

        @Override
        public void onPlayBackOfMotionPlay(String s) {
            showEventMsg("Playing Motion...");
        }

        @Override
        public void onErrorOfMotionPlay(int i) {
            showEventMsg("When playing Motion, error happen!!! error code: " + i);

            if(mRobotAPI != null){
                mRobotAPI.hideWindow(false);
            }
        }
    };

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
        return R.layout.activity_playmotion;
    }

    @Override
    protected int getToolBarTitleRes(){
        return R.string.lbl_example_2;
    }
}
