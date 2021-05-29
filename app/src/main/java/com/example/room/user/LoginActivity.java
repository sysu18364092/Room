package com.example.room.user;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.room.MainActivity;
import com.example.room.R;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {
    EditText mEtUsername ;
    EditText mEtPassword ;
    Button mBtnLogin ;
    TextView mTvSignUp ;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    /**
     * 这个类定义了LoginActivity中的各个组件
     *
     * @author : WenCong
     * @version : 1.0
     * @update : 2021/5/18 WenCong 发布初版
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mEtUsername = findViewById(R.id.et_username);
        mEtPassword = findViewById(R.id.et_password);
        mBtnLogin = findViewById(R.id.btn_login);
        mTvSignUp = findViewById(R.id.tv_sign_up);
        //跳转到注册界面
        mTvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this,SignUpActivity.class);
                startActivity(intent);
            }
        });
        mBtnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLogin();
            }
        });
    }
    /**
     * 检查用户输入的登录信息是否符合要求
     *
     * 如果符合要求，则发送给服务器，否则将Toast报错
     */
    private void startLogin(){
        String strUsername = mEtUsername.getText().toString() ;
        String strPassword = mEtPassword.getText().toString() ;

        //用户名
        if(strUsername.isEmpty()) {
            String warningText = "用户名不能为空";
            Toast.makeText(LoginActivity.this,warningText,Toast.LENGTH_SHORT).show();
            return;
        }
        else if(strUsername.length()>20) {
            String warningText = "用户名必须在20个字符以内";
            Toast.makeText(LoginActivity.this,warningText,Toast.LENGTH_SHORT).show();
            return;
        }

        //密码
        if(strPassword.isEmpty()){
            String warningText = "密码不能为空";
            Toast.makeText(LoginActivity.this,warningText,Toast.LENGTH_SHORT).show();
            return;
        }
        else{
            sendLoginMessageWithOkHttp(strUsername,strPassword);
        }
    }

    /**
     * 使用OkHttp向服务器发送HTTP请求
     *
     * @param strUsername 用户名
     * @param strPassword 密码
     */
    private void sendLoginMessageWithOkHttp(String strUsername,String strPassword){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OkHttpClient client = new OkHttpClient();
                    RequestBody requestBody = new FormBody.Builder()
                            .add("username",strUsername)
                            .add("password",strPassword)
                            .add("submit","login")
                            .build();

                    Request request = new Request.Builder()
                            .url("http://82.156.37.121/login.php")
                            .post(requestBody)
                            .build();
                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    //解析返回的json文件
                    parseJSONWithJSONObject(responseData,strUsername);
                    Log.d("LoginActivity",responseData);
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
    private void parseJSONWithJSONObject(String jsonData,String strUsername){
        try{
            JSONObject jsonObject = new JSONObject(jsonData) ;

            String success = jsonObject.getString("success");
            String message = jsonObject.getString("message");
            int scoreTime = jsonObject.getInt("TimerScore");
            int scorePass = jsonObject.getInt("PassScore");
            JSONArray jsonArray = new JSONArray(jsonObject.getString("Ostates"));
            Looper.prepare();
            if(success.equals("1")) {
                Toast.makeText(LoginActivity.this, "登陆成功",
                        Toast.LENGTH_SHORT).show();

                SharedPreferences.Editor editor = getSharedPreferences("login_state",MODE_PRIVATE).edit();
                editor.putString("username",strUsername);
                editor.putBoolean("state",true) ;
                editor.apply();

                SharedPreferences.Editor editor1 = getSharedPreferences("property",MODE_PRIVATE).edit();
                editor1.putInt("TimeScore",scoreTime) ;
                editor1.putInt("PassScore",scorePass) ;
                editor1.putString("furnitureId",jsonArray.toString());
                editor1.apply();

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
            }
            else
                Toast.makeText(LoginActivity.this,"用户名或密码错误",
                        Toast.LENGTH_SHORT).show();
            Looper.loop();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}