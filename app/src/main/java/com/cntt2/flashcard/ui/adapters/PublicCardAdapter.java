package com.cntt2.flashcard.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.remote.dto.CardDto;

import java.util.List;

public class PublicCardAdapter extends RecyclerView.Adapter<PublicCardAdapter.ViewHolder> {
    private final List<CardDto> cards;

    public PublicCardAdapter(List<CardDto> cards) {
        this.cards = cards;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CardDto card = cards.get(position);
        loadCardContent(holder.tvCardFront, card.getFront());
        loadCardContent(holder.tvCardBack, card.getBack());
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        WebView tvCardFront, tvCardBack;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCardFront = itemView.findViewById(R.id.tvCardFront);
            tvCardBack = itemView.findViewById(R.id.tvCardBack);
        }
    }

    private void loadCardContent(WebView webView, String content) {
        String cleanedContent = removeAllImageContent(content);
        String html = "<html><body style='background-color: rgb(27, 36, 51); color: white'>" + (cleanedContent != null ? cleanedContent : "") + "</body></html>";
        String baseUrl = "content://com.cntt2.flashcard.fileprovider/images/";
        webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null);
    }

    private String removeAllImageContent(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        String result = html.replaceAll("<img[^>]*>", "");
        result = result.replaceAll("data:image/[^;]*;base64,[a-zA-Z0-9+/=]*", "");
        return result;
    }

}

