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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.repository.CardRepository;
import com.cntt2.flashcard.data.repository.LearningSessionRepository;
import com.cntt2.flashcard.data.repository.ReviewRepository;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.model.LearningSession;
import com.cntt2.flashcard.model.Review;
import com.cntt2.flashcard.ui.adapters.ShowStudyCardToLearnAdapter;
import com.cntt2.flashcard.ui.animation.ZoomOutPageTransformer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudyActivity extends AppCompatActivity {

    private ViewPager2 viewPagerStudyCard;
    TextView tvShowAnswer, tvStudyCardCompletedShow, tvStudyCardStep, tvRateStudyCardCompletedShow;
    LinearLayout FrontLayoutUnder, BackLayoutUnder;
    Button btnAgain, btnHard, btnGood, btnEasy, btnStudyCardCompletedShow;
    ImageButton  imageButtonStudyCardCancel, btnHamburgerMenuStudyCardOption;
    private ShowStudyCardToLearnAdapter cardAdapter;

    CardRepository cardRepository = App.getInstance().getCardRepository();
    LearningSessionRepository sessionRepository = App.getInstance().getLearningSessionRepository();

    private int deskId;

    private LearningSession currentSession;
    private long sessionId = -1; // ID của session sau khi lưu
    private List<Card> cardList; // Danh sách thẻ để học
    private Map<Integer, Integer> cardResponses; // Lưu phản hồi của người dùng cho từng thẻ (cardId -> quality)
    private List<Card> cardsToRelearn;
    private int correctAnswers = 0; // Số câu trả lời đúng để tính hiệu suất

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
        tvRateStudyCardCompletedShow= findViewById(R.id.tvRateStudyCardCompletedShow);
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
        cardList = new ArrayList<>();
        cardList.addAll(cardRepository.getNewCards(deskId));
        cardList.addAll(cardRepository.getCardsToReview(deskId));

        if (cardList.size() == 0) {
            cardList.addAll(cardRepository.getCardsByDeskId(deskId));
        }

        cardsToRelearn = new ArrayList<>();

        currentSession = new LearningSession();
        currentSession.setDeskId(deskId);
        String currentTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());
        currentSession.setStartTime(currentTime);
        cardResponses = new HashMap<>();


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



        // Xử lý nút đánh giá
        setupResponseButtons();
        // Nút thoát
        imageButtonStudyCardCancel.setOnClickListener(v -> cancelSession());
        // Nút hoàn thành
        btnStudyCardCompletedShow.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
    }

    private void setupResponseButtons() {
        btnAgain.setOnClickListener(v -> {
            int currentPosition = viewPagerStudyCard.getCurrentItem();
            Card currentCard = cardList.get(currentPosition);
            cardsToRelearn.add(currentCard); // Thêm vào danh sách học lại
            recordResponse(0); // Ghi nhận phản hồi "Again"
            goToNextCard(); // Chuyển sang thẻ tiếp theo
        });

        btnHard.setOnClickListener(v -> {
            recordResponse(1); // Quality = 1 (Hard)
            goToNextCard();
        });

        btnGood.setOnClickListener(v -> {
            recordResponse(2); // Quality = 2 (Good)
            goToNextCard();
        });

        btnEasy.setOnClickListener(v -> {
            recordResponse(3); // Quality = 3 (Easy)
            goToNextCard();
        });
    }

    private void recordResponse(int quality) {
        int currentPosition = viewPagerStudyCard.getCurrentItem();
        Card currentCard = cardList.get(currentPosition);
        cardResponses.put(currentCard.getId(), quality);
        if (quality >= 2) { // Good or Easy is considered correct
            correctAnswers++;
        }
    }

    private void resetCardState() {
        FrontLayoutUnder.setVisibility(View.VISIBLE);
        BackLayoutUnder.setVisibility(View.GONE);
        WebView webFront = findViewById(R.id.WebViewCardItemFront);
        WebView webBack = findViewById(R.id.WebViewCardItemBack);
        if (webBack.getVisibility() == View.VISIBLE) {
            flipCard(webBack, webFront);
            fadeLayouts(FrontLayoutUnder, BackLayoutUnder, true);
        }
        cardAdapter.notifyItemChanged(viewPagerStudyCard.getCurrentItem());
    }

    private void completeSession() {
        String endTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());
        currentSession.setEndTime(endTime);
        currentSession.setCardsStudied(cardResponses.size());
        currentSession.setPerformance((double) correctAnswers / cardResponses.size());

        // Lưu session vào cơ sở dữ liệu
        sessionId = sessionRepository.insertSession(currentSession);

        // Cập nhật review cho từng thẻ
        ReviewRepository reviewRepository = App.getInstance().getReviewRepository();
        for (Map.Entry<Integer, Integer> entry : cardResponses.entrySet()) {
            int cardId = entry.getKey();
            int quality = entry.getValue();
            Review review = reviewRepository.getReviewByCardId(cardId);
            if (review != null) {
                updateReviewAfterStudy(review, quality);
                reviewRepository.updateReview(review);
            }
        }

        viewPagerStudyCard.setVisibility(View.GONE);
        FrontLayoutUnder.setVisibility(View.GONE);
        BackLayoutUnder.setVisibility(View.GONE);
        imageButtonStudyCardCancel.setVisibility(View.GONE);
        tvStudyCardCompletedShow.setVisibility(View.VISIBLE);
        btnStudyCardCompletedShow.setVisibility(View.VISIBLE);
        String performanceText = correctAnswers + "/" + cardResponses.size() + " (" + String.format(Locale.getDefault(), "%.2f", currentSession.getPerformance() * 100) + "%)";
        tvRateStudyCardCompletedShow.setText(performanceText);
        tvRateStudyCardCompletedShow.setVisibility(View.VISIBLE);
    }

    private void cancelSession() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_cancel, null);
        builder.setView(dialogView);

        // Ánh xạ các thành phần trong dialog
        Button btnYes = dialogView.findViewById(R.id.btn_dialog_yes);
        Button btnNo = dialogView.findViewById(R.id.btn_dialog_no);

        // Tạo dialog
        AlertDialog dialog = builder.create();

        // Đặt background trong suốt để hiển thị bo góc
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Xử lý sự kiện cho nút Yes
        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            finish(); // Thoát activity nếu người dùng xác nhận
        });

        // Xử lý sự kiện cho nút No
        btnNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateReviewAfterStudy(Review review, int quality) {
        if (quality < 0 || quality > 3) return;

        if (review.getInterval() == 0) { // Thẻ mới
            review.setInterval(1);
            review.setRepetition(1);
        } else {
            if (quality == 0) { // Again
                review.setRepetition(0);
                review.setInterval(1);
            } else {
                review.setRepetition(review.getRepetition() + 1);
                if (quality == 1) { // Hard
                    review.setInterval(review.getInterval());
                } else if (quality == 2) { // Good
                    review.setInterval(review.getInterval() + 1);
                } else if (quality == 3) { // Easy
                    review.setInterval(review.getInterval() * 2);
                }
            }
        }

        // Cập nhật ease
        if (quality == 0) {
            review.setEase(Math.max(1.3, review.getEase() - 0.8));
        } else if (quality == 1) {
            review.setEase(review.getEase() - 0.15);
        } else if (quality >= 2) {
            review.setEase(review.getEase() + 0.15);
        }

        // Cập nhật ngày ôn tập tiếp theo
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, review.getInterval());
        review.setNextReviewDate(new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime()));
        review.setLastReviewed(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
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

            // Kiểm tra nếu còn thẻ trong cardsToRelearn
            if (!cardsToRelearn.isEmpty()) {
                cardList.clear(); // Xóa danh sách cũ
                cardList.addAll(cardsToRelearn); // Thêm danh sách học lại
                cardsToRelearn.clear(); // Xóa danh sách tạm
                cardAdapter = new ShowStudyCardToLearnAdapter(cardList);
                viewPagerStudyCard.setAdapter(cardAdapter);
                viewPagerStudyCard.setCurrentItem(0); // Bắt đầu lại từ thẻ đầu tiên
            } else {
                completeSession(); // Hoàn thành nếu không còn thẻ nào
            }
        }
    }




}
