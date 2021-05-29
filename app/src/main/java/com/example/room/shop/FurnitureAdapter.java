package com.example.room.shop;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.room.R;

import java.util.List;

public class FurnitureAdapter extends RecyclerView.Adapter<FurnitureAdapter.ViewHolder> {
    private Context mContext;
    private List<Furniture> mFurnitureList;
    private OnItemClickListener mOnItemClickListener;
    static class ViewHolder extends RecyclerView.ViewHolder{
        CardView cardView ;
        ImageView furnitureImage ;
        TextView furnitureName ;
        TextView furnitureDescription;
        TextView furniturePrince ;

        public ViewHolder(View view){
            super(view);
            cardView = (CardView) view ;
            furnitureImage = view.findViewById(R.id.furniture_image);
            furnitureName = view.findViewById(R.id.furniture_name);
            furnitureDescription = view.findViewById(R.id.furniture_description);
            furniturePrince = view.findViewById(R.id.furniture_price);
        }
    }
    public FurnitureAdapter(List<Furniture> furnitureList){
        mFurnitureList = furnitureList ;
    }

    /**
     * 自定义点击事件
     */
    public interface  OnItemClickListener{
        void onItemClick(View view,int position);
    }
    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;}

    /**
     * onCreateViewHolder用于创建ViewHolder实例，
     * 在这个方法中可以将furniture_item布局加载进来，
     * 然后创建一个ViewHolder实例
     * @param parent
     * @param viewType
     * @return
     */
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mContext==null){
            mContext = parent.getContext();
        }
        View view = LayoutInflater.from(mContext).inflate(R.layout.furniture_item,parent,false);
        return new ViewHolder(view);
    }

    /**
     * 使用Glide加载图像
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(ViewHolder holder,int position){
        Furniture furniture = mFurnitureList.get(position);
        holder.furnitureName.setText(furniture.getName());
        holder.furnitureDescription.setText(furniture.getDescription());
        holder.furniturePrince.setText(furniture.getPrinceString());
        Glide.with(mContext).load(furniture.getImageId()).into(holder.furnitureImage);
        if(mOnItemClickListener!=null){
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = holder.getLayoutPosition();
                    mOnItemClickListener.onItemClick(holder.itemView,position);
                }
            });
        }
    }
    public void removeData(int position){
        mFurnitureList.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount(){
        return mFurnitureList.size();
    }
}
