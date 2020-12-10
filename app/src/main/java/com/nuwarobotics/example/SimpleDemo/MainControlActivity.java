package com.nuwarobotics.example.SimpleDemo;

import android.app.Dialog;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.nuwarobotics.example.R;
import com.nuwarobotics.example.SimpleDemo.selfMadeObject.UserData;

import java.util.HashMap;
import java.util.Map;

/*
* 用途：主畫面，前往三個主要功能
* 進度：目前只有隨便做3個Button去到三個功能那
* 需要新增登出的Dialog，點確認後回到LoginActivity.java
* */

public class MainControlActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = this.getClass().getSimpleName();
    private Button toChallenge;
    private Button toLab;
    private Button toUserData;
    //firebase連接測試
    private FirebaseFirestore cloudStore = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth;
    private boolean isLogOut=false;
    UserData userData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_control2);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        //取得全域變數userData 並初始化
        userData=(UserData)getApplicationContext();

        initUIcomponent();
    }
    private void initUIcomponent() {
        Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar_mainControl);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("使用者主畫面");
        toChallenge=(Button)findViewById(R.id.button_challenge);
        toChallenge.setOnClickListener(this);
        toLab=(Button)findViewById(R.id.button_lab);
        toLab.setOnClickListener(this);
        toUserData=(Button)findViewById(R.id.button_userData);
        toUserData.setOnClickListener(this);
    }


    @Override
    protected void onPause() {
        super.onPause();
        //Activity是部份可見，但無法使用時，在這裡更新資料庫
        Log.e("onPause","222222"+mAuth.getUid());
        //先更新使用者資料
        cloudStore.collection("User Account").document(mAuth.getUid())
                .set(userData.getUserData())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot userData successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing userData document", e);
                    }
                });
        for(int i=0;i<userData.getUserCommand().size();i++){
            Log.d(TAG, "指令名=="+userData.getUserCommand().get(i).get(0));
            //在每個ID下開一個subCollection，userCMD + 用指令的名字userData.getUserCommand().get(i).get(0)各自去開一個document，將單個指令存入
            Map<String,Object> cmdMap=new HashMap<>();
            cmdMap.put("commands",userData.getUserCommand().get(i));
            cloudStore.collection("User Account").document(mAuth.getUid()).collection("userCMD").document(userData.getUserCommand().get(i).get(0))
                    .set(cmdMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot userData's CMDS successfully written!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error writing userData's CMDS document", e);
                        }
                    });
        }
        //最後再將在labActivity內被砍掉的指令們(存在一個array內)把刪除的動作同步到firebase上
        for(int i=0;i<userData.getDeletedCMD().size();i++){
            cloudStore.collection("User Account").document(mAuth.getUid()).collection("userCMD").document(userData.getDeletedCMD().get(i))
                    .delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully deleted!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error deleting document", e);
                        }
                    });
        }
        userData.getDeletedCMD().clear(); //每次刪除完都清空arrayList
        if(isLogOut){  //如果真的點擊登出按鈕才登出，登出要在onPause做才能在回到LoginActivity的updateUI部分前將使用者登出，不然會登出又登入。
            //清空使用者指令 + replace userdata內的map的資料都洗掉
            userData.getUserData().replace("userName","");
            userData.getUserData().replace("email", "");
            userData.getUserData().replace("Password", "");
            userData.getUserData().replace("challengeLV",0);
            userData.getUserCommand().clear();
            mAuth.signOut();
        }
    }

    @Override
    public void onClick(View view) {  //對應三的功能按鈕去到不同的Activity
        Class clazz=null;
        switch (view.getId()){
            case R.id.button_challenge:
                clazz=ChallengeActivity.class;
                break;
            case R.id.button_lab:
                clazz=LabActivity.class;
                break;
            case R.id.button_userData:
                clazz=ShowUserDataActivity.class;
                break;
            default:
                Log.d(TAG, "Can't find the clicking action of view!!!");
        }
        if(clazz!=null){
            Intent intent=new Intent(this,clazz);
            startActivity(intent);
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {  //點到返回鍵處理 這邊點到要談出一個確定要登出的視窗
        switch (item.getItemId()) {
            case android.R.id.home:
                //show 是否要登出的Dialog
                showLogOutDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogOutDialog() { //show 是否要登出的Dialog
        Dialog dialog=new Dialog(MainControlActivity.this);
        dialog.setContentView(R.layout.dialog_log_out);
        dialog.setCancelable(true);

        Button yes=dialog.findViewById(R.id.button_dialog_logoutYes);
        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isLogOut=true;  //確認onPause時進行登出
                dialog.cancel();
                finish();
            }
        });
        Button no=dialog.findViewById(R.id.button_dialog_logoutNo);
        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
        dialog.show();
    }
}