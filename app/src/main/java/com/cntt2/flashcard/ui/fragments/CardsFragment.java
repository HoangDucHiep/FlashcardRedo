package com.cntt2.flashcard.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.repository.CardRepository;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.ui.adapters.FlashcardAdapter;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CardsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CardsFragment extends Fragment {

    private RecyclerView recyclerView;
    private FlashcardAdapter adapter;
    private List<Card> cardList = new ArrayList<>();
    private EditText edtSearch;
    private TextView txtCount;
    private final int MAX_CARDS = 200;
    private int deskId;
    private CardRepository cardRepository;

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
        cardRepository = new CardRepository(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cards, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        edtSearch = view.findViewById(R.id.edtSearch);
        txtCount = view.findViewById(R.id.txtCount);

        if (deskId != -1) {
            cardList = cardRepository.getCardsByDeskId(deskId);
        } else {
            cardList = new ArrayList<>(); // Nếu không có deskId, hiển thị danh sách rỗng
        }

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

        return view;
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



    public void addNewCard(Card newCard) {
        if (cardList.size() < MAX_CARDS) {
            cardList.add(newCard);
            updateCardCount();
            adapter.notifyDataSetChanged();
        }
    }
}