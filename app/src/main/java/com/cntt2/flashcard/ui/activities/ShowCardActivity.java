package com.cntt2.flashcard.ui.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.R;
import com.cntt2.flashcard.ui.adapters.ShowCardToPreviewAdapter;
import com.cntt2.flashcard.ui.animation.ZoomOutPageTransformer;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.model.Desk;
import com.cntt2.flashcard.utils.ConfirmDialog;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShowCardActivity extends AppCompatActivity {

    private ViewPager2 viewPagerStudyCard;
    TextView tvShowAnswer;
    TextView tvShowCardStep;
    LinearLayout FrontLayoutUnder, BackLayoutUnder;
    ImageButton btnExit;
    ImageButton btnMenu;

    int deskId;
    int totalItem;

    private ShowCardToPreviewAdapter cardAdapter;
    private List<Card> cardList;

    private static final int REQUEST_EDIT_CARD = 302;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_card);

        // Ánh xạ ViewPager2
        viewPagerStudyCard = findViewById(R.id.viewPagerShowCard);
        tvShowAnswer = findViewById(R.id.tvShowCardFromFront);
        FrontLayoutUnder = findViewById(R.id.linearLayoutShowCardFrontLayoutUnder);
        BackLayoutUnder = findViewById(R.id.linearLayoutShowCardBackLayoutUnder);
        tvShowCardStep = findViewById(R.id.tvShowCardStep);
        btnExit = findViewById(R.id.imageButtonShowCardCancel);
        btnMenu = findViewById(R.id.btnHamburgerMenuShowCardOption);

        Intent intent = getIntent();

        deskId = intent.getIntExtra("deskId", -1);
        int position = intent.getIntExtra("startPosition", 0);

        // Tạo danh sách thẻ
        cardList = App.getInstance().getCardRepository().getCardsByDeskId(deskId);

        // Tạo adapter và gắn vào ViewPager2
        cardAdapter = new ShowCardToPreviewAdapter(cardList);
        viewPagerStudyCard.setAdapter(cardAdapter);

        // Thêm hiệu ứng hoạt ảnh khi chuyển thẻ (tuỳ chọn)
        viewPagerStudyCard.setPageTransformer(new ZoomOutPageTransformer());

        totalItem = cardAdapter.getItemCount();

        if (position >= 0 && position < totalItem) {
            viewPagerStudyCard.setCurrentItem(position, false);
        }

        String stepText = (position + 1) + "/" + totalItem;
        tvShowCardStep.setText(stepText);

        // Đăng ký callback để theo dõi sự kiện khi vuốt qua các thẻ
        viewPagerStudyCard.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                fadeLayouts(FrontLayoutUnder, BackLayoutUnder, true);

                String stepText = (position + 1) + "/" + totalItem;
                tvShowCardStep.setText(stepText);

                // Lấy RecyclerView từ ViewPager2
                RecyclerView recyclerView = (RecyclerView) viewPagerStudyCard.getChildAt(0);

                // Lấy ViewHolder tại vị trí hiện tại
                RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);

                if (holder instanceof ShowCardToPreviewAdapter.CardViewHolder) {
                    ShowCardToPreviewAdapter.CardViewHolder cardHolder = (ShowCardToPreviewAdapter.CardViewHolder) holder;

                    // Đảm bảo hiển thị mặt trước và ẩn mặt sau
                    if (cardHolder != null) {
                        cardHolder.webFront.setVisibility(View.VISIBLE);
                        cardHolder.webBack.setVisibility(View.GONE);

                        cardHolder.webFront.setRotationY(0f);
                        cardHolder.webBack.setRotationY(0f);
                    }
                }
            }
        });

        // Xử lý sự kiện nhấn vào "Show Answer"
        FrontLayoutUnder.setOnClickListener(v -> {
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
                        fadeLayouts(FrontLayoutUnder, BackLayoutUnder, false);
                    } else {
                        flipCard(webBack, webFront);
                        webFront.setVisibility(View.VISIBLE);
                        webBack.setVisibility(View.GONE);
                        fadeLayouts(FrontLayoutUnder, BackLayoutUnder, true);
                    }
                }
            }
        });

        BackLayoutUnder.setOnClickListener(v -> {
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
                        fadeLayouts(FrontLayoutUnder, BackLayoutUnder, true);
                    }
                }
            }
        });

        btnExit.setOnClickListener(v -> {
            finish();
        });

        // Xử lý sự kiện nhấn vào nút menu
        btnMenu.setOnClickListener(v -> {
            int currentPosition = viewPagerStudyCard.getCurrentItem();
            if (currentPosition >= 0 && currentPosition < cardList.size()) {
                Card currentCard = cardList.get(currentPosition);
                showCardMenu(currentCard, currentPosition);
            }
        });
    }

    // Hiển thị Bottom Sheet Menu
    private void showCardMenu(Card card, int position) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_card_menu, null);
        bottomSheetDialog.setContentView(sheetView);

        // Ánh xạ các tùy chọn trong bottom sheet
        TextView optionEdit = sheetView.findViewById(R.id.option_edit);
        TextView optionMoveToDesk = sheetView.findViewById(R.id.option_move_to_desk);
        TextView optionDelete = sheetView.findViewById(R.id.option_delete);

        // Xử lý sự kiện nhấn vào từng tùy chọn
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

    // Chỉnh sửa Card
    private void editCard(Card card) {
        Intent intent = new Intent(this, AddCardActivity.class);
        intent.putExtra("isEditMode", true);
        intent.putExtra("cardId", card.getId());
        startActivityForResult(intent, REQUEST_EDIT_CARD);
    }

    // Di chuyển Card sang desk khác
    private void moveCard(Card card) {
        showChangeDeskDialog(card);
    }

    // Xóa Card
    private void deleteCard(Card card, int position) {
        App.getInstance().getCardRepository().deleteCard(card);
        cardList.remove(position);
        cardAdapter.notifyItemRemoved(position);
        totalItem = cardList.size();

        // Cập nhật lại bước hiển thị
        int currentPosition = viewPagerStudyCard.getCurrentItem();
        String stepText = (currentPosition + 1) + "/" + totalItem;
        tvShowCardStep.setText(stepText);

        Toast.makeText(this, "Đã xóa thẻ", Toast.LENGTH_SHORT).show();

        // Nếu xóa hết thẻ, thoát activity
        if (cardList.isEmpty()) {
            finish();
        }
    }

    // Hiển thị dialog để chọn desk mới
    private void showChangeDeskDialog(Card card) {
        View view = getLayoutInflater().inflate(R.layout.card_change_desk, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        Spinner deskSpinner = view.findViewById(R.id.destDeskSpinner);
        List<Map<Integer, String>> deskNames = new ArrayList<>();

        var deskRepository = App.getInstance().getDeskRepository();
        var desks = deskRepository.getAllDesks();

        for (Desk desk : desks) {
            deskNames.add(Map.of(desk.getId(), desk.getName()));
        }

        String currentDesk = deskRepository.getDeskById(card.getDeskId()).getName();

        deskNames.remove(Map.of(card.getDeskId(), currentDesk));
        deskNames.add(0, Map.of(card.getDeskId(), currentDesk));

        // Lấy danh sách tên desk
        List<String> deskNameList = new ArrayList<>();
        for (Map<Integer, String> desk : deskNames) {
            for (String name : desk.values()) {
                deskNameList.add(name);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deskNameList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deskSpinner.setAdapter(adapter);

        view.findViewById(R.id.btnCardChangeDeskSave).setOnClickListener(v -> {
            int selectedPosition = deskSpinner.getSelectedItemPosition();
            int selectedDeskId = (int) deskNames.get(selectedPosition).keySet().toArray()[0];

            card.setDeskId(selectedDeskId);
            App.getInstance().getCardRepository().updateCard(card);

            Toast.makeText(this, "Đã di chuyển thẻ", Toast.LENGTH_SHORT).show();
            dialog.dismiss();

            // Cập nhật danh sách thẻ
            updateCardList();
        });

        dialog.show();
    }

    // Cập nhật danh sách thẻ sau khi chỉnh sửa hoặc di chuyển
    private void updateCardList() {
        cardList = App.getInstance().getCardRepository().getCardsByDeskId(deskId);
        cardAdapter = new ShowCardToPreviewAdapter(cardList);
        viewPagerStudyCard.setAdapter(cardAdapter);
        totalItem = cardList.size();

        // Cập nhật lại bước hiển thị
        int currentPosition = viewPagerStudyCard.getCurrentItem();
        if (currentPosition >= totalItem) {
            currentPosition = totalItem - 1;
        }
        if (currentPosition >= 0) {
            viewPagerStudyCard.setCurrentItem(currentPosition, false);
            String stepText = (currentPosition + 1) + "/" + totalItem;
            tvShowCardStep.setText(stepText);
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_CARD && resultCode == RESULT_OK) {
            updateCardList();
        }
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

    // Hiệu ứng fade cho frontLayout và backLayout
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