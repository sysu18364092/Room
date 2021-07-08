package com.example.room.user;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.room.R;

import org.json.JSONObject;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignUpActivity extends AppCompatActivity {
    private EditText mEtSignUpUsername;
    private EditText mEtSignUpPassword;
    private EditText mEtSignUpPasswordConfirm;
    private EditText mEtSignUpMail;
    private Button mBtnSignUp;
    public final static String SignUpURL = "http://39.108.187.44/user_management.php";
    /**
     * 这个类定义了SignUpActivity中的各个组件
     *
     * @author : WenCong
     * @version : 1.0
     * @update : 2021/5/18 WenCong 发布初版
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        mEtSignUpUsername = findViewById(R.id.et_sign_up_username);
        mEtSignUpPassword = findViewById(R.id.et_sign_up_password);
        mEtSignUpPasswordConfirm = findViewById(R.id.et_sign_up_password_confirm);
        mEtSignUpMail = findViewById(R.id.et_sign_up_mail);
        mBtnSignUp = findViewById(R.id.btn_sign_up_confirm);

        mBtnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSignUp();
            }
        });

    }

    /**
     * 检查用户输入的注册信息是否符合要求
     *
     * 注册信息需要满足以下条件：
     * 1.用户名不能为空，且必须保持在20个字符以内
     * 2.密码不能为空，且两次输入密码需要一致
     * 3.邮箱不能为空
     * 如果符合要求，则发送给服务器，否则将Toast报错
     */
    private void startSignUp(){
        String strSignUpUsername = mEtSignUpUsername.getText().toString() ;
        String strSignUpPassword = mEtSignUpPassword.getText().toString() ;
        String strSignUpPasswordConfirm = mEtSignUpPasswordConfirm.getText().toString();
        String strSignUpMail = mEtSignUpMail.getText().toString() ;
        //下面判断输入格式

        //用户名
        if(strSignUpUsername.isEmpty()) {
            String warningText = "用户名不能为空";
            Toast.makeText(SignUpActivity.this,warningText,Toast.LENGTH_SHORT).show();
            return;
        }
        else if(strSignUpUsername.length()>20) {
            String warningText = "用户名必须在20个字符以内";
            Toast.makeText(SignUpActivity.this,warningText,Toast.LENGTH_SHORT).show();
            return;
        }
        else if(strSignUpUsername.length()<5) {
            String warningText = "用户名必须大于5个字符";
            Toast.makeText(SignUpActivity.this,warningText,Toast.LENGTH_SHORT).show();
            return;
        }
        boolean validUsername = checkInvalidChar(strSignUpUsername);
        if(!validUsername){
            String warningText = "用户名中包含非法字符";
            Toast.makeText(SignUpActivity.this,warningText,Toast.LENGTH_SHORT).show();
            return;
        }
        //密码
        if(strSignUpPassword.isEmpty()){
            String warningText = "密码不能为空";
            Toast.makeText(SignUpActivity.this,warningText,Toast.LENGTH_SHORT).show();
            return;
        }
        else if(strSignUpPassword.length()>20) {
            String warningText = "密码必须在20个字符以内";
            Toast.makeText(SignUpActivity.this,warningText,Toast.LENGTH_SHORT).show();
            return;
        }
        else if(strSignUpPassword.length()<6) {
            String warningText = "密码必须大于6个字符";
            Toast.makeText(SignUpActivity.this,warningText,Toast.LENGTH_SHORT).show();
            return;
        }
        else if(!strSignUpPassword.equals(strSignUpPasswordConfirm)){
            String warningText = "两次输入密码不一致";
            Toast.makeText(SignUpActivity.this,warningText,Toast.LENGTH_SHORT).show();
            return;
        }
        boolean validPassword = checkInvalidChar(strSignUpPassword);
        if(!validPassword){
            String warningText = "密码中包含非法字符";
            Toast.makeText(SignUpActivity.this,warningText,Toast.LENGTH_SHORT).show();
            return;
        }
        //邮箱
        if(strSignUpMail.isEmpty()){
            String warningText = "邮箱不能为空";
            Toast.makeText(SignUpActivity.this,warningText,Toast.LENGTH_SHORT).show();
            return;
        }
        else{
            sendSignUpMessageWithOkHttp(strSignUpUsername,strSignUpPassword,strSignUpMail);
        }
    }

    /**
     * 使用OkHttp向服务器发送HTTP请求
     *
     * 发送内容包括参数和"signup"信息
     * @param strSignUpUsername 用户名
     * @param strSignUpPassword 用户密码
     * @param strSignUpMail     用户邮箱
     */
    private void sendSignUpMessageWithOkHttp(String strSignUpUsername,String strSignUpPassword,String strSignUpMail){
        new Thread(new Runnable() {
            @Override
            public void run() {

                //下面将用户输入的注册信息封装成json文件发送给服务器
                try {
                    OkHttpClient client = new OkHttpClient();
                    RequestBody requestBody = new FormBody.Builder()
                            .add("username",strSignUpUsername)
                            .add("password",strSignUpPassword)
                            .add("email",strSignUpMail)
                            .add("type","signup")
                            .build();

                    Request request = new Request.Builder()
                            .url(SignUpURL)
                            .post(requestBody)
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();

                    parseJSONWithJSONObject(responseData);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 解析收到的json文件
     *
     * @param jsonData 待解析的json格式的字符串
     */
    private void parseJSONWithJSONObject(String jsonData){

        try{
            JSONObject jsonObject = new JSONObject(jsonData) ;
            String success = jsonObject.getString("success");
            String message = jsonObject.getString("message");
            Looper.prepare();
            //根据返回值判断注册是否成功
            if(success.equals("1")) {
                Toast.makeText(SignUpActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SignUpActivity.this,LoginActivity.class);
                startActivity(intent);
            }
            else if (success.equals("0")){
                if(message.equals("Unvalid email Address")){
                    Toast.makeText(SignUpActivity.this,"无效邮箱", Toast.LENGTH_SHORT).show();
                }
                else
                    Toast.makeText(SignUpActivity.this,"用户名已存在", Toast.LENGTH_SHORT).show();
            }
            Looper.loop();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private boolean checkInvalidChar(String checkStr){
        boolean result = true ;
        int strLen = checkStr.length();
        for(int i=0;i<strLen;++i){
            char ch = checkStr.charAt(i) ;
            if (((ch>47)&&(ch<58))||((ch>64)&&(ch<91))||(ch>96)&&(ch<123)){

            }
            else{
                Log.d("SignUpActivity","wrong ch "+ ch);
                result = false ;
                break;
            }
        }
        return result;
    }



}