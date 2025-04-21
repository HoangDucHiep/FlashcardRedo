package com.cntt2.flashcard.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.remote.dto.CardDto;
import com.cntt2.flashcard.data.remote.dto.DeskDto;
import com.cntt2.flashcard.data.repository.CardRepository;
import com.cntt2.flashcard.data.repository.DeskRepository;
import com.cntt2.flashcard.ui.activities.AddCardActivity;
import com.cntt2.flashcard.ui.activities.ShowCardActivity;
import com.cntt2.flashcard.ui.activities.StudyActivity;
import com.cntt2.flashcard.ui.adapters.FlashcardAdapter;
import com.cntt2.flashcard.utils.ConfirmDialog;
import com.cntt2.flashcard.utils.OptionsDialog;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CardsFragment extends Fragment implements FlashcardAdapter.OnCardLongClickListener, FlashcardAdapter.OnCardClickListener {
    private static final int REQUEST_START_A_LEARNING_SESSION = 301;
    private static final int REQUEST_EDIT_CARD = 302;

    private RecyclerView recyclerView;
    private FlashcardAdapter adapter;
    private List<CardDto> cardList = new ArrayList<>();
    private List<CardDto> cardsToReview = new ArrayList<>();
    private List<CardDto> newToLearn = new ArrayList<>();
    private EditText edtSearch;
    private TextView toLearn;
    private TextView toReview;
    private Button btnStartLearnSession;
    private ProgressBar progressBar;
    private final int MAX_CARDS = 200;
    private String deskId;
    private CardRepository cardRepository = App.getInstance().getCardRepository();
    private DeskRepository deskRepository = App.getInstance().getDeskRepository();

    public CardsFragment() {
    }

    public CardsFragment(List<CardDto> cardList) {
        this.cardList = cardList;
    }

    public static CardsFragment newInstance(String deskId) {
        CardsFragment fragment = new CardsFragment();
        Bundle args = new Bundle();
        args.putString("deskId", deskId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            deskId = getArguments().getString("deskId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cards, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        edtSearch = view.findViewById(R.id.edtSearch);
        btnStartLearnSession = view.findViewById(R.id.btnStartLearnSession);
        toLearn = view.findViewById(R.id.toLearn);
        toReview = view.findViewById(R.id.toReview);
        progressBar = view.findViewById(R.id.progressBar);

        adapter = new FlashcardAdapter(cardList, this, this);
        recyclerView.setAdapter(adapter);

        loadCards();

        edtSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCards(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnStartLearnSession.setOnClickListener(v -> {
            if (!cardList.isEmpty()) {
                Intent intent = new Intent(getContext(), StudyActivity.class);
                intent.putExtra("deskId", deskId);
                startActivityForResult(intent, REQUEST_START_A_LEARNING_SESSION);
            } else {
                Toast.makeText(getContext(), "Không có thẻ nào để học!", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void loadCards() {
        progressBar.setVisibility(View.VISIBLE);
        cardRepository.getCardsByDeskId(deskId, new Callback<List<CardDto>>() {
            @Override
            public void onResponse(Call<List<CardDto>> call, Response<List<CardDto>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    cardList = response.body();
                    adapter.setData(cardList);
                    updateToLearnAndToReview();
                } else {
                    Toast.makeText(getContext(), "Failed to load cards", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<CardDto>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateToLearnAndToReview() {
        progressBar.setVisibility(View.VISIBLE);
        cardRepository.getNewCards(deskId, new Callback<List<CardDto>>() {
            @Override
            public void onResponse(Call<List<CardDto>> call, Response<List<CardDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    newToLearn = response.body();
                    toLearn.setText(String.valueOf(newToLearn.size()));
                }
                loadCardsToReview();
            }

            @Override
            public void onFailure(Call<List<CardDto>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCardsToReview() {
        cardRepository.getCardsToReview(deskId, new Callback<List<CardDto>>() {
            @Override
            public void onResponse(Call<List<CardDto>> call, Response<List<CardDto>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    cardsToReview = response.body();
                    toReview.setText(String.valueOf(cardsToReview.size()));
                }
            }

            @Override
            public void onFailure(Call<List<CardDto>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_START_A_LEARNING_SESSION || requestCode == REQUEST_EDIT_CARD)
                && resultCode == getActivity().RESULT_OK) {
            loadCards();
        }
    }

    @Override
    public void onCardLongClick(CardDto card, int position) {
        List<OptionsDialog.Option> options = new ArrayList<>();
        options.add(new OptionsDialog.Option("Edit", R.drawable.ic_edit, 0xFFFFFFFF, () -> editCard(card)));
        options.add(new OptionsDialog.Option("Move to desk", R.drawable.ic_move, 0xFFFFFFFF, () -> moveCard(card)));
        options.add(new OptionsDialog.Option("Delete", R.drawable.ic_delete, 0xFFFF5555, () -> {
            ConfirmDialog.createConfirmDialog(requireContext(), "Delete card", "Are you sure you want to delete this card", view -> {
                deleteCard(card, position);
            }, view -> {
                // Do nothing
            }).show();
        }));

        OptionsDialog.showOptionsDialog(requireContext(), recyclerView.getChildAt(position), options);
    }

    @Override
    public void onCardClick(int position) {
        Intent intent = new Intent(getContext(), ShowCardActivity.class);
        intent.putExtra("deskId", deskId);
        intent.putExtra("startPosition", position);
        startActivityForResult(intent, REQUEST_START_A_LEARNING_SESSION);
    }

    private void editCard(CardDto card) {
        Intent intent = new Intent(getContext(), AddCardActivity.class);
        intent.putExtra("isEditMode", true);
        intent.putExtra("cardId", card.getId());
        intent.putExtra("deskId", deskId);
        startActivityForResult(intent, REQUEST_EDIT_CARD);
    }

    private void moveCard(CardDto card) {
        showChangeDeskDialog(card);
    }

    private void showChangeDeskDialog(CardDto card) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.card_change_desk, null);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(view);

        Spinner deskSpinner = view.findViewById(R.id.destDeskSpinner);
        List<Map<String, String>> deskNames = new ArrayList<>();

        deskRepository.getAllDesks(new Callback<List<DeskDto>>() {
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

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.spinner_item, deskNameList);
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                    deskSpinner.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<DeskDto>> call, Throwable t) {
                Toast.makeText(getContext(), "Failed to load desks", Toast.LENGTH_SHORT).show();
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
                        updateCardList();
                        dialog.dismiss();
                        Toast.makeText(getContext(), "Card moved", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to move card", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<CardDto> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void deleteCard(CardDto card, int position) {
        progressBar.setVisibility(View.VISIBLE);
        cardRepository.deleteCard(card.getId(), new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    cardList.remove(position);
                    adapter.notifyItemRemoved(position);
                    updateToLearnAndToReview();
                    Toast.makeText(getContext(), "Đã xóa thẻ", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Failed to delete card", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterCards(String keyword) {
        List<CardDto> filtered = new ArrayList<>();
        for (CardDto card : cardList) {
            if (card.getFront().toLowerCase().contains(keyword.toLowerCase())
                    || card.getBack().toLowerCase().contains(keyword.toLowerCase())) {
                filtered.add(card);
            }
        }
        adapter.setData(filtered);
    }

    public void updateCardList() {
        loadCards();
    }
}