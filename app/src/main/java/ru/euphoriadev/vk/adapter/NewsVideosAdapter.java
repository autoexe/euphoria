package ru.euphoriadev.vk.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.Serializable;
import java.util.List;

import ru.euphoriadev.vk.R;
import ru.euphoriadev.vk.util.Item;

/**
 * Created by autoexec on 08.02.2017.
 */

public class NewsVideosAdapter extends RecyclerView.Adapter<NewsVideosAdapter.MyViewHolder>{


    private int adPosition;
    private Context mContext;
    private List<Item> mItems;

    public NewsVideosAdapter(Runnable mContext, List<Item> mItems) {
        this.adPosition = 0;
        this.mContext = (Context) mContext;
        this.mItems = mItems;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout_recycler, parent, false);
        return new MyViewHolder(v);
    }

    public Item getItem(int position) {
        return this.mItems.get(position);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {

        final Item item = getItem(position);

        int duration = item.getDuration();
        int n2 = duration % 60;
        String text;
        if (duration >= 3600){
            int round = Math.round(duration / 3600);
            int n3 = duration % 3600 / 60;
            StringBuilder sb = new StringBuilder();
            Serializable s;
            if (round < 10) {
                s = "0" + round;
            }
            else {
                s = round;
            }
            StringBuilder append = sb.append(s).append(":");
            Serializable s2;
            if (n3 < 10) {
                s2 = "0" + n3;
            }
            else {
                s2 = n3;
            }
            StringBuilder append2 = append.append(s2).append(":");
            Serializable s3;
            if (n2 < 10) {
                s3 = "0" + n2;
            }
            else {
                s3 = n2;
            }
            text = append2.append(s3).toString();
        }
        else {
            int round2 = Math.round(duration / 60);
            StringBuilder sb2 = new StringBuilder();
            Serializable s4;
            if (round2 < 10) {
                s4 = "0" + round2;
            }
            else {
                s4 = round2;
            }
            final StringBuilder append3 = sb2.append(s4).append(":");
            Serializable s5;
            if (n2 < 10) {
                s5 = "0" + n2;
            }
            else {
                s5 = n2;
            }
            text = append3.append(s5).toString();
        }

        holder.textType.setText(item.getTitle());
        holder.txtDurationVid.setText(text);
        Picasso.with(mContext).load(item.getPhoto_320()).fit().centerCrop().into(holder.imageTVideo);

    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{

        ImageView imageTVideo;
        LinearLayout rootLL;
        TextView textType;
        TextView txtDurationVid;

        public MyViewHolder(View view) {
            super(view);
            this.imageTVideo = (ImageView)view.findViewById(R.id.photoTV);
            this.textType = (TextView)view.findViewById(R.id.titleTV);
            this.txtDurationVid = (TextView)view.findViewById(R.id.txtDurationVideo);
            this.rootLL = (LinearLayout)view.findViewById(R.id.rootLL);
        }
    }
}
