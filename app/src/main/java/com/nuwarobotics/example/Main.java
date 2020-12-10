package com.nuwarobotics.example;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.nuwarobotics.example.util.AIPermissionRequest;
import com.nuwarobotics.example.util.ActivityBean;
import com.nuwarobotics.example.util.ActivityUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Main extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private static final String TAG = "Main";
    private AIPermissionRequest mPermissionRequest;   // util package的權限物件

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);  //SDK最開始的畫面，他的工具列可以返回桌面(左上角的<=)

        //Grant准許、發放 permission
        mPermissionRequest = new AIPermissionRequest();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestMulti();   //這邊是確認每個會用到的權限是否被批准，如果沒有就需要 call requestMulti()這個 function。
            Log.d(TAG, "request all needed　permissions");
        }


        ListView listView = (ListView) findViewById(R.id.activity_list);
        ArrayList<HashMap<String, Object>> listItems = new ArrayList<HashMap<String, Object>>();   // <~>==<HashMap<String, Object>> ，跟左邊裝一樣的東西

        HashMap<String, Object> item = new HashMap<String, Object>();   // HashMap的 key =String型態，value=Object型態

        //Fetch activity list
        ArrayList<ActivityBean> activityArrayList = ActivityUtils.pullXML(getApplicationContext(), "cfg_functions.xml");    //ActivityBean 是 util package內的一個class，把原先再 assets > cfg_functions.xml 內寫好的 Activity的名字跟 Label拿出來。

        for (ActivityBean activityBean : activityArrayList) {  //從activityBean拿出 Label + Name 放入itme 再加入 listItems。
            item = new HashMap<>();
            try {
                item.put("activity_name", activityBean.getLabel());
                item.put("activity_class", Class.forName(getPackageName() + "." + activityBean.getName()));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            listItems.add(item);
        }

        SimpleAdapter adapter = new SimpleAdapter(this, listItems, R.layout.list_item,
                new String[]{"activity_name"}, new int[]{R.id.text_item}); //參數： 1. 執行環境 2.帶入資料 3.哪一個layout 4. 帶入資料的 key名 (item.put("activity_name", activityBean.getLabel());) 5.對應的View的ID

        listView.setAdapter(adapter);
        listView.setDividerHeight(2);

        listView.setOnItemClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {  //點到凱比的 home鍵
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Map<?, ?> map = (HashMap<?, ?>) parent.getAdapter().getItem(position);   //從 Adapter的Item 的對應 postion抓出資料
        Class<?> clazz = (Class<?>) map.get("activity_class");  // 再去取 value=Activity名

        Intent it = new Intent(this, clazz);
        this.startActivity(it);    //把畫面切到點到的 Activity。

    }

    @Override
    public void onBackPressed() {   //按下返回鍵
        finish();
        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public void requestMulti() {
        mPermissionRequest.requestMultiPermissions(this, mPermissionGrant);
    }

    private AIPermissionRequest.PermissionGrant mPermissionGrant = new AIPermissionRequest.PermissionGrant() {   //這是AIPermissionRequest的一個介面：不同物件用不同方法實作名字一樣的functions
        @Override
        public void onPermissionGranted(int requestCode) {  //照 request代碼來決定
            switch (requestCode) {
                case AIPermissionRequest.CODE_READ_PHONE_STATE:
                    Toast.makeText(Main.this, "Result Permission Grant CODE_READ_PHONE_STATE", Toast.LENGTH_SHORT).show();
                    break;
                case AIPermissionRequest.CODE_RECORD_AUDIO:
                    Toast.makeText(Main.this, "Result Permission Grant CODE_RECORD_AUDIO", Toast.LENGTH_SHORT).show();
                    break;
                case AIPermissionRequest.CODE_READ_EXTERNAL_STORAGE:
                    Toast.makeText(Main.this, "Result Permission Grant CODE_READ_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show();
                    break;
                case AIPermissionRequest.CODE_WRITE_EXTERNAL_STORAGE:
                    Toast.makeText(Main.this, "Result Permission Grant CODE_WRITE_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

}
