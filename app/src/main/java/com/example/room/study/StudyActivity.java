package com.example.room.study;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.room.R;

import java.util.ArrayList;
import java.util.List;

public class StudyActivity extends AppCompatActivity {
    private List<Chapter> chapterList = new ArrayList<>();
    private Chapter[] chapters = {
            new Chapter("第一章 函数 极限 连续",1),
            new Chapter("第二章 导数与微分",2),
            new Chapter("第三章 微分中值定理",3),
            new Chapter("第四章 不定积分",4),
            new Chapter("第五章 定积分与反常积分",5),
            new Chapter("第六章 定积分的应用",6),
            new Chapter("第七章 微分方程",7),
            new Chapter("第八章 多元函数微分学",8),
            new Chapter("第九章 二重积分",9),
            new Chapter("第十章 无穷级数",10),
            new Chapter("第十一章 几何应用",11),
            new Chapter("第十二章 多元积分学及其应用",12),
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study);
        initChapter();
        RecyclerView recyclerView = findViewById(R.id.recycle_view_chapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        ChapterAdapter adapter = new ChapterAdapter(chapterList);
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new ChapterAdapter.OnItemClickListener(){
            @Override
            public void onItemClick(View view, int position){
                if (position==0){
                    SharedPreferences.Editor editor = getSharedPreferences("study_state",MODE_PRIVATE).edit() ;
                    editor.putInt("chapter",chapters[position].getChapter());
                    boolean b = editor.commit();
                    Toast.makeText(StudyActivity.this,b+"",Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(StudyActivity.this,"后续章节正在开发中",Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    private void initChapter(){
        for (int i=0;i<chapters.length;++i) chapterList.add(chapters[i]);
    }
}