package com.cntt2.flashcard.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.remote.dto.PublicDeskDto;

import java.util.List;

public class PublicDeskAdapter extends RecyclerView.Adapter<PublicDeskAdapter.ViewHolder> {
    private final List<PublicDeskDto> desks;
    private final OnDeskClickListener listener;

    public PublicDeskAdapter(List<PublicDeskDto> desks, OnDeskClickListener listener) {
        this.desks = desks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_public_desk, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PublicDeskDto desk = desks.get(position);
        holder.tvDeskName.setText(desk.getName());
        holder.tvCardCount.setText(desk.getCardCount() + " Cards");
        holder.tvCreator.setText("by: " + (desk.getOwner() != null ? desk.getOwner() : "Unknown"));
        holder.itemView.setOnClickListener(v -> listener.onDeskClick(desk));
    }

    @Override
    public int getItemCount() {
        return desks.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeskName, tvCardCount, tvCreator, tvLearners;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeskName = itemView.findViewById(R.id.tvDeskName);
            tvCardCount = itemView.findViewById(R.id.tvCardCount);
            tvCreator = itemView.findViewById(R.id.tvCreator);
        }
    }

    public interface OnDeskClickListener {
        void onDeskClick(PublicDeskDto desk);
    }
}