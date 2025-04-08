package com.cntt2.flashcard.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.repository.CardRepository;
import com.cntt2.flashcard.data.repository.ReviewRepository;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.ui.activities.AddCardActivity;
import com.cntt2.flashcard.ui.activities.StudyActivity;
import com.cntt2.flashcard.ui.adapters.FlashcardAdapter;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CardsFragment extends Fragment implements FlashcardAdapter.OnCardLongClickListener {
    private static final int REQUEST_START_A_LEARNING_SESSION = 301;
    private static final int REQUEST_EDIT_CARD = 302;

    private RecyclerView recyclerView;
    private FlashcardAdapter adapter;
    private List<Card> cardList = new ArrayList<>();
    private List<Card> cardsToReview = new ArrayList<>();
    private List<Card> newToLearn = new ArrayList<>();

    private EditText edtSearch;
    private TextView txtCount;
    private TextView toLearn;
    private TextView toReview;
    private Button btnStartLearnSession;
    private final int MAX_CARDS = 200;
    private int deskId;
    private CardRepository cardRepository = App.getInstance().getCardRepository();
    private ReviewRepository reviewRepository = App.getInstance().getReviewRepository();

    public CardsFragment() {
        // Required empty public constructor
    }

    public CardsFragment(List<Card> cardList) {
        this.cardList = cardList;
    }

    public static CardsFragment newInstance(String param1, String param2) {
        CardsFragment fragment = new CardsFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            deskId = getArguments().getInt("deskId", -1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cards, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        edtSearch = view.findViewById(R.id.edtSearch);
        txtCount = view.findViewById(R.id.txtCount);
        btnStartLearnSession = view.findViewById(R.id.btnStartLearnSession);
        toLearn = view.findViewById(R.id.toLearn);
        toReview = view.findViewById(R.id.toReview);

        if (deskId != -1) {
            cardList = cardRepository.getCardsByDeskId(deskId);
        } else {
            cardList = new ArrayList<>();
        }

        cardsToReview = cardRepository.getCardsToReview(deskId);
        newToLearn = cardRepository.getNewCards(deskId);
        toLearn.setText(String.valueOf(newToLearn.size()));
        toReview.setText(String.valueOf(cardsToReview.size()));

        adapter = new FlashcardAdapter(cardList, this); // Truyền this làm listener
        recyclerView.setAdapter(adapter);
        updateCardCount();

        edtSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // filterCards(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnStartLearnSession.setOnClickListener(v -> {
            if (cardList.size() > 0) {
                Intent intent = new Intent(getContext(), StudyActivity.class);
                intent.putExtra("deskId", deskId);
                startActivityForResult(intent, REQUEST_START_A_LEARNING_SESSION);
            } else {
                Toast.makeText(getContext(), "Không có thẻ nào để học!", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_START_A_LEARNING_SESSION || requestCode == REQUEST_EDIT_CARD)
                && resultCode == getActivity().RESULT_OK) {
            updateToLearnAndToReview();
            cardList = cardRepository.getCardsByDeskId(deskId);
            adapter.setData(cardList);
            updateCardCount();
        }
    }

    @Override
    public void onCardLongClick(Card card, int position) {
        PopupMenu popup = new PopupMenu(getContext(), recyclerView.getChildAt(position));
        popup.getMenuInflater().inflate(R.menu.card_popup_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.mnuEditCard) {
                editCard(card);
                return true;
            } else if (itemId == R.id.mnuMoveCard) {
                moveCard(card);
                return true;
            } else if (itemId == R.id.mnuDeleteCard) {
                deleteCard(card, position);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void editCard(Card card) {
        Intent intent = new Intent(getContext(), AddCardActivity.class);
        intent.putExtra("isEditMode", true);
        intent.putExtra("cardId", card.getId());
        startActivityForResult(intent, REQUEST_EDIT_CARD);
    }

    private void moveCard(Card card) {
        // TODO: Thêm logic để di chuyển card sang desk khác
        Toast.makeText(getContext(), "Chức năng Move to Desk chưa được triển khai", Toast.LENGTH_SHORT).show();
    }

    private void deleteCard(Card card, int position) {
        cardRepository.deleteCard(card);
        cardList.remove(position);
        adapter.notifyItemRemoved(position);
        updateCardCount();
        updateToLearnAndToReview();
        Toast.makeText(getContext(), "Đã xóa thẻ", Toast.LENGTH_SHORT).show();
    }

    private void updateCardCount() {
        txtCount.setText(cardList.size() + " / " + MAX_CARDS);
    }

    private void filterCards(String keyword) {
        List<Card> filtered = new ArrayList<>();
        for (Card card : cardList) {
            if (card.getFront().toLowerCase().contains(keyword.toLowerCase())) {
                filtered.add(card);
            }
        }
        adapter.setData(filtered);
    }

    private void updateToLearnAndToReview() {
        cardsToReview = cardRepository.getCardsToReview(deskId);
        newToLearn = cardRepository.getNewCards(deskId);
        toLearn.setText(String.valueOf(newToLearn.size()));
        toReview.setText(String.valueOf(cardsToReview.size()));
    }

    public void addNewCard() {
        cardList = cardRepository.getCardsByDeskId(deskId);
        adapter.setData(cardList);
        updateCardCount();
        updateToLearnAndToReview();
    }
}