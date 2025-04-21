package com.cntt2.flashcard.ui.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.remote.ApiService;
import com.cntt2.flashcard.data.remote.dto.DeskDto;
import com.cntt2.flashcard.data.remote.dto.GetFolderDto;
import com.cntt2.flashcard.data.remote.dto.NestedFolderDto;
import com.cntt2.flashcard.data.remote.dto.PostFolderDto;
import com.cntt2.flashcard.data.repository.DeskRepository;
import com.cntt2.flashcard.data.repository.FolderRepository;
import com.cntt2.flashcard.ui.activities.ListCardActivity;
import com.cntt2.flashcard.ui.adapters.ShowFoldersAndDecksAdapter;
import com.cntt2.flashcard.utils.ConfirmDialog;
import com.cntt2.flashcard.utils.OptionsDialog;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private ListView showFolderAndDeckLV;
    private ArrayList<NestedFolderDto> nestedFolders;
    private ShowFoldersAndDecksAdapter adapter;
    private FloatingActionButton addFolderAndDeckFAB;
    private FolderRepository folderRepository;
    private DeskRepository deskRepository;
    private SearchView searchView;
    private List<DeskDto> allDesks;
    private ProgressBar progressBar;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        showFolderAndDeckLV = view.findViewById(R.id.ShowFoldersAndDecksLV);
        addFolderAndDeckFAB = view.findViewById(R.id.AddFoldersAndDecksFAB);
        progressBar = view.findViewById(R.id.progressBar); // Add to layout
        searchView = view.findViewById(R.id.SearchFoldersAndDecksSV);

        folderRepository = App.getInstance().getFolderRepository();
        deskRepository = App.getInstance().getDeskRepository();

        nestedFolders = new ArrayList<>();
        allDesks = new ArrayList<>();
        adapter = new ShowFoldersAndDecksAdapter(requireActivity(), nestedFolders, allDesks);
        showFolderAndDeckLV.setAdapter(adapter);

        fetchNestedFolders();

        addFolderAndDeckFAB.setOnClickListener(v -> showCreateBottomSheet());

        int searchTextId = searchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        EditText searchEditText = searchView.findViewById(searchTextId);
        if (searchEditText != null) {
            searchEditText.setTextColor(Color.WHITE);
        }

        showFolderAndDeckLV.setOnItemLongClickListener((parent, view1, position, id) -> {
            showPopupMenu(view1, position);
            return true;
        });

        showFolderAndDeckLV.setOnItemClickListener((parent, view1, position, id) -> {
            Object selectedItem = adapter.getItem(position);
            if (selectedItem instanceof DeskDto) {
                DeskDto selectedDesk = (DeskDto) selectedItem;
                Intent intent = new Intent(getActivity(), ListCardActivity.class);
                intent.putExtra("deskId", selectedDesk.getId());
                startActivity(intent);
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterDesks(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterDesks(newText);
                return false;
            }
        });

        return view;
    }

    private void fetchNestedFolders() {
        progressBar.setVisibility(View.VISIBLE);
        folderRepository.getNestedFolders(new Callback<List<NestedFolderDto>>() {
            @Override
            public void onResponse(Call<List<NestedFolderDto>> call, Response<List<NestedFolderDto>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    nestedFolders.clear();
                    nestedFolders.addAll(response.body());
                    allDesks.clear();
                    allDesks.addAll(getAllDesksFromFolders(nestedFolders));
                    adapter.updateFolderList(nestedFolders);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(requireContext(), "Failed to load folders", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<NestedFolderDto>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPopupMenu(View view, int position) {
        Object selectedItem = adapter.getItem(position);
        List<OptionsDialog.Option> options = new ArrayList<>();

        if (selectedItem instanceof NestedFolderDto) {
            NestedFolderDto folder = (NestedFolderDto) selectedItem;
            options.add(new OptionsDialog.Option("Edit", R.drawable.ic_edit, 0xFFFFFFFF, () -> editFolder(folder)));
            options.add(new OptionsDialog.Option("Delete", R.drawable.ic_delete, 0xFFFF5555, () -> {
                AlertDialog dialog = ConfirmDialog.createConfirmDialog(
                        requireContext(),
                        "Delete Folder",
                        "Are you sure you want to delete this folder?",
                        v -> deleteFolder(folder),
                        v -> {}
                );
                dialog.show();
            }));
        } else if (selectedItem instanceof DeskDto) {
            DeskDto desk = (DeskDto) selectedItem;
            options.add(new OptionsDialog.Option("Edit", R.drawable.ic_edit, 0xFFFFFFFF, () -> editDesk(desk)));
            options.add(new OptionsDialog.Option(desk.isPublic() ? "Make private" : "Make public", R.drawable.ic_public, 0xFFFFFFFF, () -> handlePublicDesk(desk)));
            options.add(new OptionsDialog.Option("Delete", R.drawable.ic_delete, 0xFFFF5555, () -> {
                AlertDialog dialog = ConfirmDialog.createConfirmDialog(
                        requireContext(),
                        "Delete Desk",
                        "Are you sure you want to delete this desk?",
                        v -> deleteDesk(desk),
                        v -> {}
                );
                dialog.show();
            }));
        }

        OptionsDialog.showOptionsDialog(requireContext(), view, options);
    }

    private void handlePublicDesk(DeskDto desk) {
        progressBar.setVisibility(View.VISIBLE);
        DeskDto updatedDesk = new DeskDto();
        updatedDesk.setId(desk.getId());
        updatedDesk.setName(desk.getName());
        updatedDesk.setPublic(!desk.isPublic());
        updatedDesk.setFolderId(desk.getFolderId());
        updatedDesk.setCreatedAt(desk.getCreatedAt());
        updatedDesk.setLastModified(desk.getLastModified());

        deskRepository.updateDesk(desk.getId(), updatedDesk, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    fetchNestedFolders();
                    Toast.makeText(getContext(), "Desk visibility updated", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Failed to update desk visibility", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void editFolder(NestedFolderDto folder) {
        showEditFolderDialog(folder);
    }

    private void showEditFolderDialog(NestedFolderDto folder) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_create_folder, null);
        BottomSheetDialog editFolderDialog = new BottomSheetDialog(requireContext());
        editFolderDialog.setContentView(view);

        EditText folderNameInput = view.findViewById(R.id.folderNameInput);
        folderNameInput.setText(folder.getName());

        Spinner parentFolderSpinner = view.findViewById(R.id.parentFolderSpinner);
        List<String> folderNames = getFolderNamesWithIndent(folder.getId());
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item, folderNames
        );
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        parentFolderSpinner.setAdapter(spinnerAdapter);

        List<NestedFolderDto> allFolders = getAllFoldersList(folder.getId());
        int parentPosition = 0;
        if (folder.getParentFolderId() != null) {
            for (int i = 0; i < allFolders.size(); i++) {
                if (allFolders.get(i).getId().equals(folder.getParentFolderId())) {
                    parentPosition = i + 1;
                    break;
                }
            }
        }
        parentFolderSpinner.setSelection(parentPosition);

        view.findViewById(R.id.btnFolderSave).setOnClickListener(v -> {
            String newName = folderNameInput.getText().toString().trim();
            if (newName.isEmpty()) {
                folderNameInput.setError("Folder name cannot be empty");
                return;
            }

            PostFolderDto updatedFolder = new PostFolderDto();
            updatedFolder.setName(newName);
            int selectedPosition = parentFolderSpinner.getSelectedItemPosition();
            if (selectedPosition == 0) {
                updatedFolder.setParentFolderId(null);
            } else {
                updatedFolder.setParentFolderId(allFolders.get(selectedPosition - 1).getId());
            }

            progressBar.setVisibility(View.VISIBLE);
            folderRepository.updateFolder(folder.getId(), updatedFolder, new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        fetchNestedFolders();
                        Toast.makeText(getContext(), "Folder updated", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to update folder", Toast.LENGTH_SHORT).show();
                    }
                    editFolderDialog.dismiss();
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    editFolderDialog.dismiss();
                }
            });
        });

        editFolderDialog.show();
    }

    private void deleteFolder(NestedFolderDto folder) {
        progressBar.setVisibility(View.VISIBLE);
        folderRepository.deleteFolder(folder.getId(), new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    fetchNestedFolders();
                    Toast.makeText(getContext(), "Folder deleted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Failed to delete folder", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void editDesk(DeskDto desk) {
        showEditDeskDialog(desk);
    }

    private void showEditDeskDialog(DeskDto desk) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_create_deck, null);
        BottomSheetDialog editDeskDialog = new BottomSheetDialog(requireContext());
        editDeskDialog.setContentView(view);

        EditText deckNameInput = view.findViewById(R.id.deckNameInput);
        deckNameInput.setText(desk.getName());

        Spinner folderSpinner = view.findViewById(R.id.folderSpinner);
        List<String> folderNames = getFolderNamesWithIndent();
        folderNames.set(0, getString(R.string.select_folder_prompt));
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item, folderNames
        );
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        folderSpinner.setAdapter(spinnerAdapter);

        List<NestedFolderDto> allFolders = getAllFoldersList();
        int folderPosition = 0;
        for (int i = 0; i < allFolders.size(); i++) {
            if (allFolders.get(i).getId().equals(desk.getFolderId())) {
                folderPosition = i + 1;
                break;
            }
        }
        folderSpinner.setSelection(folderPosition);

        view.findViewById(R.id.btnDeckSave).setOnClickListener(v -> {
            String newName = deckNameInput.getText().toString().trim();
            if (newName.isEmpty()) {
                deckNameInput.setError("Deck name cannot be empty");
                return;
            }

            int selectedPosition = folderSpinner.getSelectedItemPosition();
            if (selectedPosition == 0) {
                Toast.makeText(requireContext(), "Please select a folder", Toast.LENGTH_SHORT).show();
                return;
            }

            DeskDto updatedDesk = new DeskDto();
            updatedDesk.setId(desk.getId());
            updatedDesk.setName(newName);
            updatedDesk.setPublic(desk.isPublic());
            updatedDesk.setFolderId(allFolders.get(selectedPosition - 1).getId());
            updatedDesk.setCreatedAt(desk.getCreatedAt());
            updatedDesk.setLastModified(desk.getLastModified());

            progressBar.setVisibility(View.VISIBLE);
            deskRepository.updateDesk(desk.getId(), updatedDesk, new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        fetchNestedFolders();
                        Toast.makeText(getContext(), "Desk updated", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to update desk", Toast.LENGTH_SHORT).show();
                    }
                    editDeskDialog.dismiss();
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    editDeskDialog.dismiss();
                }
            });
        });

        editDeskDialog.show();
    }

    private void deleteDesk(DeskDto desk) {
        progressBar.setVisibility(View.VISIBLE);
        deskRepository.deleteDesk(desk.getId(), new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    fetchNestedFolders();
                    Toast.makeText(getContext(), "Desk deleted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Failed to delete desk", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterDesks(String query) {
        if (query.isEmpty()) {
            adapter.setSearchMode(false, null);
        } else {
            List<DeskDto> filteredDesks = new ArrayList<>();
            for (DeskDto desk : allDesks) {
                if (desk.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredDesks.add(desk);
                }
            }
            adapter.setSearchMode(true, filteredDesks);
        }
        adapter.notifyDataSetChanged();
    }

    private void showCreateBottomSheet() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_create_new, null);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(view);
        dialog.show();

        view.findViewById(R.id.CreateFolder).setOnClickListener(v -> {
            showCreateFolderDialog();
            dialog.dismiss();
        });
        view.findViewById(R.id.CreateDeck).setOnClickListener(v -> {
            showCreateDeckDialog();
            dialog.dismiss();
        });
    }

    private void showCreateFolderDialog() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_create_folder, null);
        BottomSheetDialog folderDialog = new BottomSheetDialog(requireContext());
        folderDialog.setContentView(view);

        Spinner parentFolderSpinner = view.findViewById(R.id.parentFolderSpinner);
        List<String> folderNames = getFolderNamesWithIndent();
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item, folderNames
        );
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        parentFolderSpinner.setAdapter(spinnerAdapter);

        view.findViewById(R.id.btnFolderSave).setOnClickListener(v -> {
            EditText folderNameInput = view.findViewById(R.id.folderNameInput);
            String folderName = folderNameInput.getText().toString().trim();
            if (folderName.isEmpty()) {
                folderNameInput.setError("Folder name cannot be empty");
                return;
            }

            PostFolderDto newFolder = new PostFolderDto();
            newFolder.setName(folderName);
            int selectedPosition = parentFolderSpinner.getSelectedItemPosition();
            if (selectedPosition > 0) {
                newFolder.setParentFolderId(getAllFoldersList().get(selectedPosition - 1).getId());
            }

            progressBar.setVisibility(View.VISIBLE);
            folderRepository.insertFolder(newFolder, new Callback<GetFolderDto>() {
                @Override
                public void onResponse(Call<GetFolderDto> call, Response<GetFolderDto> response) {
                    progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        fetchNestedFolders();
                        Toast.makeText(requireContext(), "Folder created successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to create folder", Toast.LENGTH_SHORT).show();
                    }
                    folderDialog.dismiss();
                }

                @Override
                public void onFailure(Call<GetFolderDto> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    folderDialog.dismiss();
                }
            });
        });

        folderDialog.show();
    }

    private void showCreateDeckDialog() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_create_deck, null);
        BottomSheetDialog deckDialog = new BottomSheetDialog(requireContext());
        deckDialog.setContentView(view);

        Spinner folderSpinner = view.findViewById(R.id.folderSpinner);
        List<String> folderNames = getFolderNamesWithIndent();
        folderNames.set(0, getString(R.string.select_folder_prompt));
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item, folderNames
        );
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        folderSpinner.setAdapter(spinnerAdapter);

        view.findViewById(R.id.btnDeckSave).setOnClickListener(v -> {
            EditText deckNameInput = view.findViewById(R.id.deckNameInput);
            String deckName = deckNameInput.getText().toString().trim();
            if (deckName.isEmpty()) {
                deckNameInput.setError("Deck name cannot be empty");
                return;
            }

            int selectedPosition = folderSpinner.getSelectedItemPosition();
            if (selectedPosition == 0) {
                Toast.makeText(requireContext(), "Please select a folder", Toast.LENGTH_SHORT).show();
                return;
            }

            DeskDto newDesk = new DeskDto();
            newDesk.setName(deckName);
            newDesk.setPublic(false);
            newDesk.setFolderId(getAllFoldersList().get(selectedPosition - 1).getId());

            progressBar.setVisibility(View.VISIBLE);
            deskRepository.insertDesk(newDesk, new Callback<DeskDto>() {
                @Override
                public void onResponse(Call<DeskDto> call, Response<DeskDto> response) {
                    progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        fetchNestedFolders();
                        Toast.makeText(requireContext(), "Desk created successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to create desk", Toast.LENGTH_SHORT).show();
                    }
                    deckDialog.dismiss();
                }

                @Override
                public void onFailure(Call<DeskDto> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    deckDialog.dismiss();
                }
            });
        });

        deckDialog.show();
    }

    private List<String> getFolderNamesWithIndent() {
        return getFolderNamesWithIndent(null);
    }

    private List<String> getFolderNamesWithIndent(String excludedFolderId) {
        List<String> folderNames = new ArrayList<>();
        folderNames.add(getString(R.string.no_parent_folder));
        getAllFoldersWithIndent(nestedFolders, folderNames, "", excludedFolderId);
        return folderNames;
    }

    private void getAllFoldersWithIndent(List<NestedFolderDto> folders, List<String> folderNames, String indent, String excludedFolderId) {
        for (NestedFolderDto folder : folders) {
            if (folder.getId().equals(excludedFolderId)) {
                continue;
            }
            folderNames.add(indent + folder.getName());
            getAllFoldersWithIndent(folder.getSubFolders(), folderNames, indent + "  ", excludedFolderId);
        }
    }

    private List<NestedFolderDto> getAllFoldersList() {
        return getAllFoldersList(null);
    }

    private List<NestedFolderDto> getAllFoldersList(String excludedFolderId) {
        List<NestedFolderDto> allFolders = new ArrayList<>();
        getAllFolders(nestedFolders, allFolders, excludedFolderId);
        return allFolders;
    }

    private void getAllFolders(List<NestedFolderDto> folders, List<NestedFolderDto> allFolders, String excludedFolderId) {
        for (NestedFolderDto folder : folders) {
            if (folder.getId().equals(excludedFolderId)) {
                continue;
            }
            allFolders.add(folder);
            getAllFolders(folder.getSubFolders(), allFolders, excludedFolderId);
        }
    }

    private List<DeskDto> getAllDesksFromFolders(List<NestedFolderDto> folders) {
        List<DeskDto> desks = new ArrayList<>();
        for (NestedFolderDto folder : folders) {
            desks.addAll(folder.getDesks());
            desks.addAll(getAllDesksFromFolders(folder.getSubFolders()));
        }
        return desks;
    }
}