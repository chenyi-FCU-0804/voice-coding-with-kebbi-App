package com.nuwarobotics.example.SimpleDemo.selfMadeObject;

import android.app.Application;

import com.google.firebase.firestore.auth.User;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
/* 因為很多地方都會用到，所以乾脆就用成全域變數吧，http://learnexp.tw/%E3%80%90android%E3%80%91global-variable-%E5%85%B1%E7%94%A8%E8%AE%8A%E6%95%B8-%E5%BE%9E-3%E5%88%B04/
* 使用者的資料的Object存取儲存都在這
* 註冊好的新帳號是由三個參數的建構子產生，已經註冊完+登入的會從firebase的cloudstore拿完整5個參數的建構子建立
* */
public class UserData extends Application {  //extends Application>>用成全域變數
    private Map<String, Object> user = new HashMap<>();
    //自訂指令，不會在創建時放入Map user內一起上傳，只有在使用者獲得新指令or自創後才會在userdata 下創立一個新的collection(User command)，每個document名都是自訂指令名，document將內部指令放在陣列內。
    private ArrayList<ArrayList<String>> userCommand; //內部的ArrayList的第一個指令都是自訂指令名
    private ArrayList<String> deletedCMDname;


    public UserData(String userName,String password,String email,int challengeLV,ArrayList<ArrayList<String>> userCommand){
        user.put("userName", userName);  //使用者名稱
        user.put("email", email); //註冊的email
        user.put("Password", password); //密碼
        user.put("challengeLV",challengeLV);  //等級、破關進度
        this.userCommand=userCommand;  //自訂指令
        this.deletedCMDname=new ArrayList<>();
    }
    public UserData(String userName,String password,String email){  //新帳號沒有等級傳入 LV=0 ， userCommand也是空的ArrayList<Map<String, Object>>
        user.put("userName", userName);  //使用者名稱
        user.put("email", email); //註冊的email
        user.put("Password", password); //密碼
        user.put("challengeLV",0);  //等級、破關進度
        //不會在創立時放入自訂指令，等用到再上傳
        userCommand=new ArrayList<ArrayList<String>>();
        this.deletedCMDname=new ArrayList<>();
    }
    public UserData(){  //沒有資料傳入時，全域變數都會先生成一個空的，之後在set資料進去
        user.put("userName", "");  //使用者名稱
        user.put("email", ""); //註冊的email
        user.put("Password", ""); //密碼
        user.put("challengeLV",0);  //等級、破關進度
        this.userCommand=new ArrayList<ArrayList<String>>();;  //自訂指令
        this.deletedCMDname=new ArrayList<>();
    }

    public Map<String,Object> getUserData(){
        return this.user;
    }

    public Object getMapKey(String key){  //取key太麻煩創一個用
        Object temp=key;
        return temp;
    }

    public ArrayList<String> getDeletedCMD(){
        return this.deletedCMDname;
    }

    public ArrayList<ArrayList<String>> getUserCommand(){
        return this.userCommand;
    }

    public void updateChallengeLV(int Lv){
        this.user.replace("challengeLV",Lv);
    }

}
