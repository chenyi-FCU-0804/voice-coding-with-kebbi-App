package com.nuwarobotics.example.motion.demo;

import android.os.Bundle;
import android.widget.Button;
import android.widget.PopupMenu;

import com.nuwarobotics.example.R;
import com.nuwarobotics.example.motion.base.BaseAppCompatActivity;
import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;

import java.util.List;

public class QueryMotionActivity extends BaseAppCompatActivity {
    private NuwaRobotAPI mRobotAPI;
    private IClientId mClientId;
    List<String> mMotionsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Step 1 : Initial Nuwa API Object   初始化API物件
        mClientId = new IClientId(this.getPackageName());
        mRobotAPI = new NuwaRobotAPI(this, mClientId);


        Button btn = findViewById(R.id.btn_query_motions);
            btn.setOnClickListener(v->{

            if(mMotionsList == null) {   //裡面放所有的 Motion指令
                mMotionsList = mRobotAPI.getMotionList();
            }

            PopupMenu popup = new PopupMenu(QueryMotionActivity.this, btn);  //同樣的宣告 popup menu來呈現指令
            popup.getMenuInflater().inflate(R.menu.popup_menu_querymotions, popup.getMenu());
            popup.getMenu().clear();

            //Step2 : Call API to get the list of all motions  指令丟到 Menu內
            for(String item : mMotionsList) {
                popup.getMenu().add(item);
            }

            popup.setOnMenuItemClickListener((item)->{ //設定所有的 menu對應的 Motin
                mRobotAPI.motionStop(true);
                mRobotAPI.motionPlay(item.getTitle().toString(), false);  //沒有臉部呈現
                return true;
            });


            popup.show();

        });
    }

    @Override
    protected void onStop(){
        super.onStop();

        mMotionsList = null;

        if(mRobotAPI != null){
            mRobotAPI.release();
        }
    }

    @Override
    protected int getLayoutRes(){
        return R.layout.activity_querymotion;
    }

    @Override
    protected int getToolBarTitleRes(){
        return R.string.lbl_example_1;
    }
}
