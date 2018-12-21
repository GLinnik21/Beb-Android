package com.app.beb.bebapp.rss;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.beb.bebapp.R;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.MyViewHolder> {
    public ArrayList<DashboardItem> getDashboardItems() {
        return dashboardItems;
    }

    public void setDashboardItems(ArrayList<DashboardItem> dashboardItems) {
        this.dashboardItems = dashboardItems;
        this.notifyDataSetChanged();
    }

    protected ArrayList<DashboardItem> dashboardItems;
    protected Context context;
    private OnItemClickListener onItemClickListener;

    public DashboardAdapter(Context context, ArrayList<DashboardItem> dashboardItems, OnItemClickListener listener){
        this.dashboardItems = dashboardItems;
        this.context=context;
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public DashboardAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.news_list_item, parent,false);
        MyViewHolder holder=new MyViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull DashboardAdapter.MyViewHolder holder, int position) {
        DashboardItem item = dashboardItems.get(position);

        holder.Title.setText(Html.fromHtml(item.getTitle()));
        holder.Description.setText(Html.fromHtml(item.getDescription()));
        holder.Date.setText(item.getPubDate());
        Glide
                .with(context)
                .load(item.getThumbnailUrl())
                .into(holder.Thumbnail);
        holder.setOnClickListener(onItemClickListener, item);
    }

    @Override
    public int getItemCount() {
        return dashboardItems.size();
    }

    public void addItem(DashboardItem item) {
        dashboardItems.add(item);
        this.notifyDataSetChanged();
    }


    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView Title,Description,Date;
        ImageView Thumbnail;

        public MyViewHolder(View itemView) {
            super(itemView);
            Title= itemView.findViewById(R.id.title_text);
            Description= itemView.findViewById(R.id.description_text);
            Date= itemView.findViewById(R.id.date_text);
            Thumbnail= itemView.findViewById(R.id.thumb_img);
        }

        public void setOnClickListener(final OnItemClickListener listener, final DashboardItem item) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(item);
                }
            });
        }
    }

    public interface OnItemClickListener{
        void onItemClick(DashboardItem item);
    }
}
