package com.example.room;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.TextView;

import com.example.room.shop.Furniture;
import com.example.room.shop.ShopActivity;
import com.example.room.study.RecordActivity;
import com.example.room.study.StudyActivity;
import com.example.room.study.WeakPointActivity;
import com.example.room.user.LoginActivity;
import com.google.android.material.navigation.NavigationView;


public class MainActivity extends AppCompatActivity {

    private DrawerLayout mDrawerLayout ;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    /**
     * 这个类定义了MainActivity中的各个组件
     *
     * @author : WenCong
     * @version : 1.0
     * @update : 2021/5/18 WenCong 发布初版
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);//设置导航按钮图标
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        }
        navView.setCheckedItem(R.id.nav_study);
        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                Intent intent = null ;
                switch (item.getItemId()){
                    case R.id.nav_sign_out:
                        toSignOut();
                        break;
                    case R.id.nav_study:
                        intent = new Intent(MainActivity.this, StudyActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.nav_shop:
                        intent = new Intent(MainActivity.this, ShopActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.nav_record:
                        intent = new Intent(MainActivity.this, RecordActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.nav_weak_point:
                        intent = new Intent(MainActivity.this, WeakPointActivity.class);
                        startActivity(intent);
                        break;
                }
                return true ;
            }
        });
    }

    /**
     * 在每次返回MainActivity时，检查一次登录状态，
     * 如果登录状态为false，则加载到登录界面。
     */
    @Override
    protected void onResume() {
        super.onResume();
        //使用SharePreference 存储登录状态
        SharedPreferences pref = getSharedPreferences("login_state", MODE_PRIVATE);
        boolean loginState = pref.getBoolean("state", false);
        if (!loginState) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
        }
        return true ;
    }

    /**
     * AlertDialog弹出对话框，确认用户是否退出登录
     */
    private void toSignOut(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle("退出登录");
        dialog.setMessage("您确认要退出登录状态吗？");
        dialog.setCancelable(true);
        dialog.setPositiveButton("确定退出", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor editor = getSharedPreferences("login_state",
                        MODE_PRIVATE).edit();
                editor.putBoolean("state",false);
                editor.apply();
                Intent intent = new Intent(MainActivity.this,LoginActivity.class);
                startActivity(intent);
            }
        });
        dialog.setNegativeButton("算了", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        dialog.show();
    }

}
