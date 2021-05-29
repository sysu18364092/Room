package com.example.room.shop;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.room.MainActivity;
import com.example.room.R;
import com.example.room.user.LoginActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ShopActivity extends AppCompatActivity {
    private Furniture[] furnitures = {
            new Furniture(0,"书本","book",R.drawable.ic_book, "一本书",20,"gold"),
            new Furniture(1,"铅笔","pencil",R.drawable.ic_pencil, "一支铅笔",10,"gold"),
            new Furniture(2,"橡皮擦","eraser",R.drawable.ic_earser,"一个橡皮擦",8,"gold")};
    private List<Furniture> furnitureList = new ArrayList<>();
    private FurnitureAdapter adapter ;
    private RecyclerView mRecyclerview ;
    private int scoreTime ;
    private int scorePass ;
    private int[] furnitureOnSale ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        updateScore();
        initFurniture();//传入数组
        RecyclerView recyclerView = findViewById(R.id.recycle_view);
        GridLayoutManager layoutManager = new GridLayoutManager(this,2);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new FurnitureAdapter(furnitureList);
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new FurnitureAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                chooseFurniture(view,position);
            }
        });
    }

    /**
     * 清空furnitureList中的数据，
     * 将刚才定义的Furniture数组中的家具导入到fruitList中
     * （需要根据用户资产导入）
     */
    private void initFurniture(){
        furnitureList.clear();
        SharedPreferences pref = getSharedPreferences("property",MODE_PRIVATE);

        try {
            JSONArray jsonArray = new JSONArray(pref.getString("furnitureId","[]"));

            int ownerLength = jsonArray.length();
            furnitureOnSale = new int[furnitures.length-ownerLength];
            int[] furnitureOwnerList = new int[ownerLength];
            for (int i=0;i<ownerLength;++i){
                furnitureOwnerList[i]=jsonArray.getInt(i);
            }
            //将没有在property中的家具加载到商城界面
            int i,j,m ;
            for (i=0,j=0,m=0;i<furnitures.length;i++){
                if (j<ownerLength && furnitures[i].getFurnitureId()==furnitureOwnerList[j]){
                    j++;
                    continue;
                }
                else{
                    furnitureOnSale[m]=furnitures[i].getFurnitureId();
                    furnitureList.add(furnitures[i]);
                    m++ ;
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 购买家具
     * @param view
     * @param position
     */
    private void chooseFurniture(View view, int position){
        int pos = furnitureOnSale[position];
        AlertDialog.Builder dialog = new AlertDialog.Builder(ShopActivity.this);
        dialog.setTitle("");
        dialog.setMessage("您确认要购买"+furnitures[pos].getName()+"吗？");
        dialog.setCancelable(true);
        dialog.setPositiveButton("确定购买", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

               if (scoreTime>=furnitures[pos].getPrice()){
                   int furnitureId = furnitures[pos].getFurnitureId();
                   int furniturePrice = furnitures[pos].getPrice();
                   sendPurchaseMessageWithOkHttp(furnitureId,furniturePrice);
                   Intent intent = new Intent(ShopActivity.this,MainActivity.class);
                   startActivity(intent);
               }
               else
               {
                   Toast.makeText(ShopActivity.this,"你没有足够的金币解锁此家具",Toast.LENGTH_SHORT).show();
               }

            }
        });
        dialog.setNegativeButton("再看看", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        dialog.show();
    }

    /**
     *  将购买信息发送给服务器
     */
    private void sendPurchaseMessageWithOkHttp(int furnitureId,int furniturePrice){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String ornamentName = furnitures[furnitureId].getOName();
                    SharedPreferences pref = getSharedPreferences("login_state", MODE_PRIVATE);
                    String username = pref.getString("username", null);
                    OkHttpClient client = new OkHttpClient();
                    RequestBody requestBody = new FormBody.Builder()
                            .add("username",username)
                            .add("ornamentName",ornamentName)
                            .add("type","purchase")
                            .build();

                    Request request = new Request.Builder()
                            .url("http://82.156.37.121/purchase.php")
                            .post(requestBody)
                            .build();
                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    //解析返回的json文件
                    Log.d("ShopActivity",responseData);
                    parseJSONWithJSONObject(responseData,furnitureId,furniturePrice);

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 解析返回的json文件,
     * 并且更新资产，
     * 刷新商店界面
      * @param jsonData
     */
    private void parseJSONWithJSONObject(String jsonData,int furnitureId,int furniturePrice){
        try{
            JSONObject jsonObject = new JSONObject(jsonData) ;
            String success = jsonObject.getString("success");
            String message = jsonObject.getString("message");
            Looper.prepare();
            if(success.equals("1")) {
                Toast.makeText(ShopActivity.this, "购买成功",
                        Toast.LENGTH_SHORT).show();
                //更新资产
                SharedPreferences pref = getSharedPreferences("property",MODE_PRIVATE);
                try {

                    JSONArray jsonArray = new JSONArray(pref.getString("furnitureId","[]"));

                    scoreTime = scoreTime - furniturePrice ;

                    int ownerLength = jsonArray.length();
                    int length = furnitures.length;

                    int[] furnitureOwnerList = new int[ownerLength];
                    for (int i=0;i<ownerLength;++i){
                        furnitureOwnerList[i]=jsonArray.getInt(i);
                    }

                    JSONArray jsonArray1 = new JSONArray();
                    int j=0 ;
                    while (j<ownerLength && furnitureOwnerList[j]<furnitureId ){
                        jsonArray1.put(furnitureOwnerList[j]);
                        j++;
                    }
                    jsonArray1.put(furnitureId);
                    while(j<ownerLength){
                        jsonArray1.put(furnitureOwnerList[j]);
                        j++;
                    }

                    Log.d("ShopActivity","this is"+jsonArray1.toString());
                    SharedPreferences.Editor editor = getSharedPreferences("property",MODE_PRIVATE).edit();
                    editor.putString("furnitureId",jsonArray1.toString());
                    editor.putInt("TimeScore",scoreTime);
                    editor.commit();
                    //initFurniture();

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            Looper.loop();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void updateScore(){
        TextView mTvTimeScore ;
        TextView mTvPassScore ;
        mTvTimeScore = findViewById(R.id.tv_time_score);
        mTvPassScore = findViewById(R.id.tv_pass_score);
        SharedPreferences pref = getSharedPreferences("property", MODE_PRIVATE);
        scoreTime = pref.getInt("TimeScore", 0);
        scorePass = pref.getInt("PassScore",0);
        mTvTimeScore.setText(String.valueOf(scoreTime));
        mTvPassScore.setText(String.valueOf(scorePass));
    }
}