package com.nuwarobotics.example.SimpleDemo.selfMadeObject;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nuwarobotics.example.R;

import java.util.ArrayList;
/*RecyclerViewAdapter_btnRemove內部的recyclerView的Adapter，為hasUseCmd的第2層。
*   經參考過第一層RecyclerViewAdapter_btnRemove後，已經可以新增第三層AdvanceCMDAdapter_layer3的Adapter。
*  */
public class AdvanceCMDAdapter_layer2 extends RecyclerView.Adapter {
    private final static int VIEWTYPE_commonCMD = 1;
    private final static int VIEWTYPE_AdvanceCMD = 2;

    private AdvanceCmdData advanceCmdData;   //負責儲存指令的資訊
    private ArrayList<AdvanceCMDAdapter_layer3> advanceCMDAdapters_Layer3;  //負責裝管理底下recyclerView的adapter，一個高級指令一個，跟指令數同樣大小(較好存取)，低級放null、高級放AdavanceCMDAdapter_layer3
    private boolean hasConstructedAdapter; //用來確認進階指令的adapter是否建立完成，再接續下面執行

    public AdvanceCMDAdapter_layer2(AdvanceCmdData advanceCmdData){
        //在傳入之前會先初始化好 AdvanceCmdData，因為指令都還沒有，所以 mData、itemsType都new一個空的 +通用的 btnRemoveCanClick 跟activity
        this.advanceCmdData=advanceCmdData;
        this.advanceCMDAdapters_Layer3=new ArrayList<AdvanceCMDAdapter_layer3>();
        this.hasConstructedAdapter=false;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //初始化View，因為有高低階指令，所以需要if else判斷
        if (viewType == VIEWTYPE_commonCMD) { //普通指令使用第一層進階指令內的member layout
            View view  = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_item_advanced_command_member_layer1, parent, false);
            return new AdvanceCMDAdapter_layer2.basicCmdViewHolder(view);
        }
        else{ //進階指令使用第二層版本的 advanceCMD
            View view  = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_item_advanced_command_layer2, parent, false);
            return new AdvanceCMDAdapter_layer2.advanceCmdViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // 設定ViewHolder的內容 +紀錄高級指令holder的型態
        //還沒有被show出來的ViewHolder不會被創立
        int type=getItemViewType(position); //先拿到型態
        Log.e("onbind position 222=",String.valueOf(position) + "，type = "+type);  //test

