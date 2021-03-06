package com.example.room.study;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.room.MainActivity;
import com.example.room.R;
import com.example.room.utils.NoMultiClickListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ReportActivity extends AppCompatActivity {
    private TextView mTvReportTitle ;
    private TextView mTvReport ;
    private TextView mTvStudyTime ;
    private TextView mTvQuestionDone ;
    private TextView mTvGetTimeScore ;
    private TextView mTvGetPassScore;
    private Button mBtnBackMain ;
    public final static String SavePath = "/data/data/com.example.room/";
    public final static String UploadURL = "http://39.108.187.44/upload_record_file.php";
    public final static String ScoreURL = "http://39.108.187.44/scoreboard.php";

    /**
     * 监听按键，如果用户按下BACK键时直接退出程序
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 初始化控件
     * 设置监听事件
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        mTvReportTitle = findViewById(R.id.tv_study_report_title);
        mTvReport = findViewById(R.id.tv_report);
        mTvStudyTime = findViewById(R.id.tv_study_time);
        mTvQuestionDone=findViewById(R.id.tv_question_done);
        mTvGetTimeScore = findViewById(R.id.tv_get_time_score);
        mTvGetPassScore=findViewById(R.id.tv_get_pass_score);
        mBtnBackMain = findViewById(R.id.btn_back_main);
        SharedPreferences pref = getSharedPreferences("study_state",MODE_PRIVATE);
        int chapter = pref.getInt("chapter",0);
        int studyTime = pref.getInt("studyTime",0);
        int questionDone = pref.getInt("questionDone",0);
        JSONArray finishedChapter = null ;
        int passChapter = 0 ;
        uploadStudyRecord();
        try {
            finishedChapter = new JSONArray(pref.getString("finishedChapter","[]"));
            if (finishedChapter.getInt(chapter)==1){
                Log.d("ReportActivity","have set");
                mTvReportTitle.setText("本章学习完成");
                mTvGetPassScore.setText("您获得了本章的通关积分");
            }
            else{
                mTvReportTitle.setText("本次学习结束");
            }
            for (int i=0;i<finishedChapter.length();++i){
                if (finishedChapter.getInt(i)==1){
                    passChapter=passChapter+1;
                }
            }

        }catch (Exception e){

        }
        mTvReport.setText("您本次学习时长为："+studyTime+" 分钟");
        mTvQuestionDone.setText("您本次完成了："+questionDone+" 道题");
        //mTvGetTimeScore.setText("您本次获得的时间积分为：");
        //mTvGetPassScore.setText("您本次获得的通关积分为：");
        updateScore(studyTime,passChapter);

        mBtnBackMain.setOnClickListener(new NoMultiClickListener() {
            @Override
            public void onNoMultiClick(View v) {
                Intent intent = new Intent(ReportActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * 输入为学习时间以及是否通关章节，
     * 结算学习奖励，
     * 并调用sendStudyReportWithOkHttp函数将输入发送给服务器
     * @param studyTime
     * @param passChapter
     */
    private void updateScore(int studyTime,int passChapter){
        int timeScore = (studyTime+1)*10 ;
        int passScore = passChapter;

        sendStudyReportWithOkHttp(timeScore,passScore);
    }

    /**
     * 输入为学习时间积分以及通关积分，
     * 发送学习记录给服务器
     * @param timeScore
     * @param passScore
     */
    private void sendStudyReportWithOkHttp(int timeScore,int passScore){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences pref = getSharedPreferences("login_state", MODE_PRIVATE);
                    String username = pref.getString("username", null);
                    OkHttpClient client = new OkHttpClient();
                    RequestBody requestBody = new FormBody.Builder()
                            .add("username",username)
                            .add("score",String.valueOf(timeScore))
                            .add("type","TimerScore")
                            .build();

                    Request request = new Request.Builder()
                            .url(ScoreURL)
                            .post(requestBody)
                            .build();
                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    Log.d("ReportActivity","submit TimeScore"+responseData);
                    parseJSONWithJSONObject(responseData,timeScore,passScore);

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 输入为服务器返回的JSON文件以及学习时间积分以及通关积分，
     * 如果服务器返回成功，
     * 则将积分写入到资产文件中
     * @param jsonData
     * @param timeScore
     * @param passScore
     */
    private void parseJSONWithJSONObject(String jsonData,int timeScore,int passScore){
        try {
            JSONObject jsonObject = new JSONObject(jsonData) ;
            int success = jsonObject.getInt("success");
            if (success==1){
                SharedPreferences pref = getSharedPreferences("property",MODE_PRIVATE);
                int lastTimeScore =pref.getInt("TimeScore",0);
                SharedPreferences.Editor editor = getSharedPreferences("property",MODE_PRIVATE).edit();
                editor.putInt("TimeScore",lastTimeScore+timeScore);
                editor.putInt("PassScore",passScore);
                editor.commit();
                //Log.d("ReportActivity","passScore"+passScore);
                mTvGetTimeScore.setText("您本次获得的时间积分为："+timeScore);
            }
        }catch (Exception e){

        }
    }

    /**
     * 将最新的学习记录提交到服务器端
     */
    private void uploadStudyRecord(){
        new Thread(new Runnable() {
            @Override
            public void run() {

                SharedPreferences pref = getSharedPreferences("login_state",MODE_PRIVATE);
                String username = pref.getString("username","");
                Log.d("ReportActivity","UploadBack username"+username);
                String fileSavePath = pref.getString("FileSavePath",null);
                File file = new File(fileSavePath);
                OkHttpClient client = new OkHttpClient();

                MediaType mediaType=MediaType.Companion.parse("text/x-markdown; charset=utf-8");
                RequestBody requestBody = RequestBody.Companion.create(file,mediaType);
                RequestBody fileBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("username",username)
                        .addFormDataPart("record",file.getName(),requestBody)
                        .build();
                Request request = new Request.Builder()
                        .header("Authorization","ClientID"+UUID.randomUUID())
                        .url(UploadURL)
                        .post(fileBody)
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    Log.d("ReportActivity","UploadBack "+responseData);
                    //parseUploadResult(responseData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
//    private void parseUploadResult(String jsonData){
//
//    }
}