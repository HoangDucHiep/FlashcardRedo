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
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.remote.dto.CardDto;
import com.cntt2.flashcard.data.remote.dto.DeskDto;
import com.cntt2.flashcard.data.remote.dto.GetFolderDto;
import com.cntt2.flashcard.data.repository.CardRepository;
import com.cntt2.flashcard.data.repository.DeskRepository;
import com.cntt2.flashcard.data.repository.FolderRepository;
import com.cntt2.flashcard.ui.adapters.PublicCardAdapter;

import java.util.ArrayList;
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
    private Button btnAction;
    private ProgressBar progressBar;
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
        progressBar = view.findViewById(R.id.progressBar);

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
            Toast.makeText(requireContext(), "Local cards not supported in this context", Toast.LENGTH_SHORT).show();
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
        progressBar.setVisibility(View.VISIBLE);
        cardRepository.getCardsByDeskId(deskId, new Callback<List<CardDto>>() {
            @Override
            public void onResponse(Call<List<CardDto>> call, Response<List<CardDto>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    cards.clear();
                    cards.addAll(response.body());
                    filteredCards.clear();
                    filteredCards.addAll(cards);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(requireContext(), "Failed to load cards", Toast.LENGTH_SHORT).show();
                    Log.e("PublicCardFragment", "Failed to fetch cards: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<CardDto>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PublicCardFragment", "Network error fetching cards", t);
            }
        });
    }

    private void cloneDesk() {
        if (deskId == null) {
            Toast.makeText(requireContext(), "Invalid desk ID", Toast.LENGTH_SHORT).show();
            return;
        }
        showFolderSelectionDialog();
    }

    private void showFolderSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme);
        builder.setTitle("Select Folder");

        // Create spinner view
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_folder_selection, null);
        Spinner spinner = dialogView.findViewById(R.id.folderSpinner);
        builder.setView(dialogView);

        // Fetch user folders
        progressBar.setVisibility(View.VISIBLE);
        folderRepository.getUserFolders(new Callback<List<GetFolderDto>>() {
            @Override
            public void onResponse(Call<List<GetFolderDto>> call, Response<List<GetFolderDto>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<GetFolderDto> folders = response.body();
                    if (folders.isEmpty()) {
                        Toast.makeText(requireContext(), "No folders available. Please create a folder first.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    List<String> folderNames = new ArrayList<>();
                    List<String> folderIds = new ArrayList<>();
                    for (GetFolderDto folder : folders) {
                        folderNames.add(folder.getName());
                        folderIds.add(folder.getId());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            R.layout.spinner_item,
                            folderNames
                    );
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                    spinner.setAdapter(adapter);

                    // Set dialog buttons
                    builder.setPositiveButton("Clone", (dialog, which) -> {
                        int selectedPosition = spinner.getSelectedItemPosition();
                        String selectedFolderId = folderIds.get(selectedPosition);
                        performClone(selectedFolderId);
                    });
                    builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                    builder.show();
                } else {
                    Toast.makeText(requireContext(), "Failed to load folders", Toast.LENGTH_SHORT).show();
                    Log.e("PublicCardFragment", "Failed to fetch folders: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<GetFolderDto>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PublicCardFragment", "Network error fetching folders", t);
            }
        });
    }

    private void performClone(String targetFolderId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Clone")
                .setMessage("Clone this desk to the selected folder?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    deskRepository.cloneDesk(deskId, targetFolderId, new Callback<DeskDto>() {
                        @Override
                        public void onResponse(Call<DeskDto> call, Response<DeskDto> response) {
                            progressBar.setVisibility(View.GONE);
                            if (response.isSuccessful() && response.body() != null) {
                                Toast.makeText(requireContext(), "Desk cloned successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                String message = response.message();
                                if (response.code() == 400) {
                                    message = "Invalid request: Desk not public, folder not owned, or folder ID missing";
                                } else if (response.code() == 404) {
                                    message = "Desk or folder not found";
                                }
                                Toast.makeText(requireContext(), "Failed to clone desk: " + message, Toast.LENGTH_SHORT).show();
                                Log.e("PublicCardFragment", "Failed to clone desk: " + response.message());
                            }
                        }

                        @Override
                        public void onFailure(Call<DeskDto> call, Throwable t) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("PublicCardFragment", "Network error cloning desk", t);
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
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
}