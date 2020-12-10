package com.nuwarobotics.example.SimpleDemo.selfMadeObject;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nuwarobotics.example.R;

import java.util.ArrayList;
/*負責裝第三層的低階指令，無法再新增高階指令。
* */
public class AdvanceCMDAdapter_layer3 extends RecyclerView.Adapter<AdvanceCMDAdapter_layer3.ViewHolder>{
    private AdvanceCmdData advanceCmdData;  //負責儲存指令的資訊
    private boolean hasConstructedAdapter; //用來確認進階指令的adapter是否建立完成，再接續下面執行

    public AdvanceCMDAdapter_layer3(AdvanceCmdData advanceCmdData){
        //在傳入之前會先初始化好 AdvanceCmdData，因為指令都還沒有，所以 mData、itemsType都new一個空的 +通用的 btnRemoveCanClick 跟activity
        this.advanceCmdData=advanceCmdData;
        this.hasConstructedAdapter=false;
    }

    @NonNull
    @Override
    public AdvanceCMDAdapter_layer3.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //初始化View，使用第二層進階指令內的member layout
        View view= LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_view_item_advanced_command_member_layer2,parent,false);
        return new AdvanceCMDAdapter_layer3.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdvanceCMDAdapter_layer3.ViewHolder holder, int position) {
        //把arrayListMemberCMD設到各個holder的 TextView上
        holder.memberName.setText(advanceCmdData.getmData().get(position));
        hasConstructedAdapter=true;
    }

    @Override //回傳內部指令數
    public int getItemCount() {
        return advanceCmdData.getmData().size();
    }

    // 刪除項目
    public void removeItem(int position){
        advanceCmdData.getItemsType().remove(position);
        advanceCmdData.getmData().remove(position);
        notifyItemRemoved(position);
    }
    // 新增項目
    public void addItem(String text,int position,int cmdType) {
        hasConstructedAdapter=false;
        advanceCmdData.getItemsType().add(position,cmdType);
        advanceCmdData.getmData().add(position,text);
        notifyItemInserted(position);
        Log.e(String.valueOf(getItemCount()),"RV count 3");    //test
    }
    //設定button remove是否可點擊
    public void setBtnRemoveEnable(boolean canClick){
        advanceCmdData.setBtnRemoveCanClick(canClick);
    }

    public AdvanceCmdData getAdvanceCmdData(){
        return this.advanceCmdData;
    }

    //用來取Adapter內部指令
    public ArrayList<String> getAdvanceCmdData_mData(){
        return this.advanceCmdData.getmData();
    }
    //確認是否新增完成
    public boolean gethasConstructedAdapter(){
        return this.hasConstructedAdapter;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        //初始化member view
        private TextView memberName;
        private Button btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            memberName=(TextView)itemView.findViewById(R.id.textView_item_advanced_command_layer2_memberName);
            btnRemove=(Button)itemView.findViewById(R.id.button_item_advanced_command_layer2_removeItem);
            btnRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(advanceCmdData.getBtnRemoveCanClick()){ //true時可以刪除
                        removeItem(getAdapterPosition());
                    }
                }
            });
        }
    }
}
