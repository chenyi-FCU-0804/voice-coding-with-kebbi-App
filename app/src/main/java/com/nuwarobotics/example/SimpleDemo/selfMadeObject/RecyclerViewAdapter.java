package com.nuwarobotics.example.SimpleDemo.selfMadeObject;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import com.nuwarobotics.example.R;
/*
* 給RecyclerView用的Adapter，原版
* 用到的 List_item模板 ： R.layout.recycler_view_item
* 一個Item只顯示一串String
* click會出現 指令：指令名
* */
public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    private List<String> mData;

    public RecyclerViewAdapter(List<String> data){
        mData=data;
    }
    //建立ViewHolder
    class ViewHolder extends RecyclerView.ViewHolder{ //一個 Holder 對應到一個 TextView
        //宣告元件
        private TextView txtItem;
        private Button btnDescription;   //按下去會有個說明視窗 還不確定要不要做

        ViewHolder(View itemView){
            super(itemView);
            txtItem=(TextView)itemView.findViewById(R.id.textView_recycleItem1);
            //btn=(Button)itemView.findViewById(R.id.btn_ItemRemove);

            // 點擊項目時，指令：指令名
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(view.getContext(),
                            "指令 ： "+txtItem.getText(),Toast.LENGTH_SHORT).show();
                }
            });
            // 點擊項目中的Button時刪除已排入的語音指令
            /*btnDescription.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //
                }
            });*/
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 連結項目布局檔list_item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_view_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // 設置txtItem要顯示的內容
        int tempP=position+1;
        holder.txtItem.setText("("+ tempP +") "+mData.get(position));
    }

    @Override
    public int getItemCount() { //共幾個 Item
        return mData.size();
    }
}
