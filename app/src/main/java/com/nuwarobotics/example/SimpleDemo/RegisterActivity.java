package com.nuwarobotics.example.SimpleDemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nuwarobotics.example.R;
import com.nuwarobotics.example.SimpleDemo.selfMadeObject.UserData;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/* 進度：製作完成，完成用email+password新增帳號
* 註冊頁面，需連接FireBase檢查是否有重複帳號，這部分firebase會做
* 帳號、密碼只要是英數皆可
*
* */
public class RegisterActivity extends AppCompatActivity implements View.OnClickListener{
    private Button btn_fin ,btn_cancel;
    private EditText editText_userName ,editText_password ,editText_email;
    private ProgressBar progressBar;
    //FireBase註冊帳號
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        //初始化View
        Toolbar toolbar=findViewById(R.id.toolbar_register);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);  //設定返回鍵
        actionBar.setTitle("帳號註冊頁面");

        btn_fin=findViewById(R.id.button_registerFIN);
        btn_fin.setOnClickListener(this);
        btn_cancel=findViewById(R.id.button_registerCancel);
        btn_cancel.setOnClickListener(this);
        editText_userName=findViewById(R.id.editText_registerName);
        editText_password=findViewById(R.id.editText_registerPassword);
        editText_email=findViewById(R.id.editText_registerEmailAddress);
        progressBar=findViewById(R.id.progressBar_register);
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

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.button_registerCancel:
                finish();
                break;
            case R.id.button_registerFIN:
                registerUser();
                break;
        }
    }

    private void registerUser(){   //點擊註冊後處理
        String userName=editText_userName.getText().toString().trim();
        String password=editText_password.getText().toString().trim();
        String email=editText_email.getText().toString().trim();
        if(userName.isEmpty()){
            editText_userName.setError("必須輸入使用者名稱");
            editText_userName.requestFocus();
            return;
        }
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
        if(password.length()<6){ //  firebase不接受長度低於6的密碼
            editText_password.setError("密碼長度需大於等於(>=)6");
            editText_password.requestFocus();
            return;
        }
        progressBar.setVisibility(View.VISIBLE); //讓隱藏的progressBar show出來
        //註冊成功後會幫使用者登入，使用getCurrentUser 來取得使用者資料
        mAuth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(this,new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){  //如果註冊成功
                            FirebaseUser currUser=mAuth.getCurrentUser(); //取得現在使用者
                            UserData userData=(UserData)getApplicationContext(); //call全域變數UserData
                            userData.getUserData().replace("userName",userName);
                            userData.getUserData().replace("email",email);
                            userData.getUserData().replace("Password",password);
                            //同步在資料庫新增使用者，把使用者物件(必須要為Hash Map)送進資料庫
                            FirebaseFirestore.getInstance().collection("User Account").document(currUser.getUid())  //取得FireBase連結並去到 Users目錄(底下放使用者資料)，一個document=使用者的ID
                                    .set(userData.getUserData())
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d("註冊後資料新增成功", "DocumentSnapshot successfully written!");
                                            progressBar.setVisibility(View.GONE);
                                            showResultDialog(true);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w("註冊後資料新增失敗", "Error adding document", e);
                                            progressBar.setVisibility(View.GONE);
                                            showResultDialog(false);
                                        }
                                    });
                        }
                        else{ //如果註冊失敗
                            Toast.makeText(RegisterActivity.this,"註冊失敗，請再試一次!",Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(View.GONE);
                            showResultDialog(false);
                        }
                    }
                });
    }

    private void showResultDialog(boolean registOK) {
        Dialog resultDialog=new Dialog(RegisterActivity.this);
        resultDialog.setContentView(R.layout.dialog_regist_ok_or_not);
        resultDialog.setCancelable(false);

        TextView textViewRegistResult=resultDialog.findViewById(R.id.textView_registResult);
        if(registOK){
            textViewRegistResult.setText("註冊成功");
            textViewRegistResult.setTextColor(0xFFFFFFFF);
        }
        else{
            textViewRegistResult.setText("註冊失敗");
            textViewRegistResult.setTextColor(0xFFEC2222);
        }
        Button btn_close=resultDialog.findViewById(R.id.button_dialogRegistClose);
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(registOK){  //成功>>關掉dialog + 回到登入畫面，因為已經登入了，所以在LoginActivity的onStart就會幫我們自動切到MainControlActivity
                    resultDialog.cancel();
                    finish();
                }
                else{ //留在同一個畫面
                    resultDialog.cancel();
                }
            }
        });
        resultDialog.show();
    }
}