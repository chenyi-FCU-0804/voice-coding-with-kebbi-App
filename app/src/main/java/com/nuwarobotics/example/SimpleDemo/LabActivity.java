package com.nuwarobotics.example.SimpleDemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nuwarobotics.example.R;
import com.nuwarobotics.example.SimpleDemo.selfMadeObject.RecyclerViewAdapter;
import com.nuwarobotics.example.SimpleDemo.selfMadeObject.RecyclerViewAdapter_lab_editingCMD;
import com.nuwarobotics.example.SimpleDemo.selfMadeObject.RecyclerViewAdapter_lab_ownCMD;
import com.nuwarobotics.example.SimpleDemo.selfMadeObject.UserData;
import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.RobotEventListener;
import com.nuwarobotics.service.agent.SimpleGrammarData;
import com.nuwarobotics.service.agent.VoiceEventListener;
import com.nuwarobotics.service.agent.VoiceResultJsonParser;

import java.lang.reflect.Array;
import java.util.ArrayList;
/*
* 語音辨識部分：因為會有新的指令出現，因此用到Local+cloud的判斷方式
*       Local+cloud只要有判斷出字，return 就會是true + 得到的字串 ，所以需要比對新的自訂指令是否有跟basic own CMD重複(比對兩個ArrayList)
* 最一開始只能點選 btnCreate btnClear ，其他三顆在不知道現在在編輯哪個自訂指令的時候是不能按的， nowEditing=-1 ：沒有在編輯 ， =0~OwnCMD.size()，編輯已有自訂指令也包含新創的指令(會在名稱確定不重複後加到OwnCMD內)
*
* 目前進度：已經完成所有功能，oncreate onstart ondestroy的處理因為 Acitivity已經是最上層，所以不管怎樣按了上一頁就會Destroy掉，資料更新也有在進行時同步做好。
*       所以就只有release nuwaAPI + 在進行其他按鈕不能被click時(代表有功能在執行)無法返回上一頁。
* */

