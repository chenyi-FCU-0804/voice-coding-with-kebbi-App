package com.nuwarobotics.example.SimpleDemo;
/*先做一個粗略版的主頁面
*
* */
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.nuwarobotics.example.R;

/*
* 用途：主畫面，前往三個主要功能
* 進度：目前只有隨便做3個Button去到三個功能那
* 我覺得需要在 selfMadeObject package內新增一個負責裝從資料庫那拿來的使用者資料的 class (UserData.class)，不然都要一直存取資料庫，等我們這邊有新東西+更新的時候才去更新資料庫端
* 拿資料的動作放在 onCreate內
* */

public class MainControlActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();
    Button toChallenge;
    Button toLab;
    Button toUserData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_control2);
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
    protected void onDestroy() {//清除拿到的資料
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
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
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}