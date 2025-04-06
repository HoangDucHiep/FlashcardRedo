package com.cntt2.flashcard.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cntt2.flashcard.R;
import com.cntt2.flashcard.model.Card;

import java.util.List;

public class FlashcardAdapter extends RecyclerView.Adapter<FlashcardAdapter.CardViewHolder> {

    private List<Card> flashcardList;

    public FlashcardAdapter(List<Card> flashcardList) {
        this.flashcardList = flashcardList;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Card card = flashcardList.get(position);
        holder.cardFront.setText(card.getFront());
    }

    @Override
    public int getItemCount() {
        return flashcardList.size();
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView cardFront;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardFront = itemView.findViewById(R.id.tvCardFront);
        }
    }

    public void setData(List<Card> newList) {
        this.flashcardList = newList;
        notifyDataSetChanged();
    }

}
