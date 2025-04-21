package com.cntt2.flashcard.ui.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.remote.dto.DeskDto;
import com.cntt2.flashcard.data.remote.dto.NestedFolderDto;

import java.util.ArrayList;
import java.util.List;

public class ShowFoldersAndDecksAdapter extends BaseAdapter {
    private Activity context;
    private List<NestedFolderDto> folderList;
    private List<DeskDto> allDesks;
    private LayoutInflater inflater;
    private int indentWidth = 60;
    private boolean isSearchMode = false;
    private List<DeskDto> filteredDesks;

    private static final int TYPE_FOLDER = 0;
    private static final int TYPE_DESK = 1;

    private class ListItem {
        Object item; // folder or desk
        int level; // level 0 is root
        int type; // folder or desk

        ListItem(Object item, int level, int type) {
            this.item = item;
            this.level = level;
            this.type = type;
        }
    }

    private List<ListItem> flattenedList = new ArrayList<>();
    private List<NestedFolderDto> expandedFolders = new ArrayList<>();

    public ShowFoldersAndDecksAdapter(Activity context, List<NestedFolderDto> folderList, List<DeskDto> allDesks) {
        this.context = context;
        this.folderList = folderList;
        this.allDesks = allDesks;
        this.inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        updateFlattenedList();
    }

    private void updateFlattenedList() {
        flattenedList.clear();
        if (!isSearchMode) {
            for (NestedFolderDto folder : folderList) {
                addFolderToFlattenedList(folder, 0);
            }
        } else {
            for (DeskDto desk : filteredDesks) {
                flattenedList.add(new ListItem(desk, 0, TYPE_DESK));
            }
        }
    }

    private void addFolderToFlattenedList(NestedFolderDto folder, int level) {
        flattenedList.add(new ListItem(folder, level, TYPE_FOLDER));
        if (expandedFolders.contains(folder)) {
            for (NestedFolderDto subFolder : folder.getSubFolders()) {
                addFolderToFlattenedList(subFolder, level + 1);
            }
            for (DeskDto desk : folder.getDesks()) {
                flattenedList.add(new ListItem(desk, level + 1, TYPE_DESK));
            }
        }
    }

    @Override
    public int getCount() {
        return flattenedList.size();
    }

    @Override
    public Object getItem(int position) {
        return flattenedList.get(position).item;
    }

    @Override
    public int getItemViewType(int position) {
        return flattenedList.get(position).type;
    }

    @Override
    public long getItemId(int position) {
        Object item = getItem(position);
        if (item instanceof NestedFolderDto) {
            return position; // Use position as ID since string IDs can't be long
        } else if (item instanceof DeskDto) {
            return position;
        }
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ListItem listItem = flattenedList.get(position);

        if (listItem.type == TYPE_FOLDER) {
            return getFolderView(position, convertView, parent, (NestedFolderDto) listItem.item, listItem.level);
        } else {
            return getDeskView(position, convertView, parent, (DeskDto) listItem.item, listItem.level);
        }
    }

    private View getFolderView(int position, View convertView, ViewGroup parent, NestedFolderDto folder, int level) {
        View view = convertView;
        if (view == null || !(TYPE_FOLDER == (Integer) view.getTag())) {
            view = inflater.inflate(R.layout.folder_item, null);
            view.setTag(TYPE_FOLDER);
        }

        TextView folderName = view.findViewById(R.id.folderName);
        ImageView dropdownIcon = view.findViewById(R.id.imageView);
        ImageView folderIcon = view.findViewById(R.id.imageView2);
        folderName.setText(folder.getName());

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) folderIcon.getLayoutParams();
        params.leftMargin = level * indentWidth;
        folderIcon.setLayoutParams(params);

        boolean hasSubFoldersOrDesks = !folder.getSubFolders().isEmpty() || !folder.getDesks().isEmpty();
        if (!hasSubFoldersOrDesks) {
            dropdownIcon.setVisibility(View.INVISIBLE);
        } else {
            dropdownIcon.setVisibility(View.VISIBLE);
            dropdownIcon.setImageResource(expandedFolders.contains(folder) ?
                    R.drawable.dropdownup : R.drawable.dropdown);
        }

        view.setOnClickListener(v -> {
            if (hasSubFoldersOrDesks) {
                if (expandedFolders.contains(folder)) {
                    expandedFolders.remove(folder);
                } else {
                    expandedFolders.add(folder);
                }
                updateFlattenedList();
                notifyDataSetChanged();
            }
        });

        view.setOnLongClickListener(v -> false);

        return view;
    }

    private View getDeskView(int position, View convertView, ViewGroup parent, DeskDto desk, int level) {
        View view = convertView;
        if (view == null || !(TYPE_DESK == (Integer) view.getTag())) {
            view = inflater.inflate(R.layout.deck_item, null);
            view.setTag(TYPE_DESK);
        }
        TextView deskName = view.findViewById(R.id.deckName);
        ImageView deskIcon = view.findViewById(R.id.deckIcon);
        deskName.setText(desk.getName());

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) deskIcon.getLayoutParams();
        params.leftMargin = level * indentWidth;
        deskIcon.setLayoutParams(params);
        return view;
    }

    public void setSearchMode(boolean isSearchMode, List<DeskDto> filteredDesks) {
        this.isSearchMode = isSearchMode;
        this.filteredDesks = filteredDesks;
        updateFlattenedList();
        notifyDataSetChanged();
    }

    public void updateFolderList(List<NestedFolderDto> newFolderList) {
        this.folderList = newFolderList;
        this.allDesks = getAllDesksFromFolders(newFolderList);
        updateFlattenedList();
        notifyDataSetChanged();
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