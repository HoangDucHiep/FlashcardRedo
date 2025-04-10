package com.cntt2.flashcard.data.local.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cntt2.flashcard.data.local.DatabaseHelper;
import com.cntt2.flashcard.model.IdMapping;

import java.util.ArrayList;
import java.util.List;

public class IdMappingDao {
    private final DatabaseHelper dbHelper;

    public IdMappingDao(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    // Thêm một ánh xạ mới
    // Thêm một ánh xạ mới
    public void insertIdMapping(IdMapping mapping) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("local_id", mapping.getLocalId());
        values.put("server_id", mapping.getServerId());
        values.put("entity_type", mapping.getEntityType());
        db.insert("id_mapping", null, values);
        db.close();
    }

    // Cập nhật một ánh xạ (dựa trên local_id và entity_type)
    public void updateIdMapping(IdMapping mapping) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("server_id", mapping.getServerId());
        db.update("id_mapping", values, "local_id = ? AND entity_type = ?",
                new String[]{String.valueOf(mapping.getLocalId()), mapping.getEntityType()});
        db.close();
    }

    // Xóa một ánh xạ
    public int deleteIdMapping(int localId, String entityType) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = db.delete("id_mapping", "local_id = ? AND entity_type = ?",
                new String[]{String.valueOf(localId), entityType});
        db.close();
        return rowsAffected;
    }

    // Lấy tất cả ánh xạ
    public List<IdMapping> getAllMappings() {
        List<IdMapping> mappings = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM id_mapping", null);
        if (cursor.moveToFirst()) {
            do {
                IdMapping mapping = new IdMapping();
                mapping.setLocalId(cursor.getInt(cursor.getColumnIndexOrThrow("local_id")));
                mapping.setServerId(cursor.getString(cursor.getColumnIndexOrThrow("server_id")));
                mapping.setEntityType(cursor.getString(cursor.getColumnIndexOrThrow("entity_type")));
                mappings.add(mapping);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return mappings;
    }

    // Lấy local_id từ server_id và entity_type
    public Integer getLocalIdByServerId(String serverId, String entityType) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT local_id FROM id_mapping WHERE server_id = ? AND entity_type = ?",
                new String[]{serverId, entityType});
        Integer localId = null;
        if (cursor.moveToFirst()) {
            localId = cursor.getInt(cursor.getColumnIndexOrThrow("local_id"));
        }
        cursor.close();
        db.close();
        return localId;
    }

    // Lấy server_id từ local_id và entity_type
    public String getServerIdByLocalId(int localId, String entityType) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT server_id FROM id_mapping WHERE local_id = ? AND entity_type = ?",
                new String[]{String.valueOf(localId), entityType});
        String serverId = null;
        if (cursor.moveToFirst()) {
            serverId = cursor.getString(cursor.getColumnIndexOrThrow("server_id"));
        }
        cursor.close();
        db.close();
        return serverId;
    }

    // Lấy tất cả ánh xạ cho một entity_type cụ thể
    public List<IdMapping> getMappingsByEntityType(String entityType) {
        List<IdMapping> mappings = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM id_mapping WHERE entity_type = ?", new String[]{entityType});
        if (cursor.moveToFirst()) {
            do {
                IdMapping mapping = new IdMapping();
                mapping.setLocalId(cursor.getInt(cursor.getColumnIndexOrThrow("local_id")));
                mapping.setServerId(cursor.getString(cursor.getColumnIndexOrThrow("server_id")));
                mapping.setEntityType(cursor.getString(cursor.getColumnIndexOrThrow("entity_type")));
                mappings.add(mapping);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return mappings;
    }
}