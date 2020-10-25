package com.nuwarobotics.example.SimpleDemo;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nuwarobotics.example.R;
/*目前進度：因為資料庫部分尚未完成，所以使用寫死的帳號密碼來登入。
    帳號：k
    密碼：1
    作為登入的介面負責連接資料庫核對帳號+新增新註冊的帳號。
    註冊部分也以Toast來表示。
*/
public class LoginActivity extends AppCompatActivity {
    //宣告layout上的View物件們
    private Button btn_login;
    private Button btn_register;
    private EditText editText_account;
    private EditText editText_password;
    private TextView textView_failHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {  //以後應該會在 onCreate時呼叫一個資料庫連接function在進行登入判斷。
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //在initUI之前需要先連結資料庫，不然要是先觸發了Button的事件會出問題
        initUIcomponent(); //初始化UI物件

    }

    private void initUIcomponent(){
        Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar_logIn);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);  //設定返回鍵
        actionBar.setTitle("Login page of using mouth to CODE :D");

        btn_login=(Button)findViewById(R.id.button_login);
        btn_login.setOnClickListener(v->{
            loginDataCheck();
        });
        btn_register=(Button)findViewById(R.id.button_regist);
        btn_register.setOnClickListener(v->{
            createNewAccount();
        });
        editText_account=(EditText)findViewById(R.id.editText_account);
        editText_password=(EditText)findViewById(R.id.editTextText_Password);
        textView_failHint=(TextView)findViewById(R.id.textView_failHint);
        textView_failHint.setVisibility(View.INVISIBLE);
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

    private void loginDataCheck(){  //確認帳號密碼
        String account,password;
        account=editText_account.getText().toString();
        password=editText_password.getText().toString();
        if(account.equals("k")&&password.equals("1")){
            Toast.makeText(this,"登入成功 :D Welcom Back", Toast.LENGTH_SHORT).show();
            Intent intent=new Intent(this,MainControlActivity.class);
            startActivity(intent);
        }
        else if(account.length()==0 ||password.length()==0){  //若帳號密碼為空 show提醒
            Toast.makeText(this, "帳號 or 密碼不可以是空白喔 ! !", Toast.LENGTH_SHORT).show();
        }
        else{ //當帳密錯誤時，顯示出錯誤訊息
            textView_failHint.setVisibility(View.VISIBLE);
        }
    }
    private void createNewAccount(){ //創建新帳號
        Toast.makeText(this, "創立帳號頁面Unbuild", Toast.LENGTH_SHORT).show();
    }
}