package com.cntt2.flashcard.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cntt2.flashcard.R;
import com.cntt2.flashcard.model.Card;

import java.util.List;

public class ShowStudyCardToLearnAdapter extends RecyclerView.Adapter<ShowStudyCardToLearnAdapter.CardViewHolder> {

    private final List<Card> cardList;

    public ShowStudyCardToLearnAdapter(List<Card> cardList) {
        this.cardList = cardList;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_flashcard, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Card card = cardList.get(position);

        String front = card.getFront() != null ? card.getFront() : "";
        String back = card.getBack() != null ? card.getBack() : "";

        holder.webFront.loadDataWithBaseURL(null, wrapHtml(front), "text/html", "UTF-8", null);
        holder.webBack.loadDataWithBaseURL(null, wrapHtml(back), "text/html", "UTF-8", null);

        holder.webFront.setVisibility(View.VISIBLE);
        holder.webBack.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return cardList.size();
    }

    // Hàm gói nội dung HTML
    private String wrapHtml(String content) {
        if (content == null) {
            content = "";
        }
        return "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0' />"
                + "<style>body{ background-color: rgb(67, 78, 170); color:white;font-size:16px;text-align:center;word-wrap:break-word;max-width:100%; margin: 0 auto; max-height: 2000px; overflow-y: scroll;} img{max-width:100%; height:auto;}</style></head><body>"
                + content + "</body></html>";
    }


    public static class CardViewHolder extends RecyclerView.ViewHolder {
        public WebView webFront, webBack;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            webFront = itemView.findViewById(R.id.WebViewCardItemFront);
            webBack = itemView.findViewById(R.id.WebViewCardItemBack);
        }
    }
}
