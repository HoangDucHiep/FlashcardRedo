package com.cntt2.flashcard.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.repository.CardRepository;
import com.cntt2.flashcard.data.repository.ReviewRepository;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.ui.activities.AddCardActivity;
import com.cntt2.flashcard.ui.activities.ListCardActivity;
import com.cntt2.flashcard.ui.activities.StudyActivity;
import com.cntt2.flashcard.ui.adapters.FlashcardAdapter;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CardsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CardsFragment extends Fragment {
    private static final int REQUEST_START_A_LEARNING_SESSION = 301;


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

    public CardsFragment(List<Card> cardList) {
        this.cardList = cardList;
    }


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public CardsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CardsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CardsFragment newInstance(String param1, String param2) {
        CardsFragment fragment = new CardsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            deskId = getArguments().getInt("deskId", -1); // Lấy deskId từ Bundle
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cards, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        edtSearch = view.findViewById(R.id.edtSearch);
        txtCount = view.findViewById(R.id.txtCount);
        btnStartLearnSession = view.findViewById(R.id.btnStartLearnSession);

        if (deskId != -1) {
            cardList = cardRepository.getCardsByDeskId(deskId);
        } else {
            cardList = new ArrayList<>(); // Nếu không có deskId, hiển thị danh sách rỗng
        }

        toLearn = view.findViewById(R.id.toLearn);
        toReview = view.findViewById(R.id.toReview);

        cardsToReview = cardRepository.getCardsToReview(deskId);
        newToLearn = cardRepository.getNewCards(deskId);

        toLearn.setText(String.valueOf(newToLearn.size()));
        toReview.setText(String.valueOf(cardsToReview.size()));

        adapter = new FlashcardAdapter(cardList);
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        updateCardCount();
        // Handle search functionality
        edtSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                //filterCards(charSequence.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable editable) {}
        });

        btnStartLearnSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cardList.size() > 0) {
                    // call StudyActivity
                    Intent intent = new Intent(getContext(), StudyActivity.class);
                    intent.putExtra("deskId", deskId);

                    // Chuyển đến StudyActivity
                    startActivityForResult(intent, REQUEST_START_A_LEARNING_SESSION);

                } else {
                    // Hiển thị thông báo nếu không có thẻ nào
                    Toast.makeText(getContext(), "Không có thẻ nào để học!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_START_A_LEARNING_SESSION && resultCode == getActivity().RESULT_OK) {
            updateToLearnAndToReview();
        }
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
        adapter.setData(filtered); //
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