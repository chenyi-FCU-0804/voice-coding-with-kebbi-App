package com.nuwarobotics.example.SimpleDemo.selfMadeObject;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nuwarobotics.example.R;
import com.nuwarobotics.example.SimpleDemo.LabActivity;

import java.util.ArrayList;

public class RecyclerViewAdapter_lab_ownCMD extends RecyclerView.Adapter<RecyclerViewAdapter_lab_ownCMD.ViewHolder> {
    private ArrayList<String> mData;
    private boolean ItemCanClick;  //recyclerView是否能點擊
    private int nowEditingPosition;  //現在編輯對象，-1等於沒有，不能超過mData.size()-1
    private int lastOnclickPosition;  //最後一個被點擊的對象

    public RecyclerViewAdapter_lab_ownCMD(ArrayList<String> data){
        mData=data;
        ItemCanClick=true;
        nowEditingPosition=-1;
        lastOnclickPosition=-1;
    }

    @NonNull
    @Override
    public RecyclerViewAdapter_lab_ownCMD.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 連結項目布局檔list_item，設定你每一個item的樣式，XML檔
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_view_item_lab_own_cmd, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter_lab_ownCMD.ViewHolder holder, int position) {
        //將View綁上對應position的資料
        holder.cmdName.setText(mData.get(position));
    }
    public void setItemCanClick(boolean canClick){ //設定是否能點擊Item+按鈕
        this.ItemCanClick=canClick;
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public int getNowEditingPosition(){
        return nowEditingPosition;
    }

    public int getLastOnclickPosition(){
        return lastOnclickPosition;
    }

    public boolean getItemCanClick(){
        return this.ItemCanClick;
    }

    public ArrayList<String> getmData(){
        return mData;
    }

    public int updateNowEditingPosition(){ //回傳最後被點到的View的position當作編輯指令的position，LabActivity的ownCMD的編輯按鈕觸發時會call的。
        if(nowEditingPosition!=-1 &&lastOnclickPosition==-1){ //刪除發生後，lastOnclickPosition會變-1
            //nowEditing維持原樣
            return nowEditingPosition;
        }
        else{
            nowEditingPosition=lastOnclickPosition;
            return nowEditingPosition;
        }
    }

    // 建立ViewHolder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // 宣告元件
        private TextView cmdName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cmdName=(TextView)itemView.findViewById(R.id.textView_recyclerItem_lab_ownCMDname);

            itemView.setOnClickListener(new View.OnClickListener() {  //設為最後點擊的位置
                @Override
                public void onClick(View view) {
                    if(ItemCanClick){
                        lastOnclickPosition=getAdapterPosition();
                        Toast.makeText(view.getContext(), "指令： "+mData.get(getAdapterPosition())+" ，位置："+(getAdapterPosition()+1)+"，已設定為最後點擊指令", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    public void addItem(String newCMD){
        mData.add(newCMD);
        notifyItemInserted(getItemCount()-1);
    }
    public void removeItem(int adapterPosition) {
        //從labActivity的ownCMD刪除button的dialog的yes會call
        if(adapterPosition==nowEditingPosition){
            nowEditingPosition=-1;
        }
        else if(adapterPosition<nowEditingPosition){  //如果是前面的指令被砍掉，現在editing的position就要-1
            nowEditingPosition-=1;
        }
        lastOnclickPosition=-1;
        mData.remove(adapterPosition);
        notifyItemRemoved(adapterPosition);
    }

}
