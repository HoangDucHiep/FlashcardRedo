package com.cntt2.flashcard.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "flashcard.db";
    private static final int DATABASE_VERSION = 3;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create tables here
        db.execSQL("CREATE TABLE folders (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "parent_folder_id INTEGER, " +
                "name TEXT, " +
                "created_at TEXT, " +
                "FOREIGN KEY (parent_folder_id) REFERENCES folders(id))");

        db.execSQL("CREATE TABLE desks (" +
                "id INTEGER PRIMARY KEY," +
                "folder_id INTEGER," +
                "name TEXT," +
                "created_at TEXT," +
                "is_public INTEGER DEFAULT 0," +
                "FOREIGN KEY(folder_id) REFERENCES folders(id))");

        db.execSQL("CREATE TABLE cards (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "desk_id INTEGER, " +
                "front TEXT, " +
                "back TEXT, " +
                "front_image TEXT, " +
                "back_image TEXT, " +
                "created_at TEXT, " +
                "FOREIGN KEY (desk_id) REFERENCES desks(id))");

        db.execSQL("CREATE TABLE reviews (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "card_id INTEGER, " +
                "ease REAL, " +
                "interval INTEGER, " +
                "repetition INTEGER, " +
                "next_review_date TEXT, " +
                "last_reviewed TEXT, " +
                "FOREIGN KEY (card_id) REFERENCES cards(id))");

        db.execSQL("CREATE TABLE learning_sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "desk_id INTEGER, " +
                "start_time TEXT, " +
                "end_time TEXT, " +
                "cards_studied INTEGER, " +
                "performance REAL, " +
                "FOREIGN KEY (desk_id) REFERENCES desks(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS learning_sessions");
        db.execSQL("DROP TABLE IF EXISTS reviews");
        db.execSQL("DROP TABLE IF EXISTS cards");
        db.execSQL("DROP TABLE IF EXISTS desks");
        db.execSQL("DROP TABLE IF EXISTS folders");
        onCreate(db);
    }
}
