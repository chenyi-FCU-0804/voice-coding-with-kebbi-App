package com.nuwarobotics.example.SimpleDemo.selfMadeObject;

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

import java.util.ArrayList;

public class RecyclerViewAdapter_lab_editingCMD extends RecyclerView.Adapter<RecyclerViewAdapter_lab_editingCMD.ViewHolder>{
    private ArrayList<String> mData; //內部指令
    private boolean ItemCanClick;  //recyclerView是否能點擊
    private boolean haveSave; //判斷有沒有存檔，需不需要更新資料的flag，之後離開需要更新UserData + 資料庫


    public RecyclerViewAdapter_lab_editingCMD(ArrayList<String> cmds){
        mData=cmds;
        ItemCanClick=true;
        haveSave=true;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 連結項目布局檔list_item，設定你每一個item的樣式，XML檔
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_view_item_lab_editing_cmd, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter_lab_editingCMD.ViewHolder holder, int position) {
        holder.cmdName.setText(mData.get(position));
        if(position==0){
            holder.cmdName.setTextColor(Color.parseColor("#0A2EF6"));
        }
        else{
            holder.cmdName.setTextColor(Color.parseColor("#000000"));
        }
    }

    public void setItemCanClick(boolean canClick){ //設定是否能點擊Item+按鈕
        this.ItemCanClick=canClick;
    }

    public void setHaveSave(boolean haveSave){
        this.haveSave=haveSave;
    }

    public ArrayList<String> getmData(){
        return mData;
    }

    public boolean getHaveSave(){
        return this.haveSave;
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private Button btnRemove;
        private TextView cmdName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cmdName=(TextView) itemView.findViewById(R.id.textView_recyclerItem_lab_editingCMDname);
            btnRemove=(Button) itemView.findViewById(R.id.button_recyclerItem_lab_editingCMDremove);
            cmdName.setTextColor(Color.parseColor("#0A2EF6"));

            btnRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(ItemCanClick){
                        if(getAdapterPosition()==0){
                            Toast.makeText(view.getContext(),"若要刪除第一項內部指令，請在自己新創的指令欄選擇後刪除。",Toast.LENGTH_LONG).show();
                        }
                        else{
                            removeItem(getAdapterPosition());
                        }
                    }
                }
            });
        }
    }

    public void addItem(String cmd){
        mData.add(cmd);
        notifyItemInserted(getItemCount()-1);
        if(getItemCount()>=2){  //2or以上才算有新增，因為第一個是本來就塞著的自訂指令名
            haveSave=false;
        }
    }

    public void removeAllItem(int start,int end){
        mData.subList(start,end).clear();
        notifyItemRangeRemoved(start,end);
        if(start!=0){  //代表不是刪掉整個指令，有對指令進行改動
            haveSave=false;
        }
    }

    private void removeItem(int adapterPosition) {
        mData.remove(adapterPosition);
        haveSave=false;
        notifyItemRemoved(adapterPosition);
    }

}
