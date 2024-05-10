package com.ticonsys.screencapture.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.ticonsys.screencapture.R;
import com.ticonsys.screencapture.model.ImageDTO;

import java.util.List;
public class AdapterImageListAWS extends RecyclerView.Adapter<AdapterImageListAWS.ViewHolder> {
    private Context context;
    private List<ImageDTO> list;
    private OnItemClickListener listener;

    ImageDTO imageDTO;
    ViewHolder holder;
    String CallingFrom;

    public AdapterImageListAWS(String CallingFrom, List<ImageDTO> list, Context context, OnItemClickListener listener) {
        this.CallingFrom = CallingFrom;
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.custom_grid_layout_text, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        imageDTO = list.get(position);
        holder.tvTitle.setText("File: " + imageDTO.getImage_name());
        holder.tvTotal.setText("Total Detected : " + imageDTO.getTotal());
        holder.tvDetected.setText("Detected Items : " + imageDTO.getDetected());
        holder.tvDate.setText("Date : " + imageDTO.getDate() + ", Time :" + imageDTO.getTime());

        Glide.with(context)
                .load(imageDTO.getImage_path())
                .apply(new RequestOptions().centerCrop())
                .into(holder.gridIcon);

        holder.bind(list.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView gridIcon;
        public TextView tvTitle, tvTotal, tvDetected, tvDate;

        public ViewHolder(View itemView) {
            super(itemView);
            gridIcon = (ImageView) itemView.findViewById(R.id.gridIcon);
            tvTitle = (TextView) itemView.findViewById(R.id.tvTitle);
            tvTotal = (TextView) itemView.findViewById(R.id.tvTotal);
            tvDetected = (TextView) itemView.findViewById(R.id.tvDetected);
            tvDate = (TextView) itemView.findViewById(R.id.tvDate);
        }

        public void bind(final ImageDTO clicked, final OnItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(clicked, getAdapterPosition());
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(ImageDTO imageDTO, int position);
    }
}