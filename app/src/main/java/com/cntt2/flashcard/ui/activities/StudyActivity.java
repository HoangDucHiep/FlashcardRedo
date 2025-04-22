package com.cntt2.flashcard.ui.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.cntt2.flashcard.data.remote.ApiService;
import com.cntt2.flashcard.data.remote.dto.CardDto;
import com.cntt2.flashcard.data.remote.dto.ReviewDto;
import com.cntt2.flashcard.data.remote.dto.SessionDto;
import com.cntt2.flashcard.data.repository.CardRepository;
import com.cntt2.flashcard.data.repository.LearningSessionRepository;
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
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudyActivity extends BaseActivity {

    private ViewPager2 viewPagerStudyCard;
    TextView tvShowAnswer, tvStudyCardCompletedShow, tvStudyCardStep, tvRateStudyCardCompletedShow;
    LinearLayout FrontLayoutUnder, BackLayoutUnder;
    Button btnAgain, btnHard, btnGood, btnEasy, btnStudyCardCompletedShow;
    ImageButton imageButtonStudyCardCancel, btnHamburgerMenuStudyCardOption;
    private ShowStudyCardToLearnAdapter cardAdapter;

    CardRepository cardRepository = App.getInstance().getCardRepository();
    LearningSessionRepository sessionRepository = App.getInstance().getLearningSessionRepository();
    ApiService apiService = App.getInstance().getApiService();

    private int totalItems;
    private String deskId;
    private SessionDto currentSession;
    private String sessionId;
    private List<CardDto> cardList;
    private Map<String, Integer> cardResponses;
    private List<CardDto> cardsToRelearn;
    private int correctAnswers = 0;

    private static final String TAG = "StudyActivity";
    private static final String BASE_URL = "http://10.0.2.2:5029";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startstudy);

        // Initialize views
        viewPagerStudyCard = findViewById(R.id.viewPagerStudyCard);
        tvShowAnswer = findViewById(R.id.tvStudyCardFrontShowAnswer);
        FrontLayoutUnder = findViewById(R.id.linearLayoutStudyCardFrontLayoutUnder);
        BackLayoutUnder = findViewById(R.id.linearLayoutStudyCardBackLayoutUnder);
        btnAgain = findViewById(R.id.btnStudyCardBackAgain);
        btnHard = findViewById(R.id.btnStudyCardBackHard);
        btnGood = findViewById(R.id.btnStudyCardBackGood);
        btnEasy = findViewById(R.id.btnStudyCardBackEady);
        tvStudyCardCompletedShow = findViewById(R.id.tvStudyCardCompletedShow);
        tvRateStudyCardCompletedShow = findViewById(R.id.tvRateStudyCardCompletedShow);
        btnStudyCardCompletedShow = findViewById(R.id.btnStudyCardCompletedShow);
        tvStudyCardStep = findViewById(R.id.tvStudyCardStep);
        imageButtonStudyCardCancel = findViewById(R.id.imageButtonStudyCardCancel);
        btnHamburgerMenuStudyCardOption = findViewById(R.id.btnHamburgerMenuStudyCardOption);

        // Get deskId from intent
        Intent intent = getIntent();
        deskId = intent.getStringExtra("deskId");
        if (deskId == null) {
            Log.e(TAG, "deskId is null");
            finish();
            return;
        }

        // Initialize data
        cardList = new ArrayList<>();
        cardsToRelearn = new ArrayList<>();
        cardResponses = new HashMap<>();
        currentSession = new SessionDto();
        currentSession.setDeskId(deskId);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String currentTime = sdf.format(new Date());
        currentSession.setStartTime(currentTime);

        // Fetch cards
        fetchCards();

        // Setup UI
        viewPagerStudyCard.setUserInputEnabled(false);
        viewPagerStudyCard.setPageTransformer(new ZoomOutPageTransformer());

        // Handle Show Answer click
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

            cardAdapter.notifyItemChanged(viewPagerStudyCard.getCurrentItem());
        });

        // Setup response buttons
        setupResponseButtons();

        // Handle cancel
        imageButtonStudyCardCancel.setOnClickListener(v -> cancelSession());

        // Handle session completion
        btnStudyCardCompletedShow.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
    }

    private void fetchCards() {
        cardList.clear();
        // Fetch new cards
        cardRepository.getNewCards(deskId, new Callback<List<CardDto>>() {
            @Override
            public void onResponse(Call<List<CardDto>> call, Response<List<CardDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cardList.addAll(response.body());
                    // Fetch cards to review
                    cardRepository.getCardsToReview(deskId, new Callback<List<CardDto>>() {
                        @Override
                        public void onResponse(Call<List<CardDto>> call, Response<List<CardDto>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                cardList.addAll(response.body());
                                initializeAdapter();
                            } else {
                                Log.e(TAG, "Failed to fetch review cards: " + response.message());
                                initializeAdapter();
                            }
                        }

                        @Override
                        public void onFailure(Call<List<CardDto>> call, Throwable t) {
                            Log.e(TAG, "Network error fetching review cards: " + t.getMessage());
                            initializeAdapter();
                        }
                    });
                } else {
                    Log.e(TAG, "Failed to fetch new cards: " + response.message());
                    initializeAdapter();
                }
            }

            @Override
            public void onFailure(Call<List<CardDto>> call, Throwable t) {
                Log.e(TAG, "Network error fetching new cards: " + t.getMessage());
                initializeAdapter();
            }
        });
    }

    private void initializeAdapter() {
        if (cardList.isEmpty()) {
            cardRepository.getCardsByDeskId(deskId, new Callback<List<CardDto>>() {
                @Override
                public void onResponse(Call<List<CardDto>> call, Response<List<CardDto>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        cardList.addAll(response.body());
                        setupAdapter();
                    } else {
                        Log.e(TAG, "Failed to fetch all cards: " + response.message());
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<List<CardDto>> call, Throwable t) {
                    Log.e(TAG, "Network error fetching all cards: " + t.getMessage());
                    finish();
                }
            });
        } else {
            setupAdapter();
        }
    }

    private void setupAdapter() {
        // Wrap HTML content
        cardList.forEach(card -> {
            card.setFront(wrapHtml(card.getFront()));
            card.setBack(wrapHtml(card.getBack()));
        });

        cardAdapter = new ShowStudyCardToLearnAdapter(cardList);
        viewPagerStudyCard.setAdapter(cardAdapter);
        totalItems = cardAdapter.getItemCount();

        // Update step text
        viewPagerStudyCard.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                fadeLayouts(FrontLayoutUnder, BackLayoutUnder, true);
                String stepText = getString(R.string.study_card_step, position + 1, totalItems);
                tvStudyCardStep.setText(stepText);

                RecyclerView recyclerView = (RecyclerView) viewPagerStudyCard.getChildAt(0);
                RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
                if (holder instanceof ShowStudyCardToLearnAdapter.CardViewHolder) {
                    ShowStudyCardToLearnAdapter.CardViewHolder cardHolder = (ShowStudyCardToLearnAdapter.CardViewHolder) holder;
                    cardHolder.webFront.setVisibility(View.VISIBLE);
                    cardHolder.webBack.setVisibility(View.GONE);
                    cardHolder.webFront.setRotationY(0f);
                    cardHolder.webBack.setRotationY(0f);
                }
            }
        });
    }

    private void setupResponseButtons() {
        btnAgain.setOnClickListener(v -> {
            int currentPosition = viewPagerStudyCard.getCurrentItem();
            CardDto currentCard = cardList.get(currentPosition);
            cardsToRelearn.add(currentCard);
            recordResponse(currentCard.getId(), 0);
            goToNextCard();
        });

        btnHard.setOnClickListener(v -> {
            int currentPosition = viewPagerStudyCard.getCurrentItem();
            CardDto currentCard = cardList.get(currentPosition);
            recordResponse(currentCard.getId(), 1);
            goToNextCard();
        });

        btnGood.setOnClickListener(v -> {
            int currentPosition = viewPagerStudyCard.getCurrentItem();
            CardDto currentCard = cardList.get(currentPosition);
            recordResponse(currentCard.getId(), 2);
            goToNextCard();
        });

        btnEasy.setOnClickListener(v -> {
            int currentPosition = viewPagerStudyCard.getCurrentItem();
            CardDto currentCard = cardList.get(currentPosition);
            recordResponse(currentCard.getId(), 3);
            goToNextCard();
        });
    }

    private void recordResponse(String cardId, int quality) {
        cardResponses.put(cardId, quality);
        if (quality >= 2) {
            correctAnswers++;
        }
    }

    private void completeSession() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String endTime = sdf.format(new Date());
        currentSession.setEndTime(endTime);
        currentSession.setCardsStudied(cardResponses.size());
        currentSession.setPerformance(cardResponses.size() > 0 ? (double) correctAnswers / cardResponses.size() : 0);

        // Save session
        sessionRepository.insertSession(currentSession, new Callback<SessionDto>() {
            @Override
            public void onResponse(Call<SessionDto> call, Response<SessionDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessionId = response.body().getId();
                    Log.d(TAG, "Session saved - ID: " + sessionId);
                    updateReviews();
                } else {
                    Log.e(TAG, "Failed to save session: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<SessionDto> call, Throwable t) {
                Log.e(TAG, "Network error saving session: " + t.getMessage());
            }
        });
    }

    private void updateReviews() {
        for (Map.Entry<String, Integer> entry : cardResponses.entrySet()) {
            String cardId = entry.getKey();
            int quality = entry.getValue();

            apiService.getReviewByCardId(cardId).enqueue(new Callback<ReviewDto>() {
                @Override
                public void onResponse(Call<ReviewDto> call, Response<ReviewDto> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ReviewDto review = response.body();
                        updateReviewAfterStudy(review, quality);
                        apiService.updateReview(review.getId(), review).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    Log.d(TAG, "Review updated - Card ID: " + cardId);
                                } else {
                                    Log.e(TAG, "Failed to update review: " + response.message());
                                }
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Log.e(TAG, "Network error updating review: " + t.getMessage());
                            }
                        });
                    } else {
                        Log.e(TAG, "Failed to fetch review for card: " + cardId);
                    }
                }

                @Override
                public void onFailure(Call<ReviewDto> call, Throwable t) {
                    Log.e(TAG, "Network error fetching review: " + t.getMessage());
                }
            });
        }

        // Show completion UI
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

        Button btnYes = dialogView.findViewById(R.id.btn_dialog_yes);
        Button btnNo = dialogView.findViewById(R.id.btn_dialog_no);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateReviewAfterStudy(ReviewDto review, int quality) {
        if (quality < 0 || quality > 3) return;

        if (review.getInterval() == 0) {
            review.setInterval(1);
            review.setRepetition(1);
        } else {
            if (quality == 0) {
                review.setRepetition(0);
                review.setInterval(1);
            } else {
                review.setRepetition(review.getRepetition() + 1);
                if (quality == 1) {
                    review.setInterval(review.getInterval());
                } else if (quality == 2) {
                    review.setInterval(review.getInterval() + 1);
                } else if (quality == 3) {
                    review.setInterval(review.getInterval() * 2);
                }
            }
        }

        if (quality == 0) {
            review.setEase(Math.max(1.3, review.getEase() - 0.8));
        } else if (quality == 1) {
            review.setEase(review.getEase() - 0.15);
        } else if (quality >= 2) {
            review.setEase(review.getEase() + 0.15);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, review.getInterval());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        review.setNextReviewDate(sdf.format(calendar.getTime()));
        review.setLastReviewed(sdf.format(new Date()));
    }

    private String wrapHtml(String content) {
        if (content == null) {
            content = "";
        }
        return "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0' />"
                + "<style>body{ background-color: rgb(67, 78, 170); color:white;font-size:16px;text-align:center;word-wrap:break-word;max-width:100%; margin: 0 auto; max-height: 2000px; overflow-y: scroll;} img{max-width:100%; height:auto;}</style></head><body>"
                + content + "</body></html>";
    }

    private void flipCard(View fromView, View toView) {
        if (fromView.getVisibility() == View.VISIBLE && toView.getVisibility() == View.GONE) {
            ObjectAnimator hideFront = ObjectAnimator.ofFloat(fromView, "rotationY", 0f, 90f);
            ObjectAnimator showBack = ObjectAnimator.ofFloat(toView, "rotationY", -90f, 0f);

            hideFront.setDuration(200);
            showBack.setDuration(200);

            hideFront.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    fromView.setVisibility(View.GONE);
                    toView.setVisibility(View.VISIBLE);
                    showBack.start();
                }
            });

            hideFront.start();
        }
    }

    private void fadeLayouts(View frontLayout, View backLayout, boolean showFront) {
        frontLayout.setVisibility(showFront ? View.VISIBLE : View.GONE);
        backLayout.setVisibility(showFront ? View.GONE : View.VISIBLE);

        ObjectAnimator frontFade = ObjectAnimator.ofFloat(frontLayout, "alpha", showFront ? 0f : 1f, showFront ? 1f : 0f);
        frontFade.setDuration(500);

        ObjectAnimator backFade = ObjectAnimator.ofFloat(backLayout, "alpha", showFront ? 1f : 0f, showFront ? 0f : 1f);
        backFade.setDuration(500);

        frontFade.start();
        backFade.start();
    }

    private void goToNextCard() {
        int currentItem = viewPagerStudyCard.getCurrentItem();
        int totalItems = cardAdapter.getItemCount();

        if (currentItem < totalItems - 1) {
            viewPagerStudyCard.setCurrentItem(currentItem + 1);
        } else {
            if (!cardsToRelearn.isEmpty()) {
                cardList.clear();
                cardList.addAll(cardsToRelearn);
                cardsToRelearn.clear();
                cardAdapter = new ShowStudyCardToLearnAdapter(cardList);
                viewPagerStudyCard.setAdapter(cardAdapter);
                viewPagerStudyCard.setCurrentItem(0);
            } else {
                completeSession();
            }
        }
    }
}