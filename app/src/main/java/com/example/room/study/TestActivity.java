package com.example.room.study;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.quickaccesswallet.QuickAccessWalletService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.daquexian.flexiblerichtextview.FlexibleRichTextView;
import com.example.room.MainActivity;
import com.example.room.R;
import com.example.room.shop.ShopActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.scilab.forge.jlatexmath.core.AjLatexMath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TestActivity extends AppCompatActivity {
    Chronometer chronometer;
    private RadioGroup mRgOptions;
    private RadioButton mRbOption;
    private Button mBtnSubmit ;
    private Button mBtnGiveUp ;
    private FlexibleRichTextView mFRTvQuestion;
    private FlexibleRichTextView mFRTvOptionA;
    private FlexibleRichTextView mFRTvOptionB;
    private FlexibleRichTextView mFRTvOptionC;
    private FlexibleRichTextView mFRTvOptionD;
    private FlexibleRichTextView mFRTvAnalysis;
    static int appCount ;
    static boolean isRunInBackground;
    private Handler handler = new Handler();
    private int questionStudy ;
    /**
     * 用户退出生成学习报告
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            createStudyReport();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        initBackgroundCallBack();//后台调用

        AjLatexMath.init(this);
        // 如果希望自动识别代码段中的语言以实现高亮  // CodeProcessor.init(this);
        chronometer = findViewById(R.id.chronometer);
        mRgOptions = findViewById(R.id.rg_options);
        mBtnSubmit = findViewById(R.id.btn_submit);
        mBtnGiveUp = findViewById(R.id.btn_give_up);

        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
        //
        mFRTvQuestion = findViewById(R.id.fr_tv_question);
        mFRTvOptionA = findViewById(R.id.fr_tv_optionA);
        mFRTvOptionB = findViewById(R.id.fr_tv_optionB);
        mFRTvOptionC = findViewById(R.id.fr_tv_optionC);
        mFRTvOptionD = findViewById(R.id.fr_tv_optionD);
        mFRTvAnalysis = findViewById(R.id.fr_tv_analysis);
        questionStudy = 0 ;
        startTest();
        sendQuestionContentWithOkHttp();
        // 监听选项
        // 选项按钮
        mRgOptions.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mRbOption = group.findViewById(checkedId);
            }
        });
        // 提交按钮
        mBtnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mRgOptions.getCheckedRadioButtonId() == -1){
                    Toast.makeText(TestActivity.this,"还没选",Toast.LENGTH_SHORT).show();
                }
                else{
                    mBtnSubmit.setEnabled(false);
                    mBtnGiveUp.setEnabled(false);
                    check();
                    mBtnSubmit.setEnabled(true);
                    mBtnGiveUp.setEnabled(true);
                }
            }
        });
        // 放弃按钮
        mBtnGiveUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtnSubmit.setEnabled(false);
                mBtnGiveUp.setEnabled(false);
                showAnalysis();
                sendQuestionResultWithOkHttp(false);
                mBtnSubmit.setEnabled(true);
                mBtnGiveUp.setEnabled(true);
            }

        });
        
    }

    /**
     * 开始测试，
     * 首先给服务器发送一个startChapter信息，
     * 然后返回一个Qid
     * 然后 通过函数parseInitialQId(responseData)对获得返回Qid
     */
    private void startTest(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences userPref = getSharedPreferences("login_state", MODE_PRIVATE);
                    String username = userPref.getString("username", null);

                    SharedPreferences studyPref = getSharedPreferences("study_state",MODE_PRIVATE);
                    int chapter = studyPref.getInt("chapter",0);
                    OkHttpClient client = new OkHttpClient();
                    RequestBody requestBody = new FormBody.Builder()
                            .add("username",username)
                            .add("ChapterID",chapter+"")
                            .add("type","startChapter")
                            .build();

                    Request request = new Request.Builder()
                            .url("http://119.23.237.245/question_request.php")
                            .post(requestBody)
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    // 解析返回json文件获取Qid
                    parseInitialQId(responseData);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

    }
    /**
     * 解析开始测试的第一题序号
     * @param jsonData
     */
    private void parseInitialQId(String jsonData){
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            int QId = jsonObject.getInt("Qid");
            Log.d("TestActivity","Qid is"+String.valueOf(QId));
            SharedPreferences.Editor editor = getSharedPreferences("study_state",MODE_PRIVATE).edit();
            editor.putInt("Qid",QId);
            editor.commit();
        }catch (Exception e){
            e.printStackTrace();
        }

    }
    /**
     * 检查回答的正误
     */
    private void check(){
        String option = mRbOption.getText().toString();
        SharedPreferences pref = getSharedPreferences("study_state",MODE_PRIVATE);
        String answer = pref.getString("QAnswer","");
        boolean result ;
        if (option.equals(answer)){
            Toast.makeText(TestActivity.this,"回答正确",Toast.LENGTH_SHORT).show();
            result = true ;
        }
        else{
            Toast.makeText(TestActivity.this,"回答错误",Toast.LENGTH_SHORT).show();
            result = false ;
        }
        showAnalysis();
        sendQuestionResultWithOkHttp(result);
    }
    /**
     * 向服务器查询Qid对应的题目
     * 发送 Qid以及type"QuestionContent"
     * 接收Qcontent、Qanswer以及Qanalysis
     * 并且更新题目表
     */
    private void sendQuestionContentWithOkHttp(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences userPref = getSharedPreferences("login_state",MODE_PRIVATE);
                    String username = userPref.getString("username", null);

                    SharedPreferences studyPref = getSharedPreferences("study_state",MODE_PRIVATE);
                    int QId = studyPref.getInt("Qid",0);
                    OkHttpClient client = new OkHttpClient();
                    RequestBody requestBody = new FormBody.Builder()
                            .add("username",username)
                            .add("Qid",QId+"")
                            .add("type","QuestionContent")
                            .build();

                    Request request = new Request.Builder()
                            .url("http://119.23.237.245/question_request.php")
                            .post(requestBody)
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    //Log.d("TestActivity","this is  "+responseData);
                    parseQuestionContent(responseData);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

    }

    /**
     * 返回答题结果给服务器
     * @param result
     */
    private void sendQuestionResultWithOkHttp(boolean result){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences userPref = getSharedPreferences("login_state",MODE_PRIVATE);
                    String username = userPref.getString("username", null);

                    SharedPreferences studyPref = getSharedPreferences("study_state",MODE_PRIVATE);
                    int QId = studyPref.getInt("Qid",0);
                    OkHttpClient client = new OkHttpClient();
                    RequestBody requestBody ;
                    if (result){
                        requestBody = new FormBody.Builder()
                                .add("correct","1")
                                .add("Qid",QId+"")
                                .add("type","QuestionResult")
                                .build();
                    }
                    else{
                        requestBody = new FormBody.Builder()
                                .add("username",username)
                                .add("Qid",QId+"")
                                .add("type","QuestionResult")
                                .add("correct","0")
                                .build();
                    }
                    Request request = new Request.Builder()
                            .url("http://119.23.237.245/question_request.php")
                            .post(requestBody)
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    parseQuestionResult(responseData,result);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
    /**
     * 更新题目
     * @param jsonData
     */
    private void parseQuestionContent(String jsonData){
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            String QContent = jsonObject.getString("content");
            String QAnswer = jsonObject.getString("answer");
            String QAnalysis = jsonObject.getString("analysis");
            String QOptionA = "A. "+jsonObject.getString("optionA");
            String QOptionB = "B. "+jsonObject.getString("optionB");
            String QOptionC ;
            String QOptionD ;
            if (jsonObject.getString("optionC")!=""){
                QOptionC = "C. "+jsonObject.getString("optionC");
                QOptionD = "D. "+jsonObject.getString("optionD");
            }
            else {
                QOptionC = "";
                QOptionD = "";
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mFRTvQuestion.setText(QContent);
                    mFRTvOptionA.setText(QOptionA);
                    mFRTvOptionB.setText(QOptionB);
                    mFRTvOptionC.setText(QOptionC);
                    mFRTvOptionD.setText(QOptionD);
                    mFRTvAnalysis.setText("");
                }
            });
            SharedPreferences.Editor editor = getSharedPreferences("study_state",MODE_PRIVATE).edit();
            editor.putString("QAnswer", QAnswer);
            editor.putString("QAnalysis",QAnalysis);
            editor.commit();

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 解析返回的Qid
     * 在本地选择下一道题
     * @param jsonData
     */
    private void parseQuestionResult(String jsonData,boolean result){
        Log.d("TestActivity","jsonData "+jsonData);
        try {
            JSONObject jsonObject =  new JSONObject(jsonData);
            JSONArray jsonArray = new JSONArray(jsonObject.getString("Qid"));
            SharedPreferences pref = getSharedPreferences("login_state",MODE_PRIVATE);
            String fileSavePath = pref.getString("FileSavePath",null);
            int nextQuestion = updateQuestionBank(fileSavePath,jsonArray,result);
            if (nextQuestion!=-1){
                SharedPreferences.Editor editor = getSharedPreferences("study_state",MODE_PRIVATE).edit();
                editor.putInt("Qid",nextQuestion);
                editor.commit();
                sendQuestionContentWithOkHttp();
            }
            Log.d("TestActivity","nextQuestion  "+nextQuestion);
        }catch (Exception e){

        }
    }

    /**
     * 更新题库
     * 返回一道未写过的题
     * @param savePath
     * @param jsonArray
     * @return
     */
    private int updateQuestionBank(String savePath,JSONArray jsonArray,boolean result){
        // 读
        File file = new File(savePath);
        FileInputStream fis = null;
        byte[] bytes = null ;
        try {
            fis  = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int n = 0;
            String s = null;
            while ((n=fis.read(buffer))!=-1){
                s = new String(buffer,0,n);
                // Log.d("TestActivity","bytes read"+s);
            }
            //Log.d("TestActivity","bytes length"+s.length());
            bytes = new byte[s.length()];
            bytes=s.getBytes();

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try{
                fis.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        // 读

        // 写
        if(result==true){
            //SharedPreferences.Editor editor = getSharedPreferences("study_state",MODE_PRIVATE).edit();
            SharedPreferences pref = getSharedPreferences("study_state",MODE_PRIVATE);
            int questionDone = pref.getInt("questionDone",0);
            int Qid = pref.getInt("Qid",0);

            SharedPreferences.Editor editor = getSharedPreferences("study_state",MODE_PRIVATE).edit();
            editor.putInt("questionDone",questionDone+1);
            editor.commit();

            bytes[Qid]=0x01;
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                fos.write(bytes);
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                try {
                    fos.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        // 写

        boolean finished = true ;// 判断所给题是否完成
        try {
            Log.d("TestActivity","judging");
            int index = 0 ;
            //检索返回的Qid表，判断是否完成
            for (int i=0;i<jsonArray.length();i++){
                index = jsonArray.getInt(i);
                if (bytes[index]==0x00){
                    Log.d("TestActivity","index "+index);
                    finished=false;
                    break;
                }
            }

            if (finished&&result==false){
                return jsonArray.getInt(0);
            }
            //表示已经完成了本章节的学习
            else if (finished&&result==true){
                Log.d("TestActivity","finished");
                long elapsedTime = (SystemClock.elapsedRealtime()-chronometer.getBase())/60000 ;
                Log.d("TestActivity","pass time "+(int)elapsedTime);
                int time = (int) elapsedTime ;
                SharedPreferences pref = getSharedPreferences("study_state",MODE_PRIVATE);
                int chapter = pref.getInt("chapter",0);
                //更新完成章节表
                JSONArray jsonArray1= new JSONArray(pref.getString("finishedChapter","[]"));
                int totalChapter = jsonArray1.length();
                int[] finishedChapter = new int[totalChapter];
                for (int i=0;i<totalChapter;i++){
                    finishedChapter[i]=jsonArray1.getInt(i);
                }
                finishedChapter[chapter]=1;
                JSONArray jsonArray2 = new JSONArray();
                for (int j=0;j<totalChapter;++j){
                    jsonArray2.put(finishedChapter[j]);
                }
                //
                SharedPreferences.Editor editor = getSharedPreferences("study_state",MODE_PRIVATE).edit();
                editor.putInt("studyTime",time);
                editor.putString("finishedChapter",jsonArray2.toString());
                editor.commit();

                Intent intent = new Intent(TestActivity.this,ReportActivity.class);
                startActivity(intent);
            }
            else {
                return index;
            }

        }catch (Exception e){ }
        return -1 ;
    }


    /**
     * 显示解析
     */
    private void showAnalysis() {
        SharedPreferences pref = getSharedPreferences("study_state", MODE_PRIVATE);
        String analysis = pref.getString("QAnalysis", "");
        mFRTvAnalysis.setText(analysis);
    }

    private void createStudyReport(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(TestActivity.this);
        dialog.setTitle("");
        dialog.setMessage("您确认要结束本次学习吗？");
        dialog.setCancelable(true);
        dialog.setPositiveButton("离开", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(TestActivity.this,ReportActivity.class);
                startActivity(intent);

            }
        });

        dialog.show();
    }

    /**
     * 当app切换到后台
     * 将停止计时
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void initBackgroundCallBack() {
        registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
                appCount++;
                if (isRunInBackground) {
                    //应用从后台回到前台 需要做的操作
                    back2app();
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    chronometer.start();
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
                appCount--;
                if (appCount == 0) {
                    //应用进入后台 需要做的操作
                    leaveApp();
                    chronometer.stop();
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }
    private void back2app(){
        isRunInBackground = false;
    }
    private void leaveApp(){
        isRunInBackground = true;
    }

}