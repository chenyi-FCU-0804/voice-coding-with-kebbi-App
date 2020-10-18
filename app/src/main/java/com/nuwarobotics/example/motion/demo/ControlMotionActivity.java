package com.nuwarobotics.example.motion.demo;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.nuwarobotics.example.R;
import com.nuwarobotics.example.motion.base.BaseAppCompatActivity;
import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.RobotEventCallback;

public class ControlMotionActivity extends BaseAppCompatActivity implements View.OnClickListener{
    private NuwaRobotAPI mRobotAPI;
    private IClientId mClientId;

    private TextView mTexPlayStatus;   //播放Motion執行情況

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUIComponents();

        //Step 1 : Initial Nuwa API Object
        mClientId = new IClientId(this.getPackageName());
        mRobotAPI = new NuwaRobotAPI(this, mClientId);
        mRobotAPI.registerRobotEventListener(robotEventCallback); //listen callback of robot service event

    }

    private RobotEventCallback robotEventCallback = new RobotEventCallback() {   //宣告 Motion各種情況的訊息回傳
        @Override
        public void onStartOfMotionPlay(String motion) {
            showEventMsg("[Event]Start playing Motion... ,Motion: " + motion);
        }

        @Override
        public void onStopOfMotionPlay(String motion) {
            showEventMsg("[Event]Stop playing Motion... ,Motion: " + motion);
        }

        @Override
        public void onCompleteOfMotionPlay(String motion) {
            showEventMsg("[Event]Playing Motion is complete!!! Motion: " + motion);
        }

        @Override
        public void onPlayBackOfMotionPlay(String motion) {
            showEventMsg("[Event]Playing Motion... ,Motion: " + motion);
        }

        @Override
        public void onErrorOfMotionPlay(int i) {
            showEventMsg("[Event]When playing Motion, error happen!!! error code: " + i);
        }

        @Override
        public void onPauseOfMotionPlay(String motion) {
            showEventMsg("[Event]Pausing Motion... ,Motion: " + motion);
        }

        @Override
        public void onPrepareMotion(boolean isError, String motion, float duration) {
            showEventMsg("[Event]Prepare status, isError: " + isError + " ,Motion: " + motion
                + " ,duration: " + duration);
        }

    };

    @Override
    protected void onStop(){  //離開時處理
        super.onStop();

        //Step 3 : Release robotAPI before closing activity
        if(mRobotAPI != null){
            mRobotAPI.release();
        }
    }

    @Override
    protected int getLayoutRes(){
        return R.layout.activity_controlmotion;
    } //設定layout

    @Override
    protected int getToolBarTitleRes(){
        return R.string.lbl_example_3;
    }

    private void showEventMsg(String status){    //在 mTexPlayStatus 新增一行新的status
        runOnUiThread(()->{    //在UI thread時更新UI
            mTexPlayStatus.append(status);
            mTexPlayStatus.append("\n");
            Log.d(TAG, status);     // TAG 在BaseAppCompatActivity宣告過 = NuwaSDKMotion
        });

    }

    private void initUIComponents(){
        mTexPlayStatus = findViewById(R.id.play_status);
        mTexPlayStatus.setMovementMethod(ScrollingMovementMethod.getInstance());  //設定可滑動的TextView
        mTexPlayStatus.setOnClickListener(this);   //設定點擊 Listener


        int[] btnResIdList = {      //抓四個按鈕的 ID
                R.id.btn_play,
                R.id.btn_pause,
                R.id.btn_resume,
                R.id.btn_stop,
        };

        for(int resId : btnResIdList){
            Button btn = findViewById(resId);

            if(btn != null){
                btn.setOnClickListener(this);
            }
        }
    }

    @Override
    public void onClick(View v){    //處理剛剛所有宣告的 onClickListener
        //Step 2 : Setup the click action of  "Play/Pause/Resume/Stop motion"
        //Play motion without transparent透明的 view for the demo of other actions
        switch(v.getId()){   //從被 click的 ID分別
            case R.id.btn_play:
                showEventMsg("[Click Button]Play");
                PopupMenu popup = new PopupMenu(ControlMotionActivity.this, findViewById(R.id.btn_play));   //第二個參數(View) 代表 Menu在被什麼點到時會觸發
                popup.getMenuInflater().inflate(R.menu.popup_menu_querymotions, popup.getMenu());  //需要在 res>> menu 創建一個 menu的文件 ，後面那個參數會將原本再 res內的 menu的 選項帶進來
                popup.getMenu().clear();   //但要的選項不是在menu內預設的 ，所以先清空一次 再用 for裝進去

                String[] itemList = {
                        "666_RE_Bye",
                        "666_TA_LookRL",
                        "666_PE_Killed",
                        "666_DA_Scratching",
                        "666_IM_Rooster",
                        "666_TA_LookLR",
                        "666_PE_PlayGuitar",
                        "666_DA_PickUp"};

                for(String item : itemList) { //把 Menu的選項都加進去
                    popup.getMenu().add(item);
                }

                popup.setOnMenuItemClickListener((item)->{
                    mRobotAPI.motionStop(true);    //先停止舊動作
                    mRobotAPI.motionPlay(item.getTitle().toString(), false);  //再執行新動作  用 666_RE_Bye的代號去呼叫 Motion
                    return true;
                });

                popup.show();

                break;
            case R.id.btn_pause:
                showEventMsg("[Click Button]Pause");
                mRobotAPI.motionPause();
                break;
            case R.id.btn_resume:
                showEventMsg("[Click Button]Resume");
                mRobotAPI.motionResume();
                break;
            case R.id.btn_stop:
                showEventMsg("[Click Button]Stop");
                mRobotAPI.motionStop(true);
                break;
            case R.id.play_status:   //清空textview
                mTexPlayStatus.setText("");
            default:
                Log.d(TAG, "Can't find the clicking action of view!!!");  // TAG 在BaseAppCompatActivity宣告過 = NuwaSDKMotion
        }
    }

}



