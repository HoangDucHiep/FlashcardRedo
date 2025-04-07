package com.cntt2.flashcard.ui.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.repository.CardRepository;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.ui.adapters.ShowStudyCardToLearnAdapter;
import com.cntt2.flashcard.ui.animation.ZoomOutPageTransformer;

import java.util.ArrayList;
import java.util.List;

public class StudyActivity extends AppCompatActivity {

    private ViewPager2 viewPagerStudyCard;
    TextView tvShowAnswer, tvStudyCardCompletedShow, tvStudyCardStep;
    LinearLayout FrontLayoutUnder, BackLayoutUnder;
    Button btnAgain, btnHard, btnGood, btnEasy, btnStudyCardCompletedShow;
    ImageButton  imageButtonStudyCardCancel, btnHamburgerMenuStudyCardOption;
    private ShowStudyCardToLearnAdapter cardAdapter;

    CardRepository cardRepository = App.getInstance().getCardRepository();

    private int deskId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startstudy); // Sử dụng layout study.xml

        // Ánh xạ ViewPager2
        viewPagerStudyCard = findViewById(R.id.viewPagerStudyCard);
        tvShowAnswer = findViewById(R.id.tvStudyCardFrontShowAnswer);
        FrontLayoutUnder = findViewById(R.id.linearLayoutStudyCardFrontLayoutUnder);
        BackLayoutUnder = findViewById(R.id.linearLayoutStudyCardBackLayoutUnder);
        btnAgain = findViewById(R.id.btnStudyCardBackAgain);
        btnHard = findViewById(R.id.btnStudyCardBackHard);
        btnGood = findViewById(R.id.btnStudyCardBackGood);
        btnEasy = findViewById(R.id.btnStudyCardBackEady);
        tvStudyCardCompletedShow= findViewById(R.id.tvStudyCardCompletedShow);
        btnStudyCardCompletedShow= findViewById(R.id.btnStudyCardCompletedShow);
        tvStudyCardStep= findViewById(R.id.tvStudyCardStep);
        imageButtonStudyCardCancel= findViewById(R.id.imageButtonStudyCardCancel);
        btnHamburgerMenuStudyCardOption= findViewById(R.id.btnHamburgerMenuStudyCardOption);

        Intent intent = getIntent();

        deskId = intent.getIntExtra("deskId", -1);

        if (deskId == -1) {
            finish();
            return;
        }

        // Tạo danh sách thẻ (sample data)
        List<Card> cardList = cardRepository.getCardsByDeskId(deskId);

        // Tạo adapter và gắn vào ViewPager2
        cardAdapter = new ShowStudyCardToLearnAdapter(cardList);
        viewPagerStudyCard.setAdapter(cardAdapter);

        viewPagerStudyCard.setUserInputEnabled(false); // Khóa vuốt ngang


        // Thêm hiệu ứng hoạt ảnh khi chuyển thẻ (tuỳ chọn)
        viewPagerStudyCard.setPageTransformer(new ZoomOutPageTransformer());

        // Xử lý sự kiện nhấn vào "Show Answer"
        FrontLayoutUnder.setOnClickListener(v -> {
            View webFront = findViewById(R.id.WebViewCardItemFront);
            View webBack = findViewById(R.id.WebViewCardItemBack);

            if (webFront.getVisibility() == View.VISIBLE) {
                flipCard(webFront, webBack);
                webFront.setVisibility(View.GONE);
                webBack.setVisibility(View.VISIBLE);
                fadeLayouts(FrontLayoutUnder, BackLayoutUnder, false);
            } else {
                flipCard(webBack, webFront);
                webFront.setVisibility(View.VISIBLE);
                webBack.setVisibility(View.GONE);
                fadeLayouts(FrontLayoutUnder, BackLayoutUnder, true);
            }

            // Cập nhật lại dữ liệu trong adapter khi mặt trước/sau thay đổi
            cardAdapter.notifyItemChanged(viewPagerStudyCard.getCurrentItem());
        });

        // Đăng ký callback để theo dõi sự kiện khi vuốt qua các thẻ
        viewPagerStudyCard.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                fadeLayouts(FrontLayoutUnder, BackLayoutUnder, true);

                // Cập nhật trạng thái thẻ hiện tại vào tvStudyCardStep
                int totalItems = cardAdapter.getItemCount();  // Tổng số thẻ

                String stepText = getString(R.string.study_card_step, position + 1, totalItems);
                tvStudyCardStep.setText(stepText);
                //tvStudyCardStep.setText((position + 1) + "/" + totalItems);  // Cập nhật theo định dạng x/y

                // Lấy RecyclerView từ ViewPager2
                RecyclerView recyclerView = (RecyclerView) viewPagerStudyCard.getChildAt(0);

                // Lấy ViewHolder tại vị trí hiện tại
                RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);

                if (holder instanceof ShowStudyCardToLearnAdapter.CardViewHolder) {
                    ShowStudyCardToLearnAdapter.CardViewHolder cardHolder = (ShowStudyCardToLearnAdapter.CardViewHolder) holder;

                    // Đảm bảo hiển thị mặt trước và ẩn mặt sau
                    cardHolder.webFront.setVisibility(View.VISIBLE);
                    cardHolder.webBack.setVisibility(View.GONE);



                    cardHolder.webFront.setRotationY(0f);
                    cardHolder.webBack.setRotationY(0f);
                }

            }
        });

        btnAgain.setOnClickListener(v -> {
            // Đặt lại trạng thái thẻ hiện tại
            FrontLayoutUnder.setVisibility(View.VISIBLE);  // Hiển thị mặt trước
            BackLayoutUnder.setVisibility(View.GONE);     // Ẩn mặt sau
            WebView webFront = findViewById(R.id.WebViewCardItemFront);
            WebView webBack = findViewById(R.id.WebViewCardItemBack);

            // Lật lại thẻ về trạng thái ban đầu nếu đã lật
            if (webBack.getVisibility() == View.VISIBLE) {
                flipCard(webBack, webFront);
                fadeLayouts(FrontLayoutUnder, BackLayoutUnder, true);
            }

            // Cập nhật lại dữ liệu trong adapter khi mặt trước/sau thay đổi
            cardAdapter.notifyItemChanged(viewPagerStudyCard.getCurrentItem());
        });

        btnHard.setOnClickListener(v -> goToNextCard());
        btnGood.setOnClickListener(v -> goToNextCard());
        btnEasy.setOnClickListener(v -> goToNextCard());

    }

    // Hàm gói nội dung HTML
    private String wrapHtml(String content) {
        if (content == null) {
            content = "";
        }
        return "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0' />"
                + "<style>body{font-size:16px;text-align:center;word-wrap:break-word;max-width:100%; margin: 0 auto; max-height: 2000px; overflow-y: scroll;} img{max-width:100%; height:auto;}</style></head><body>"
                + content + "</body></html>";
    }


    // Hàm lật thẻ với hiệu ứng
    private void flipCard(View fromView, View toView) {
        // Kiểm tra nếu mặt trước đang hiển thị và mặt sau đang ẩn
        if (fromView.getVisibility() == View.VISIBLE && toView.getVisibility() == View.GONE) {

            // Tạo hiệu ứng lật từ mặt trước sang mặt sau
            ObjectAnimator hideFront = ObjectAnimator.ofFloat(fromView, "rotationY", 0f, 90f);
            ObjectAnimator showBack = ObjectAnimator.ofFloat(toView, "rotationY", -90f, 0f);

            // Đặt thời gian cho hiệu ứng
            hideFront.setDuration(200);
            showBack.setDuration(200);

            // Lắng nghe sự kiện khi hiệu ứng hideFront kết thúc
            hideFront.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    // Ẩn mặt trước
                    fromView.setVisibility(View.GONE);
                    // Hiển thị mặt sau
                    toView.setVisibility(View.VISIBLE);
                    // Bắt đầu hiệu ứng cho mặt sau
                    showBack.start();
                }
            });

            // Bắt đầu hiệu ứng lật mặt trước
            hideFront.start();
        }
    }
    // Hiệu ứng fade cho frontLayout và backLayout
    private void fadeLayouts(View frontLayout, View backLayout, boolean showFront) {
        // Đặt trạng thái hiển thị của các layout
        frontLayout.setVisibility(showFront ? View.VISIBLE : View.GONE);
        backLayout.setVisibility(showFront ? View.GONE : View.VISIBLE);

        // Ánh xạ animation cho frontLayout (fade vào hoặc ra)
        ObjectAnimator frontFade = ObjectAnimator.ofFloat(frontLayout, "alpha", showFront ? 0f : 1f, showFront ? 1f : 0f);
        frontFade.setDuration(500);

        // Ánh xạ animation cho backLayout (fade vào hoặc ra)
        ObjectAnimator backFade = ObjectAnimator.ofFloat(backLayout, "alpha", showFront ? 1f : 0f, showFront ? 0f : 1f);
        backFade.setDuration(500);

        // Chạy các animation đồng thời
        frontFade.start();
        backFade.start();
    }

    private void goToNextCard() {
        int currentItem = viewPagerStudyCard.getCurrentItem();
        int totalItems = cardAdapter.getItemCount();

        if (currentItem < totalItems - 1) {
            viewPagerStudyCard.setCurrentItem(currentItem + 1);
        } else {
            viewPagerStudyCard.setVisibility(View.GONE);
            FrontLayoutUnder.setVisibility(View.GONE);
            BackLayoutUnder.setVisibility(View.GONE);
            tvStudyCardCompletedShow.setVisibility(View.VISIBLE);
            btnStudyCardCompletedShow.setVisibility(View.VISIBLE);
        }
    }




}
