package com.nuwarobotics.example.SimpleDemo.selfMadeObject;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.nuwarobotics.example.R;

import java.util.ArrayList;

/*目前問題：高級指令的內部指令存取還沒實作 + recycler_view_advanced_command 要增加編輯與刪除整個高級指令的button + 高級指令實際功能實作 。
* 用途：給RecyclerView用的Adapter，有刪除按鈕版，為hasUseCmd的第1層，內部可以包高級指令(需新增AdvanceCMDAdapter)。
*       目前打算只做三層(可供高級指令內放高級指令)
* 用到的 List_item模板 ： R.layout.recycler_view_item_with_remove
* click item=>>一樣顯示第幾個+指令名
* click 刪除按鈕>>刪除掉那個 View
* 未完成：***
* */
public class RecyclerViewAdapter_btnRemove extends RecyclerView.Adapter{
    private final static int VIEWTYPE_commonCMD = 1;
    private final static int VIEWTYPE_AdvanceCMD = 2;

    private AdvanceCmdData advanceCmdData; //指令(基礎+進階)的儲存class
    private ArrayList<AdvanceCMDAdapter_layer2> advanceCMDAdapters_Layer2;  //負責裝管理底下recyclerView的adapter，一個高級指令一個，跟指令數同樣大小(較好存取)，低級放null、高級放AdavanceCMDAdapter_layer2
    private boolean hasConstructedAdvanceAdapter; //用來確認進階指令的adapter是否建立完成，以免取錯adapter

    //初始化時，沒有丟進過任何指令
    public RecyclerViewAdapter_btnRemove(Activity activity, ArrayList<String> data,ArrayList<Integer> itemsType, boolean btnCanClick){
        //要操作+紀錄的參數一定要在建構子初始化，不然去存取會crash
        advanceCmdData=new AdvanceCmdData();
        this.advanceCmdData.setActivity(activity);
        this.advanceCmdData.setmData(data);
        this.advanceCmdData.setItemsType(itemsType);
        this.advanceCmdData.setBtnRemoveCanClick(btnCanClick);
        advanceCMDAdapters_Layer2 =new ArrayList<AdvanceCMDAdapter_layer2>();
        hasConstructedAdvanceAdapter=false;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 創建一個item對應到的一個ViewHolder
        //因為有多種view所以需要辨別型態
        if (viewType == VIEWTYPE_commonCMD) { //使用第一層的普通指令member layout
            View view  = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_item_with_remove, parent, false);
            return new basicCmdViewHolder(view);
        }
        else{   //使用第一層進階指令layout
            View view  = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_item_advanced_command_layer1, parent, false);
            return new advanceCmdViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {  //這個function會被call多次，畫面上只會出現固定數量的View，滑到新的地方時，他就要回收一些View + 用滑到的地方的資料來bind到View上
        // 設定ViewHolder的內容 +紀錄高級指令holder的型態
        //還沒有被show出來的ViewHolder不會被創立
        int type=getItemViewType(position); //先拿到型態
        Log.e("onbind position=",String.valueOf(position) + "，type = "+type);  //test
        if (type==VIEWTYPE_commonCMD) {
            ((basicCmdViewHolder) holder).txtItem.setText(advanceCmdData.getmData().get(position));
            hasConstructedAdvanceAdapter=true;  //確認新增完adapter，因為不用更改，所以直接set true
            Log.i("new 1","111111");  //test
        }
        else if (type==VIEWTYPE_AdvanceCMD) {
            ((advanceCmdViewHolder) holder).cmdName.setText((advanceCmdData.getmData().get(position))); //把textView設成 高階指令名再開始新增低階指令的RecyclerView

            //初始化Adapter內部的AdvanceCmdData資料
            AdvanceCMDAdapter_layer2 temp_advanceCMDAdapterLayer;
            //如果是null才要重新生成，不然保留原本的Adapter
            if(advanceCMDAdapters_Layer2.get(position)==null){
                AdvanceCmdData temp_advanceCmdData=new AdvanceCmdData();
                temp_advanceCmdData.setActivity(advanceCmdData.getActivity());
                if(!advanceCmdData.getBtnRemoveCanClick()){
                    //如果刪除鍵不能點 >>disable刪除鍵 + advanceCmdData2的boolean也要設成false
                    temp_advanceCmdData.setBtnRemoveCanClick(false);
                }
                //初始化 管理高階指令內部的低階指令的adapter
                temp_advanceCMDAdapterLayer =new AdvanceCMDAdapter_layer2(temp_advanceCmdData);
                //把adapter存起來，之後要操作底下的recyclerView(add item、remove item、remove all item)
                advanceCMDAdapters_Layer2.set(position, temp_advanceCMDAdapterLayer);
            }
            else{  //使用原本就有的Adapter
                temp_advanceCMDAdapterLayer = advanceCMDAdapters_Layer2.get(position);
            }
            hasConstructedAdvanceAdapter=true; //確認新增完Adapter
            Log.i("new 2","222222");   //test
            // 初始化layout manager
            LinearLayoutManager layoutManagerMember=new LinearLayoutManager(advanceCmdData.getActivity());
            //Set layout manager + adapter
            ((advanceCmdViewHolder) holder).advanceRecyclerView.setLayoutManager(layoutManagerMember);
            ((advanceCmdViewHolder) holder).advanceRecyclerView.setAdapter(temp_advanceCMDAdapterLayer);
        }
    }
    // 刪除項目
    public void removeItem(int position){
        Log.e(String.valueOf(advanceCmdData.getmData().get(position))+"  總指令數："+getItemCount(),"刪除位置："+position);  //test
        advanceCmdData.getItemsType().remove(position);
        advanceCmdData.getmData().remove(position);
        advanceCMDAdapters_Layer2.remove(position);
        for(int i = 0; i< advanceCMDAdapters_Layer2.size(); i++){   //test
            Log.e("advanceCmdData.getItemsType",String.valueOf(advanceCmdData.getItemsType().get(i)));
            Log.e("dvanceCmdData.getmData()",advanceCmdData.getmData().get(i));
            if(advanceCMDAdapters_Layer2.get(i)==null){
                Log.e("advanceCMDAdapters_Layer2","NULL");
            }
            else{
                Log.e("advanceCMDAdapters_Layer2","有東西");
            }
        }   //test
        notifyItemRemoved(position);
    }
    //清空整個 List+ RecyclerView
    public void removeAllItem(int startPosition,int itemCount){
        advanceCmdData.getItemsType().clear();
        advanceCmdData.getmData().clear();
        advanceCMDAdapters_Layer2.clear();
        notifyItemRangeRemoved(startPosition,itemCount);
    }
    // 新增項目
    public void addItem(String text,int position,int cmdType) {
        // 開頭為0
        hasConstructedAdvanceAdapter=false;
        advanceCmdData.getItemsType().add(position,cmdType);
        advanceCmdData.getmData().add(position,text);
        advanceCMDAdapters_Layer2.add(position,null); //預設是低階指令，不需要Adapter來管理，放null進去，如果是高階之後再加
        notifyItemInserted(position);
        Log.e(String.valueOf(getItemCount()),"RV count 1 ");   //test
    }