        if (type==VIEWTYPE_commonCMD) {
            ((basicCmdViewHolder) holder).memberName.setText(advanceCmdData.getmData().get(position));
            hasConstructedAdapter=true;  //確認新增完adapter，因為不用更改，所以直接set true
            Log.i("new 1.2","1.22222");  //test
        }
        else if (type==VIEWTYPE_AdvanceCMD) {
            ((advanceCmdViewHolder) holder).cmdName.setText((advanceCmdData.getmData().get(position))); //把textView設成 高階指令名再開始新增低階指令的RecyclerView

            //初始化Adapter內部的AdvanceCmdData資料
            AdvanceCMDAdapter_layer3 temp_advanceCMDAdapterLayer;
            //如果是null才要重新生成，不然保留原本的Adapter
            if(advanceCMDAdapters_Layer3.get(position)==null){
                AdvanceCmdData temp_advanceCmdData=new AdvanceCmdData();
                temp_advanceCmdData.setActivity(advanceCmdData.getActivity());
                if(!advanceCmdData.getBtnRemoveCanClick()){
                    //如果刪除鍵不能點 >>disable刪除鍵 + temp_advanceCmdData的boolean也要設成false
                    temp_advanceCmdData.setBtnRemoveCanClick(false);
                }
                //初始化 管理高階指令內部的低階指令的adapter
                temp_advanceCMDAdapterLayer =new AdvanceCMDAdapter_layer3(temp_advanceCmdData);
                //把adapter存起來，之後要操作底下的recyclerView(add item、remove item、remove all item)
                advanceCMDAdapters_Layer3.set(position, temp_advanceCMDAdapterLayer);
            }
            else{  //使用原本就有的Adapter
                temp_advanceCMDAdapterLayer = advanceCMDAdapters_Layer3.get(position);
            }
            hasConstructedAdapter=true; //確認新增完Adapter
            Log.i("new 2.2","2.22222");   //test
            // 初始化layout manager
            LinearLayoutManager layoutManagerMember=new LinearLayoutManager(advanceCmdData.getActivity());
            //Set layout manager + adapter
            ((advanceCmdViewHolder) holder).advanceRecyclerView.setLayoutManager(layoutManagerMember);
            ((advanceCmdViewHolder) holder).advanceRecyclerView.setAdapter(temp_advanceCMDAdapterLayer);
        }
    }

    @Override
    public int getItemViewType(int position) {  //靠建立的時後留下來的 type list決定是哪種是初階還是進階指令
        //滑動RecyclerView會呼叫很多次，會依照position進去當初建立的type list內回傳指令型態
        return advanceCmdData.getItemsType().get(position);
    }

    @Override //回傳內部指令數
    public int getItemCount() {
        return advanceCmdData.getmData().size();
    }


    // 刪除項目
    public void removeItem(int position){
        advanceCmdData.getItemsType().remove(position);
        advanceCmdData.getmData().remove(position);
        advanceCMDAdapters_Layer3.remove(position);
        notifyItemRemoved(position);
    }
    //沒有一次刪除性的操作，所以沒有removeAllItem()

    // 新增項目
    public void addItem(String text,int position,int cmdType) {
        hasConstructedAdapter=false;
        advanceCmdData.getItemsType().add(position,cmdType);
        advanceCmdData.getmData().add(position,text);
        advanceCMDAdapters_Layer3.add(position,null); //預設是低階指令，不需要Adapter來管理，放null進去，如果是高階之後再設定
        notifyItemInserted(position);
        Log.e(String.valueOf(getItemCount()),"RV count 2 ");   //test
    }

    //設定button remove是否可點擊，也要記得去設定內部第三層的指令
    public void setBtnRemoveEnable(boolean canClick){
        this.advanceCmdData.setBtnRemoveCanClick(canClick);
        for(int i = 0; i< advanceCMDAdapters_Layer3.size(); i++){
            if(advanceCMDAdapters_Layer3.get(i)!=null){
                advanceCMDAdapters_Layer3.get(i).setBtnRemoveEnable(canClick);
            }
        }
    }

    public ArrayList<AdvanceCMDAdapter_layer3> getAdvanceCMDAdapters_layer3() { //取得管理內部高級指令的adapters，非高級指令則=null
        return advanceCMDAdapters_Layer3;
    }

    public AdvanceCmdData getAdvanceCmdData(){ //指令本身資料存放物件
        return this.advanceCmdData;
    }
    //用來取Adapter內部指令
    public ArrayList<String> getAdvanceCmdData_mData(){
        return this.advanceCmdData.getmData();
    }
    //確認是否新增Adapter完成
    public boolean gethasConstructedAdapter(){
        return this.hasConstructedAdapter;
    }


    public class basicCmdViewHolder extends RecyclerView.ViewHolder{
        //初始化member view
        private TextView memberName;
        private Button btnRemove;

        public basicCmdViewHolder(@NonNull View itemView) {
            super(itemView);

            memberName=(TextView)itemView.findViewById(R.id.textView_item_advanced_command_layer1_memberName);
            btnRemove=(Button)itemView.findViewById(R.id.button_item_advanced_command_layer1_removeItem);
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

    public class advanceCmdViewHolder extends RecyclerView.ViewHolder{ //一個 Holder 對應到一個 TextView
        //宣告元件
        private TextView cmdName;
        private RecyclerView advanceRecyclerView;
        private Button btnRemove;

        advanceCmdViewHolder(View itemView){
            super(itemView);
            cmdName=(TextView)itemView.findViewById(R.id.textView_advancedCmd_layer2_Name);
            advanceRecyclerView=(RecyclerView)itemView.findViewById(R.id.RecyclerView_AdvanceCMD_layer2);
            btnRemove=(Button)itemView.findViewById(R.id.button_advancedCmd_layer2_remove);

            //點擊項目時
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int tempP=1+getAdapterPosition();
                    Toast.makeText(view.getContext(),"點擊了第 "+tempP+" 個指令 ： "+cmdName.getText(),Toast.LENGTH_SHORT).show();
                }
            });
            btnRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(advanceCmdData.getBtnRemoveCanClick()){
                        removeItem(getAdapterPosition());
                    }
                }
            });
        }
    }
}
