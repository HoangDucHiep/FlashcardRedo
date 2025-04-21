package com.cntt2.flashcard.ui.adapters;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.remote.dto.CardDto;

import java.util.ArrayList;
import java.util.List;

public class FlashcardAdapter extends RecyclerView.Adapter<FlashcardAdapter.CardViewHolder> {

    private List<CardDto> flashcardList;
    private OnCardLongClickListener longClickListener;
    private OnCardClickListener clickListener;

    public interface OnCardLongClickListener {
        void onCardLongClick(CardDto card, int position);
    }

    public interface OnCardClickListener {
        void onCardClick(int position);
    }

    public FlashcardAdapter(List<CardDto> flashcardList, OnCardLongClickListener listener, OnCardClickListener clickListener) {
        this.flashcardList = flashcardList != null ? flashcardList : new ArrayList<>();
        this.longClickListener = listener;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, parent, false);
        return new CardViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        CardDto card = flashcardList.get(position);

        // Configure WebView
        configureWebView(holder.cardFront);
        configureWebView(holder.cardBack);

        // Load HTML content
        loadCardContent(holder.cardFront, card.getFront());
        loadCardContent(holder.cardBack, card.getBack());

        holder.cardFront.setOnTouchListener((v, event) -> {
            holder.itemView.onTouchEvent(event);
            return true;
        });
        holder.cardBack.setOnTouchListener((v, event) -> {
            holder.itemView.onTouchEvent(event);
            return true;
        });

        // Handle long click
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onCardLongClick(card, position);
                return true;
            }
            return false;
        });

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCardClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return flashcardList.size();
    }

    private void configureWebView(WebView webView) {
        webView.setBackgroundColor(0x00000000);
        WebSettings settings = webView.getSettings();
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                Log.e("WebViewError", "Error loading resource: " + error.getDescription());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });
    }

    private void loadCardContent(WebView webView, String content) {
        String cleanedContent = removeAllImageContent(content);
        String html = "<html><body style='background-color: rgb(27, 36, 51); color: white'>" + (cleanedContent != null ? cleanedContent : "") + "</body></html>";
        webView.loadData(html, "text/html", "UTF-8");
    }

    public void setData(List<CardDto> newList) {
        this.flashcardList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    private String removeAllImageContent(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        String result = html.replaceAll("<img[^>]*>", "");
        result = result.replaceAll("data:image/[^;]*;base64,[a-zA-Z0-9+/=]*", "");
        return result;
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        WebView cardFront;
        WebView cardBack;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardFront = itemView.findViewById(R.id.tvCardFront);
            cardBack = itemView.findViewById(R.id.tvCardBack);
        }
    }
}