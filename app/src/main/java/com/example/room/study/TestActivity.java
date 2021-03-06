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
     *  wait?????????????????????????????????,???????????? W
     *  true???????????????false?????????
     *  ????????????????????????4??????????????????????????????W???????????????
     *  1.??????????????????mBtnGiveUp??????W??????true
     *  2.check()??????questionType???1?????????W??????true
     *  3.???parseQuestionContent????????????????????????????????????W??????false
     *  4.???parseQuestionResult????????????????????????result?????????W??????true
     */
    static boolean wait ;
    /**
     *  questionType?????????????????????????????????QT
     *  0??????????????????1???????????????,-1???????????????????????????
     *  ????????????????????????4??????????????????????????????QT???????????????
     *  1.?????????????????????mBtnNextQuestion????????????QT???-1?????????QT??????0????????????????????????
     *  2.???????????????????????????????????????QT??????1
     *  3.??????????????????????????????????????????QT??????-1
     *  4.????????????????????????????????????QT??????0
     *
     */
    static int questionType ;
    static int appCount ;
    static boolean isRunInBackground;
    private Handler handler = new Handler();
    private int questionStudy ;
    /**
     * ??????????????????????????????
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
        initBackgroundCallBack();//????????????

        AjLatexMath.init(this);
        wait = false ;
        questionType = 0 ;
        // ????????????????????????????????????????????????????????????  // CodeProcessor.init(this);
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
        // ????????????
        // ????????????
        mRgOptions.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mRbOption = group.findViewById(checkedId);
            }
        });
        // ????????????
        mBtnSubmit.setOnClickListener(new NoMultiClickListener() {
            @Override
            public void onNoMultiClick(View v) {
                if(mRgOptions.getCheckedRadioButtonId() == -1){
                    Toast.makeText(TestActivity.this,"?????????",Toast.LENGTH_SHORT).show();
                }
                else{
                    if (!wait){
                        check();
                    }
                }
            }
        });
        // ????????????
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
        // ???????????????
        /**
         * ??????wait??????????????????
         * ???wait==false?????????????????????????????????
         * ???parseQuestionResult????????????wait??????false
         * ???wait==true?????????????????????????????????
         * ???parseQuestionContent????????????wait??????false
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
                    Toast.makeText(TestActivity.this,"???????????????????????????",Toast.LENGTH_SHORT).show();
                    Log.d("TestActivity","busy");
                }

            }
        });
        // ??????????????????
        mBtnBasicPractise.setOnClickListener(new NoMultiClickListener() {
            @Override
            public void onNoMultiClick(View v) {
                EndTraining();
            }
        });
    }

    /**
     * ???????????????
     * ??????????????????????????????startChapter?????????
     * ??????????????????Qid
     * ?????? ????????????parseInitialQId(responseData)???????????????Qid
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
                    // ????????????json????????????Qid
                    parseInitialQId(responseData);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

    }
    /**
     * ????????????????????????????????????
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
     * ?????????????????????
     */
    private void check(){
        String option = mRbOption.getText().toString();
        SharedPreferences pref = getSharedPreferences("study_state",MODE_PRIVATE);
        String answer = pref.getString("QAnswer","");
        boolean result ;
        if (option.equals(answer)){
            Toast.makeText(TestActivity.this,"????????????",Toast.LENGTH_SHORT).show();
            result = true ;
        }
        else{
            if (questionType==0){
                Toast.makeText(TestActivity.this,"??????????????????????????????????????????",Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(TestActivity.this,"????????????",Toast.LENGTH_SHORT).show();
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
     * ??????????????????Qid???????????????
     * ?????? Qid??????type"QuestionContent"
     * ??????Qcontent???Qanswer??????Qanalysis
     * ?????????????????????
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
     * ??????????????????????????????
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
     * ????????????
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
     * ???????????????Qid
     * ???????????????????????????
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
     * ????????????????????????Qid???????????????????????????
     * ???????????????????????????ID
     * @param savePath
     * @param jsonArray
     * @return
     */
    private int updateQuestionBank(String savePath,JSONArray jsonArray){
        // ???
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
        // ???

        // ???
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
        // ???????????????????????????
        boolean finished = true ;
        try {
            Log.d("TestActivity","judging");
            int index = 0 ;
            //???????????????Qid????????????????????????
            for (int i=0;i<jsonArray.length();i++){
                index = jsonArray.getInt(i);
                if (bytes[index]==0x00){
                    Log.d("TestActivity","index "+index);
                    finished=false;
                    break;
                }
            }

            //???????????????????????????????????????
            if (finished){
                Log.d("TestActivity","finished");
                long elapsedTime = (SystemClock.elapsedRealtime()-chronometer.getBase())/60000 ;
                Log.d("TestActivity","pass time "+(int)elapsedTime);
                int time = (int) elapsedTime ;
                SharedPreferences pref1 = getSharedPreferences("study_state",MODE_PRIVATE);
                int chapter = pref1.getInt("chapter",0);
                //?????????????????????
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
        // ???
        return -1 ;
    }


    /**
     * ????????????
     */
    private void showAnalysis() {
        SharedPreferences pref = getSharedPreferences("study_state", MODE_PRIVATE);
        String analysis = pref.getString("QAnalysis", "");
        mFRTvAnalysis.setText(analysis);
    }

    /**
     *  ??????????????????
     *  ????????????????????????
     */
    private void createStudyReport(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(TestActivity.this);
        dialog.setTitle("");
        dialog.setMessage("????????????????????????????????????");
        dialog.setCancelable(true);
        dialog.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(TestActivity.this,ReportActivity.class);
                startActivity(intent);
            }
        });

        dialog.show();
    }

    /**
     *  ??????QT???
     *  ??????QT??????????????????????????????????????????
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
     * ???????????????????????????ID
     * ???????????????????????????BQid???????????????????????????ID
     * ???????????????????????????????????????QT???-1
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
                questionType = -1 ;//?????????????????????????????????
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
     *  ?????????????????????????????????
     *  ????????????????????????????????????QT?????????????????????
     */
    private void EndTraining(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(TestActivity.this);
        dialog.setTitle("");
        dialog.setMessage("????????????????????????????????????");
        dialog.setCancelable(true);
        dialog.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                questionType = 0 ;
                mBtnBasicPractise.setVisibility(View.GONE);
                showAnalysis();
                sendQuestionContentWithOkHttp();
            }
        });
        dialog.setNegativeButton("????????????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        dialog.show();
    }

    /**
     *  ?????????????????????????????????????????????????????????
     *  ???????????????
     */
    private void showEndBasicTraining(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(TestActivity.this);
        dialog.setTitle("");
        dialog.setMessage("???????????????????????????????????????");
        dialog.setCancelable(true);
        dialog.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        dialog.show();
    }
    /**
     * ???app???????????????
     * ???????????????
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
                    //??????????????????????????? ??????????????????
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
                    //?????????????????? ??????????????????
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