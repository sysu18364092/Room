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
import com.example.room.utils.NoMultiClickListener;

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
    private Chronometer chronometer;
    private RadioGroup mRgOptions;
    private RadioButton mRbOption;
    private Button mBtnSubmit ;
    private Button mBtnGiveUp ;
    private Button mBtnBasicPractise ;
    private Button mBtnNextQuestion ;
    private FlexibleRichTextView mFRTvQuestion;
    private FlexibleRichTextView mFRTvOptionA;
    private FlexibleRichTextView mFRTvOptionB;
    private FlexibleRichTextView mFRTvOptionC;
    private FlexibleRichTextView mFRTvOptionD;
    private FlexibleRichTextView mFRTvAnalysis;
    public final static String TestURL = "http://39.108.187.44/question_request.php";
    /**
     *  wait表示等待用户进入下一题,以下简称 W
     *  true表示等待，false则相反
     *  在整个类中一共有4处（不计初始化）会对W进行更新：
     *  1.点击放弃按钮mBtnGiveUp，将W置为true
     *  2.check()如果questionType为1，则将W置为true
     *  3.在parseQuestionContent中，更新完题目内容后，将W置为false
     *  4.在parseQuestionResult中，解析完返回的result后，将W置为true
     */
    static boolean wait ;
    /**
     *  questionType表示题目种类，以下简称QT
     *  0表示测试题，1表示基础题,-1表示基础题结束状态
     *  在整个类中一共有4处（不计初始化）会对QT进行更新：
     *  1.在按下一题按钮mBtnNextQuestion时，如果QT为-1时，将QT置为0（后续发送请求）
     *  2.当测试时题目写错或者放弃将QT置为1
     *  3.当基础题完成到最后一道时，将QT置为-1
     *  4.当手动结束基础题训练，将QT置为0
     *
     */
    static int questionType ;
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
        wait = false ;
        questionType = 0 ;
        // 如果希望自动识别代码段中的语言以实现高亮  // CodeProcessor.init(this);
        chronometer = findViewById(R.id.chronometer);
        mRgOptions = findViewById(R.id.rg_options);
        mBtnSubmit = findViewById(R.id.btn_submit);
        mBtnGiveUp = findViewById(R.id.btn_give_up);
        mBtnBasicPractise = findViewById(R.id.btn_basic_practise);
        mBtnNextQuestion = findViewById(R.id.btn_next_question);
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
        mBtnBasicPractise.setVisibility(View.GONE);
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
        mBtnSubmit.setOnClickListener(new NoMultiClickListener() {
            @Override
            public void onNoMultiClick(View v) {
                if(mRgOptions.getCheckedRadioButtonId() == -1){
                    Toast.makeText(TestActivity.this,"还没选",Toast.LENGTH_SHORT).show();
                }
                else{
                    if (!wait){
                        check();
                    }
                }
            }
        });
        // 放弃按钮
        mBtnGiveUp.setOnClickListener(new NoMultiClickListener() {
            @Override
            public void onNoMultiClick(View v) {
                showAnalysis();
                if (questionType==0){
                    sendQuestionResultWithOkHttp(false);
                }
                else {
                    wait = true ;
                }
            }
        });
        // 下一题按钮
        /**
         * 使用wait控制按钮工作
         * 当wait==false时，可以进行提交工作，
         * 在parseQuestionResult函数中将wait置为false
         * 当wait==true时，可以切换到下一题，
         * 在parseQuestionContent函数中将wait置为false
         */
        mBtnNextQuestion.setOnClickListener(new NoMultiClickListener() {
            @Override
            public void onNoMultiClick(View v) {
                if (wait){
                    mRgOptions.clearCheck();
                    checkQuestionType();
                    if (questionType==-1){
                        showEndBasicTraining();
                        questionType = 0 ;
                    }
                    sendQuestionContentWithOkHttp();
                }
                else {
                    Toast.makeText(TestActivity.this,"请先选择提交或放弃",Toast.LENGTH_SHORT).show();
                    Log.d("TestActivity","busy");
                }

            }
        });
        // 结束训练按钮
        mBtnBasicPractise.setOnClickListener(new NoMultiClickListener() {
            @Override
            public void onNoMultiClick(View v) {
                EndTraining();
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
                            .url(TestURL)
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
            if (questionType==0){
                Toast.makeText(TestActivity.this,"回答错误，下面开始基础题训练",Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(TestActivity.this,"回答错误",Toast.LENGTH_SHORT).show();
            }
            result = false ;
        }
        if (questionType==0){
            sendQuestionResultWithOkHttp(result);
            showAnalysis();
        }
        else {
            showAnalysis();
            wait = true ;
        }
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
                    RequestBody requestBody = null ;
                    OkHttpClient client = new OkHttpClient();
                    if (questionType==0){
                        SharedPreferences studyPref = getSharedPreferences("study_state",MODE_PRIVATE);
                        int QId = studyPref.getInt("Qid",0);
                        requestBody = new FormBody.Builder()
                                .add("username",username)
                                .add("Qid",QId+"")
                                .add("type","QuestionContent")
                                .build();
                    }
                    else{
                        int BQId = getNextBasicQuestion();
                        requestBody = new FormBody.Builder()
                                .add("BQid",BQId+"")
                                .add("type","BasicQuestionContent")
                                .build();
                    }

                    Request request = new Request.Builder()
                            .url(TestURL)
                            .post(requestBody)
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    Log.d("TestActivity","responseData"+responseData);
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
                            .url(TestURL)
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
        wait = false ;
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

            if (result==true){
                JSONArray jsonArrayQid = new JSONArray(jsonObject.getString("Qid"));
                SharedPreferences pref = getSharedPreferences("login_state",MODE_PRIVATE);
                String fileSavePath = pref.getString("FileSavePath",null);
                int nextQuestion = updateQuestionBank(fileSavePath,jsonArrayQid);
                if (nextQuestion!=-1){
                    SharedPreferences.Editor editor = getSharedPreferences("study_state",MODE_PRIVATE).edit();
                    editor.putInt("Qid",nextQuestion);
                    editor.commit();
                    //sendQuestionContentWithOkHttp();
                }
                Log.d("TestActivity","nextQuestion  "+nextQuestion);

            }
            else{
                String BQid = jsonObject.getString("BQid") ;
                SharedPreferences.Editor editor = getSharedPreferences("study_state",MODE_PRIVATE).edit();
                editor.putString("BQid",BQid);
                editor.commit();
                questionType = 1 ;
                Log.d("TestActivity","QuestionType Changed");
            }

        }catch (Exception e){

        }
        wait = true ;
    }

    /**
     * 首先将本地存储的Qid数组读出，更新题库
     * 返回一道未写过的题ID
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
            }
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
        //-----------------------------------
        // 判断所给题是否完成
        boolean finished = true ;
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

            //表示已经完成了本章节的学习
            if (finished){
                Log.d("TestActivity","finished");
                long elapsedTime = (SystemClock.elapsedRealtime()-chronometer.getBase())/60000 ;
                Log.d("TestActivity","pass time "+(int)elapsedTime);
                int time = (int) elapsedTime ;
                SharedPreferences pref1 = getSharedPreferences("study_state",MODE_PRIVATE);
                int chapter = pref1.getInt("chapter",0);
                //更新完成章节表
                JSONArray jsonArray1= new JSONArray(pref1.getString("finishedChapter","[]"));
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
                SharedPreferences.Editor editor1 = getSharedPreferences("study_state",MODE_PRIVATE).edit();
                editor1.putInt("studyTime",time);
                editor1.putString("finishedChapter",jsonArray2.toString());
                editor1.commit();
                Intent intent = new Intent(TestActivity.this,ReportActivity.class);
                startActivity(intent);
            }
            else {
                return index;
            }

            }catch (Exception e){ }
        // 写
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

    /**
     *  生成学习报告
     *  切换到下一个界面
     */
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
     *  检查QT，
     *  根据QT取值设置结束训练按钮是否显示
     */
    private void checkQuestionType(){
        if (questionType==1){
            mBtnBasicPractise.setVisibility(View.VISIBLE);
        }
        else {
            mBtnBasicPractise.setVisibility(View.GONE);
        }
    }

    /**
     * 获得下一道基础题的ID
     * 每次获取将在内存中BQid数组中删除获取题目ID
     * 当基础题全部完成，将会给将QT置-1
     * @return
     */
    private int getNextBasicQuestion(){
        SharedPreferences pref = getSharedPreferences("study_state",MODE_PRIVATE);
        int nextBasicQuestionID = -1 ;
        try{
            JSONArray jsonArray = new JSONArray(pref.getString("BQid","[]"));
            Log.d("TestActivity",jsonArray.toString());
            int remainingQuestionsLength = jsonArray.length();
            nextBasicQuestionID = jsonArray.getInt(0);
            /*if(remainingQuestionsLength!=0){

                JSONArray remainingQuestions = new JSONArray();
                for (int i=0;i<remainingQuestionsLength-1;++i){
                    remainingQuestions.put(jsonArray.getInt(i+1));
                }
                SharedPreferences.Editor editor = getSharedPreferences("study_state",MODE_PRIVATE).edit();
                editor.putString("BQid",remainingQuestions.toString());
                editor.commit();
            }*/
            if (remainingQuestionsLength==1){
                questionType = -1 ;//表示基础题已经全部完成
            }else{
                JSONArray remainingQuestions = new JSONArray();
                for (int i=0;i<remainingQuestionsLength-1;++i){
                    remainingQuestions.put(jsonArray.getInt(i+1));
                }
                SharedPreferences.Editor editor = getSharedPreferences("study_state",MODE_PRIVATE).edit();
                editor.putString("BQid",remainingQuestions.toString());
                editor.commit();
            }

        }catch (Exception e){

        }
        Log.d("TestActivity","nextBasicQuestionID "+nextBasicQuestionID);
        return nextBasicQuestionID;
    }

    /**
     *  用户手动退出基础题训练
     *  如果用户选择退出，将更改QT，并且更新题目
     */
    private void EndTraining(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(TestActivity.this);
        dialog.setTitle("");
        dialog.setMessage("您确认要结束本次训练吗？");
        dialog.setCancelable(true);
        dialog.setPositiveButton("离开", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                questionType = 0 ;
                mBtnBasicPractise.setVisibility(View.GONE);
                showAnalysis();
                sendQuestionContentWithOkHttp();
            }
        });
        dialog.setNegativeButton("继续训练", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        dialog.show();
    }

    /**
     *  当用户完成所有基础题训练时显示一个弹窗
     *  无实际作用
     */
    private void showEndBasicTraining(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(TestActivity.this);
        dialog.setTitle("");
        dialog.setMessage("恭喜您已经完成了基础题训练");
        dialog.setCancelable(true);
        dialog.setPositiveButton("离开", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
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