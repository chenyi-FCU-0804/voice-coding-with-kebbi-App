package com.nuwarobotics.example.SimpleDemo.selfMadeObject;

import android.app.Activity;

import java.util.ArrayList;

public class AdvanceCmdData {
    private Activity activity;  //給內部Adapter初始化時要用的
    private ArrayList<String> mData;  //紀錄指令名，不包含高級指令內部指令
    private ArrayList<Integer> itemsType;   //在創立的時候紀錄每個item的型態，之後更新UI會用到
    private boolean btnRemoveCanClick;   //disable 刪除按鈕
    private int loopTimes;   //執行次數，只有迴圈需要更改

    public AdvanceCmdData(AdvanceCmdData cmdData){
        this.activity=cmdData.activity;
        this.mData=cmdData.mData;
        this.itemsType=cmdData.itemsType;
        this.btnRemoveCanClick=cmdData.btnRemoveCanClick;
        this.loopTimes=cmdData.loopTimes;
    }
    public AdvanceCmdData(){
        this.activity=null;
        this.mData=new ArrayList<String>();
        this.itemsType=new ArrayList<Integer>();
        this.btnRemoveCanClick=true;
        this.loopTimes=1;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void setmData(ArrayList<String> mData) {
        this.mData = mData;
    }

    public void setItemsType(ArrayList<Integer> itemsType) {
        this.itemsType = itemsType;
    }

    public void setBtnRemoveCanClick(boolean btnRemoveCanClick) {
        this.btnRemoveCanClick = btnRemoveCanClick;
    }

    public void setLoopTimes(int times){
        this.loopTimes=times;
    }

    public Activity getActivity() {
        return activity;
    }

    public ArrayList<String> getmData() {
        return mData;
    }

    public ArrayList<Integer> getItemsType() {
        return itemsType;
    }

    public boolean getBtnRemoveCanClick(){
        return this.btnRemoveCanClick;
    }

    public int getLoopTimes(){
        return this.loopTimes;
    }
}
