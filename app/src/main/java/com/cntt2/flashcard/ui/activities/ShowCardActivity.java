package com.cntt2.flashcard.ui.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.remote.dto.CardDto;
import com.cntt2.flashcard.data.remote.dto.DeskDto;
import com.cntt2.flashcard.data.repository.CardRepository;
import com.cntt2.flashcard.ui.adapters.ShowCardToPreviewAdapter;
import com.cntt2.flashcard.ui.animation.ZoomOutPageTransformer;
import com.cntt2.flashcard.utils.ConfirmDialog;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShowCardActivity extends BaseActivity {

    private ViewPager2 viewPagerStudyCard;
    private TextView tvShowAnswer;
    private TextView tvShowCardStep;
    private LinearLayout frontLayoutUnder, backLayoutUnder;
    private ImageButton btnExit;
    private ImageButton btnMenu;
    private ProgressBar progressBar;

    private String deskId;
    private int totalItem;
    private ShowCardToPreviewAdapter cardAdapter;
    private List<CardDto> cardList = new ArrayList<>();
    private CardRepository cardRepository = App.getInstance().getCardRepository();

    private static final int REQUEST_EDIT_CARD = 302;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_card);

        // Initialize views
        viewPagerStudyCard = findViewById(R.id.viewPagerShowCard);
        tvShowAnswer = findViewById(R.id.tvShowCardFromFront);
        frontLayoutUnder = findViewById(R.id.linearLayoutShowCardFrontLayoutUnder);
        backLayoutUnder = findViewById(R.id.linearLayoutShowCardBackLayoutUnder);
        tvShowCardStep = findViewById(R.id.tvShowCardStep);
        btnExit = findViewById(R.id.imageButtonShowCardCancel);
        btnMenu = findViewById(R.id.btnHamburgerMenuShowCardOption);
        progressBar = findViewById(R.id.progressBar); // Ensure progressBar is in layout

        // Get deskId and startPosition from Intent
        Intent intent = getIntent();
        deskId = intent.getStringExtra("deskId");
        int position = intent.getIntExtra("startPosition", 0);

        if (deskId == null) {
            Toast.makeText(this, "Invalid desk ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize adapter
        cardAdapter = new ShowCardToPreviewAdapter(cardList);
        viewPagerStudyCard.setAdapter(cardAdapter);
        viewPagerStudyCard.setPageTransformer(new ZoomOutPageTransformer());

        // Load cards from API
        loadCards(position);

        // Update step text
        viewPagerStudyCard.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                fadeLayouts(frontLayoutUnder, backLayoutUnder, true);
                String stepText = (position + 1) + "/" + totalItem;
                tvShowCardStep.setText(stepText);

                RecyclerView recyclerView = (RecyclerView) viewPagerStudyCard.getChildAt(0);
                RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);

                if (holder instanceof ShowCardToPreviewAdapter.CardViewHolder) {
                    ShowCardToPreviewAdapter.CardViewHolder cardHolder = (ShowCardToPreviewAdapter.CardViewHolder) holder;
                    if (cardHolder != null) {
                        cardHolder.webFront.setVisibility(View.VISIBLE);
                        cardHolder.webBack.setVisibility(View.GONE);
                        cardHolder.webFront.setRotationY(0f);
                        cardHolder.webBack.setRotationY(0f);
                    }
                }
            }
        });

        // Handle "Show Answer" click
        frontLayoutUnder.setOnClickListener(v -> {
            RecyclerView recyclerView = (RecyclerView) viewPagerStudyCard.getChildAt(0);
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(viewPagerStudyCard.getCurrentItem());

            if (holder instanceof ShowCardToPreviewAdapter.CardViewHolder) {
                ShowCardToPreviewAdapter.CardViewHolder cardHolder = (ShowCardToPreviewAdapter.CardViewHolder) holder;
                if (cardHolder != null) {
                    WebView webFront = cardHolder.webFront;
                    WebView webBack = cardHolder.webBack;

                    if (webFront.getVisibility() == View.VISIBLE) {
                        flipCard(webFront, webBack);
                        webFront.setVisibility(View.GONE);
                        webBack.setVisibility(View.VISIBLE);
                        fadeLayouts(frontLayoutUnder, backLayoutUnder, false);
                    }
                }
            }
        });

        backLayoutUnder.setOnClickListener(v -> {
            RecyclerView recyclerView = (RecyclerView) viewPagerStudyCard.getChildAt(0);
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(viewPagerStudyCard.getCurrentItem());

            if (holder instanceof ShowCardToPreviewAdapter.CardViewHolder) {
                ShowCardToPreviewAdapter.CardViewHolder cardHolder = (ShowCardToPreviewAdapter.CardViewHolder) holder;
                if (cardHolder != null) {
                    WebView webFront = cardHolder.webFront;
                    WebView webBack = cardHolder.webBack;

                    if (webBack.getVisibility() == View.VISIBLE) {
                        flipCard(webBack, webFront);
                        webFront.setVisibility(View.VISIBLE);
                        webBack.setVisibility(View.GONE);
                        fadeLayouts(frontLayoutUnder, backLayoutUnder, true);
                    }
                }
            }
        });

        btnExit.setOnClickListener(v -> finish());

        btnMenu.setOnClickListener(v -> {
            int currentPosition = viewPagerStudyCard.getCurrentItem();
            if (currentPosition >= 0 && currentPosition < cardList.size()) {
                CardDto currentCard = cardList.get(currentPosition);
                showCardMenu(currentCard, currentPosition);
            }
        });
    }

    private void loadCards(int startPosition) {
        progressBar.setVisibility(View.VISIBLE);
        cardRepository.getCardsByDeskId(deskId, new Callback<List<CardDto>>() {
            @Override
            public void onResponse(Call<List<CardDto>> call, Response<List<CardDto>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    cardList.clear();
                    cardList.addAll(response.body());
                    cardAdapter.notifyDataSetChanged();
                    totalItem = cardList.size();

                    if (cardList.isEmpty()) {
                        Toast.makeText(ShowCardActivity.this, "No cards found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    if (startPosition >= 0 && startPosition < totalItem) {
                        viewPagerStudyCard.setCurrentItem(startPosition, false);
                    }
                    String stepText = (startPosition + 1) + "/" + totalItem;
                    tvShowCardStep.setText(stepText);
                } else {
                    Toast.makeText(ShowCardActivity.this, "Failed to load cards", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<List<CardDto>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ShowCardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void showCardMenu(CardDto card, int position) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_card_menu, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView optionEdit = sheetView.findViewById(R.id.option_edit);
        TextView optionMoveToDesk = sheetView.findViewById(R.id.option_move_to_desk);
        TextView optionDelete = sheetView.findViewById(R.id.option_delete);

        optionEdit.setOnClickListener(v -> {
            editCard(card);
            bottomSheetDialog.dismiss();
        });

        optionMoveToDesk.setOnClickListener(v -> {
            moveCard(card);
            bottomSheetDialog.dismiss();
        });

        optionDelete.setOnClickListener(v -> {
            ConfirmDialog.createConfirmDialog(this, "Delete card", "Are you sure you want to delete this card", view -> {
                deleteCard(card, position);
            }, view -> {
                // Do nothing
            }).show();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void editCard(CardDto card) {
        Intent intent = new Intent(this, AddCardActivity.class);
        intent.putExtra("isEditMode", true);
        intent.putExtra("cardId", card.getId());
        intent.putExtra("deskId", deskId);
        startActivityForResult(intent, REQUEST_EDIT_CARD);
    }

    private void moveCard(CardDto card) {
        showChangeDeskDialog(card);
    }

    private void deleteCard(CardDto card, int position) {
        progressBar.setVisibility(View.VISIBLE);
        cardRepository.deleteCard(card.getId(), new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    cardList.remove(position);
                    cardAdapter.notifyItemRemoved(position);
                    totalItem = cardList.size();
                    int currentPosition = viewPagerStudyCard.getCurrentItem();
                    String stepText = (currentPosition + 1) + "/" + totalItem;
                    tvShowCardStep.setText(stepText);
                    Toast.makeText(ShowCardActivity.this, "Đã xóa thẻ", Toast.LENGTH_SHORT).show();

                    if (cardList.isEmpty()) {
                        finish();
                    }
                } else {
                    Toast.makeText(ShowCardActivity.this, "Failed to delete card", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ShowCardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showChangeDeskDialog(CardDto card) {
        View view = getLayoutInflater().inflate(R.layout.card_change_desk, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        Spinner deskSpinner = view.findViewById(R.id.destDeskSpinner);
        List<Map<String, String>> deskNames = new ArrayList<>();

        App.getInstance().getDeskRepository().getAllDesks(new Callback<List<DeskDto>>() {
            @Override
            public void onResponse(Call<List<DeskDto>> call, Response<List<DeskDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (DeskDto desk : response.body()) {
                        deskNames.add(Map.of(desk.getId(), desk.getName()));
                    }

                    String currentDesk = deskNames.stream()
                            .filter(d -> d.containsKey(card.getDeskId()))
                            .findFirst()
                            .map(d -> d.get(card.getDeskId()))
                            .orElse("");

                    deskNames.removeIf(d -> d.containsKey(card.getDeskId()));
                    deskNames.add(0, Map.of(card.getDeskId(), currentDesk));

                    List<String> deskNameList = new ArrayList<>();
                    for (Map<String, String> desk : deskNames) {
                        deskNameList.add(desk.values().iterator().next());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(ShowCardActivity.this, android.R.layout.simple_spinner_item, deskNameList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    deskSpinner.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<DeskDto>> call, Throwable t) {
                Toast.makeText(ShowCardActivity.this, "Failed to load desks", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btnCardChangeDeskSave).setOnClickListener(v -> {
            int selectedPosition = deskSpinner.getSelectedItemPosition();
            String selectedDeskId = deskNames.get(selectedPosition).keySet().iterator().next();

            card.setDeskId(selectedDeskId);
            progressBar.setVisibility(View.VISIBLE);
            cardRepository.updateCard(card.getId(), card, new ArrayList<>(), new Callback<CardDto>() {
                @Override
                public void onResponse(Call<CardDto> call, Response<CardDto> response) {
                    progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        Toast.makeText(ShowCardActivity.this, "Đã di chuyển thẻ", Toast.LENGTH_SHORT).show();
                        updateCardList();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(ShowCardActivity.this, "Failed to move card", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<CardDto> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ShowCardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void updateCardList() {
        loadCards(viewPagerStudyCard.getCurrentItem());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_CARD && resultCode == RESULT_OK) {
            updateCardList();
        }
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
}