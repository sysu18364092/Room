package com.example.room.study;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.quickaccesswallet.QuickAccessWalletService;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.daquexian.flexiblerichtextview.FlexibleRichTextView;
import com.example.room.R;

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

    private Handler handler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
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
        startTest();
        sendQuestionContentWithOkHttp();
        // 监听选项
        //选项按钮
        mRgOptions.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mRbOption = group.findViewById(checkedId);
            }
        });
        //提交按钮
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
        //放弃按钮
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

                    parseInitialQId(responseData);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
        /*sendQuestionMessageWithOkHttp();
        loadQuestion();*/
    }
    private void loadQuestion(){
        SharedPreferences pref = getSharedPreferences("study_state",MODE_PRIVATE);


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
                    Log.d("TestActivity","this is  "+responseData);
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
                    parseQuestionResult(responseData);
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
     * 解析返回的Qid
     * 在本地选择下一道题
     * @param jsonData
     */
    private void parseQuestionResult(String jsonData){
        Log.d("TestActivity","jsonData "+jsonData);
        try {
            JSONObject jsonObject =  new JSONObject(jsonData);
            JSONArray jsonArray = new JSONArray(jsonObject.getString("Qid"));
            SharedPreferences pref = getSharedPreferences("login_state",MODE_PRIVATE);
            String fileSavePath = pref.getString("FileSavePath",null);
            int nextQuestion = updateQuestionBank(fileSavePath,jsonArray);
            if (nextQuestion!=-1){
                SharedPreferences.Editor editor = getSharedPreferences("study_state",MODE_PRIVATE).edit();
                editor.putInt("Qid",nextQuestion);
                editor.commit();
                sendQuestionContentWithOkHttp();
            }
            Log.d("TestActivity","nextQuestion  "+nextQuestion);
        }catch (Exception e){

        }
        /*try{
            SharedPreferences pref = getSharedPreferences("study_state",MODE_PRIVATE);
            JSONArray jsonArray = new JSONArray(pref.getString("","[]"));

        }catch (Exception e){
            e.printStackTrace();
        }*/
    }

    /**
     * 更新题库
     * 返回一道未写过的题
     * @param savePath
     * @param jsonArray
     * @return
     */
    private int updateQuestionBank(String savePath,JSONArray jsonArray){
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
        boolean finished = false ;// 判断题库是否完成
        try {
            int index = 0 ;
            for (int i=0;i<jsonArray.length();i++){
                index = jsonArray.getInt(i);
                if (bytes[index]==0){
                    Log.d("TestActivity","index "+index);
                    bytes[index]=0x01;
                    finished = true ;
                    break;
                }
                Log.d("TestActivity","bytes "+ index +bytes[index]);
            }
            if (!finished){
                return jsonArray.getInt(0);
            }
            else {
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

                return jsonArray.getInt(index);
            }

        }catch (Exception e){ }
        // 写
        return -1 ;
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
     * 显示解析
     */
    private void showAnalysis() {
        SharedPreferences pref = getSharedPreferences("study_state", MODE_PRIVATE);
        String analysis = pref.getString("QAnalysis", "");
        mFRTvAnalysis.setText(analysis);
    }


}