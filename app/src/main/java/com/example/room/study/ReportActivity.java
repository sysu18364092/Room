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

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ReportActivity extends AppCompatActivity {
    TextView mTvReportTitle ;
    TextView mTvReport ;
    TextView mTvStudyTime ;
    TextView mTvQuestionDone ;
    TextView mTvGetTimeScore ;
    TextView mTvGetPassScore;
    Button mBtnBackMain ;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
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
        mBtnBackMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ReportActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     *
     * @param studyTime
     * @param passChapter
     */
    private void updateScore(int studyTime,int passChapter){
        int timeScore = (studyTime+1)*10 ;
        int passScore = passChapter;

        sendStudyReportWithOkHttp(timeScore,passScore);
    }
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
                            .url("http://119.23.237.245/scoreboard.php")
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
}