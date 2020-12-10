package com.nuwarobotics.example.SimpleDemo;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.nuwarobotics.example.R;
import com.nuwarobotics.example.SimpleDemo.selfMadeObject.UserData;

import java.util.ArrayList;

/*目前進度：交給Firebase就沒問題啦:D。
    作為登入的介面負責連接資料庫核對帳號+新增新註冊的帳號。
    1.登出，OK。
    2.登入，OK。
    3.註冊(email、沒email)，email OK ，生成10個帳號(不需要)。
    4.無帳號體驗(從0等開始，空的UserData)，還沒設定無帳號體驗的限制。
    5.忘記密碼(還沒做)
    6.同步資料，updateUIandUserData的自訂指令部分，已經可以拿出來了，但還沒裝到userdata.usercommand的Arraylist內。
*/
public class LoginActivity extends AppCompatActivity {
    //宣告layout上的View物件們
    private Button btn_login ,btn_register ,btn_forgotPassword ,btn_playWithoutLogin;
    private EditText editText_email , editText_password;
    private TextView textView_failHint;
    //FireBase登入驗證
    private FirebaseAuth mAuth;  //Firebase的認證API

    @Override
    protected void onCreate(Bundle savedInstanceState) {  //以後應該會在 onCreate時呼叫一個資料庫連接function在進行登入判斷。
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        //在initUI之前需要先連結資料庫，不然要是先觸發了Button的事件會出問題
        initUIcomponent(); //初始化UI物件

    }

    private void initUIcomponent(){
        Toolbar toolbar=findViewById(R.id.toolbar_logIn);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);  //設定返回鍵
        actionBar.setTitle("Login to Use Your Mouth to CODE :D");

        btn_login=findViewById(R.id.button_login); //登入button
        btn_login.setOnClickListener(v->{
            loginDataCheck();
        });
        btn_register=findViewById(R.id.button_regist); //註冊button
        btn_register.setOnClickListener(v->{
            startActivity(new Intent(this, RegisterActivity.class));
        });
        btn_forgotPassword=findViewById(R.id.button_forgetPassword);  //忘記密碼
        btn_forgotPassword.setOnClickListener(v->{
           Toast.makeText(this,"忘記密碼",Toast.LENGTH_SHORT).show();
        });
        btn_playWithoutLogin=findViewById(R.id.button_playWithoutLogin);
        btn_playWithoutLogin.setOnClickListener(v->{
            startActivity(new Intent(this,MainControlActivity.class));
        });

        editText_email=findViewById(R.id.editText_account);  //帳號輸入
        editText_email.setText("");
        editText_password=findViewById(R.id.editTextText_Password); //密碼輸入
        editText_password.setText("");
        textView_failHint=findViewById(R.id.textView_failHint); //錯誤提示
        textView_failHint.setVisibility(View.GONE);
    }
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly. 確認之前是否已經登錄過
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUIandUserData(currentUser);
    }

    private void updateUIandUserData(FirebaseUser currentUser) { //登入後處理
        if(currentUser!=null){   //這裡去User Account 找對應ID的document
            DocumentReference documentReference=FirebaseFirestore.getInstance().collection("User Account").document(currentUser.getUid());
            UserData userData=(UserData) getApplicationContext();
            documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) { //如果document存在
                            Log.d("LoginActivity getData", "DocumentSnapshot data: " + document.getData());
                            for(String key:document.getData().keySet()){
                                userData.getUserData().replace(key , document.getData().get(key));
                            }
//                            for(String key:userData.getUserData().keySet()){  //test
//                                Log.d("userdata"+key,String.valueOf(userData.getUserData().get(key)));
//                            }
                            CollectionReference subCollection=FirebaseFirestore.getInstance().collection("User Account").document(currentUser.getUid()).collection("userCMD");
                            if(subCollection!=null) {
                                subCollection.get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                                        Log.d("LoginActivity getDataCMD", document.getId() + " => " + document.getData());
                                                        for(String key:document.getData().keySet()){
                                                            ArrayList<String> cmd=(ArrayList<String>)document.getData().get(key);
                                                            for(int j=0;j<cmd.size();j++){
                                                                Log.d("j==",cmd.get(j));
                                                            }
                                                            userData.getUserCommand().add(cmd);
                                                        }
                                                    }
                                                } else {
                                                    Log.d("LoginActivity getDataCMD", "Error getting documents: ", task.getException());
                                                }
                                            }
                                        });
                            }
                        }
                        else {
                            Log.d("LoginActivity getData", "No such document");
                        }
                    }
                    else {
                        Log.d("LoginActivity getData", "get failed with ", task.getException());
                    }
                }
            });
            startActivity(new Intent(this,MainControlActivity.class));
            textView_failHint.setVisibility(View.GONE);  //隱藏帳密錯誤hint
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {  //點到返回鍵處理
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loginDataCheck(){  //登入確認
        String email,password;
        email=editText_email.getText().toString();
        password=editText_password.getText().toString();

        if(email.isEmpty()){
            editText_email.setError("必須輸入e-mail");
            editText_email.requestFocus();
            return;
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){ //若不符合 email格式
            editText_email.setError("不符合e-mail格式");
            editText_email.requestFocus();
            return;
        }
        if(password.isEmpty()){
            editText_password.setError("必須輸入密碼");
            editText_password.requestFocus();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d("LoginActivty.class", "signInWithEmail:success");
                            Toast.makeText(LoginActivity.this,"登入成功 :D Welcom Back ", Toast.LENGTH_SHORT).show();
                            updateUIandUserData(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("LoginActivty.class", "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Account Login failed.",
                                    Toast.LENGTH_SHORT).show();
                            textView_failHint.setVisibility(View.VISIBLE);  //當帳密錯誤時，顯示出錯誤訊息
                            updateUIandUserData(null);
                        }
                    }
                });
    }
}