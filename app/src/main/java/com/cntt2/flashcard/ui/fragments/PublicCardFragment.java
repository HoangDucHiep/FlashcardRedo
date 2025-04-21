package com.cntt2.flashcard.ui.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.remote.ApiClient;
import com.cntt2.flashcard.data.remote.ApiService;
import com.cntt2.flashcard.data.remote.dto.CardDto;
import com.cntt2.flashcard.data.remote.dto.DeskDto;
import com.cntt2.flashcard.data.repository.CardRepository;
import com.cntt2.flashcard.data.repository.DeskRepository;
import com.cntt2.flashcard.data.repository.FolderRepository;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.model.Desk;
import com.cntt2.flashcard.model.Folder;
import com.cntt2.flashcard.ui.adapters.PublicCardAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PublicCardFragment extends Fragment {

    private static final String ARG_DESK_ID = "desk_id";
    private static final String ARG_IS_PUBLIC = "is_public";

    private String deskId;
    private boolean isPublic;
    private RecyclerView recyclerView;
    private EditText edtSearch;
    private TextView txtCount;
    private Button btnAction;
    private PublicCardAdapter adapter;
    private List<CardDto> cards = new ArrayList<>();
    private List<CardDto> filteredCards = new ArrayList<>();
    private DeskRepository deskRepository;
    private CardRepository cardRepository;
    private FolderRepository folderRepository;

    public PublicCardFragment() {
        // Required empty public constructor
    }

    public static PublicCardFragment newInstance(String deskId, boolean isPublic) {
        PublicCardFragment fragment = new PublicCardFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DESK_ID, deskId);
        args.putBoolean(ARG_IS_PUBLIC, isPublic);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            deskId = getArguments().getString(ARG_DESK_ID);
            isPublic = getArguments().getBoolean(ARG_IS_PUBLIC);
        }
        deskRepository = App.getInstance().getDeskRepository();
        cardRepository = App.getInstance().getCardRepository();
        folderRepository = App.getInstance().getFolderRepository();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cards, container, false);

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerView);
        edtSearch = view.findViewById(R.id.edtSearch);
        btnAction = view.findViewById(R.id.btnStartLearnSession);

        // Hide To Learn and To Review sections
        view.findViewById(R.id.constraintLayout).setVisibility(View.GONE);

        btnAction.setText("Clone Desk");
        btnAction.setOnClickListener(v -> cloneDesk());

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PublicCardAdapter(filteredCards);
        recyclerView.setAdapter(adapter);

        // Fetch cards
        if (isPublic) {
            fetchPublicCards();
        } else {
            fetchLocalCards();
        }

        // Set up search functionality
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCards(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void fetchPublicCards() {
        ApiService apiService = ApiClient.getApiService();
        Call<List<CardDto>> call = apiService.getCardsByDeskId(deskId);
        call.enqueue(new Callback<List<CardDto>>() {
            @Override
            public void onResponse(Call<List<CardDto>> call, Response<List<CardDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cards.clear();
                    cards.addAll(response.body());
                    filteredCards.clear();
                    filteredCards.addAll(cards);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(requireContext(), "Failed to load cards", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<CardDto>> call, Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PublicCardFragment", "Failed to fetch cards", t);
            }
        });
    }

    private void fetchLocalCards() {
        Toast.makeText(requireContext(), "Local cards not supported in this context", Toast.LENGTH_SHORT).show();
    }

    private void filterCards(String query) {
        filteredCards.clear();
        if (query.isEmpty()) {
            filteredCards.addAll(cards);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            for (CardDto card : cards) {
                if (card.getFront().toLowerCase(Locale.getDefault()).contains(lowerQuery) ||
                        card.getBack().toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                    filteredCards.add(card);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void cloneDesk() {
        // Show folder selection dialog
        showFolderSelectionDialog();
    }

    private void showFolderSelectionDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
//        builder.setTitle("Select Folder");
//
//        // Fetch all folders using FolderRepository
//        List<Folder> folders = folderRepository.getAllFolders();
//        List<String> folderNames = new ArrayList<>();
//        List<Integer> folderIds = new ArrayList<>();
//        folderNames.add("No Folder");
//        folderIds.add(null);
//        for (Folder folder : folders) {
//            folderNames.add(folder.getName());
//            folderIds.add(folder.getId());
//        }
//
//        // Create spinner
//        Spinner spinner = new Spinner(requireContext());
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(
//                requireContext(),
//                R.layout.spinner_item,  // Layout tùy chỉnh cho item
//                folderNames
//        );
//        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);  // Layout tùy chỉnh cho dropdown
//        spinner.setAdapter(adapter);
//
//        builder.setView(spinner);
//        builder.setPositiveButton("Clone", (dialog, which) -> {
//            int selectedPosition = spinner.getSelectedItemPosition();
//            Integer selectedFolderId = folderIds.get(selectedPosition);
//
//            // Proceed with cloning
//            performClone(selectedFolderId);
//        });
//        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
//        builder.show();
//    }
//
//    private void performClone(Integer selectedFolderId) {
//        ApiService apiService = ApiClient.getApiService();
//        Call<DeskDto> deskCall = apiService.getDeskById(deskId);
//        deskCall.enqueue(new Callback<DeskDto>() {
//            @Override
//            public void onResponse(Call<DeskDto> call, Response<DeskDto> response) {
//                String originalDeskName = "Cloned Desk";
//                if (response.isSuccessful() && response.body() != null) {
//                    originalDeskName = response.body().getName();
//                } else if (response.code() == 404) {
//                    Toast.makeText(requireContext(), "Desk not found", Toast.LENGTH_SHORT).show();
//                    return;
//                } else {
//                    Toast.makeText(requireContext(), "Failed to load desk", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//
//                Desk newDesk = new Desk();
//                newDesk.setName(originalDeskName + " - Cloned");
//                newDesk.setFolderId(selectedFolderId);
//                newDesk.setPublic(false);
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
//                newDesk.setCreatedAt(sdf.format(new Date()));
//                newDesk.setLastModified(sdf.format(new Date()));
//                newDesk.setSyncStatus("pending_create");
//
//                long newdeskId = deskRepository.insertDesk(newDesk);
//                if (newdeskId == -1) {
//                    Toast.makeText(requireContext(), "Failed to clone desk", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//
//                Call<List<CardDto>> cardCall = apiService.getCardsByDeskId(deskId);
//                cardCall.enqueue(new Callback<List<CardDto>>() {
//                    @Override
//                    public void onResponse(Call<List<CardDto>> call, Response<List<CardDto>> response) {
//                        if (response.isSuccessful() && response.body() != null) {
//                            List<CardDto> cards = response.body();
//                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
//                            for (CardDto cardDto : cards) {
//                                Card newCard = new Card();
//                                newCard.setFront(cardDto.getFront());
//                                newCard.setBack(cardDto.getBack());
//                                newCard.setDeskId((int) newdeskId);
//                                newCard.setCreatedAt(sdf.format(new Date()));
//                                newCard.setLastModified(sdf.format(new Date()));
//                                newCard.setSyncStatus("pending_create");
//
//                                long newCardId = cardRepository.insertCard(newCard, false);
//                                if (newCardId == -1) {
//                                    Log.e("PublicCardFragment", "Failed to insert card for deskId: " + newdeskId);
//                                }
//                            }
//
//                            Toast.makeText(requireContext(), "Desk cloned successfully", Toast.LENGTH_SHORT).show();
//                        } else {
//                            Toast.makeText(requireContext(), "Failed to load cards", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//
//                    @Override
//                    public void onFailure(Call<List<CardDto>> call, Throwable t) {
//                        Toast.makeText(requireContext(), "Error fetching cards: " + t.getMessage(), Toast.LENGTH_SHORT).show();
//                        Log.e("PublicCardFragment", "Failed to fetch cards for cloning", t);
//                    }
//                });
//            }
//
//
//            @Override
//            public void onFailure(Call<DeskDto> call, Throwable t) {
//                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
//                Log.e("PublicCardFragment", "Failed to fetch desk information", t);
//            }
//        });
    }
}