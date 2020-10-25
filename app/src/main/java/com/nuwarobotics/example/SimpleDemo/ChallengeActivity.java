package com.nuwarobotics.example.SimpleDemo;

import android.content.DialogInterface;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.nuwarobotics.example.SimpleDemo.selfMadeObject.RecyclerViewAdapter;

import com.nuwarobotics.example.R;
import com.nuwarobotics.example.SimpleDemo.selfMadeObject.RecyclerViewAdapter_btnRemove;
import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.RobotEventListener;
import com.nuwarobotics.service.agent.SimpleGrammarData;
import com.nuwarobotics.service.agent.VoiceEventListener;
import com.nuwarobotics.service.agent.VoiceResultJsonParser;

import java.util.ArrayList;
/*
* 用途:闖關的Activity                            目前進度：446行 Dialog的XML檔還沒做 + 按鈕功能實作
* 對應到： R.layout.activity_challenge
* 因為沒有訂出要 LocalCMD還是CloudASR，所以先用 localCMD的方式實作語音輸入部分 (已完成)
* 目前完成：中間兩個 RecyclerView的實作 ，在尚未添加指令前，右邊的 RecyclerView會是空的 +localCMD + 實作語音輸入功能 + 用mRobotAPI.startTTS實作輸入回饋 or 提醒 + btnStart 開始、停止功能切換 and 延後Enable的處理 + 過關時長摸凱比感應器判斷 。
* 部分完成：button_cmdRun 實作 (前進、後退、左轉、右轉、結束、執行。 已實作) 、dialog的內容只是草稿
* 尚未完成：資料庫連接 + 過關時更新TexView textView_stageIndex 、textView_mission2 、 可用指令 RecyclerView在過關時的更新。
*
*問題： 208 更新 btnRun狀態失敗(目前放棄)
* */
public class ChallengeActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = this.getClass().getSimpleName();  //==ChallengeActivity
    NuwaRobotAPI mRobotAPI;
    IClientId mClientId;

    private RecyclerViewAdapter Radapter_canUse;    //兩組會用到的 RecyclerView
    private RecyclerView recyclerView_canUse;
    private ArrayList<String> cmd_canUse = new ArrayList<String>() {{    //可以用的指令，如果有資料庫，會從等級判斷+匯入     add("函示(function)");這個指令應該適用在新增(程式實驗室)那邊才有
        add("前進 (一個格子)");
        add("後退 (一個格子)");
        add("左轉 (90度)");
        add("右轉 (90度)");
        add("迴圈1號 (重複X次)");  //需要在畫面上輸入重複幾次
        add("迴圈2號 (for迴圈)");  //需要在畫面上設定for的參數
        add("迴圈3號 (while迴圈)");//需要在畫面上設定參數
        add("結束 (停止接收語音指令)");
        add("執行 (結束+自動執行)"); //會直接結束語音接收，執行btnRun 的 onclick
    }};

    private RecyclerViewAdapter_btnRemove Radapter_hasUse;  //跟上面class不一樣 因為多出一個刪除鍵
    private RecyclerView recyclerView_hasUse;
    //這邊只是為了測效果寫死的，真的在用的時候是用空的
    private ArrayList<String> cmd_hasUse=new ArrayList<String>(){{
        add("前進");
    }};
    // local CMD指令
    private ArrayList<String> cmdList = new ArrayList<String>() {{
        add("前進");add("後退");add("左轉");add("右轉");
        add("迴圈一號");add("迴圈二號");add("迴圈三號");
        add("結束");add("執行");
    }};//you can customize this list
    private Button btnStart;   //開始輸入語音
    private Button btnClear;   //清空待執行指令RecyclerView
    private Button btnRun;      //開始執行
    private boolean isWaitingforConfirm;   //確認是否在確認答案的環節

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);
        initUIcomponent();
        //Step 1 : Initial Nuwa API Object
        mClientId = new IClientId(this.getPackageName());
        mRobotAPI = new NuwaRobotAPI(this, mClientId);

        //Step 2 : Register receive Robot Event
        Log.d(TAG, "register EventListener ");
        mRobotAPI.registerRobotEventListener(robotEventListener);//listen callback of robot service event
    }

    private void initUIcomponent() {
        Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar_challenge);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);  //設定返回鍵
        actionBar.setTitle("闖關模式。破關後學習更多新指令吧!!");
        //初始化兩個 RecyclerView
        // 連結元件
        recyclerView_canUse = (RecyclerView) findViewById(R.id.RecyclerView_cmdCanUse);
        // 設置RecyclerView為列表型態
        recyclerView_canUse.setLayoutManager(new LinearLayoutManager(this));
        // 設置格線
        recyclerView_canUse.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        // 將資料交給adapter
        Radapter_canUse = new RecyclerViewAdapter(cmd_canUse);
        // 設置adapter給recycler_view
        recyclerView_canUse.setAdapter(Radapter_canUse);

        // 連結元件
        recyclerView_hasUse = (RecyclerView) findViewById(R.id.RecyclerView_cmdHasUse);
        // 設置RecyclerView為列表型態
        recyclerView_hasUse.setLayoutManager(new LinearLayoutManager(this));
        // 設置格線
        recyclerView_hasUse.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        // 將資料交給adapter
        Radapter_hasUse = new RecyclerViewAdapter_btnRemove(cmd_hasUse);
        // 設置adapter給recycler_view
        recyclerView_hasUse.setAdapter(Radapter_hasUse);
        //初始化下面三個按鈕
        btnStart=(Button)findViewById(R.id.button_startInputCMD);
        btnStart.setOnClickListener(this);
        btnStart.setEnabled(false);  //直到文法準備完成才能設為 Enabled
        btnClear=(Button)findViewById(R.id.button_cmdClear);
        btnClear.setOnClickListener(this);
        btnClear.setEnabled(false);  //在接收語音時 false，平常狀態 Enabled ，與btnStart同時 Enabled
        btnRun=(Button)findViewById(R.id.button_cmdRun);
        btnRun.setOnClickListener(this);
        btnRun.setEnabled(false);   //一直 unabled 直到語音接收出現了結束關鍵字後
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {  //點到返回鍵處理  取消凱比可能正在執行的動作
        switch (item.getItemId()) {
            case android.R.id.home:
                mRobotAPI.turn(0);
                mRobotAPI.move(0);
                mRobotAPI.stopInAccelerationEx();
                mRobotAPI.stopTurnEx();
                mRobotAPI.enableSystemLED();
                mRobotAPI.stopListen();
                mRobotAPI.stopTTS();
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // release Nuwa Robot SDK resource
        mRobotAPI.release();
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.button_startInputCMD:
                String btnText=btnStart.getText().toString();
                //Step 6 : call start listen by local command which registered by createGrammar
                Log.d(TAG, "onClick to start startLocalCommand");
                // 文字 + 語音上的提示
                if(btnText.equals("輸入指令開始")){  //目前功能是開始接收
                    mRobotAPI.startTTS("開始接收語音指令");
                    mRobotAPI.startLocalCommand();//Start listen without wakeup, callback on onMixUnderstandComplete
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnStart.setText("結束指令輸入");  //將按鈕切換成結束語音指令輸入功能
                            btnStart.setEnabled(false);   //要是在開始 輸入後馬上點擊結束會有Bug 所以延後幾秒才讓 btnStart可以點擊
                            btnClear.setEnabled(false);
                            btnRun.setEnabled(false);
                        }
                    });
                    new Handler().postDelayed(new Runnable() {  //延後 3 秒才讓 btnStart可以點擊
                        @Override
                        public void run() {
                            //等 3000/1000秒後要做的事
                            btnStart.setEnabled(true);
                        }
                    }, 3000);
                }
                else{   //目前功能是結束接收
                    mRobotAPI.startTTS("結束語音指令輸入");
                    mRobotAPI.stopListen();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnStart.setText("輸入指令開始");
                            btnStart.setEnabled(false);   //同樣 在結束指令後馬上點擊結束會有問題 所以延後幾秒才讓 btnStart可以點擊
                            btnClear.setEnabled(true);
                            btnRun.setEnabled(true);
                        }
                    });
                    new Handler().postDelayed(new Runnable() {  //延後 3 秒才讓 btnStart可以點擊
                        @Override
                        public void run() {
                            //等 3000/1000秒後要做的事
                            btnStart.setEnabled(true);
                        }
                    }, 3000);
                }
                break;
            case R.id.button_cmdClear:
                if(cmd_hasUse.size()==0){
                    mRobotAPI.startTTS("已經沒有指令了喔，請新增一些吧");
                }
                else{
                    mRobotAPI.startTTS("清空了待執行指令區");
                    Radapter_hasUse.removeAllItem(0,cmd_hasUse.size());
                }
                break;
            case R.id.button_cmdRun:   //因為一直沒辦法更新 UI ，所以不搞了
                //把cmd_hasUse的指令一個一個抓出來，丟到funtion內實施動作
                if(cmd_hasUse.size()==0){
                    mRobotAPI.startTTS("沒有指令的話沒辦法執行喔");
                }
                else{
//                    runOnUiThread(new Runnable() {  //更新 btnRun狀態失敗1
//                        @Override
//                        public void run() {
//                            btnRun.setText("暫停執行");
//                            btnStart.setEnabled(false);   //同樣 在結束指令後馬上點擊結束會有問題 所以延後幾秒才讓 btnRun可以點擊
//                            btnClear.setEnabled(false);
//                            btnRun.setEnabled(false);
//                            new Handler().postDelayed(new Runnable() {  //延後 3 秒才讓 btnStart可以點擊
//                                @Override
//                                public void run() {
//                                    //等 3000/1000秒後要做的事
//                                    btnRun.setEnabled(true);
//                                }
//                            }, 3000);
//                        }
//                    });
                    mRobotAPI.startTTS("開始執行指令");

                    try {  //延後 3 秒才讓開始執行 為了要等凱比說完剛剛的話
                        Thread.sleep(3000);
                        runCmd(cmd_hasUse);  //執行指令
                    } catch (InterruptedException e) {
                        Log.d(TAG,"RunCmd's sleep_cmdRun1 went wrong");
                        return;
                    }
                    mRobotAPI.startTTS("指令執行完畢，等待過關檢驗");    ////這邊還要做過關 摸頭三秒的檢驗
                    mRobotAPI.startTTS("請長摸我其中一個感應器直到我發出紅光");
                    isWaitingforConfirm=true;
//                    runOnUiThread(new Runnable() {  //執行完畢允許按鈕被按   //更新 btnRun狀態失敗2
//                        @Override
//                        public void run() {
//                            btnStart.setEnabled(true);
//                            btnClear.setEnabled(true);
//                            btnRun.setEnabled(true);
//                        }
//                    });
                    mRobotAPI.disableSystemLED();   //先拿到系統LED的控制權 ，記得還回去  + 凱比的LED全設成黑的
                    mRobotAPI.setLedColor(1,255,0,0,0);
                    mRobotAPI.setLedColor(2,255,0,0,0);
                    mRobotAPI.setLedColor(3,255,0,0,0);
                    mRobotAPI.setLedColor(4,255,0,0,0);
                    //關卡更新 跟 確認對話框 都在 onLongPress
                }
                break;
            default:  //沒有對應的 ID的時候
                Log.d(TAG,"Onclick view cant found");
        }
    }

    private void runCmd(ArrayList<String> cmd_hasUse) {
        for(int i=0;i<cmd_hasUse.size();i++){
            String tempStr=cmd_hasUse.get(i);
            //依指令名執行動作
            Log.d(TAG,tempStr);
            switch (tempStr){
                case "前進":  //延遲1.05秒結束 =走21公分
                    mRobotAPI.move(0.2f);
                    try {
                        Thread.sleep(1050);
                    } catch (InterruptedException e) {
                        Log.d(TAG,"RunCmd's sleep_foward went wrong");
                        return;
                    }
                    break;
                case "後退": //延遲1.05秒結束 =走21公分
                    mRobotAPI.move(-0.2f);
                    try {
                        Thread.sleep(1050);
                    } catch (InterruptedException e) {
                        Log.d(TAG,"RunCmd's sleep_backward went wrong");
                        return;
                    }
                    break;
                case "左轉":  //延遲三秒結束  =轉90度
                    mRobotAPI.turn(30);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Log.d(TAG,"RunCmd's sleep_leftTurn went wrong");
                        return;
                    }
                    break;
                case "右轉": //延遲三秒結束 =轉90度
                    mRobotAPI.turn(-30);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Log.d(TAG,"RunCmd's sleep_rightTurn went wrong");
                        return;
                    }
                    break;
                case"執行":
                        Log.d(TAG,"execute start :D");
                    break;
            }
            //一個指令完後結束機器人所有動作再執行下一個
            mRobotAPI.turn(0);
            mRobotAPI.move(0);
            mRobotAPI.stopInAccelerationEx();
            mRobotAPI.stopTurnEx();
        }
    }

    void prepareGrammarToRobot(){
        Log.d(TAG, "prepareGrammarToRobot ");
        //Create Grammar class object  創一個 文法物件
        //NOTICE : please only use "lower case letter" as naming of grammar name  例如：下面的 "example"都是小寫
        SimpleGrammarData mGrammarData = new SimpleGrammarData("command");
        //setup local command list to grammar class
        for (String string : cmdList) {
            mGrammarData.addSlot(string);
            Log.d(TAG, "add string : " + string);
        }
        //generate grammar data  產生文法資料
        mGrammarData.updateBody();
        //create and update Grammar to Robot
        Log.d(TAG, "createGrammar " + mGrammarData.body);   // .body =輸入的指令們
        //NOTICE : please only use "lower case letter" as naming of grammar name
        mRobotAPI.createGrammar(mGrammarData.grammar, mGrammarData.body); // Regist cmd   .gammar就 = new SimpleGrammarData("example"); 的 example
    }

    RobotEventListener robotEventListener = new RobotEventListener() {
        @Override
        public void onWikiServiceStart() {
            // Nuwa Robot SDK is ready now, you call call Nuwa SDK API now.
            Log.d(TAG, "onWikiServiceStart, robot ready to be control");
            //Step 3 : Start Control Robot after Service ready.
            //Register Voice Callback event
            mRobotAPI.registerVoiceEventListener(voiceEventListener);//listen callback of robot voice related event
            //Step 4 : prepare local command grammar  建構文法function
            prepareGrammarToRobot();//prepare local command grammar after service ready
            // request touch sensor event   感應器
            //NOTICE : PLEASE REQUEST ON SERVICE_START
            mRobotAPI.requestSensor(NuwaRobotAPI.SENSOR_TOUCH | NuwaRobotAPI.SENSOR_PIR | NuwaRobotAPI.SENSOR_DROP);
            //touch event will received by onTouchEvent
            //PIR event will received by onPIREvent    可以用紅外線
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
        public void onTouchEvent(int type, int touch) { //假如 如果感應器被短暫觸摸+執行完畢>> 全身綠色LED = 感應中
            if(isWaitingforConfirm && touch==1 ){
                mRobotAPI.setLedColor(1,255,0,255,0);
                mRobotAPI.setLedColor(2,255,0,255,0);
                mRobotAPI.setLedColor(3,255,0,255,0);
                mRobotAPI.setLedColor(4,255,0,255,0);
            }
        }

        @Override
        public void onPIREvent(int i) {

        }

        @Override
        public void onTap(int i) {

        }

        @Override
        public void onLongPress(int i) {  //如果感應器被長時間觸摸+執行完畢 >>顯示成功(全身紅色LED) +跳出對話框
            if(isWaitingforConfirm){
                mRobotAPI.setLedColor(1,255,255,0,0);
                mRobotAPI.setLedColor(2,255,255,0,0);
                mRobotAPI.setLedColor(3,255,255,0,0);
                mRobotAPI.setLedColor(4,255,255,0,0);

                mRobotAPI.startTTS("請問是否闖關成功??");
                //跳出確認視窗   教學：https://blog.xuite.net/viplab/blog/241319945   因為 Dialog的UI跟想要的不一樣，所以打算自己拉一個XML出來
                AlertDialog.Builder dialog=new AlertDialog.Builder(ChallengeActivity.this);
                dialog.setTitle("請問是否闖關成功??");
                dialog.setMessage("成功：前往下一關\n失敗：看看提示再想想看:D");
                dialog.setPositiveButton("成功~", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        isWaitingforConfirm=false;   //設回執行完畢前
                        mRobotAPI.enableSystemLED(); //交還系統LED控制權
                    }
                });
                dialog.setNeutralButton("提示", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //用startTTS說一段提示
                        isWaitingforConfirm=false;  //設回執行完畢前
                        mRobotAPI.enableSystemLED(); //交還系統LED控制權
                    }
                });
                dialog.setNegativeButton("失敗", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //可能需要提示按紐
                        isWaitingforConfirm=false; //設回執行完畢前
                        mRobotAPI.enableSystemLED(); //交還系統LED控制權
                    }
                });
                dialog.show();
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
        public void onDropSensorEvent(int i) {

        }

        @Override
        public void onMotorErrorEvent(int i, int i1) {

        }
    };
    VoiceEventListener voiceEventListener = new VoiceEventListener() {
        @Override
        public void onWakeup(boolean isError, String score, float direction) {

        }

        @Override
        public void onTTSComplete(boolean isError) { //TTS完成
            Log.d(TAG, "onTTSComplete:" + !isError);
        }

        @Override
        public void onSpeechRecognizeComplete(boolean isError, ResultType iFlyResult, String json) {

        }

        @Override
        public void onSpeech2TextComplete(boolean isError, String json) { //這邊沒用到
            Log.d(TAG, "onSpeech2TextComplete:" + !isError + ", json:" + json);
        }

        @Override
        public void onMixUnderstandComplete(boolean isError, ResultType resultType, String s) {    //開始接收指令的回傳處   這邊的 isError true=有問題，沒有找到對應的 ，false=沒出問題。
            //如果回傳 false就需要使用者在重新講一次指令
            Log.d(TAG, "onMixUnderstandComplete isError:" + !isError + ", json:" + s);   //所以才 !isError
            //Step 7 : Robot recognized the word of user speaking on  onMixUnderstandComplete
            //both startMixUnderstand and startLocalCommand will receive this callback       //startMixUnderstand and startLocalCommand都會 call到這個 function
            //設定一個結束的 flag
            boolean isEnd=false;
            if(!isError){ //如果有讀到相符的指令>>新增已使用指令
                //將結果從 Json 轉成字串。
                String result_string = VoiceResultJsonParser.parseVoiceResult(s);
                if(result_string.equals("END")||result_string.equals("結束")){
                    //只有這時才停止輸入 + 允許 執行Button 被點擊
                    isEnd=true;
                    mRobotAPI.startTTS("接收到結束指令，結束指令輸入");
                    mRobotAPI.stopListen();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //允許 執行 and 清除 Button 被點擊
                            btnStart.setText("輸入指令開始");
                            btnRun.setEnabled(true);
                            btnClear.setEnabled(true);
                        }
                    });
                }
                else if(result_string.equals("執行")){ //執行=結束+執行
                    isEnd=true;
                    mRobotAPI.startTTS("接到執行指令，即將執行");
                    mRobotAPI.stopListen();
                    try {  //延後 3 秒才讓開始執行 為了要等凱比說完剛剛的話
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Log.d(TAG,"RunCmd's sleep_cmdRun2 went wrong");
                        return;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(cmd_hasUse.size()>0 &&!cmd_hasUse.get(0).toString().equals("執行")){ //現在指令列第一個不是執行，才要插入到第一個
                                Radapter_hasUse.addItem(result_string,0);  //把執行指令插入到第一個去
                            }
                            //允許 執行 and 清除 Button 被點擊
                            btnStart.setText("輸入指令開始");
                            btnRun.setEnabled(true);
                            btnClear.setEnabled(true);
                            new Handler().postDelayed(new Runnable() {  //延後 3 秒，等UI更新完成後再執行
                                @Override
                                public void run() {
                                    //等 3000/1000秒後要做的事
                                    btnRun.performClick(); //到 btnRun的 onclick去
                                }
                            }, 2000);
                        }
                    });
                }
                else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Radapter_hasUse.addItem(result_string,-1);  //新增指令 因為這時已經在執行 commandListen這個任務，所以才需要用runOnUiThread來更新 RecyclerView
                        }
                    });
                    mRobotAPI.startTTS(result_string);
                }
            }
            else{ //沒有讀到正確的
                mRobotAPI.startTTS("無法判斷指令，請再試一次");
            }
            // 不管有沒有讀到，在出現關鍵字 END 或結束前 都會繼續聽指令
            if(!isEnd){ //沒出現結束結束關鍵字
                mRobotAPI.startLocalCommand();
            }
        }

        @Override
        public void onSpeechState(ListenType listenType, SpeechState speechState) {

        }

        @Override
        public void onSpeakState(SpeakType speakType, SpeakState speakState) {   //顯示現在 凱比說話的狀況
            Log.d(TAG, "onSpeakState:" + speakType + ", state:" + speakState);
        }

        @Override
        public void onGrammarState(boolean isError, String s) {//確認文法是否準備完成 ， 因為要等文法部分被設定好才能 START ，兩個同時setEnabled true
            //Step 5 : Aallow user press button to trigger startLocalCommand after grammar setup ready
            //startLocalCommand only allow calling after Grammar Ready
            if (!isError) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnStart.setEnabled(true);
                        btnClear.setEnabled(true);
                    }
                });
            } else {
                Log.d(TAG, "onGrammarState error, " + s);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Allow user click button.
                        btnStart.setEnabled(true);
                        btnClear.setEnabled(true);
                    }
                });
            }
        }

        @Override
        public void onListenVolumeChanged(ListenType listenType, int i) {

        }

        @Override
        public void onHotwordChange(HotwordState hotwordState, HotwordType hotwordType, String s) {

        }
    };
}