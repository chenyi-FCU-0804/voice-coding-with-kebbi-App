package com.nuwarobotics.example.SimpleDemo;

import android.app.Dialog;
import android.os.Handler;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Looper;
import android.os.Message;
import android.util.Log;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.nuwarobotics.example.SimpleDemo.selfMadeObject.AdvanceCMDAdapter_layer2;
import com.nuwarobotics.example.SimpleDemo.selfMadeObject.AdvanceCMDAdapter_layer3;
import com.nuwarobotics.example.SimpleDemo.selfMadeObject.ChallengeData;
import com.nuwarobotics.example.SimpleDemo.selfMadeObject.RecyclerViewAdapter;

import com.nuwarobotics.example.R;
import com.nuwarobotics.example.SimpleDemo.selfMadeObject.RecyclerViewAdapter_btnRemove;
import com.nuwarobotics.example.SimpleDemo.selfMadeObject.UserData;
import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.RobotEventListener;
import com.nuwarobotics.service.agent.SimpleGrammarData;
import com.nuwarobotics.service.agent.VoiceEventListener;
import com.nuwarobotics.service.agent.VoiceResultJsonParser;

import java.util.ArrayList;
/*
* 用途:闖關的Activity                !!!!!!!!!雖然1格30公分，但凱比會走得不準    現在的BUG：連續輸入自訂指令會crash?????????????????????????????????????? 前進前進(前進、前進)
*
* 對應到： R.layout.activity_challenge
* 決定用localandCloud的方式實作語音輸入部分 (因為local的文法更新太麻煩)
* 目前完成：中間兩個 RecyclerView的實作 ，在尚未添加指令前，右邊的 RecyclerView會是空的 + localandCloud + 實作語音輸入功能 +醒 + 過關時長 用mRobotAPI.startTTS實作輸入回饋 or 提摸凱比感應器判斷  + 語音輸入執行後 和 闖關UI更新控制  + Activity在 onCreate destroy pause resume的處理。
*           可用指令 RecyclerView在過關時的更新 + 使用者等級(userLV)連資料庫的更新。
*           較高級指令(迴圈)的實作：
*               在新增Adapter時的while(gethasConstructedAdvanceAdapter)要用new thread來跑，不然不會等他建立好。之後再用message +  UIupdateHandler來新增自訂指令的內部指令，case 6+7+8。
 *              四個基本指令的gethasConstructedAdvanceAdapter的寫法，雖然能動，但不知道是否真的有卡住等到Adapter建立完成???????????但現在先不管。
* 部分完成：button_cmdRun 實作 (前進、後退、左轉、右轉、迴圈、結束、執行。 只有實作實作這幾個+自訂指令)  + 闖過所有關的 motion + tt ，641行 。
*
* ChallengeActivity也是只有實作onCerate跟onDestroy部分 + 返回鍵的控制(其他按鈕不能點擊時，返回鍵同樣不能點)。 因為是最上層Activity被返回必定 = onDestroy，要同步的資料只有使用者等級，會在確保完成更新後才Destroy。
* */
public class ChallengeActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = this.getClass().getSimpleName();  //==ChallengeActivity
    NuwaRobotAPI mRobotAPI;
    IClientId mClientId;
    UserData userData;
    private int userLV;
    private ChallengeData challengeData=new ChallengeData();   //闖關資料class
    private TextView textViewStageName;   //顯示第幾關
    private TextView textViewStageQuest; //顯示關卡任務

    private RecyclerViewAdapter Radapter_canUse;    //顯示可使用指令的Adapter
    private RecyclerView recyclerView_canUse;
    //cmd_canUse的資料會被Adapter的addItem removeItem給影響，因為當時創建Adapter時直接將cmd_canUse給丟進去建構子
        //在操作時都是以Radapter_canUse內資料為主
    private ArrayList<String> cmd_canUse=new ArrayList<>();   //可用指令 + 在創建local文法時要丟去產生grammar
    private ArrayList<String> new_cmd;  //過關拿到的新指令

    private RecyclerViewAdapter_btnRemove Radapter_hasUse;  //Adapter型態不一樣，顯示已經使用的指令，多出一個刪除鍵
    private RecyclerView recyclerView_hasUse;
    //下面兩個只是為了創建Adapter時會用到的空參數，之後的操作都會以Radapter_hasUse內的資料為主，而不是從這兩個
        //不過Adapter內資料的更改也會影響到這兩個ArrayList內的資料ㄡ。
    private ArrayList<String> cmd_hasUse=new ArrayList<>();
    private ArrayList<Integer> cmd_hasUse_type=new ArrayList<>();
    //新增指令時會用到的變數
    private  int commandLayer=1;   //來確認現在是 1最外層~3最內層指令
    private AdvanceCMDAdapter_layer2 tempAdvanceCmdAdapter_layer2 = null; //第二層Adapter，因為call起來麻煩，直接宣告一個變數
    private AdvanceCMDAdapter_layer3 tempAdvanceCmdAdapter_layer3=null;  //第三層Adapter
    //private =null;第三層Adapter，因為call起來麻煩，直接宣告一個變數
    private ArrayList<String> targetCMD;   //用來暫存自訂指令的arraylist

    private Button btnStart;   //開始輸入語音
    private boolean isEndCmd;  //確認是否收到 結束or執行指令
    private Button btnClear;   //清空待執行指令RecyclerView
    private boolean canClean;  //是否能clean指令的 flag
    private Button btnRun;      //開始執行
    private boolean InterruptCmdRun=false;
    private boolean isWaitingforConfirm;   //確認是否在確認答案的環節
    private Dialog confirmDialog=null;   //過關的Dialog
    private boolean dialogIsShowing=false; //確認Dialog有沒有開 防止

    private Handler motionHandler;  //執行 robot action用的
    private Handler UIupdateHandler; // 控制UI更新用
    private boolean mTts_complete = true;  //確認TTS完成
    private boolean mMotion_complete = true; //確認Motion完成

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);
        //所有的data都會在LoginActivity就拿好，之後的操作都在UserData上，要關閉App時才進行跟Firebase的更新
        userData=(UserData)getApplicationContext();
        //拿使用者等級，用來設定關卡資訊
        userLV=Integer.parseInt(userData.getUserData().get(userData.getMapKey("challengeLV")).toString());
        // 初始化
        initUIcomponent();
        //Step 1 : Initial Nuwa API Object
        mClientId = new IClientId(this.getPackageName());
        mRobotAPI = new NuwaRobotAPI(this, mClientId);
        //Step 2 : Register receive Robot Event
        Log.d(TAG, "register EventListener ");
        mRobotAPI.registerRobotEventListener(robotEventListener);//listen callback of robot service event

        //UI handler初始化
        UIupdateHandler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                switch (msg.what){
                    case 1: //開始接收語音指令講完後enable btnStart
                        btnStart.setEnabled(true);
                        break;
                    case 2:
                        // btnClear的TTS完成後 + btnStart結束接收語音指令講完後 +button_cmdRun沒指令時TTS結束
                        // + 語音結束指令輸入後
                        // 才給所有的button可以點擊
                        btnStart.setEnabled(true);
                        btnClear.setEnabled(true);
                        btnRun.setEnabled(true);
                        break;
                    case 3:  //button_cmdRun 按下執行+ 開始執行指令TTS結束後才讓 btnRun可以點擊，此時的功能是中斷執行
                        btnRun.setEnabled(true);
                        break;
                    case 4:  //指令執行完畢，等待過關檢驗 TTS完成後將 button中斷指令給unEnable
                        btnRun.setEnabled(false);
                        break;
                    case 5:  //btnRun中斷指令執行TTS結束後，禁止所有recyclerView上的按鈕的點擊
                        Radapter_hasUse.setBtnRemoveEnable(true);
                        btnStart.setEnabled(true);
                        btnClear.setEnabled(true);
                        btnRun.setEnabled(true);
                        break;
                    case 6:   //執行自訂指令在第一層的新增
                        if(!targetCMD.isEmpty()){
                            for(int i=1;i<targetCMD.size();i++){ //從1開始，因為 0 =自訂指令名已經新增過了
                                //自訂指令內部只有低階指令=1。
                                tempAdvanceCmdAdapter_layer2.addItem(targetCMD.get(i),tempAdvanceCmdAdapter_layer2.getItemCount(),1);
                                recyclerView_hasUse.scrollBy(0,100); //因為沒辦法直接抓position就只能向下y(y要是>極限長度，會滑到極限長度)。
                                Log.i("tartar"+i,targetCMD.get(i));
                            }
                        }
                        break;
                    case 7:   //執行自訂指令在第二層的新增
                        if(!targetCMD.isEmpty()){
                            for(int i=1;i<targetCMD.size();i++){ //從1開始，因為 0 =自訂指令名已經新增過了
                                //自訂指令內部只有低階指令=1。
                                tempAdvanceCmdAdapter_layer3.addItem(targetCMD.get(i),tempAdvanceCmdAdapter_layer3.getItemCount(),1);
                                recyclerView_hasUse.scrollBy(0,100); //因為沒辦法直接抓position就只能向下y(y要是>極限長度，會滑到極限長度)。
                                Log.i("tartar"+i,targetCMD.get(i));
                            }
                        }
                        break;
                    case 8:   //讀到迴圈指令後，show出設定迴圈次數的dialog
                        Log.i("Adapter建立完成，層數："+commandLayer,"即將show出設定迴圈次數dialog");
                        buildAndShowLoopTimesDialog(commandLayer);
                        break;
                    default:
                        break;
                }
            }
        };
        // Motion的Handler初始化
        motionHandler=new Handler(Looper.getMainLooper());
        //初始化 過關確認的Diaglog
    }

    private void initUIcomponent() {
        Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar_challenge);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);  //設定返回鍵
        actionBar.setTitle("闖關模式。破關後學習更多新指令吧!!");
        //設定關卡名稱 and 任務 +拿可以用的指令
        textViewStageName=(TextView)findViewById(R.id.textView_stageIndex);
        textViewStageName.setText(challengeData.getStageName(userLV));
        textViewStageQuest=(TextView)findViewById(R.id.textView_mission2);
        textViewStageQuest.setText(challengeData.getStageQuest(userLV));

        cmd_canUse=challengeData.getCmdCanUse(userLV,null);  //從challengeData取出能用的指令
        challengeData.setUserCMDcount(userData.getUserCommand().size());
        for(int i=0;i<userData.getUserCommand().size();i++){    //從userData抓出自訂指令，放到執行與結束後。
            cmd_canUse.add(userData.getUserCommand().get(i).get(0));
        }
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
        Radapter_hasUse = new RecyclerViewAdapter_btnRemove(this, cmd_hasUse,cmd_hasUse_type,true);
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
                //如果按鈕可以點擊，因為所有按鈕都是同時被disable enable，懶得新增變數button來記，就用別的button的狀態來判斷了
                if(Radapter_hasUse.getAcvanceCmdData_BtnRemoveCanClick()){ //如果是可以點擊的狀況，才允許離開challenage.Activity，不然可能會影響到正在進行的功能。
                    mRobotAPI.turn(0);
                    mRobotAPI.move(0);
                    mRobotAPI.stopInAccelerationEx();
                    mRobotAPI.stopTurnEx();
                    mRobotAPI.enableSystemLED();
                    mRobotAPI.stopListen();
                    mRobotAPI.stopTTS();
                    mRobotAPI.motionStop(true);
                    mRobotAPI.hideWindow(true);
                    finish();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {  //通常都拿來把onCreate()時的資料做釋放的動作
        super.onDestroy();
        // release Nuwa Robot SDK resource
        mRobotAPI.release();
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.button_startInputCMD:
                String btnStartText=btnStart.getText().toString();
                TextView textStart=findViewById(R.id.textView_cmdInput);

                if(btnStartText.equals("輸入指令開始")){  //目前功能是開始接收
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnStart.setText("結束指令輸入");  //將按鈕切換成結束語音指令輸入功能
                            textStart.setText("想暫停輸入的話可以按");
                            btnStart.setEnabled(false);   //要是在開始 輸入後馬上點擊結束會有Bug 所以延後幾秒才讓 btnStart可以點擊
                            btnClear.setEnabled(false);
                            btnRun.setEnabled(false);
                            Radapter_hasUse.setBtnRemoveEnable(false);
                        }
                    });
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            commandLayer=1;
                            mRobotAPI.startTTS("開始接收語音指令");
                            mTts_complete=false;
                            mRobotAPI.startMixUnderstand();//Start listen without wakeup, callback on onMixUnderstandComplete
                            while(!mTts_complete){}

                            Message msg = new Message();
                            msg.what =1;
                            UIupdateHandler.sendMessage(msg);
                        }
                    }).start();
                }
                else{   //目前功能是結束接收指令
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnStart.setText("輸入指令開始");
                            textStart.setText("點\"輸入指令\"後才會開始接收語音指令");
                            btnStart.setEnabled(false);   //同樣 在結束指令後馬上點擊結束會有問題  將btnStart unenable
                            Radapter_hasUse.setBtnRemoveEnable(true);
                        }
                    });
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mRobotAPI.startTTS("結束語音指令輸入");
                            mRobotAPI.stopListen();
                            mTts_complete=false;
                            while(!mTts_complete){}

                            Message msg = new Message();
                            msg.what =2;
                            UIupdateHandler.sendMessage(msg);
                        }
                    }).start();
                }
                break;
            case R.id.button_cmdClear:
                canClean=false;
                if(Radapter_hasUse.getItemCount()>0){
                    canClean=true;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() { //為防止多次觸發button 按下去後要TTS完成才能再按
                        btnStart.setEnabled(false);
                        btnClear.setEnabled(false);
                        btnRun.setEnabled(false);
                        if(canClean){  //清除hasUse指令列
                            Radapter_hasUse.removeAllItem(0,Radapter_hasUse.getItemCount());
                        }
                    }
                });
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(!canClean){
                            mRobotAPI.startTTS("已經沒有指令了喔，請新增一些吧");
                            mTts_complete=false;
                        }
                        else{
                            mRobotAPI.startTTS("清空了待執行指令區");
                            mTts_complete=false;
                        }
                        while(!mTts_complete){}

                        Message msg = new Message();
                        msg.what =2;
                        UIupdateHandler.sendMessage(msg);
                    }
                }).start();
                break;
            case R.id.button_cmdRun:
                //把cmd_hasUse的指令(在Radapter_haseUse內)一個一個抓出來，丟到funtion內實施動作
                String btnRunText=btnRun.getText().toString();
                TextView textRun=findViewById(R.id.textView_cmdRun);
                if(Radapter_hasUse.getItemCount()==0){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnStart.setEnabled(false);   //同樣 在結束指令後馬上點擊結束會有問題 所以延後幾秒才讓 btnRun可以點擊
                            btnClear.setEnabled(false);
                            btnRun.setEnabled(false);
                        }
                    });
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mRobotAPI.startTTS("沒有指令的話沒辦法執行喔");
                            mTts_complete=false;
                            while(!mTts_complete){}  //等到TTS執行完才讓所有的button可以點，不然連點會出 Bug

                            Message msg = new Message();
                            msg.what =2;
                            UIupdateHandler.sendMessage(msg);
                        }
                    }).start();
                }
                else{
                    if(btnRunText.equals("執行!!!")){   //執行處理
                        runOnUiThread(new Runnable() {  //更新 btnRun，Text設成 暫停執行 + 所有按鈕不能按
                            @Override
                            public void run() {
                                //把RecyclerView的刪除按鈕 disable，直接讓 btnRemove的onclick不觸發。
                                Radapter_hasUse.setBtnRemoveEnable(false);
                                btnRun.setText("中斷執行");
                                textRun.setText("只能在指令執行完畢前中斷喔");
                                btnStart.setEnabled(false);
                                btnClear.setEnabled(false);
                                btnRun.setEnabled(false);
                            }
                        });
                        //講完 開始執行指令再將btnRun 設為Enable
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                mRobotAPI.startTTS("開始執行指令");
                                mTts_complete=false;
                                while(!mTts_complete){}  //等到TTS執行完才執行

                                Message msg = new Message();
                                msg.what =3;   //先將btnRun設為enable
                                UIupdateHandler.sendMessage(msg);

                                InterruptCmdRun=false; //將中斷執行的flag設為 false
                                runCmd(Radapter_hasUse.getAdvanceCmdData_mData());  //執行指令
                                if(!InterruptCmdRun) { //如果沒被中斷的跑完指令 >>就什麼都不執行
                                    msg = new Message();   //注意!! message不能重複使用會crash
                                    msg.what =4;
                                    UIupdateHandler.sendMessage(msg);

                                    mRobotAPI.startTTS("指令執行完畢，等待過關檢驗");    //// 說出這句話時 unEnable btnRun，因為不能暫停了
                                    mTts_complete=false;
                                    while(!mTts_complete){}

                                    mRobotAPI.startTTS("請長摸我其中一個感應器直到我發出紅光");
                                    mTts_complete=false;
                                    while(!mTts_complete){}  //等到TTS執行完才讓所有的button可以點，不然連點會出 Bug

                                    isWaitingforConfirm=true;
                                    mRobotAPI.disableSystemLED();   //先拿到系統LED的控制權 ，記得還回去  + 凱比的LED全設成黑的
                                    mRobotAPI.setLedColor(1,255,0,0,0);
                                    mRobotAPI.setLedColor(2,255,0,0,0);
                                    mRobotAPI.setLedColor(3,255,0,0,0);
                                    mRobotAPI.setLedColor(4,255,0,0,0);
                                    //關卡更新跟確認對話框 都在 onLongPress
                                }
                            }
                        }).start();
                    }
                    else{ //中斷指令處理
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnRun.setText("執行!!!");
                                textRun.setText("按執行看結果");
                                btnRun.setEnabled(false);
                            }
                        });
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                InterruptCmdRun=true;
                                mRobotAPI.turn(0);  //暫停凱比動作
                                mRobotAPI.move(0);
                                mRobotAPI.stopInAccelerationEx();
                                mRobotAPI.stopTurnEx();
                                mRobotAPI.startTTS("中斷指令執行");
                                mTts_complete=false;
                                mRobotAPI.enableSystemLED();   //釋放LED權限
                                while(!mTts_complete){}  //等到TTS執行完才讓所有的button可以點，不然連點會出 Bug

                                Message msg = new Message();
                                msg.what =5;  //重新讓所有button + cmdHasUse的recyclerView的刪除butto Enable
                                UIupdateHandler.sendMessage(msg);
                            }
                        }).start();
                    }
                }
                break;
            default:  //沒有對應的 ID的時候
                Log.d(TAG,"Onclick view cant found");
        }
    }

    private void runCmd(ArrayList<String> cmd_hasUse) { //因為在執行指令時，只有這段code在運作，所以不用特別用new thread?
        //先拿出第二層的Adapter，等等才能看有沒有指令要執行
        ArrayList<AdvanceCMDAdapter_layer2> layer2Command=Radapter_hasUse.getAdvanceCMDAdapters_layer2();
        //從第一層指令開始
        for(int l1=0;l1<cmd_hasUse.size();l1++) {
            if (InterruptCmdRun) {   //如果按下了停止執行指令
                return;
            }
            //先執行第一層指令
            //Log.e("第一層指令= ", cmd_hasUse.get(l1));   //test
            executeCMDaction(cmd_hasUse.get(l1));
            //再去確認剛剛執行的是否為高級指令，抓出內部(第二層)指令
            if (layer2Command.get(l1) != null) { //非null =第一層是高級指令
                int layer2Iteration = 1; //第二層重複次數
                //如果是迴圈指令才會更改重複次數，自訂指令除外layer2Iteration=1
                layer2Iteration = layer2Command.get(l1).getAdvanceCmdData().getLoopTimes();  //取得重複次數，之前在宣告迴圈時的Dialog設定過
                //Log.i("第二層執行次數"+layer2Iteration,"layer2 position="+l1);
                //拿出第二層Adapter內裝的第三層的Adapter的list，layer3Command。
                ArrayList<AdvanceCMDAdapter_layer3> layer3Command = layer2Command.get(l1).getAdvanceCMDAdapters_layer3();
                //執行第二層指令
                for (int l2Times = 0; l2Times < layer2Iteration; l2Times++) {
                    //每個指令執行layer2Iteration次
                    for (int l2 = 0; l2 < layer2Command.get(l1).getItemCount(); l2++) {
                        if (InterruptCmdRun) {   //如果按下了停止執行指令
                            return;
                        }
                        //Log.e("第二層指令= ", layer2Command.get(l1).getAdvanceCmdData_mData().get(l2));   //test
                        executeCMDaction(layer2Command.get(l1).getAdvanceCmdData_mData().get(l2));
                        if (layer3Command.get(l2) != null) {
                            //如果第二層的指令又是高級指令 = 有第三層指令 =第三層Adapter存在
                            int layer3Iteration = 1; //第二層重複次數
                            //如果是迴圈指令才會更改重複次數，自訂指令除外layer2Iteration=1
                            layer3Iteration = layer3Command.get(l2).getAdvanceCmdData().getLoopTimes();  //取得重複次數，之前在宣告迴圈時的Dialog設定過
                            //Log.i("第三層執行次數"+layer3Iteration,"layer3 position= "+l2);
                            //執行第三層指令 layer3Iteration次
                            for(int l3Times=0;l3Times<layer3Iteration;l3Times++){
                                for (int l3 = 0; l3 < layer3Command.get(l2).getItemCount(); l3++) {
                                    if (InterruptCmdRun) {   //如果按下了停止執行指令
                                        return;
                                    }
                                    //Log.e("第三層指令= ", layer3Command.get(l2).getAdvanceCmdData_mData().get(l3));   //test
                                    executeCMDaction(layer3Command.get(l2).getAdvanceCmdData_mData().get(l3));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    //因為會被runCMD call很多次，所以另立function
    private void executeCMDaction(String cmd) {
        switch (cmd){
            case "前進":  //延遲1.5秒結束 =走30公分
                mRobotAPI.move(0.2f);
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Log.d(TAG,"RunCmd's sleep_foward went wrong");
                    return ;
                }
                break;
            case "後退": //延遲1.5秒結束 =走30公分
                mRobotAPI.move(-0.2f);
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Log.d(TAG,"RunCmd's sleep_backward went wrong");
                    return ;
                }
                break;
            case "左轉":  //延遲三秒結束  =轉90度
                mRobotAPI.turn(30);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Log.d(TAG,"RunCmd's sleep_leftTurn went wrong");
                    return ;
                }
                break;
            case "右轉": //延遲三秒結束 =轉90度
                mRobotAPI.turn(-30);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Log.d(TAG,"RunCmd's sleep_rightTurn went wrong");
                    return ;
                }
                break;
            case"執行":
                Log.d(TAG,"execute start :D");
                break;
            default:
                break;
        }
        //一個指令完後結束機器人所有動作+等待0.2秒再執行下一個
        mRobotAPI.turn(0);
        mRobotAPI.move(0);
        mRobotAPI.stopInAccelerationEx();
        mRobotAPI.stopTurnEx();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Log.d(TAG,"RunCmd's wait for 0.5sec went wrong");
            return ;
        }
    }

    void prepareGrammarToRobot(){
        Log.d(TAG, "prepareGrammarToRobot ");
        //Create Grammar class object  創一個 文法物件
        //NOTICE : please only use "lower case letter" as naming of grammar name  例如：下面的 "example"都是小寫
        SimpleGrammarData mGrammarData = new SimpleGrammarData("command");
        //setup local command list to grammar class
        for (String string : Radapter_canUse.getmData()) {
            if(string.contains("(")){
                String temp=string.substring(0,string.indexOf("(")-1);
                mGrammarData.addSlot(temp);
            }
            else{
                mGrammarData.addSlot(string);
            }
            Log.d(TAG, "add string : " + string);
        }
        //generate grammar data  產生文法資料
        mGrammarData.updateBody();
        //create and update Grammar to Robot
        Log.d(TAG, "createGrammar " + mGrammarData.body);   // .body =輸入的指令們
        //NOTICE : please only use "lower case letter" as naming of grammar name
        mRobotAPI.createGrammar(mGrammarData.grammar, mGrammarData.body); // Regist cmd，.gammar就 = new SimpleGrammarData("example"); 的 example
    }

    // 成功的 rannable
    Runnable robotClearAction = new Runnable() {
        @Override
        public void run() {
            ArrayList<String> temp=new ArrayList<>(challengeData.randomClearMotionTTS());
            String current_motion = temp.get(0);     //從兩個ArrayList拿到 index 0位置的 motion string
            String current_tts = temp.get(1); //TTS string
            //如果兩個都有取到值 >> 將兩個 flag設為 false
            if(!current_tts.isEmpty() ) mTts_complete = false;
            if(!current_motion.isEmpty()) mMotion_complete = false;

            //Start play tts and motion if need  執行TTS and Motion
            if(!current_tts.isEmpty()) mRobotAPI.startTTS(current_tts);
            //Please NOTICE that auto_fadein should assign false when motion file nothing to display
            if(!current_motion.isEmpty())mRobotAPI.motionPlay(current_motion,true);

            while(! mTts_complete || ! mMotion_complete ){
                //wait both action complete 等到兩個都完成
            }
            //both TTS and Motion complete, we end action
            mRobotAPI.motionReset();//Reset Robot pose to default
            mRobotAPI.hideWindow(true); //交還螢幕
        }
    };
    // 失敗的 rannable
    Runnable robotFailAction = new Runnable() {
        @Override
        public void run() {
            ArrayList<String> temp=new ArrayList<>(challengeData.randomFailMotionTTS());
            String current_motion = temp.get(0);     //從兩個ArrayList拿到 index 0位置的 motion string
            String current_tts = temp.get(1); //TTS string
            //如果兩個都有取到值 >> 將兩個 flag設為 false
            if(!current_tts.isEmpty()) mTts_complete = false;
            if(!current_motion.isEmpty()) mMotion_complete = false;

            //Start play tts and motion if need  執行TTS and Motion
            if(!current_tts.isEmpty()) mRobotAPI.startTTS(current_tts);
            //Please NOTICE that auto_fadein should assign false when motion file nothing to display
            if(!current_motion.isEmpty())mRobotAPI.motionPlay(current_motion,true);

            while(!mTts_complete || !mMotion_complete ){
                //wait both action complete 等到兩個都完成
            }
            //both TTS and Motion complete, we end action
            mRobotAPI.motionReset();//Reset Robot pose to default
            mRobotAPI.hideWindow(true); //交還螢幕
        }
    };
    // Hint的 rannable
    Runnable robotHintAction = new Runnable() {
        @Override
        public void run() {
            String current_motion=new String(challengeData.randomHintMotion());
            String current_tts = challengeData.getStageHint(userLV); //TTS string
            //如果兩個都有取到值 >> 將兩個 flag設為 false
            if(!current_tts.isEmpty()) mTts_complete = false;
            if(!current_motion.isEmpty()) mMotion_complete = false;

            //Start play tts and motion if need  執行TTS and Motion
            if(!current_tts.isEmpty()) mRobotAPI.startTTS(current_tts);
            //Please NOTICE that auto_fadein should assign false when motion file nothing to display
            if(!current_motion.isEmpty())mRobotAPI.motionPlay(current_motion,true);

            while(!mTts_complete ||!mMotion_complete){
                //wait both action complete 等到兩個都完成
            }
            //both TTS and Motion complete, we end action
            mRobotAPI.motionReset();//Reset Robot pose to default
            mRobotAPI.hideWindow(true); //交還螢幕
        }
    };
    public void buildAndShowConfirmDialog(){
        //跳出確認視窗   教學：https://www.youtube.com/watch?v=4Geo2Uz4hOo&ab_channel=NguyenDucHoang  因為 Dialog的UI跟想要的不一樣，所以打算自己拉一個XML出來
        //記得，如果是要多次開關的視窗要使用dialog.cancel() ，只開一次的dialog就用dismiss()
        if(confirmDialog==null){
            confirmDialog=new Dialog(ChallengeActivity.this);
            confirmDialog.setContentView(R.layout.dialog_challenge_clear_confirm);
            confirmDialog.setCancelable(false);

            Button btnYes= confirmDialog.findViewById(R.id.button_clear);
            Button btnNo= confirmDialog.findViewById(R.id.button_fail);
            Button btnHint= confirmDialog.findViewById(R.id.button_challengeHint);
            btnYes.setEnabled(true);
            btnNo.setEnabled(true);
            btnHint.setEnabled(true);
            btnYes.setOnClickListener(new View.OnClickListener() { //新增 userLV 更新關卡
                @Override
                public void onClick(View view) {
                    //更新關卡
                    userLV+=1;
                    userData.updateChallengeLV(userLV);
                    if(userLV>=challengeData.getStageAmount()){  //通過所有關卡  尚未完成 ////////////////////////////////////////////////////////////////////////////////////// motion and tts
                        mRobotAPI.startTTS("恭喜通關");
                    }
                    else{  //更新到下個關卡
                        motionHandler.post(robotClearAction); //成功凱比Action
                        new_cmd=challengeData.getCmdCanUse(userLV,Radapter_canUse.getmData());  //去拿到新的指令
                        TextView textRun=findViewById(R.id.textView_cmdRun);
                        runOnUiThread(new Runnable() {//更新UI
                            @Override
                            public void run() {
                                for(int i=0;i<new_cmd.size();i++){
                                    Radapter_canUse.addItem(new_cmd.get(i),Radapter_canUse.getItemCount()-(2+challengeData.getUserCMDcount())); //因為新的指令是插入在結束、執行 + 自訂指令前，所以要-(2 + 自訂指令的個數)
                                }
                                Radapter_hasUse.removeAllItem(0,Radapter_hasUse.getItemCount());
                                Radapter_hasUse.setBtnRemoveEnable(true);  //重新讓button可以點擊

                                textViewStageName.setText(challengeData.getStageName(userLV));
                                textViewStageQuest.setText(challengeData.getStageQuest(userLV));
                                btnRun.setText("執行!!!");
                                textRun.setText("按執行看結果");
                                btnStart.setEnabled(true);
                                btnClear.setEnabled(true);
                                btnRun.setEnabled(true);
                            }
                        });
                        //關掉dialog
                        isWaitingforConfirm=false;   //設回執行完畢前
                        mRobotAPI.enableSystemLED(); //交還系統LED控制權
                        dialogIsShowing=false;
                        confirmDialog.cancel();
                    }
                }
            });
            btnNo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TextView textRun=findViewById(R.id.textView_cmdRun);
                    motionHandler.post(robotFailAction); //失敗凱比Action
                    runOnUiThread(new Runnable() {//更新UI
                        @Override
                        public void run() { // show出dialog
                            btnRun.setText("執行!!!");
                            textRun.setText("按執行看結果");
                            btnStart.setEnabled(true);
                            btnClear.setEnabled(true);
                            btnRun.setEnabled(true);
                            //重新讓button可以點擊
                            Radapter_hasUse.setBtnRemoveEnable(true);
                        }
                    });
                    //關掉dialog
                    isWaitingforConfirm=false;   //設回執行完畢前
                    mRobotAPI.enableSystemLED(); //交還系統LED控制權
                    dialogIsShowing=false;
                    confirmDialog.cancel();
                }
            });
            btnHint.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    motionHandler.post(robotHintAction); //提示凱比Action
                }
            });
        }
    }

    private void buildAndShowLoopTimesDialog(int commandLayer) { //顯示出迴圈次數的Dialog
        Dialog loopTimesDialog=new Dialog(ChallengeActivity.this);
        loopTimesDialog.setContentView(R.layout.dialog_challenge_set_loop_times);
        loopTimesDialog.setCancelable(false);

        EditText timesInput=loopTimesDialog.findViewById(R.id.editTextNumber_dialog_challenge_loopTimes_input);
        timesInput.requestFocus(); //自動點擊editText
        Button btnYes=loopTimesDialog.findViewById(R.id.button_dialog_challenge_loopTimes_yes);
        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String number=timesInput.getText().toString();
                if(number.isEmpty()){
                    timesInput.requestFocus();
                    timesInput.setError("一定要打一個數字喔");
                    return;
                }
                //去拿到剛新增的Adapter >>往Adapter內部的AdvanceCmdData設定迴圈次數
                if(commandLayer==2){   //設定Radapter_hasUse內的Adapter的資料
                    tempAdvanceCmdAdapter_layer2=Radapter_hasUse.getAdvanceCMDAdapters_layer2().get(Radapter_hasUse.getAdvanceCMDAdapters_layer2().size()-1);
                    //Log.i("old position2= ", String.valueOf((Radapter_hasUse.getAdvanceCMDAdapters_layer2().size()-1))); test
                    tempAdvanceCmdAdapter_layer2.getAdvanceCmdData().setLoopTimes(Integer.valueOf(number));
                    //Log.i("times2 = "+tempAdvanceCmdAdapter_layer2.getAdvanceCmdData().getLoopTimes(),"是第幾個? "+(Radapter_hasUse.getAdvanceCMDAdapters_layer2().size()-1));  test
                }
                else if(commandLayer==3){ //設定AdavanceCMDAdapter內的adapter的資料(迴圈內的迴圈)
                    tempAdvanceCmdAdapter_layer2=Radapter_hasUse.getAdvanceCMDAdapters_layer2().get(Radapter_hasUse.getAdvanceCMDAdapters_layer2().size()-1);
                    tempAdvanceCmdAdapter_layer3=tempAdvanceCmdAdapter_layer2.getAdvanceCMDAdapters_layer3().get(tempAdvanceCmdAdapter_layer2.getAdvanceCMDAdapters_layer3().size()-1);
                    //Log.i("old position3= ",(Radapter_hasUse.getAdvanceCMDAdapters_layer2().size()-1)+" "+(tempAdvanceCmdAdapter_layer2.getAdvanceCMDAdapters_layer3().size()-1));  test
                    tempAdvanceCmdAdapter_layer3.getAdvanceCmdData().setLoopTimes(Integer.valueOf(number));
                    //Log.i("times3 = "+tempAdvanceCmdAdapter_layer3.getAdvanceCmdData().getLoopTimes(),"是第幾個?"+(Radapter_hasUse.getAdvanceCMDAdapters_layer2().size()-1)+(tempAdvanceCmdAdapter_layer2.getAdvanceCMDAdapters_layer3().size()-1)); tset
                }
                dialogIsShowing=false;
                mRobotAPI.startTTS("開始新增迴圈內部指令");
                mRobotAPI.startMixUnderstand();  //開始接收內部語音指令
                loopTimesDialog.cancel();
            }
        });
        loopTimesDialog.show();
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
            Log.d(TAG,"Play Motion Complete " + s);
            mMotion_complete = true;
        }

        @Override
        public void onPlayBackOfMotionPlay(String s) {

        }

        @Override
        public void onErrorOfMotionPlay(int i) {
            if(mRobotAPI != null){
                mRobotAPI.hideWindow(true);
            }
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
            if(isWaitingforConfirm && touch==1 && !dialogIsShowing){
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
        public void onLongPress(int i) {  //如果感應器被長時間觸摸+執行完畢 >>顯示成功 >>先設定全身紅色LED +初始化對話框 >>再show出對話框 + 等TTS結束enable所有的按鈕
            if(isWaitingforConfirm && !dialogIsShowing){ //在等待確認+沒有顯示dialog時
                mRobotAPI.setLedColor(1,255,255,0,0);
                mRobotAPI.setLedColor(2,255,255,0,0);
                mRobotAPI.setLedColor(3,255,255,0,0);
                mRobotAPI.setLedColor(4,255,255,0,0);

                mRobotAPI.startTTS("請問是否闖關成功??");
                mTts_complete=false;

                buildAndShowConfirmDialog();
                while(!mTts_complete){}  //等到 請問是否闖關成功??TTS完成後再顯示
                confirmDialog.show();
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
            mTts_complete = true;
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
            Log.d(TAG, "onMixUnderstandComplete isError:" + !isError + ", json:" + s);   //所以才 !isError
            //Step 7 : Robot recognized the word of user speaking on  onMixUnderstandComplete
            //both startMixUnderstand and startLocalCommand will receive this callback       //startMixUnderstand and startLocalCommand都會 call到這個 function
            //設定一個結束的 flag
            isEndCmd=false;
            mRobotAPI.stopListen();  //先停止語音輸入

            if(!isError){ //如果沒出error代表有接收到結果，但不一定是有的cmd_canUse。
                //將結果從 Json 轉成字串。
                String result_string = VoiceResultJsonParser.parseVoiceResult(s);
                Log.e("目前層數="+ commandLayer,"指令："+result_string);
                //test
                ArrayList<String> temp=Radapter_hasUse.getAdvanceCmdData_mData();
                for(int i=0;i<temp.size();i++){
                    Log.e("指令"+i,temp.get(i));
                    if(Radapter_hasUse.getAdvanceCMDAdapters_layer2().get(i)!=null){
                        ArrayList<String> temp2=Radapter_hasUse.getAdvanceCMDAdapters_layer2().get(i).getAdvanceCmdData_mData();
                        ArrayList<AdvanceCMDAdapter_layer3> adapter3=Radapter_hasUse.getAdvanceCMDAdapters_layer2().get(i).getAdvanceCMDAdapters_layer3();
                        for(int j=0;j<temp2.size();j++){
                            Log.e("指令"+i+j,temp2.get(j));
                            if(adapter3.get(j)!=null){
                                ArrayList<String> temp3=adapter3.get(j).getAdvanceCmdData_mData();
                                for(int x=0;x<temp3.size();x++){
                                    Log.e("指令"+i+j+x,temp3.get(x));
                                }
                            }
                        }
                    }
                }
                if(commandLayer==2){ //如果是第二層指令就要拿出對應的AdvanceCMDAdapter
                    //Adapter建構完畢，去AdavanceCMDAdapters1最後一個位置(size-1，最新的指令)拿出adapter，向Adapter內的資料集放入指令
                    tempAdvanceCmdAdapter_layer2=Radapter_hasUse.getAdvanceCMDAdapters_layer2().get(Radapter_hasUse.getAdvanceCMDAdapters_layer2().size()-1);
                }
                if(commandLayer==3){  //抓回來第三層Adapter
                    if(tempAdvanceCmdAdapter_layer2==null){ //如果沒有第二層Adapter，去Radapter_haseUse的最後一個抓回來
                        tempAdvanceCmdAdapter_layer2=Radapter_hasUse.getAdvanceCMDAdapters_layer2().get(Radapter_hasUse.getAdvanceCMDAdapters_layer2().size()-1);
                    }
                    //第三層的目標Adapter在剛抓出來的Adapter的getAdvanceCMDAdapters_layer3()的最後一個位置。
                    tempAdvanceCmdAdapter_layer3=tempAdvanceCmdAdapter_layer2.getAdvanceCMDAdapters_layer3().get(tempAdvanceCmdAdapter_layer2.getAdvanceCMDAdapters_layer3().size()-1);
                }
                if(result_string.equals("結束")){
                    if(commandLayer==1){
                        isEndCmd=true;  //要在更新第一次UI前把這個設為True，不然在new Thread才設的話會太遲，凱比會繼續mRobotAPI.startMixUnderstand();
                        TextView textStart=findViewById(R.id.textView_cmdInput);
                        //先將 btnStart setText + setUnenable
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() { // 更新關卡
                                btnStart.setEnabled(false);
                                btnStart.setText("輸入指令開始");
                                textStart.setText("點\"輸入指令\"後才會開始接收語音指令");
                                Radapter_hasUse.setBtnRemoveEnable(true);
                            }
                        });
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                mRobotAPI.startTTS("接收到結束指令，結束指令輸入");
                                mTts_complete=false;
                                while(!mTts_complete){}

                                Message msg = new Message();
                                msg.what =2;
                                UIupdateHandler.sendMessage(msg);
                            }
                        }).start();
                    }
                    else{  //如果是在 2 3層接到這指令>>代表結束那段的輸入，commandlayer-1
                        commandLayer-=1;
                        mRobotAPI.startTTS("結束迴圈內容輸入");
                    }
                }
                else if(result_string.equals("執行")){ //執行=結束+執行
                    //執行不管在哪一個commandlayer都是做同樣的事
                    isEndCmd=true;
                    TextView textStart=findViewById(R.id.textView_cmdInput);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //現在指令列第一個不是執行，才要插入到第一個
                            if(Radapter_hasUse.getItemCount()>0 &&!Radapter_hasUse.getAdvanceCmdData_mData().get(0).equals("執行")){
                                Radapter_hasUse.addItem(result_string,0,1);  //把執行指令插入到第一個去
                                recyclerView_hasUse.scrollToPosition(0); //將畫面移到新增的position，為了更新recyclerView，因為沒看到就不會創立View
                            }
                            //使所有的 Button 不能被點擊
                            btnStart.setEnabled(false);
                            btnStart.setText("輸入指令開始");
                            textStart.setText("點\"輸入指令\"後才會開始接收語音指令");
                        }
                    });
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mRobotAPI.startTTS("接到執行指令，即將執行");
                            mTts_complete=false;
                            //等到執行add進去Adapter中，不然對應的adapter會錯位造成crash
                            while(!mTts_complete||!Radapter_hasUse.gethasConstructedAdvanceAdapter()){}

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Radapter_hasUse.setBtnRemoveEnable(false);
                                    btnRun.performClick();
                                }
                            });
                        }
                    }).start();
                }
                else if(result_string.equals("前進")||result_string.equals("後退")||result_string.equals("左轉")||result_string.equals("右轉")){
                    //新增基礎指令 因為這時已經在執行 commandListen這個任務，所以才需要用runOnUiThread來更新 RecyclerView
                    //低級指令 = 1
                    if(commandLayer==1){ //如果第一層，就普通的加入
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Radapter_hasUse.addItem(result_string,Radapter_hasUse.getItemCount(),1); //加到最後一個去
                                recyclerView_hasUse.scrollToPosition(Radapter_hasUse.getItemCount()-1); //將畫面移到新增的position，為了更新recyclerView，因為沒看到就不會創立View
                            }
                        });
                        mRobotAPI.startTTS(result_string);
                        //因為新增holder需要時間，所以等到建立好再執行下次mRobotAPI.startMixUnderstand();
                        while(!Radapter_hasUse.gethasConstructedAdvanceAdapter()){}
                    }
                    else if(commandLayer==2 && tempAdvanceCmdAdapter_layer2 != null){ //第二層，加入tempAdvanceCmdAdapter_layer2內
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tempAdvanceCmdAdapter_layer2.addItem(result_string,tempAdvanceCmdAdapter_layer2.getItemCount(),1);  //指令、新增位置、指令型態
                                recyclerView_hasUse.scrollBy(0,100); //因為沒辦法直接抓position就只能向下y(y要是>極限長度，會滑到極限長度)。
                            }
                        });
                        mRobotAPI.startTTS(result_string);
                        while(!tempAdvanceCmdAdapter_layer2.gethasConstructedAdapter()){}
                        //因為新增holder需要時間，所以等到建立好再執行下次mRobotAPI.startMixUnderstand();
                    }
                    else if(commandLayer==3 && tempAdvanceCmdAdapter_layer3!=null ){  //如果第三層+Adapter物件非null
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tempAdvanceCmdAdapter_layer3.addItem(result_string,tempAdvanceCmdAdapter_layer3.getItemCount(),1);  //指令、新增位置、指令型態
                                recyclerView_hasUse.scrollBy(0,100); //因為沒辦法直接抓position就只能向下y(y要是>極限長度，會滑到極限長度)。
                            }
                        });
                        mRobotAPI.startTTS(result_string);
                        while(!tempAdvanceCmdAdapter_layer3.gethasConstructedAdapter()){}
                        //因為新增holder需要時間，所以等到建立好再執行下次mRobotAPI.startMixUnderstand();
                    }
                }
                else { //新增高級指令：迴圈、複合指令
                    if(commandLayer>=3){ //沒有處理那麼多層
                        mRobotAPI.startTTS("無法繼續新增新的指令層");
                    }
                    else {
                        boolean isCMD=false;
                        if(result_string.equals("迴圈")){
                            isCMD=true;
                            if(commandLayer==1){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //Radapter_hasUse.getItemCount() =加到最後一個去
                                        Radapter_hasUse.addItem(result_string,Radapter_hasUse.getItemCount(),2);
                                        recyclerView_hasUse.scrollToPosition(Radapter_hasUse.getItemCount()-1); //將畫面移到新增的position，為了更新recyclerView，因為沒看到就不會創立View
                                    }
                                });
                            }
                            else if(commandLayer==2){  //在加到第二層的最後一個去
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //加到最後一個去
                                        tempAdvanceCmdAdapter_layer2.addItem(result_string,tempAdvanceCmdAdapter_layer2.getItemCount(),2);
                                        recyclerView_hasUse.scrollBy(0,100); //因為沒辦法直接抓position，就只能向下y(y要是>極限長度，會滑到極限長度)。
                                    }
                                });
                            }
                            mRobotAPI.startTTS("請先設定迴圈次數");
                            commandLayer+=1;
                            dialogIsShowing=true; //現在正在showing dialog，下次語音輸入不會自動觸發
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    //這邊需要卡一下，給Radapter_hasUse or tempAdvanceCmdAdapter_layer2 新增item的時間，不然 tempAdvanceCmdAdapter_layer2 或 tempAdvanceCmdAdapter_layer3 在getSize時會拿到尚未addItem前的大小，導致拿到的Adapter也是錯的。
                                    while(!Radapter_hasUse.gethasConstructedAdvanceAdapter()){}
                                    if(tempAdvanceCmdAdapter_layer2!=null){
                                        while(!tempAdvanceCmdAdapter_layer2.gethasConstructedAdapter()){}
                                    }

                                    Message msg = new Message();
                                    msg.what =8;   //show 迴圈次數 dialog
                                    UIupdateHandler.sendMessage(msg);
                                    //等到dialog關閉後才允許下次語音指令輸入
                                }
                            }).start();
                        }
                        else{  //處理自訂指令 or 讀到不存在的指令
                            for(int i=0;i<userData.getUserCommand().size() && !isCMD;i++){
                                if(userData.getUserCommand().get(i).get(0).equals(result_string)){
                                    targetCMD=userData.getUserCommand().get(i); //拿到要新增的自訂指令，存在targetCMD內
                                    isCMD=true;
                                    break;  //找到正確相應指令後break
                                }
                            }
                            if(isCMD){ //讀到存在的自訂指令
                                mRobotAPI.startTTS("新增自訂指令："+result_string);
                                if(commandLayer==1){
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            //Radapter_hasUse.getItemCount() =加到最後一個去
                                            Radapter_hasUse.addItem(result_string,Radapter_hasUse.getItemCount(),2);
                                            recyclerView_hasUse.scrollToPosition(Radapter_hasUse.getItemCount()-1); //將畫面移到新增的position，為了更新recyclerView，因為沒看到就不會創立View
                                        }
                                    });
                                }
                                else if(commandLayer==2){  //在加到第二層的最後一個去
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            tempAdvanceCmdAdapter_layer2.addItem(result_string,tempAdvanceCmdAdapter_layer2.getItemCount(),2);
                                            recyclerView_hasUse.scrollToPosition(Radapter_hasUse.getItemCount()-1); //將畫面移到新增的position，為了更新recyclerView，因為沒看到就不會創立View
                                        }
                                    });
                                }
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //這邊需要卡一下，給Radapter_hasUse or tempAdvanceCmdAdapter_layer2 新增item的時間，不然 tempAdvanceCmdAdapter_layer2 或 tempAdvanceCmdAdapter_layer3 在getSize時會拿到尚未addItem前的大小，導致拿到的Adapter也是錯的。
                                        while(!Radapter_hasUse.gethasConstructedAdvanceAdapter()){}
                                        if(tempAdvanceCmdAdapter_layer2!=null){
                                            while(!tempAdvanceCmdAdapter_layer2.gethasConstructedAdapter()){}
                                        }
                                        //如果是自定義指令，從UserData中的對應自訂指令拿出內部指令，全塞進對應指令層(2 or 3)的Adapter之中。
                                        //只有迴圈類會改變commandLayer，所以完成指令的加入就好了。
                                        if(commandLayer==1){ //在第一層新增自訂指令
                                            //取出對應的Adapter
                                            //Log.i("test3333","are you the bug?"+(Radapter_hasUse.getAdvanceCMDAdapters_layer2().size()-1));   //test
                                            tempAdvanceCmdAdapter_layer2=Radapter_hasUse.getAdvanceCMDAdapters_layer2().get(Radapter_hasUse.getAdvanceCMDAdapters_layer2().size()-1);
                                            //Log.i("tes4444 size= "+tempAdvanceCmdAdapter_layer2.getItemCount(),"are you the bug?"+targetCMD.size());   //test
                                            //等adapter拿到後再進行addItem，為了保證執行順序，使用了message
                                            Message msg = new Message();
                                            msg.what =6;
                                            UIupdateHandler.sendMessage(msg);
                                        }
                                        else if(commandLayer==2){ //在第二層新增自訂指令
                                            if(tempAdvanceCmdAdapter_layer2==null){ //如果沒有第二層Adapter，去Radapter_haseUse的最後一個抓回來
                                                tempAdvanceCmdAdapter_layer2=Radapter_hasUse.getAdvanceCMDAdapters_layer2().get(Radapter_hasUse.getAdvanceCMDAdapters_layer2().size()-1);
                                            }
                                            tempAdvanceCmdAdapter_layer3=tempAdvanceCmdAdapter_layer2.getAdvanceCMDAdapters_layer3().get(tempAdvanceCmdAdapter_layer2.getAdvanceCMDAdapters_layer3().size()-1);
                                            //等adapter拿到後再進行addItem，為了保證執行順序，使用了message
                                            Message msg = new Message();
                                            msg.what =7;
                                            UIupdateHandler.sendMessage(msg);
                                        }
                                    }
                                }).start();
                            }
                            else{  //讀到不存在的指令
                                mRobotAPI.startTTS(result_string);
                                mRobotAPI.startTTS("這個指令不存在喔");
                            }
                        }
                    }
                }
            }
            else{ //沒有讀到正確的，isError=true
                mRobotAPI.startTTS("無法判斷指令，請再試一次");
            }
            // 不管有沒有讀到，在出現關鍵字 END 或結束前 +或是現在沒有dialog正在showing，都會繼續聽指令
            if(!isEndCmd && !dialogIsShowing){
                mRobotAPI.startMixUnderstand();
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