package com.cntt2.flashcard.ui.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cntt2.flashcard.R;
import com.cntt2.flashcard.model.Desk;
import com.cntt2.flashcard.model.Folder;

import java.util.ArrayList;
import java.util.List;

public class ShowFoldersAndDecksAdapter extends BaseAdapter {
    private Activity context;
    private List<Folder> folderList;
    private List<Desk> allDesks;
    private LayoutInflater inflater;
    private int indentWidth = 60;
    private boolean isSearchMode = false;
    private List<Desk> filteredDesks;

    private static final int TYPE_FOLDER = 0;
    private static final int TYPE_DESK = 1;

    private class ListItem {
        Object item; // folder hay deck
        int level; // cấp 0 là cha
        int type; // folder hay deck

        ListItem(Object item, int level, int type) {
            this.item = item;
            this.level = level;
            this.type = type;
        }
    }

    private List<ListItem> flattenedList = new ArrayList<>();

    public ShowFoldersAndDecksAdapter(Activity context, List<Folder> folderList, List<Desk> allDesks) {
        this.context = context;
        this.folderList = folderList;
        this.allDesks = allDesks;
        this.inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        updateFlattenedList();
    }

    private void updateFlattenedList() {
        flattenedList.clear();
        if (!isSearchMode) {
            for (Folder folder : folderList) {
                addFolderToFlattenedList(folder, 0);
            }
        } else {
            for (Desk desk : filteredDesks) {
                flattenedList.add(new ListItem(desk, 0, TYPE_DESK)); // Level = 0 để không thụt lề
            }
        }
    }

    private void addFolderToFlattenedList(Folder folder, int level) {
        flattenedList.add(new ListItem(folder, level, TYPE_FOLDER));
        if (folder.isExpanded()) {
            for (Folder subFolder : folder.getSubFolders()) {
                addFolderToFlattenedList(subFolder, level + 1);
            }
            for (Desk desk : folder.getDesks()) {
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
        if (item instanceof Folder) {
            return ((Folder) item).getId();
        } else if (item instanceof Desk) {
            return ((Desk) item).getId();
        }
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ListItem listItem = flattenedList.get(position);

        if (listItem.type == TYPE_FOLDER) {
            return getFolderView(position, convertView, parent, (Folder) listItem.item, listItem.level);
        } else {
            return getDeskView(position, convertView, parent, (Desk) listItem.item, listItem.level);
        }
    }

    private View getFolderView(int position, View convertView, ViewGroup parent, Folder folder, int level) {
        View view = convertView;
        if (view == null || !(TYPE_FOLDER == (Integer) view.getTag())) {
            view = inflater.inflate(R.layout.folder_item, null);
            view.setTag(TYPE_FOLDER);
        }

        TextView folderName = view.findViewById(R.id.folderName);
        ImageView dropdownIcon = view.findViewById(R.id.imageView);
        ImageView folderIcon = view.findViewById(R.id.imageView2);
        folderName.setText(folder.getName());

        // Đặt margin thụt lề
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) folderIcon.getLayoutParams();
        params.leftMargin = level * indentWidth;
        folderIcon.setLayoutParams(params);

        // Nếu có thư mục con hoặc desk thì hiện dropdownIcon
        boolean hasSubFoldersOrDesks = !folder.getSubFolders().isEmpty() || !folder.getDesks().isEmpty();
        if (!hasSubFoldersOrDesks) {
            dropdownIcon.setVisibility(View.INVISIBLE);
        } else {
            dropdownIcon.setVisibility(View.VISIBLE);
            // Thay đổi icon dropdown dựa trên trạng thái mở rộng
            dropdownIcon.setImageResource(folder.isExpanded() ?
                    R.drawable.dropdownup : R.drawable.dropdown);
        }

        // Gắn sự kiện click cho toàn bộ view của thư mục
        view.setOnClickListener(v -> {
            // Chỉ mở rộng/thu gọn nếu thư mục có subfolder hoặc desk
            if (hasSubFoldersOrDesks) {
                folder.setExpanded(!folder.isExpanded());
                updateFlattenedList();
                notifyDataSetChanged();
            }
        });

        view.setOnLongClickListener(v ->{
            return false;
        });

        return view;
    }

    private View getDeskView(int position, View convertView, ViewGroup parent, Desk desk, int level) {
        View view = convertView;
        if (view == null || !(TYPE_DESK == (Integer) view.getTag())) {
            view = inflater.inflate(R.layout.deck_item, null);
            view.setTag(TYPE_DESK);
        }
        TextView deskName = view.findViewById(R.id.deckName);
        ImageView deskIcon = view.findViewById(R.id.deckIcon);
        deskName.setText(desk.getName());

        // Đặt margin thụt lề
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) deskIcon.getLayoutParams();
        params.leftMargin = level * indentWidth;
        deskIcon.setLayoutParams(params);
        return view;
    }

    public void setSearchMode(boolean isSearchMode, List<Desk> filteredDesks) {
        this.isSearchMode = isSearchMode;
        this.filteredDesks = filteredDesks;
        updateFlattenedList();
    }

    public void updateFolderList(List<Folder> newFolderList) {
        this.folderList = newFolderList;
        this.allDesks = getAllDesksFromFolders(newFolderList); // Cập nhật allDesks
        updateFlattenedList();
        notifyDataSetChanged();
    }

    private List<Desk> getAllDesksFromFolders(List<Folder> folders) {
        List<Desk> desks = new ArrayList<>();
        for (Folder folder : folders) {
            desks.addAll(folder.getDesks());
            desks.addAll(getAllDesksFromFolders(folder.getSubFolders()));
        }
        return desks;
    }
}