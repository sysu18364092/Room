package com.example.room.study;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.daquexian.flexiblerichtextview.FlexibleRichTextView;
import com.example.room.MainActivity;
import com.example.room.R;
import com.example.room.utils.NoMultiClickListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.scilab.forge.jlatexmath.core.AjLatexMath;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WeakPointActivity extends AppCompatActivity {
    private FlexibleRichTextView mFRTvWeakPointQuestion;
    private FlexibleRichTextView mFRTvWeakPointAnalysis;
    private Button mBtnNextWeakPoint ;
    private Button mBtnDelete ;
    static boolean wait ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weak_point);
        AjLatexMath.init(this);
        mFRTvWeakPointQuestion = findViewById(R.id.fr_tv_weak_point_question);
        mFRTvWeakPointAnalysis = findViewById(R.id.fr_tv_weak_point_analysis);
        mBtnNextWeakPoint = findViewById(R.id.btn_next_weak_point);
        mBtnDelete = findViewById(R.id.btn_delete);
        wait = true ;
        startReview();
        mBtnNextWeakPoint.setOnClickListener(new NoMultiClickListener() {
            @Override
            public void onNoMultiClick(View v) {
                if (!wait){
                    check();
                    sendNextWeakPointMessageWithOkHttp();
                    showWeakPoint();
                }
                else {
                    Toast.makeText(WeakPointActivity.this,"加载中",Toast.LENGTH_SHORT).show();
                }
            }
        });
        mBtnDelete.setOnClickListener(new NoMultiClickListener() {
            @Override
            public void onNoMultiClick(View v) {
                sendDeleteMessageWithOkHttp();
            }
        });
    }

    private void startReview(){
        getWeakPointWithOkHttp();
        if (!wait){
            check();
            sendNextWeakPointMessageWithOkHttp();
            showWeakPoint();
        }
        else {
            Toast.makeText(WeakPointActivity.this,"加载中",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     *  获得薄弱点数组并存储本地
     */
    private void getWeakPointWithOkHttp(){
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
                            .add("type","CheckMistakeNote")
                            .build();

                    Request request = new Request.Builder()
                            .url("http://119.23.237.245/question_request.php")
                            .post(requestBody)
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    // 解析返回json文件获取Qid
                    parseWeakPointQId(responseData);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 将返回的薄弱点数组存储到内存中
     * @param jsonData
     */
    private void parseWeakPointQId(String jsonData){
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray jsonArrayQid = new JSONArray(jsonObject.getString("Qid"));
            Log.d("WeakPointActivity",jsonArrayQid.toString());
            SharedPreferences.Editor editor = getSharedPreferences("weak_point",MODE_PRIVATE).edit();
            editor.putString("WeakPointQid",jsonArrayQid.toString());
            int length = jsonArrayQid.length();
            editor.putInt("WeakPointCount",length);
            if (length==0){
                editor.putInt("index",-1);
            }
            else {
                editor.putInt("index",0);
            }
            editor.commit();
            wait = false ;
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     *  选取一个薄弱点的Qid，
     *  并向服务器获取内容
     */
    private void sendNextWeakPointMessageWithOkHttp(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences userPref = getSharedPreferences("login_state",MODE_PRIVATE);
                    String username = userPref.getString("username", null);
                    RequestBody requestBody = null ;
                    OkHttpClient client = new OkHttpClient();

                    SharedPreferences weakPref = getSharedPreferences("weak_point",MODE_PRIVATE);
                    int index = weakPref.getInt("index",0);
                    JSONArray jsonArray = new JSONArray(weakPref.getString("WeakPointQid","[]"));

                    requestBody = new FormBody.Builder()
                            .add("username",username)
                            .add("Qid",jsonArray.getInt(index)+"")
                            .add("type","QuestionContent")
                            .build();


                    Request request = new Request.Builder()
                            .url("http://119.23.237.245/question_request.php")
                            .post(requestBody)
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    Log.d("TestActivity","responseData"+responseData);

                    SharedPreferences.Editor editor = getSharedPreferences("weak_point",MODE_PRIVATE).edit();
                    editor.putInt("index",index+1);
                    editor.commit();
                    parseNextWeakPoint(responseData);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

    }

    /**
     *  将服务器返回的问题的内容和解析存储到内存
     * @param jsonData
     */
    private void parseNextWeakPoint(String jsonData){
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            String QContent = jsonObject.getString("content");
            String QAnalysis = jsonObject.getString("analysis");

            SharedPreferences.Editor editor = getSharedPreferences("weak_point",MODE_PRIVATE).edit();
            editor.putString("QContent",QContent);
            editor.putString("QAnalysis",QAnalysis);
            editor.commit();

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void sendDeleteMessageWithOkHttp(){

    }

    /**
     *  检查是否还有剩余薄弱点
     */
    private void check(){
        SharedPreferences pref = getSharedPreferences("weak_point", MODE_PRIVATE);
        int length = pref.getInt("WeakPointCount",0);
        int index = pref.getInt("index",-1);
        if (length==0){
            Toast.makeText(WeakPointActivity.this,"您现在还没有错题",Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(WeakPointActivity.this, MainActivity.class);
            startActivity(intent);
        }
        else if (index==length){
            Toast.makeText(WeakPointActivity.this,"您已经完成了本轮复习",Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(WeakPointActivity.this,MainActivity.class);
            startActivity(intent);
        }

    }
    /**
     *  显示薄弱点以及解析
     */
    private void showWeakPoint(){
        SharedPreferences pref = getSharedPreferences("weak_point", MODE_PRIVATE);
        String content = pref.getString("QContent","");
        String analysis = pref.getString("QAnalysis", "");
        mFRTvWeakPointQuestion.setText(content);
        mFRTvWeakPointAnalysis.setText(analysis);
    }
}