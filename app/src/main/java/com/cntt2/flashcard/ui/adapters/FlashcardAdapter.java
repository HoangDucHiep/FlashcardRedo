package com.cntt2.flashcard.ui.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
        String frontHtml = "<html><body style='color:white;'>" + card.getFront() + "</body></html>";
        String backHtml = "<html><body style='color:white;'>" + card.getBack() + "</body></html>";
        holder.cardFront.loadDataWithBaseURL("", frontHtml, "text/html", "UTF-8", null);
        holder.cardBack.loadDataWithBaseURL("", backHtml, "text/html", "UTF-8", null);
    }

    @Override
    public int getItemCount() {
        return flashcardList.size();
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        WebView cardFront;
        WebView cardBack;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardFront = itemView.findViewById(R.id.tvCardFront);
            cardFront.setBackgroundColor(0x00000000); // Set transparent background
            cardBack = itemView.findViewById(R.id.tvCardBack);
            cardBack.setBackgroundColor(0x00000000); // Set transparent background
        }
    }

    public void setData(List<Card> newList) {
        this.flashcardList = newList;
        notifyDataSetChanged();
    }
}