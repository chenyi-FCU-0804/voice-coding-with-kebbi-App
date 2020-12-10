package com.nuwarobotics.example.SimpleDemo.selfMadeObject;

import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

/*
* 用途 ：儲存關卡資料，在挑戰 and 過關時 要更新 challengaeActivity的 View
* 為了取資料方便 ArrayList<String>從0開始，所以 level也從0開始。
* */
public class ChallengeData {
    private int userCMDcount=0;

    private final ArrayList<String> stageName = new ArrayList<String>() {{    //關卡名
        add("教學關卡 1");
        add("教學關卡 2");
        add("第一關 單色收集");
        add("第二關 尋找同花順");
        add("第三關 護航騎士團");
        add("第四關 演算法2，二元搜尋法");
    }};
    private final ArrayList<String> stageQuest= new ArrayList<String>() {{    //關卡說明
        add("試著用左邊你擁有的指令來走到撲克牌在的位置。");
        add("同樣是走到撲克牌上，不過需要用到新的指令喔。");
        add("用指令去收集指定花色的撲克牌。");
        add("同花順是一樣花色的 1 到 K，找齊他們吧。");
        add("找到任 1 張的 Q 後，再去尋找 4 張 J。");
        add("牌面上已有照順序排好的數字牌，幫助凱比按照二元搜尋法搜尋我們指定的數字。");
    }};
    private final ArrayList<String> stageHint= new ArrayList<String>() {{    //關卡提示
        add("數過凱比與撲克牌的距離後決定要用多少指令");
        add("了解左、右轉後應該就能成功了");
        add("一次找一張，慢慢新增指令才不容易搞混喔");
        add("讓場面上不留下任何一張指定的花色的牌");
        add("將任務分成兩部分後會變得比較容易");
        add("搞懂二元的意思的話，很快就能過關了");
    }};

    private ArrayList<String> cmd_canUse = new ArrayList<String>() {{    //可以用的指令，如果有資料庫，會從等級判斷+匯入     add("函示(function)");這個指令應該適用在新增(程式實驗室)那邊才有
        add("前進 (一個格子)");
        add("後退 (一個格子)");
        add("左轉 (90度)");
        add("右轉 (90度)");
        add("迴圈 (重複X次)");  //需要在畫面上輸入重複幾次
        add("結束 (停止接收語音指令)");
        add("執行 (結束+自動執行)"); //會直接結束語音接收，執行btnRun 的 onclick
    }};

    //過關 Motion+TTS
    private final ArrayList<String> stageClearMotion=new ArrayList<String>() {{    //成功motion
        add("000_P4_RPSWin");
        add("888_ML_Musclemuscle_02");
        add("000_TL_Applaud");
    }};
    private final ArrayList<String> stageClearTTS=new ArrayList<String>() {{    //成功TTS
        add("恭喜過關!!!");
        add("真厲害，做的真不錯");
        add("不錯的思考方向喔");
    }};
    //失敗Motion+TTS
    private final ArrayList<String> stageFailMotion=new ArrayList<String>() {{    //失敗motion
        add("666_SA_Shocked");
        add("001_S2_Sad");
        add("888_ML_Struggle_10");
        add("888_ML_Getidea_14");
        add("888_ML_Searching_11");
    }};
    private final ArrayList<String> stageFailTTS=new ArrayList<String>() {{    //失敗TTS
        add("錯了!?在挑戰一次吧");
        add("不用想得太快，給自己多點時間");
        add("還差一點點，真是可惜");
        add("想想看哪裡錯了呢?");
        add("跟別人討論看看吧");
    }};
    //提示 Motion+TTS(因為都一樣所以不用建立)
    private final ArrayList<String> stageHintMotion=new ArrayList<String>() {{    //提示motion
        add("666_PE_PushGlasses");
        add("888_ML_Whisper_01");
        add("666_DA_Think");
    }};

    public ChallengeData(){  //建構子，初始化使用者指令數，在過關新增指令時會用到
        userCMDcount=0;
    }


    public void setUserCMDcount(int CMDcount) {
        userCMDcount=CMDcount;
    }

    public int getUserCMDcount(){
        return userCMDcount;
    }

    public String getStageName(int level){  //初始化 or 過關時回傳新的 關卡名
        if(level>=stageName.size()){
            Log.d("getStageName","out of range");
            return null;
        }
        return stageName.get(level);
    }
    public String getStageQuest(int level){ //回傳關卡任務
        if(level>=stageName.size()){
            Log.d("getStageQuest","out of range");
            return null;
        }
        return stageQuest.get(level);
    }
    public String getStageHint(int level){  //回傳提示
        if(level>=stageName.size()){
            Log.d("getStageHint","out of range");
            return null;
        }
        return stageHint.get(level);
    }
    public int getStageAmount(){
        return stageName.size();
    }

    public ArrayList<String> randomClearMotionTTS(){    //回傳隨機 Clear motion、TTS
        ArrayList<String> temp=new ArrayList<>();
        Random ran = new Random();
        int tempIndex=ran.nextInt(this.stageClearMotion.size());
        temp.add(this.stageClearMotion.get(tempIndex));
        temp.add(this.stageClearTTS.get(tempIndex));

        return temp;
    }
    public ArrayList<String> randomFailMotionTTS(){    //回傳隨機 Fail motion、TTS
        ArrayList<String> temp=new ArrayList<>();
        Random ran = new Random();
        int tempIndex=ran.nextInt(this.stageFailMotion.size());
        temp.add(this.stageFailMotion.get(tempIndex));
        temp.add(this.stageFailTTS.get(tempIndex));

        return temp;
    }
    public String randomHintMotion(){    //回傳隨機 Hint motion
        Random ran = new Random();
        int tempIndex=ran.nextInt(this.stageHintMotion.size());

        return this.stageHintMotion.get(tempIndex);
    }

    public ArrayList<String> getCmdCanUse(int level,ArrayList<String> CMD){  // 初始化 and過關時都要call來更新能用的技能
        ArrayList<String> cmdCanUse=new ArrayList<>();
        if(CMD==null){  //代表尚未初始化過(是App創立時的情況)，需要所有能用的指令
            if(level>-1){  //基本的前進後退
                for(int i=0;i<2;i++){
                    cmdCanUse.add(this.cmd_canUse.get(i));
                }
            }
            if(level>0){  //左右轉指令加入
                for(int i=2;i<4;i++){
                    cmdCanUse.add(this.cmd_canUse.get(i));
                }
            }
            if(level>1){ //迴圈指令加入
                for(int i=4;i<this.cmd_canUse.size()-2;i++){
                    cmdCanUse.add(this.cmd_canUse.get(i));
                }
            }
            cmdCanUse.add(this.cmd_canUse.get(this.cmd_canUse.size()-1));
            cmdCanUse.add(this.cmd_canUse.get(this.cmd_canUse.size()-2));
        }
        else{ //代表初始化過，只須回傳過關新獲得的指令
            if(level==1){  //過了教學1，回傳左右轉
                for(int i=2;i<4;i++){
                    cmdCanUse.add(this.cmd_canUse.get(i));
                }
            }
            else if(level==2){  //過了教學2，給迴圈指令
                for(int i=4;i<this.cmd_canUse.size()-2;i++){
                    cmdCanUse.add(this.cmd_canUse.get(i));
                }
            }
        }
        return cmdCanUse;
    }


}
