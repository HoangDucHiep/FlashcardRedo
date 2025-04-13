package com.cntt2.flashcard.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "flashcard.db";
    private static final int DATABASE_VERSION = 4; // Tăng version để hỗ trợ đồng bộ

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Bảng folders
        db.execSQL("CREATE TABLE folders (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "parent_folder_id INTEGER, " +
                "name TEXT, " +
                "created_at TEXT, " +
                "last_modified TEXT, " + // Thêm để đồng bộ
                "sync_status TEXT DEFAULT 'synced', " + // Thêm để đồng bộ
                "FOREIGN KEY (parent_folder_id) REFERENCES folders(id))");

        // Bảng desks
        db.execSQL("CREATE TABLE desks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " + // Đổi thành AUTOINCREMENT để đồng bộ local
                "folder_id INTEGER, " +
                "name TEXT, " +
                "created_at TEXT, " +
                "is_public INTEGER DEFAULT 0, " +
                "last_modified TEXT, " +
                "sync_status TEXT DEFAULT 'synced', " +
                "FOREIGN KEY(folder_id) REFERENCES folders(id))");

        // Bảng cards (loại bỏ front_image và back_image)
        db.execSQL("CREATE TABLE cards (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "desk_id INTEGER, " +
                "front TEXT, " + // HTML chứa ảnh nếu có
                "back TEXT, " +  // HTML chứa ảnh nếu có
                "created_at TEXT, " +
                "last_modified TEXT, " +
                "sync_status TEXT DEFAULT 'synced', " +
                "FOREIGN KEY (desk_id) REFERENCES desks(id))");

        // Bảng reviews
        db.execSQL("CREATE TABLE reviews (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "card_id INTEGER, " +
                "ease REAL, " +
                "interval INTEGER, " +
                "repetition INTEGER, " +
                "next_review_date TEXT, " +
                "last_reviewed TEXT, " +
                "last_modified TEXT, " +
                "sync_status TEXT DEFAULT 'synced', " +
                "FOREIGN KEY (card_id) REFERENCES cards(id))");

        // Bảng learning_sessions
        db.execSQL("CREATE TABLE learning_sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "desk_id INTEGER, " +
                "start_time TEXT, " +
                "end_time TEXT, " +
                "cards_studied INTEGER, " +
                "performance REAL, " +
                "last_modified TEXT, " +
                "sync_status TEXT DEFAULT 'synced', " +
                "FOREIGN KEY (desk_id) REFERENCES desks(id))");

        // Bảng id_mapping để ánh xạ local_id và server_id
        db.execSQL("CREATE TABLE id_mapping (" +
                "local_id INTEGER, " +
                "server_id TEXT, " +
                "entity_type TEXT, " + // 'folder', 'desk', 'card', 'review', 'session'
                "PRIMARY KEY (local_id, entity_type))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            // Thêm cột last_modified và sync_status cho các bảng cũ
            db.execSQL("ALTER TABLE folders ADD COLUMN last_modified TEXT");
            db.execSQL("ALTER TABLE folders ADD COLUMN sync_status TEXT DEFAULT 'synced'");
            db.execSQL("ALTER TABLE desks ADD COLUMN last_modified TEXT");
            db.execSQL("ALTER TABLE desks ADD COLUMN sync_status TEXT DEFAULT 'synced'");
            db.execSQL("ALTER TABLE cards ADD COLUMN last_modified TEXT");
            db.execSQL("ALTER TABLE cards ADD COLUMN sync_status TEXT DEFAULT 'synced'");
            db.execSQL("ALTER TABLE reviews ADD COLUMN last_modified TEXT");
            db.execSQL("ALTER TABLE reviews ADD COLUMN sync_status TEXT DEFAULT 'synced'");
            db.execSQL("ALTER TABLE learning_sessions ADD COLUMN last_modified TEXT");
            db.execSQL("ALTER TABLE learning_sessions ADD COLUMN sync_status TEXT DEFAULT 'synced'");

            // Xóa cột front_image và back_image từ bảng cards
            db.execSQL("CREATE TABLE cards_temp (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "desk_id INTEGER, " +
                    "front TEXT, " +
                    "back TEXT, " +
                    "created_at TEXT, " +
                    "last_modified TEXT, " +
                    "sync_status TEXT DEFAULT 'synced', " +
                    "FOREIGN KEY (desk_id) REFERENCES desks(id))");
            db.execSQL("INSERT INTO cards_temp (id, desk_id, front, back, created_at, last_modified, sync_status) " +
                    "SELECT id, desk_id, front, back, created_at, last_modified, sync_status FROM cards");
            db.execSQL("DROP TABLE cards");
            db.execSQL("ALTER TABLE cards_temp RENAME TO cards");

            // Thêm bảng id_mapping
            db.execSQL("CREATE TABLE id_mapping (" +
                    "local_id INTEGER, " +
                    "server_id TEXT, " +
                    "entity_type TEXT, " +
                    "PRIMARY KEY (local_id, entity_type))");

            // Đổi desks.id thành AUTOINCREMENT (tạo bảng tạm)
            db.execSQL("CREATE TABLE desks_temp (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "folder_id INTEGER, " +
                    "name TEXT, " +
                    "created_at TEXT, " +
                    "is_public INTEGER DEFAULT 0, " +
                    "last_modified TEXT, " +
                    "sync_status TEXT DEFAULT 'synced', " +
                    "FOREIGN KEY(folder_id) REFERENCES folders(id))");
            db.execSQL("INSERT INTO desks_temp (id, folder_id, name, created_at, is_public, last_modified, sync_status) " +
                    "SELECT id, folder_id, name, created_at, is_public, last_modified, sync_status FROM desks");
            db.execSQL("DROP TABLE desks");
            db.execSQL("ALTER TABLE desks_temp RENAME TO desks");
        }
    }

    public void clearAllData() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // Delete all records from all tables
            db.execSQL("DELETE FROM folders");
            db.execSQL("DELETE FROM desks");
            db.execSQL("DELETE FROM cards");
            db.execSQL("DELETE FROM reviews");
            db.execSQL("DELETE FROM learning_sessions");
            db.execSQL("DELETE FROM id_mapping");

            // Reset the AUTOINCREMENT counters for all tables
            db.execSQL("DELETE FROM sqlite_sequence WHERE name='folders'");
            db.execSQL("DELETE FROM sqlite_sequence WHERE name='desks'");
            db.execSQL("DELETE FROM sqlite_sequence WHERE name='cards'");
            db.execSQL("DELETE FROM sqlite_sequence WHERE name='reviews'");
            db.execSQL("DELETE FROM sqlite_sequence WHERE name='learning_sessions'");
            db.execSQL("DELETE FROM sqlite_sequence WHERE name='id_mapping'");

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }
}