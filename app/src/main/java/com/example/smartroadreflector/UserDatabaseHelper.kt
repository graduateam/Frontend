package com.example.smartroadreflector

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UserDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_USERS (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_USERNAME TEXT UNIQUE," +
                "$COLUMN_NICKNAME TEXT," +
                "$COLUMN_PASSWORD TEXT," +
                "$COLUMN_EMAIL TEXT UNIQUE)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun registerUser(username: String, nickname: String, password: String, email: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_NICKNAME, nickname)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_EMAIL, email)
        }
        return db.insert(TABLE_USERS, null, values) != -1L
    }

    fun loginUser(username: String, password: String): Boolean {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?"
        val cursor = db.rawQuery(query, arrayOf(username, password))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    companion object {
        private const val DATABASE_NAME = "UserDatabase.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_USERS = "users"
        private const val COLUMN_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_NICKNAME = "nickname"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_EMAIL = "email"
    }

    fun getNickname(username: String): String? {
        val db = readableDatabase
        val query = "SELECT $COLUMN_NICKNAME FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ?"
        val cursor = db.rawQuery(query, arrayOf(username))
        var nickname: String? = null

        if (cursor.moveToFirst()) {
            nickname = cursor.getString(0)
        }
        cursor.close()
        return nickname
    }

    fun updatePassword(username: String, newPassword: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("password", newPassword) // 새 비밀번호 저장
        }
        val result = db.update("users", values, "username = ?", arrayOf(username))
        return result > 0 // 업데이트 성공 여부 반환
    }
}
