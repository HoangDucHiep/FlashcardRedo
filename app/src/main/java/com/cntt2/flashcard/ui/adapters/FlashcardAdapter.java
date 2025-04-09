package com.cntt2.flashcard.ui.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cntt2.flashcard.R;
import com.cntt2.flashcard.model.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlashcardAdapter extends RecyclerView.Adapter<FlashcardAdapter.CardViewHolder> {

    private List<Card> flashcardList;
    private OnCardLongClickListener longClickListener;
    private OnCardClickListener clickListener;

    // Callback interface để xử lý long click
    public interface OnCardLongClickListener {
        void onCardLongClick(Card card, int position);
    }

    public interface OnCardClickListener {
        void onCardClick(int position);
    }

    public FlashcardAdapter(List<Card> flashcardList, OnCardLongClickListener listener, OnCardClickListener clickListener) {
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

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Card card = flashcardList.get(position);

        // Cấu hình WebView
        configureWebView(holder.cardFront);
        configureWebView(holder.cardBack);

        // Tải nội dung HTML
        loadCardContent(holder.cardFront, card.getFront());
        loadCardContent(holder.cardBack, card.getBack());

        holder.cardFront.setOnTouchListener((v, event) -> {
            holder.itemView.onTouchEvent(event); // Chuyển sự kiện lên itemView
            return true; // Chặn WebView xử lý sự kiện
        });
        holder.cardBack.setOnTouchListener((v, event) -> {
            holder.itemView.onTouchEvent(event); // Chuyển sự kiện lên itemView
            return true; // Chặn WebView xử lý sự kiện
        });

        // Xử lý sự kiện long click
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

    // Cấu hình chung cho WebView
    private void configureWebView(WebView webView) {
        webView.setBackgroundColor(0x00000000); // Nền trong suốt
        WebSettings settings = webView.getSettings();
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptEnabled(true); // Cho phép JS nếu cần
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                Log.e("WebViewError", "Lỗi tải tài nguyên: " + error.getDescription());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });
    }

    // Tải nội dung HTML vào WebView
    private void loadCardContent(WebView webView, String content) {
        String cleanedContent = removeAllImageContent(content);
        String html = "<html><body style='color:white;'>" + (cleanedContent != null ? cleanedContent : "") + "</body></html>";
        String baseUrl = "content://com.cntt2.flashcard.fileprovider/images/"; // Đảm bảo ảnh tải đúng
        webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null);
    }

    // Cập nhật danh sách thẻ
    public void setData(List<Card> newList) {
        this.flashcardList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    // Loại bỏ nội dung ảnh khỏi HTML (nếu cần)
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