package com.nuwarobotics.example.SimpleDemo.selfMadeObject;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.nuwarobotics.example.R;

import java.util.List;
/*
* 給RecyclerView用的Adapter，有刪除按鈕版
* 用到的 List_item模板 ： R.layout.recycler_view_item_with_remove
* 一個View內有 一個TextView+ Button
* click item=>>一樣顯示第幾個+指令名
* click 刪除按鈕>>刪除掉那個 View*/
public class RecyclerViewAdapter_btnRemove extends RecyclerView.Adapter<RecyclerViewAdapter_btnRemove.ViewHolder>{
    private List<String> mData;

    public RecyclerViewAdapter_btnRemove(List<String> data){
        mData=data;
    }
    //建立ViewHolder
    class ViewHolder extends RecyclerView.ViewHolder{ //一個 Holder 對應到一個 TextView
        //宣告元件
        private TextView txtItem;
        private Button btnRemove;

        ViewHolder(View itemView){
            super(itemView);
            txtItem=(TextView)itemView.findViewById(R.id.textView_recycleItem2);
            btnRemove=(Button)itemView.findViewById(R.id.btn_ItemRemove);

            // 點擊項目時
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int tempP=1+getAdapterPosition();
                    Toast.makeText(view.getContext(),
                            "點擊了第 "+tempP+" 個指令 ： "+txtItem.getText(),Toast.LENGTH_SHORT).show();
                }
            });
            // 點擊項目中的Button時刪除已排入的語音指令
            btnRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    removeItem(getAdapterPosition());
                }
            });
        }
    }
    // 刪除項目
    public void removeItem(int position){
        mData.remove(position);
        notifyItemRemoved(position);
    }
    //清空整個 List+ RecyclerView
    public void removeAllItem(int startPosition,int itemCount){
        mData.clear();
        notifyItemRangeRemoved(startPosition,itemCount);
    }
    // 新增項目
    public void addItem(String text,int position) {
        // 開頭為0
        if(position<0){  //加到最後一個
            mData.add(text);
            notifyItemInserted(mData.size());
        }
        else{ //插入到 position個
            mData.add(position,text);
            notifyItemInserted(position);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 連結項目布局檔list_item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_view_item_with_remove, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // 設置txtItem要顯示的內容
        holder.txtItem.setText(mData.get(position));
    }

    @Override
    public int getItemCount() { //共幾個 Item
        return mData.size();
    }
}
