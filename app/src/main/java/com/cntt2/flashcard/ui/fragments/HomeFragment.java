package com.cntt2.flashcard.ui.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.repository.DeskRepository;
import com.cntt2.flashcard.data.repository.FolderRepository;
import com.cntt2.flashcard.model.Desk;
import com.cntt2.flashcard.model.Folder;
import com.cntt2.flashcard.ui.activities.ListCardActivity;
import com.cntt2.flashcard.ui.adapters.ShowFoldersAndDecksAdapter;
import com.cntt2.flashcard.utils.ConfirmDialog;
import com.cntt2.flashcard.utils.OptionsDialog;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private ListView ShowFolderAndDeckLV;
    private ArrayList<Folder> nestedFoldersDesks;
    private ShowFoldersAndDecksAdapter adapter;
    private FloatingActionButton AddFolderAndDeckFAB;
    private FolderRepository folderRepository = App.getInstance().getFolderRepository();
    private DeskRepository deskRepository = App.getInstance().getDeskRepository();
    private SearchView searchView;
    private List<Desk> allDesks;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        ShowFolderAndDeckLV = view.findViewById(R.id.ShowFoldersAndDecksLV);
        AddFolderAndDeckFAB = view.findViewById(R.id.AddFoldersAndDecksFAB);
        nestedFoldersDesks = getFoldersFromLocalDb();
        allDesks = deskRepository.getAllDesks();
        adapter = new ShowFoldersAndDecksAdapter(getActivity(), nestedFoldersDesks, allDesks);
        ShowFolderAndDeckLV.setAdapter(adapter);
        AddFolderAndDeckFAB.setOnClickListener(v -> showCreateBottomSheet());
        searchView = view.findViewById(R.id.SearchFoldersAndDecksSV);

        ShowFolderAndDeckLV.setOnItemLongClickListener((parent, view1, position, id) -> {
            showPopupMenu(view1, position);
            return true;
        });

        ShowFolderAndDeckLV.setOnItemClickListener((parent, view1, position, id) -> {
            Object selectedItem = adapter.getItem(position);
            if (selectedItem instanceof Desk) {
                Desk selectedDesk = (Desk) selectedItem;
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

    private void showPopupMenu(View view, int position) {
        Object selectedItem = adapter.getItem(position);
        List<OptionsDialog.Option> options = new ArrayList<>();

        if (selectedItem instanceof Folder) {
            Folder folder = (Folder) selectedItem;
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
        } else if (selectedItem instanceof Desk) {
            Desk desk = (Desk) selectedItem;
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

    private void handlePublicDesk(Desk desk) {
        desk.setPublic(!desk.isPublic());
        deskRepository.updateDesk(desk, false);
        nestedFoldersDesks = getFoldersFromLocalDb();  // Cập nhật danh sách
        adapter.updateFolderList(nestedFoldersDesks);  // Cập nhật giao diện
        Toast.makeText(getContext(), "Desk visibility updated", Toast.LENGTH_SHORT).show();
    }


    private void editFolder(Folder folder) {
        showEditFolderDialog(folder);  // Mở dialog để sửa thông tin Folder
    }

    private void showEditFolderDialog(Folder folder) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_create_folder, null);
        BottomSheetDialog editFolderDialog = new BottomSheetDialog(requireContext());
        editFolderDialog.setContentView(view);

        EditText folderNameInput = view.findViewById(R.id.folderNameInput);
        folderNameInput.setText(folder.getName());

        Spinner parentFolderSpinner = view.findViewById(R.id.parentFolderSpinner);
        List<String> folderNames = getFolderNamesWithIndent(folder.getId());
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, folderNames
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        parentFolderSpinner.setAdapter(spinnerAdapter);

        List<Folder> allFolders = getAllFoldersList(folder.getId());
        int parentPosition = 0;
        if (folder.getParentFolderId() != null) {
            for (int i = 0; i < allFolders.size(); i++) {
                if (allFolders.get(i).getId() == folder.getParentFolderId()) {
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

            int selectedPosition = parentFolderSpinner.getSelectedItemPosition();
            if (selectedPosition == 0) {
                folder.setParentFolderId(null);  // Không có thư mục cha
            } else {
                Folder parentFolder = allFolders.get(selectedPosition - 1);
                folder.setParentFolderId(parentFolder.getId());
            }

            folder.setName(newName);  // Cập nhật tên mới
            folderRepository.updateFolder(folder, false);  // Lưu vào cơ sở dữ liệu

            // **Sửa đổi ở đây**: Tải lại dữ liệu và cập nhật adapter
            nestedFoldersDesks = getFoldersFromLocalDb();  // Lấy cấu trúc folder mới từ cơ sở dữ liệu
            adapter.updateFolderList(nestedFoldersDesks);  // Cập nhật dữ liệu trong adapter
            adapter.notifyDataSetChanged();  // Thông báo cập nhật giao diện

            editFolderDialog.dismiss();
            Toast.makeText(getContext(), "Folder updated", Toast.LENGTH_SHORT).show();
        });

        editFolderDialog.show();
    }
    private void deleteFolder(Folder folder) {
        folderRepository.deleteFolder(folder);
        nestedFoldersDesks = getFoldersFromLocalDb();
        adapter.updateFolderList(nestedFoldersDesks);
        allDesks = deskRepository.getAllDesks();
        Toast.makeText(getContext(), "Folder deleted", Toast.LENGTH_SHORT).show();
    }

    private void editDesk(Desk desk) {
        showEditDeskDialog(desk);
    }

    public void showEditDeskDialog(Desk desk) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_create_deck, null);
        BottomSheetDialog editDeskDialog = new BottomSheetDialog(requireContext());
        editDeskDialog.setContentView(view);

        EditText deckNameInput = view.findViewById(R.id.deckNameInput);
        deckNameInput.setText(desk.getName());

        Spinner folderSpinner = view.findViewById(R.id.folderSpinner);
        List<String> folderNames = getFolderNamesWithIndent();
        folderNames.set(0, getString(R.string.select_folder_prompt)); // "Please select a folder"
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, folderNames
        );

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        folderSpinner.setAdapter(spinnerAdapter);

        List<Folder> allFolders = getAllFoldersList();


        int folderPosition = 0;

        for (int i = 0; i < allFolders.size(); i++) {
            if (allFolders.get(i).getId() == desk.getFolderId()) {
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

            Folder parentFolder = allFolders.get(selectedPosition - 1);
            desk.setFolderId(parentFolder.getId());
            desk.setName(newName);  // Cập nhật tên mới
            deskRepository.updateDesk(desk, false);  // Lưu vào cơ sở dữ liệu

            nestedFoldersDesks = getFoldersFromLocalDb();  // Lấy cấu trúc folder mới từ cơ sở dữ liệu
            adapter.updateFolderList(nestedFoldersDesks);  // Cập nhật dữ liệu trong adapter
            adapter.notifyDataSetChanged();  // Thông báo cập nhật giao diện

            editDeskDialog.dismiss();
            Toast.makeText(getContext(), "Desk updated", Toast.LENGTH_SHORT).show();
        });

        editDeskDialog.show();
    }

    private void deleteDesk(Desk desk) {
        deskRepository.deleteDesk(desk);  // Xóa Desk từ cơ sở dữ liệu
        nestedFoldersDesks = getFoldersFromLocalDb();  // Cập nhật danh sách
        adapter.updateFolderList(nestedFoldersDesks);  // Cập nhật giao diện
        allDesks = deskRepository.getAllDesks();  // Cập nhật danh sách Desk
        Toast.makeText(getContext(), "Desk deleted", Toast.LENGTH_SHORT).show();
    }

    private void filterDesks(String query) {
        if (query.isEmpty()) {
            adapter.setSearchMode(false, null); // Quay lại chế độ bình thường
        } else {
            List<Desk> filteredDesks = new ArrayList<>();
            for (Desk desk : allDesks) {
                if (desk.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredDesks.add(desk);
                }
            }
            adapter.setSearchMode(true, filteredDesks); // Chuyển sang chế độ tìm kiếm
        }
        adapter.notifyDataSetChanged(); // Cập nhật giao diện
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
                requireContext(), android.R.layout.simple_spinner_item, folderNames
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        parentFolderSpinner.setAdapter(spinnerAdapter);

        view.findViewById(R.id.btnFolderSave).setOnClickListener(v -> {
            EditText folderNameInput = view.findViewById(R.id.folderNameInput);
            String folderName = folderNameInput.getText().toString().trim();
            if (folderName.isEmpty()) {
                folderNameInput.setError("Folder name cannot be empty");
                return;
            }
            int selectedPosition = parentFolderSpinner.getSelectedItemPosition();
            createNewFolder(folderName, selectedPosition);
            folderDialog.dismiss();
        });

        folderDialog.show();
    }

    private void createNewFolder(String folderName, int selectedPosition) {
        String createdAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
        Folder newFolder = new Folder(folderName, createdAt);
        List<Folder> allFolders = getAllFoldersList();

        if (selectedPosition == 0) {
            nestedFoldersDesks.add(newFolder);
        } else {
            Folder parentFolder = allFolders.get(selectedPosition - 1);
            newFolder.setParentFolderId(parentFolder.getId());
            parentFolder.addSubFolder(newFolder);
            //parentFolder.setExpanded(true);
        }

        long insertedId = folderRepository.insertFolder(newFolder);
        if (insertedId != -1) {
            newFolder.setId((int) insertedId);
            adapter.updateFolderList(nestedFoldersDesks);
            Toast.makeText(requireContext(), "Folder created successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Failed to create folder", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCreateDeckDialog() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_create_deck, null);
        BottomSheetDialog deckDialog = new BottomSheetDialog(requireContext());
        deckDialog.setContentView(view);

        Spinner folderSpinner = view.findViewById(R.id.folderSpinner);
        List<String> folderNames = getFolderNamesWithIndent();
        folderNames.set(0, getString(R.string.select_folder_prompt)); // "Please select a folder"
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, folderNames
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
            createNewDesk(deckName, selectedPosition);
            deckDialog.dismiss();
        });

        deckDialog.show();
    }

    private void createNewDesk(String deckName, int selectedPosition) {
        String createdAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
        Desk newDesk = new Desk(deckName, 0, createdAt);
        List<Folder> allFolders = getAllFoldersList();
        Folder parentFolder = allFolders.get(selectedPosition - 1);
        newDesk.setFolderId(parentFolder.getId());

        long insertedId = deskRepository.insertDesk(newDesk);
        if (insertedId != -1) {
            newDesk.setId((int) insertedId);
            parentFolder.addDesk(newDesk);
            //parentFolder.setExpanded(true);
            adapter.updateFolderList(nestedFoldersDesks);
            allDesks = deskRepository.getAllDesks();  // Cập nhật danh sách Desk
            Toast.makeText(requireContext(), "Desk created successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Failed to create desk", Toast.LENGTH_SHORT).show();
        }
    }

    private List<String> getFolderNamesWithIndent() {
        return getFolderNamesWithIndent(-1);
    }

    private List<String> getFolderNamesWithIndent(int excludedFolderId) {
        List<String> folderNames = new ArrayList<>();
        folderNames.add(getString(R.string.no_parent_folder)); // "No Parent Folder"
        getAllFoldersWithIndent(nestedFoldersDesks, folderNames, "", excludedFolderId);
        return folderNames;
    }

    private void getAllFoldersWithIndent(List<Folder> folders, List<String> folderNames, String indent, int excludedFolderId) {
        for (Folder folder : folders) {
            if (folder.getId() == excludedFolderId) {
                continue; // Bỏ qua thư mục đang được chỉnh sửa
            }
            folderNames.add(indent + folder.getName());
            getAllFoldersWithIndent(folder.getSubFolders(), folderNames, indent + "  ", excludedFolderId);
        }
    }

    private List<Folder> getAllFoldersList() {
        return getAllFoldersList(-1);
    }

    private List<Folder> getAllFoldersList(int excludedFolderId) {
        List<Folder> allFolders = new ArrayList<>();
        getAllFolders(nestedFoldersDesks, allFolders, excludedFolderId);
        return allFolders;
    }

    private void getAllFolders(List<Folder> folders, List<Folder> allFolders, int excludedFolderId) {
        for (Folder folder : folders) {
            if (folder.getId() == excludedFolderId) {
                continue; // Bỏ qua thư mục đang được chỉnh sửa
            }
            allFolders.add(folder);
            getAllFolders(folder.getSubFolders(), allFolders, excludedFolderId);
        }
    }

    private ArrayList<Folder> getFoldersFromLocalDb() {
        return (ArrayList<Folder>) folderRepository.getNestedFolders();
    }
}