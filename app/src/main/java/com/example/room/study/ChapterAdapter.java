package com.example.room.study;

import android.content.Context;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import androidx.recyclerview.widget.RecyclerView;

import com.example.room.R;

import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ViewHolder>{
    private Context mContext;
    private List<Chapter> mChapterList ;
    private OnItemClickListener mOnItemClickListener;


    static class ViewHolder extends RecyclerView.ViewHolder{
        Button chapterButton;
        public ViewHolder(View view){
            super(view);
            chapterButton = view.findViewById(R.id.btn_chapter);
        }
    }
    public ChapterAdapter(List<Chapter> chapterList){
        mChapterList = chapterList ;
    }

    /**
     * 自定义点击事件
     */
    public interface OnItemClickListener{
        void onItemClick(View view,int position);
    }
    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
       this.mOnItemClickListener = mOnItemClickListener;}

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,int viewType){
        if (mContext==null){
            mContext = parent.getContext();
        }
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.chapter_item,parent,false);

        final ViewHolder holder = new ViewHolder(view);

        return holder ;
    }
    @Override
    public void onBindViewHolder(ViewHolder holder,int position){
        Chapter chapter = mChapterList.get(position);
        holder.chapterButton.setText(chapter.getName());
        if(mOnItemClickListener!=null){
            holder.chapterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = holder.getLayoutPosition();
                    Chapter chapter = mChapterList.get(position);
                    /*Toast.makeText(v.getContext(),chapter.getName(),
                            Toast.LENGTH_SHORT).show();*/
                    mOnItemClickListener.onItemClick(holder.itemView,position);
                }
            });
        }
    }
    @Override
    public int getItemCount(){
        return mChapterList.size();
    }

}
