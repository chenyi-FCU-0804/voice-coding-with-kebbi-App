package com.nuwarobotics.example.SimpleDemo;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

import com.nuwarobotics.example.R;
import com.nuwarobotics.example.SimpleDemo.selfMadeObject.UserData;

public class ShowUserDataActivity extends AppCompatActivity {
    UserData userData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_user_data);
        userData=(UserData)getApplicationContext();

        initUIcomponent();
    }

    private void initUIcomponent() {
        Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar_showData);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("使用者資料");
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
}