    @Override
    public int getItemCount() { //共幾個指令(進階指令內部不算，只算for、while等標題)
        return advanceCmdData.getmData().size();
    }

    @Override
    public int getItemViewType(int position) {  //靠建立的時後留下來的 type list決定是哪種是初階還是進階指令
        //滑動RecyclerView會呼叫很多次，不過已經可以回傳正確型態
        return advanceCmdData.getItemsType().get(position);
    }

    //一個高級指令會生成一個Adapter，return管理他們的Adapter + 由外部 get Adapter or size的管道
    public ArrayList<AdvanceCMDAdapter_layer2> getAdvanceCMDAdapters_layer2() {
        return advanceCMDAdapters_Layer2;
    }
    //用來取Adapter內部指令
    public ArrayList<String> getAdvanceCmdData_mData(){
        return this.advanceCmdData.getmData();
    }

    public boolean getAcvanceCmdData_BtnRemoveCanClick(){ //看現在按鈕能不能點，為了作為左上角返回鍵是否觸發的判斷而去取的值
        return this.advanceCmdData.getBtnRemoveCanClick();
    }

    public boolean gethasConstructedAdvanceAdapter(){  //回傳是否完成建立Adapter
        return this.hasConstructedAdvanceAdapter;
    }

    public void setBtnRemoveEnable(boolean canClick){  //把btnRemove的onclick關掉or打開
        advanceCmdData.setBtnRemoveCanClick(canClick);
        for(int i = 0; i< advanceCMDAdapters_Layer2.size(); i++){
            if(advanceCMDAdapters_Layer2.get(i)!=null){
                advanceCMDAdapters_Layer2.get(i).setBtnRemoveEnable(canClick);
            }
        }
    }
    //建立ViewHolder
    public class basicCmdViewHolder extends RecyclerView.ViewHolder{ //一個 Holder 對應到一個 TextView
        //宣告元件
        private TextView txtItem;
        private Button btnRemove;

        basicCmdViewHolder(View itemView){
            super(itemView);
            txtItem=(TextView)itemView.findViewById(R.id.textView_recycleItem2);
            btnRemove=(Button)itemView.findViewById(R.id.button_recyclerView_remove);

            // 點擊項目時
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int tempP=1+getAdapterPosition();
                    Toast.makeText(view.getContext(), "點擊了第 "+tempP+" 個指令 ： "+txtItem.getText(),Toast.LENGTH_SHORT).show();
                }
            });
            // 點擊項目中的Button時刪除已排入的語音指令
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
    public class advanceCmdViewHolder extends RecyclerView.ViewHolder{ //一個 Holder 對應到一個 TextView
        //宣告元件
        private TextView cmdName;
        private RecyclerView advanceRecyclerView;
        private Button btnRemove;

        advanceCmdViewHolder(View itemView){
            super(itemView);
            cmdName=(TextView)itemView.findViewById(R.id.textView_advancedCmd_layer1_Name);
            advanceRecyclerView=(RecyclerView)itemView.findViewById(R.id.RecyclerView_AdvanceCMD_layer1);
            btnRemove=(Button)itemView.findViewById(R.id.button_advancedCmd_layer1_remove);

            //點擊項目時
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int tempP=1+getAdapterPosition();
                    Toast.makeText(view.getContext(),
                            "點擊了第 "+tempP+" 個指令 ： "+cmdName.getText(),Toast.LENGTH_SHORT).show();
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