public class LabActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = this.getClass().getSimpleName();  //=LabActivity
    UserData userData;
    NuwaRobotAPI mRobotAPI;
    IClientId mClientId;

    private RecyclerViewAdapter Radapter_basicCMD;    //基本指令的Adapter
    private RecyclerView recyclerView_basicCMD;  //基本指令的recyclerView
    private final ArrayList<String> basicCMD=new ArrayList<String>() {{  //四個基本指令
        add("前進");add("後退");add("左轉");add("右轉");
    }};

    private RecyclerViewAdapter_lab_ownCMD Radapter_ownCMD;    //自訂指令的Adapter ，需要有編輯+查看內部指令按鈕
    private RecyclerView recyclerView_ownCMD;  //自訂指令的recyclerView
    private final ArrayList<String> ownCMD=new ArrayList<String>(); //要從UserData抓過來，只有所有自訂指令名而已，內部指令會放到newCMD內
    private Button btn_ownCMD_edit;  //選一個ownCMD list內的指令+點擊編輯後才會將右邊正在編輯的指令先儲存再把畫面給切換調
    private Button btn_ownCMD_checkInfo; //跟也是先點擊Item，再按下去就會看到自訂指令的內部指令，用dialog顯示
    private Button btn_ownCMD_delet;  //也是先點擊，再刪除

    private RecyclerViewAdapter_lab_editingCMD Radapter_editingCMD; //正在被編輯的指令的Adapter
    private RecyclerView recyclerView_editingCMD;  //正在被編輯的指令的recyclerView
    private ArrayList<String> editingCMD=new ArrayList<String>();  //從目前正在編輯的指令名(OwnCMD其中一個)，去到UserData抓出對應名稱的所有內部指令，新指令為空

    private TextView textView_showEditingCMD;  //顯示目前正在編輯的指令

    private Button btnCreate; //創造新指令
    private Button btnStart;  //開始輸入內部指令
    private Button btnClear; //刪除正在建立的指令的所有內部指令
    private Button btnTest; //試跑
    private Button btnRules; //存檔

    private Handler UIupdateHandler; // 控制UI更新用
    private boolean isCreatingNewCMDname=false;  //如果在取新指令的名字
    private boolean mTts_complete=true; //控制UI更新的boolean，如果TTS完成
    private boolean endCMDtest=false; //控制是否中斷指令test

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab);

        //Step 1 : Initial Nuwa API Object
        mClientId = new IClientId(this.getPackageName());
        mRobotAPI = new NuwaRobotAPI(this,mClientId);

        //Step 2 : Register receive Robot Event
        Log.d(TAG,"register EventListener ") ;
        mRobotAPI.registerRobotEventListener(robotEventListener);//listen callback of robot service event

        userData=(UserData)getApplicationContext();
        initUIcomponent();

        //UI handler初始化
        UIupdateHandler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                switch (msg.what){
                    case 1: //按下創立新指令後，請問新指令叫什麼?講完後enable btnCreate
                        btnCreate.setEnabled(true);
                        break;
                    case 2:  //結束接收新指令名稱，語音+按鈕的方式 //結束指令輸入，語音+按鈕的方式，恢復所有button enable
                        btn_ownCMD_edit.setEnabled(true);
                        btn_ownCMD_checkInfo.setEnabled(true);
                        btn_ownCMD_delet.setEnabled(true);
                        btnCreate.setEnabled(true);
                        btnStart.setEnabled(true);
                        btnClear.setEnabled(true);
                        btnTest.setEnabled(true);
                        btnRules.setEnabled(true);
                        Radapter_ownCMD.setItemCanClick(true);
                        Radapter_editingCMD.setItemCanClick(true);
                        break;
                    case 3:  //按下開始接收後，等TTS結束，enable btnStart
                        btnStart.setEnabled(true);
                        break;
                    case 4:     //show出確認清空編輯區的dialog
                        buildAndShowConfirmClearEditingCMDdialog();
                        break;
                    case 5:  //按下測試後，等TTS結束，enable btnTest
                        btnTest.setEnabled(true);
                    default:
                        break;
                }
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // release Nuwa Robot SDK resource
        mRobotAPI.release();
    }

    private void initUIcomponent() {
        Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar_lab);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("程式實驗室：運用基本指令打造更方便的自訂指令");

        //初始化basicCMD RecyclerView
        recyclerView_basicCMD = (RecyclerView) findViewById(R.id.RecyclerView_lab_basicCMD);
        // 設置RecyclerView為列表型態
        recyclerView_basicCMD.setLayoutManager(new LinearLayoutManager(this));
        // 設置格線
        recyclerView_basicCMD.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        // 將資料交給adapter
        Radapter_basicCMD = new RecyclerViewAdapter(basicCMD);
        // 設置adapter給recycler_view
        recyclerView_basicCMD.setAdapter(Radapter_basicCMD);

        //初始化OwnCMD RecyclerView
        for(int i =0;i<userData.getUserCommand().size();i++){
            //去拿所有使用者指令的第一個=自訂指令名
            ownCMD.add(userData.getUserCommand().get(i).get(0));
        }
        recyclerView_ownCMD = (RecyclerView)findViewById(R.id.RecyclerView_lab_OwnCMD);
        // 設置RecyclerView為列表型態
        recyclerView_ownCMD.setLayoutManager(new LinearLayoutManager(this));
        // 設置格線
        recyclerView_ownCMD.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        // 將資料交給adapter
        Radapter_ownCMD = new RecyclerViewAdapter_lab_ownCMD(ownCMD);
        // 設置adapter給recycler_view
        recyclerView_ownCMD.setAdapter(Radapter_ownCMD);
        btn_ownCMD_edit=findViewById(R.id.button_lab_ownCMD_btnEdit);
        btn_ownCMD_edit.setOnClickListener(this);
        btn_ownCMD_checkInfo=findViewById(R.id.button_lab_ownCMD_btnCheck);
        btn_ownCMD_checkInfo.setOnClickListener(this);
        btn_ownCMD_delet=findViewById(R.id.button_lab_ownCMD_btnDelet);
        btn_ownCMD_delet.setOnClickListener(this);

        recyclerView_editingCMD = (RecyclerView)findViewById(R.id.RecyclerView_lab_editingCMD);
        // 設置RecyclerView為列表型態
        recyclerView_editingCMD.setLayoutManager(new LinearLayoutManager(this));
        // 設置格線
        recyclerView_editingCMD.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        // 將資料交給adapter
        Radapter_editingCMD = new RecyclerViewAdapter_lab_editingCMD(editingCMD);
        // 設置adapter給recycler_view
        recyclerView_editingCMD.setAdapter(Radapter_editingCMD);

        //設定顯示目前編輯指令的textView
        textView_showEditingCMD=findViewById(R.id.textView_lab_nowEditingPosition2);

        btnCreate=findViewById(R.id.button_lab_btnCreate);
        btnCreate.setOnClickListener(this);
        btnStart=findViewById(R.id.button_lab_btnStart);
        btnStart.setOnClickListener(this);
        btnClear=findViewById(R.id.button_lab_btnClear);
        btnClear.setOnClickListener(this);
        btnTest=findViewById(R.id.button_lab_btnTest);
        btnTest.setOnClickListener(this);
        btnRules=findViewById(R.id.button_lab_btnRules);
        btnRules.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.button_lab_ownCMD_btnEdit:  //只有在編輯的時候，havesave會被改成false，當false時要先同步(userdata)之前編輯的指令，再匯入新的position的指令來編輯
                disableAllButton();
                if(!Radapter_editingCMD.getHaveSave()){  //如果之前編輯的尚未更新到userdata.usercommand內
                    ArrayList<String> templist=new ArrayList<>(Radapter_editingCMD.getmData()); //set之前記得先新創一個ArrayList，不然用舊的會在removeAllItem那邊把資料刪光
                    userData.getUserCommand().set(Radapter_ownCMD.getNowEditingPosition(),templist); //把之前編輯的eidtingCMD(要去adapter內拿mData)更新至userdata
                    Radapter_editingCMD.setHaveSave(true);
                }
                int position=Radapter_ownCMD.updateNowEditingPosition();
                if(position==-1){ //還沒設定要編輯的指令
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mRobotAPI.startTTS("請先指定一個要編輯的指令");
                            mTts_complete=false;
                            while(!mTts_complete){}

                            Message msg = new Message();
                            msg.what =2;
                            UIupdateHandler.sendMessage(msg);
                        }
                    }).start();
                }
                else{ //已設定要編輯的指令，清空editingCMD的Adapter + UserData的對應指令位置將所有內部指令add進editingCMD的Adapter內
                    runOnUiThread(new Runnable() { //更新editingCMD的recyclerView
                        @Override
                        public void run() {
                            Log.i("size= ",String.valueOf(Radapter_ownCMD.getmData().size()));
                            textView_showEditingCMD.setText(Radapter_ownCMD.getmData().get(position));
                            //清空editingCMD的Adapter
                            Radapter_editingCMD.removeAllItem(0,Radapter_editingCMD.getItemCount()); //完全清除指令編輯區
                            //UserData的對應指令位置將所有內部指令add進editingCMD的Adapter內
                            editingCMD=userData.getUserCommand().get(position);
                            for(int i=0;i<userData.getUserCommand().get(position).size();i++){
                                Radapter_editingCMD.addItem(editingCMD.get(i));
                            }
                        }
                    });
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //編輯不再直接改動haveSave的值，當editingCMD被改動時才會change haveSave的狀態
                            mRobotAPI.startTTS("開始編輯 "+Radapter_ownCMD.getmData().get(position) );
                            mTts_complete=false;
                            while(!mTts_complete){}

                            Message msg = new Message();
                            msg.what =2;
                            UIupdateHandler.sendMessage(msg);
                        }
                    }).start();
                }
                break;
            case R.id.button_lab_ownCMD_btnCheck:
                int lastPosition=Radapter_ownCMD.getLastOnclickPosition();
                if(lastPosition!=-1){ //有指定到指令才會觸發，將position的指令送進去Dialog
                    //如果沒有儲存，要更新一下userData的指令
                    if(!Radapter_editingCMD.getHaveSave()){
                        ArrayList<String> templist=new ArrayList<>(Radapter_editingCMD.getmData());
                        for(int i=0;i<Radapter_editingCMD.getItemCount();i++){
                            Log.i(Radapter_ownCMD.getNowEditingPosition()+"pp1："+i,Radapter_editingCMD.getmData().get(i));
                        }
                        userData.getUserCommand().set(Radapter_ownCMD.getNowEditingPosition(),templist);
                        for(int i=0;i<userData.getUserCommand().get(Radapter_ownCMD.getNowEditingPosition()).size();i++){
                            Log.i(Radapter_ownCMD.getNowEditingPosition()+"pp2："+i,userData.getUserCommand().get(Radapter_ownCMD.getNowEditingPosition()).get(i));
                        }
                        Radapter_editingCMD.setHaveSave(true); //更新完畢後，儲存完畢=true
                    }
                    buildAndShowCheckCMDdialog(userData.getUserCommand().get(lastPosition));
                }
                else{
                    disableAllButton();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mRobotAPI.startTTS("還沒有指定要看哪個指令喔");
                            mTts_complete=false;
                            while(!mTts_complete){}

                            Message msg = new Message();
                            msg.what =2;
                            UIupdateHandler.sendMessage(msg);
                        }
                    }).start();
                }
                break;
            case R.id.button_lab_ownCMD_btnDelet:
                int deletPosition=Radapter_ownCMD.getLastOnclickPosition();
                if(deletPosition!=-1){
                    buildAndShowConfirmRemoveDialog(deletPosition);
                }
                else{  //刪除失敗，沒有指定position時
                    disableAllButton();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mRobotAPI.startTTS("因沒有指定的指令，刪除失敗");
                            mTts_complete=false;
                            while(!mTts_complete){}

                            Message msg = new Message();
                            msg.what =2;
                            UIupdateHandler.sendMessage(msg);
                        }
                    }).start();
                }
                break;
            case R.id.button_lab_btnCreate:
                String temp=btnCreate.getText().toString();
                if(temp.equals("創立新指令")){
                    disableAllButton();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnCreate.setText("取消");  //放棄建立新指令
                        }
                    });
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mRobotAPI.startTTS("請問新指令叫什麼?");
                            isCreatingNewCMDname=true;
                            mTts_complete=false;
                            mRobotAPI.startMixUnderstand();//Start listen without wakeup, callback on onMixUnderstandComplete  //因為會有新的詞出現，所以需要用Local+Cloud
                            while(!mTts_complete){}

                            Message msg = new Message();
                            msg.what =1;
                            UIupdateHandler.sendMessage(msg);
                        }
                    }).start();
                }
                else{ //取消創立新指令處理
                    mRobotAPI.stopListen();
                    isCreatingNewCMDname=false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnCreate.setText("創立新指令");  //放棄建立新指令
                            btnCreate.setEnabled(false); //等凱比說完話才讓btnCreate可以取消
                        }
                    });
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mRobotAPI.startTTS("結束接收新指令名稱");
                            mTts_complete=false;
                            while(!mTts_complete){}

                            Message msg = new Message();
                            msg.what =2;
                            UIupdateHandler.sendMessage(msg);
                        }
                    }).start();
                }
                break;
            case R.id.button_lab_btnStart:
                String btntxt=btnStart.getText().toString();
                TextView startHint=findViewById(R.id.textView_lab_btnStartHint);
                if(Radapter_ownCMD.getNowEditingPosition()!=-1){
                    //如果有指定正在編輯的
                    if(btntxt.equals("開始接收")){
                        disableAllButton();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnStart.setText("結束接收");
                                startHint.setText("結束語音指令輸入");
                            }
                        });
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //開始接收自訂指令的內部指令
                                mRobotAPI.startTTS("開始接收內部指令");
                                mTts_complete=false;
                                while(!mTts_complete){}
                                mRobotAPI.startMixUnderstand();

                                Message msg = new Message();
                                msg.what =3;
                                UIupdateHandler.sendMessage(msg);
                            }
                        }).start();
                    }
                    else{
                        mRobotAPI.stopListen();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnStart.setEnabled(false);
                                btnStart.setText("開始接收");
                                startHint.setText("輸入語音指令");
                            }
                        });
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //結束接收自訂指令的內部指令
                                mRobotAPI.startTTS("結束指令輸入");
                                mTts_complete=false;
                                while(!mTts_complete){}

                                Message msg = new Message();
                                msg.what =2;
                                UIupdateHandler.sendMessage(msg);
                            }
                        }).start();
                    }
                }
                else{  //沒有指定編輯指令的狀況
                    disableAllButton();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mRobotAPI.startTTS("尚未指定要編輯的指令，無法接收");
                            mTts_complete=false;
                            while(!mTts_complete){}

                            Message msg = new Message();
                            msg.what =2;
                            UIupdateHandler.sendMessage(msg);
                        }
                    }).start();
                }

                break;
            case R.id.button_lab_btnClear:
                disableAllButton();
                if(Radapter_editingCMD.getItemCount()==0){   //編輯區內沒東西
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mRobotAPI.startTTS("編輯區內已經是空的了");
                            mTts_complete=false;
                            while(!mTts_complete){}

                            Message msg = new Message();
                            msg.what =2;
                            UIupdateHandler.sendMessage(msg);
                        }
                    }).start();
                }
                else if(Radapter_editingCMD.getItemCount()==1){ //只有自訂指令名
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mRobotAPI.startTTS("這裡沒辦法直接刪除自訂指令喔");
                            mTts_complete=false;
                            while(!mTts_complete){}

                            Message msg = new Message();
                            msg.what =2;
                            UIupdateHandler.sendMessage(msg);
                        }
                    }).start();
                }
                else{
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mRobotAPI.startTTS("真的要清空編輯區嗎?");
                            mTts_complete=false;
                            while(!mTts_complete){}

                            Message msg = new Message();
                            msg.what =4;
                            UIupdateHandler.sendMessage(msg);
                        }
                    }).start();
                }
                break;
            case R.id.button_lab_btnTest:
                TextView testHint=findViewById(R.id.textView_lab_btnTestHint);
                String tempstr=btnTest.getText().toString();
                if(Radapter_ownCMD.getNowEditingPosition()==-1){//沒有指定編輯對象，無法測試
                    disableAllButton();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mRobotAPI.startTTS("還沒有指定要測試的指令");
                            mTts_complete=false;
                            while(!mTts_complete){}

                            Message msg = new Message();
                            msg.what =2;
                            UIupdateHandler.sendMessage(msg);
                        }
                    }).start();
                }
                else{
                    if(tempstr.equals("試跑 / 執行")){
                        disableAllButton();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnTest.setText("中斷測試");
                                testHint.setText("再點我一次可以中斷測試");
                            }
                        });
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                mRobotAPI.startTTS("開始測試指令");
                                mTts_complete=false;
                                while(!mTts_complete){}
                                //先enable btnTest
                                Message msg = new Message();
                                msg.what =5;
                                UIupdateHandler.sendMessage(msg);
                                //再執行指令
                                endCMDtest=false;
                                testEditingCMD();
                                if(!endCMDtest){ //如果沒有被中斷
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            btnTest.setText("試跑 / 執行");
                                            testHint.setText("看看程式的效果吧");
                                            btnTest.setEnabled(false);
                                        }
                                    });
                                    mRobotAPI.startTTS("測試完畢");
                                    mTts_complete=false;
                                    while (!mTts_complete){}

                                    msg=new Message();
                                    msg.what=2;
                                    UIupdateHandler.sendMessage(msg);
                                }
                            }
                        }).start();
                    }
                    else{ //中斷執行
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnTest.setText("試跑 / 執行");
                                testHint.setText("看看程式的效果吧");
                                btnTest.setEnabled(false);
                            }
                        });
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                endCMDtest=true;
                                mRobotAPI.turn(0);
                                mRobotAPI.move(0);
                                mRobotAPI.stopInAccelerationEx();
                                mRobotAPI.stopTurnEx();
                                mRobotAPI.startTTS("中斷指令測試");
                                mTts_complete=false;
                                while(!mTts_complete){}

                                Message msg = new Message();
                                msg.what =2;
                                UIupdateHandler.sendMessage(msg);
                            }
                        }).start();
                    }
                }

                break;
            case R.id.button_lab_btnRules:
                showRulesDialog();
                break;
            default:
                break;
        }
    }

    public void disableAllButton(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btn_ownCMD_edit.setEnabled(false);
                btn_ownCMD_checkInfo.setEnabled(false);
                btn_ownCMD_delet.setEnabled(false);
                btnCreate.setEnabled(false); //等凱比說完話才讓btnCreate可以取消
                btnStart.setEnabled(false);  //其他的button也不能按
                btnClear.setEnabled(false);
                btnTest.setEnabled(false);
                btnRules.setEnabled(false);
                Radapter_ownCMD.setItemCanClick(false);
                Radapter_editingCMD.setItemCanClick(false);
            }
        });
    }

    private void testEditingCMD() { //測試自製指令
        for(int i=0;i<Radapter_editingCMD.getItemCount();i++){
            if(endCMDtest){
                break;
            }
            switch (Radapter_editingCMD.getmData().get(i)){
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
                Log.d(TAG, "RunCmd's wait for 0.2sec went wrong");
                return;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {  //點到返回鍵處理
        switch (item.getItemId()) {
            case android.R.id.home:
                if(Radapter_ownCMD.getItemCanClick()){  //如果按鈕可以點擊，因為所有按鈕都是同時被disable enable，懶得新增變數button來記，就用別的button的狀態來判斷了
                    if(!Radapter_editingCMD.getHaveSave()){  //如果之前編輯的尚未更新到userdata.usercommand內
                        ArrayList<String>tempCMD = new ArrayList<>(Radapter_editingCMD.getmData());
                        userData.getUserCommand().set(Radapter_ownCMD.getNowEditingPosition(),tempCMD); //把之前編輯的eidtingCMD(Radapter_editingCMD的mdata)更新至userdata
                        Radapter_editingCMD.setHaveSave(true);
                    }
                    mRobotAPI.turn(0);
                    mRobotAPI.move(0);
                    mRobotAPI.stopInAccelerationEx();
                    mRobotAPI.stopTurnEx();
                    mRobotAPI.stopListen();
                    mRobotAPI.stopTTS();
                    finish();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void prepareGrammarToRobot() {  //準備local的文法，要從
        Log.d(TAG, "prepareGrammarToRobot ");

        //Create Grammar class object
        //NOTICE : please only use "lower case letter" as naming of grammar name
        SimpleGrammarData mGrammarData = new SimpleGrammarData("example");
        //setup local command list to grammar class
        for (String string : basicCMD) {
            mGrammarData.addSlot(string);
            Log.d(TAG, "add string : " + string);
        }
        //generate grammar data
        mGrammarData.updateBody();
        //create and update Grammar to Robot
        Log.d(TAG, "createGrammar " + mGrammarData.body);
        //NOTICE : please only use "lower case letter" as naming of grammar name
        mRobotAPI.createGrammar(mGrammarData.grammar, mGrammarData.body); // Regist cmd
    }

    RobotEventListener robotEventListener = new RobotEventListener() {
        @Override
        public void onWikiServiceStart() {
            // Nuwa Robot SDK is ready now, you call call Nuwa SDK API now.
            Log.d(TAG,"onWikiServiceStart, robot ready to be control ") ;
            //Step 3 : Start Control Robot after Service ready.
            //Register Voice Callback event
            mRobotAPI.registerVoiceEventListener(voiceEventListener);//listen callback of robot voice related event
            //Allow user start demo after service ready
            prepareGrammarToRobot();//準備文法
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
        public void onTouchEvent(int i, int i1) {

        }

        @Override
        public void onPIREvent(int i) {

        }

        @Override
        public void onTap(int i) {

        }

        @Override
        public void onLongPress(int i) {

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
        public void onWakeup(boolean b, String s, float v) {
            Log.d(TAG, "onWakeup:" + !b + ", score:" + s);
        }

        @Override
        public void onTTSComplete(boolean b) {
            mTts_complete = true;
        }

        @Override
        public void onSpeechRecognizeComplete(boolean b, ResultType resultType, String s) {

        }

        @Override
        public void onSpeech2TextComplete(boolean isError, String json) {
            Log.d(TAG, "onSpeech2TextComplete:" + !isError + ", json:" + json);
        }

        @Override
        public void onMixUnderstandComplete(boolean isError, ResultType resultType, String s) {
            Log.d(TAG, "onMixUnderstandComplete isError:" + !isError + ", json:" + s);
            //Step 7 : Robot recognized the word of user speaking on  onMixUnderstandComplete
            //both startMixUnderstand and startLocalCommand will receive this callback
            //得到的結果
            String result_string = VoiceResultJsonParser.parseVoiceResult(s);
            mRobotAPI.stopListen();
            if(!isError){  //代表有讀到東西，result_string非null
                //比對result_string跟之前存在的指令是否有重複
                boolean isDuplicate=false;
                if(Radapter_ownCMD.getmData().contains(result_string)|| basicCMD.contains(result_string)){
                    isDuplicate=true;
                    Log.i("有重複嗎?.....", String.valueOf(isDuplicate));
                }
                if(result_string.equals("結束")||result_string.equals("結束接收")){ //結束接收指令
                    if(isCreatingNewCMDname){  //如果是在創立新指令的時候
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnCreate.setEnabled(false);
                                btnCreate.setText("創立新指令");
                            }
                        });
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                isCreatingNewCMDname=false;
                                mRobotAPI.startTTS("結束接收新指令名稱");
                                mTts_complete=false;
                                while(!mTts_complete){}

                                Message msg = new Message();
                                msg.what =2;
                                UIupdateHandler.sendMessage(msg);
                            }
                        }).start();
                    }
                    else{ //結束接收正在建立的指令的內部指令
                        TextView startHint=findViewById(R.id.textView_lab_btnStartHint);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnStart.setEnabled(false);
                                btnStart.setText("開始接收");
                                startHint.setText("輸入語音指令");
                            }
                        });
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                isCreatingNewCMDname=false;
                                mRobotAPI.startTTS("結束指令輸入");
                                mTts_complete=false;
                                while(!mTts_complete){}

                                Message msg = new Message();
                                msg.what =2;
                                UIupdateHandler.sendMessage(msg);
                            }
                        }).start();
                    }
                }
                else if(result_string.equals("執行")||result_string.equals("試跑")){
                    //disable所有按鈕，唯獨btnTest可以在 開始測試執行後變成中斷按鈕
                    TextView startHint=findViewById(R.id.textView_lab_btnStartHint);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnStart.setEnabled(false);
                            btnStart.setText("開始接收");
                            startHint.setText("輸入語音指令");
                            btnTest.performClick();
                        }
                    });

                }
                else{
                    if(isDuplicate){
                        //如果名稱重複了
                        if(isCreatingNewCMDname){
                            //如果是建立新指令名的時候，就得重用
                            mRobotAPI.startTTS(result_string);
                            mRobotAPI.startTTS("指令名稱重複囉，換一個吧");
                            mRobotAPI.startMixUnderstand();
                        }
                        else{ //處理原本已經有的指令的部分(basic+Own)
                            if(result_string.equals("前進")||result_string.equals("後退")||result_string.equals("左轉")||result_string.equals("右轉")){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Radapter_editingCMD.addItem(result_string);  //放進正在編輯的adapter內
                                        recyclerView_editingCMD.scrollToPosition(Radapter_editingCMD.getItemCount()-1); //滑到新增的指令那
                                    }
                                });
                                mRobotAPI.startTTS(result_string); //講出加入的指令
                            }
                            else if(Radapter_ownCMD.getmData().contains(result_string)){
                                //加入自訂指令的部分，會跳過ArrayList的第一個自訂指令名
                                mRobotAPI.startTTS(result_string);
                                for(int i=0;i<Radapter_ownCMD.getItemCount();i++){
                                    if(Radapter_ownCMD.getmData().get(i).equals(result_string)){
                                        if(i==Radapter_ownCMD.getNowEditingPosition()){
                                            //如果是現在在編輯的，無法新增
                                            mRobotAPI.startTTS("無法新增目前正在編輯的指令");
                                            break;
                                        }
                                        editingCMD=userData.getUserCommand().get(i);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    for (int j = 1; j < editingCMD.size(); j++) {
                                                        Radapter_editingCMD.addItem(editingCMD.get(j));  //放進正在編輯的adapter內
                                                        recyclerView_editingCMD.scrollToPosition(Radapter_editingCMD.getItemCount() - 1); //滑到新增的指令那
                                                    }
                                                }
                                            });
                                        break;
                                    }
                                }
                            }
                            mRobotAPI.startMixUnderstand();
                        }
                    }
                    else{
                        if(isCreatingNewCMDname){
                            //建立新指令成功，因為可能會讀錯，要給使用者一個修改的Dialog
                            mRobotAPI.startTTS(result_string);
                            mRobotAPI.startTTS("新指令建立成功，有需要修改名稱嗎?");
                            //show dialog
                            new Handler().postDelayed(new Runnable(){
                                public void run() {
                                    buildChangeNewCMDdialog(result_string);
                                    //將新指令加入OwnCMD的Adapter的資料中addItem + 將newCMD內容設為新指令的內部指令(第一個為新指令名+內部還沒有東西)，也是要用到newCMD的Adapter的addItem
                                    //在yes button觸發處完成
                                }
                            }, 6000);
                            isCreatingNewCMDname=false;  //建立完成設回false
                        }
                        else{ //沒在建立指令+沒有重複=沒有呼叫對應指令
                            mRobotAPI.startTTS(result_string);
                            mRobotAPI.startTTS("好像沒有這個指令，再說一次吧");
                            mRobotAPI.startMixUnderstand();
                        }
                    }
                }
            }
            else{  //如果沒讀到isError=true，等個一秒鐘在重新接收指令
                mRobotAPI.startTTS("沒有接收到指令");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.d(TAG,"wait for 2sec and get CMD went wrong");
                    return ;
                }
                mRobotAPI.startMixUnderstand();
            }
        }

        @Override
        public void onSpeechState(ListenType listenType, SpeechState speechState) {

        }

        @Override
        public void onSpeakState(SpeakType speakType, SpeakState speakState) {

        }

        @Override
        public void onGrammarState(boolean isError, String s) {
            //Step 5 : Aallow user press button to trigger startLocalCommand after grammar setup ready
            //startLocalCommand only allow calling after Grammar Ready
            if (!isError) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnCreate.setEnabled(true);
                        btnClear.setEnabled(true);
                    }
                });
            } else {
                Log.d(TAG, "onGrammarState error, " + s);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Allow user click button.
                        btnCreate.setEnabled(true);
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

    private void showRulesDialog() {
        Dialog ruleDialog=new Dialog(LabActivity.this);
        ruleDialog.setContentView(R.layout.dialog_lab_rules);
        ruleDialog.setCancelable(true);

        Button btnClose=ruleDialog.findViewById(R.id.button_dialog_lab_rules_close);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ruleDialog.cancel();
            }
        });
        ruleDialog.show();
    }

    private void buildAndShowConfirmRemoveDialog(int adapterPosition) {
        Dialog deletDialog=new Dialog(LabActivity.this);
        deletDialog.setContentView(R.layout.dialog_lab_delet_own_cmd);
        deletDialog.setCancelable(true);

        Button yes=deletDialog.findViewById(R.id.button_dialog_lab_delet_ownCMD_yes);
        Button no=deletDialog.findViewById(R.id.button_dialog_lab_delet_ownCMD_no);
        TextView title=deletDialog.findViewById(R.id.textView_dialog_lab_delet_ownCMD_title);
        title.setText("確定要刪除 "+ Radapter_ownCMD.getmData().get(adapterPosition) +" 指令嗎?");
        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //如果是現在正在編輯的指令，call editingCMD的recyclerView的remove all
                if(adapterPosition==Radapter_ownCMD.getNowEditingPosition()){
                    textView_showEditingCMD.setText("-1");
                    Radapter_editingCMD.removeAllItem(0 , Radapter_editingCMD.getItemCount()); //把editingCMD的recyclerView全清掉
                    Radapter_editingCMD.setHaveSave(true); //因為刪掉正在編輯的，所以不用擔心update問題
                }
                //刪除UserData內對應指令 + 新增進去被刪除指令內
                userData.getUserCommand().remove(adapterPosition);
                userData.getDeletedCMD().add(Radapter_ownCMD.getmData().get(adapterPosition));
                //執行刪除
                Radapter_ownCMD.removeItem(adapterPosition); //設定nowEditingPosition + lastclickposition的值 + 刪除資料
                deletDialog.cancel();
            }
        });
        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //單純關掉dialog
                deletDialog.cancel();
            }
        });
        deletDialog.show();
    }

    private void buildChangeNewCMDdialog(String newCMD) {
        Dialog changeNameDialog=new Dialog(LabActivity.this);
        changeNameDialog.setContentView(R.layout.dialog_lab_change_new_cmd_name);
        changeNameDialog.setCancelable(false);

        Button btnYes=changeNameDialog.findViewById(R.id.button_dialog_lab_changeCMD_yes);
        Button btnNo=changeNameDialog.findViewById(R.id.button_dialog_lab_changeCMD_no);
        EditText editText_inputName=changeNameDialog.findViewById(R.id.editText_dialog_lab_changeName_input);
        editText_inputName.setText(newCMD); //把它設成剛剛收到的新指令名

        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String temp=editText_inputName.getText().toString();
                if(temp.isEmpty()){
                    editText_inputName.setError("需要輸入指令名喔");
                    editText_inputName.requestFocus();
                }
                else{
                    //將temp = 新指令名 加入OwnCMD的最後一個
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Radapter_ownCMD.addItem(temp);
                            recyclerView_ownCMD.scrollToPosition(Radapter_ownCMD.getItemCount()-1);
                            btnCreate.setText("創立新指令");
                            btnCreate.setEnabled(false);
                        }
                    });
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //將新指令加入userData.usercommand
                            ArrayList<String> newCMD=new ArrayList<String>();
                            newCMD.add(temp);
                            if(userData.getDeletedCMD().contains(temp)){
                                //如果新增的指令跟剛剛刪除的一樣，要把刪除array內的拿掉
                                userData.getDeletedCMD().remove(userData.getDeletedCMD().indexOf(temp));
                            }
                            userData.getUserCommand().add(newCMD); //因為直接新增進去userData內，不用擔心指令沒同步，haveSave=true
                            mRobotAPI.startTTS("新增完畢");
                            mTts_complete=false;
                            while(!mTts_complete){}

                            Message msg = new Message();
                            msg.what =2;
                            UIupdateHandler.sendMessage(msg);
                        }
                    }).start();
                    changeNameDialog.cancel();
                }
            }
        });
        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //取消新增
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnCreate.setText("創立新指令");
                        btnCreate.setEnabled(false);
                    }
                });
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mRobotAPI.startTTS("取消新增指令");
                        mTts_complete=false;
                        while(!mTts_complete){}

                        Message msg = new Message();
                        msg.what =2;
                        UIupdateHandler.sendMessage(msg);
                    }
                }).start();
                changeNameDialog.cancel();
            }
        });
        changeNameDialog.show();
    }

    private void buildAndShowConfirmClearEditingCMDdialog() {  //是否要清除編輯區指令的dialog
        Dialog clearEditingCMDdialog=new Dialog(LabActivity.this);
        clearEditingCMDdialog.setContentView(R.layout.dialog_lab_clear_editing_cmd);
        clearEditingCMDdialog.setCancelable(false);

        Button btnYes=clearEditingCMDdialog.findViewById(R.id.button_dialog_lab_clear_editingCMD_yes);
        Button btnNo=clearEditingCMDdialog.findViewById(R.id.button_dialog_lab_clear_editingCMD_no);
        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //清空編輯區
                        Radapter_editingCMD.removeAllItem(1,Radapter_editingCMD.getItemCount());
                    }
                });
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mRobotAPI.startTTS("確認清空編輯區");
                        mTts_complete=false;
                        while(!mTts_complete){}

                        Message msg = new Message();
                        msg.what =2;
                        UIupdateHandler.sendMessage(msg);
                    }
                }).start();
                clearEditingCMDdialog.cancel();
            }
        });
        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mRobotAPI.startTTS("取消清除");
                        mTts_complete=false;
                        while(!mTts_complete){}

                        Message msg = new Message();
                        msg.what =2;
                        UIupdateHandler.sendMessage(msg);
                    }
                }).start();
                clearEditingCMDdialog.cancel();
            }
        });
        clearEditingCMDdialog.show();
    }

    private void buildAndShowCheckCMDdialog(ArrayList<String> checkCMD) {
        for(int a=0;a<checkCMD.size();a++){
            Log.i("i=="+a,checkCMD.get(a));
        }
        Dialog showCMDdialog=new Dialog(LabActivity.this);
        showCMDdialog.setContentView(R.layout.dialog_lab_show_cmd);
        showCMDdialog.setCancelable(true);

        Button btnClose=showCMDdialog.findViewById(R.id.button_dialog_lab_showCMD_close);
        TextView title=showCMDdialog.findViewById(R.id.textView_dialog_lab_showCMD_title);
        title.setText("正在查看："+checkCMD.get(0));
        RecyclerView recyclerView_showCMD=showCMDdialog.findViewById(R.id.recyclerView_dialog_lab_showCMD);
        // 設置RecyclerView為列表型態
        recyclerView_showCMD.setLayoutManager(new LinearLayoutManager(this));
        // 設置格線
        recyclerView_showCMD.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        // 將資料交給adapter
        RecyclerViewAdapter rvShowCMD = new RecyclerViewAdapter(checkCMD);
        // 設置adapter給recycler_view
        recyclerView_showCMD.setAdapter(rvShowCMD);
        //rvShowCMD.notifyDataSetChanged();

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCMDdialog.cancel();
            }
        });

        showCMDdialog.show();
    }
}