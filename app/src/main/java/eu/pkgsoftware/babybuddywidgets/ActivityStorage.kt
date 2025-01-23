package eu.pkgsoftware.babybuddywidgets

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.pkgsoftware.babybuddywidgets.debugging.GlobalDebugObject
import java.io.IOException

fun addJsonFieldValue(value: String, key: String, newValue: String): String {
    val map = jacksonObjectMapper().readValue(value, Map::class.java).toMutableMap()
    map[key] = newValue
    return jacksonObjectMapper().writeValueAsString(map)
}

class ActivityDatabaseV1(context: Context) : SQLiteOpenHelper(
    context, "store", null, 1
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("create table global_kv (key_name text primary key, value text)")
        db.execSQL("create table login_kv (key_name text primary key, value text)")
        db.execSQL("create table child_kv (child INTEGER, key_name text, value text, primary key (child, key_name))")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        throw IllegalStateException("Initial version of database cannot be upgraded")
    }
}

class ActivityDatabaseV2(context: Context) : SQLiteOpenHelper(
    context, "store", null, 2
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("create table global_kv (key_name text primary key, value text)")
        db.execSQL("create table login_kv (key_name text primary key, value text)")
        db.execSQL("create table child_kv (child INTEGER, key_name text, value text, primary key (child, key_name))")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (db == null) {
            return
        }

        if (oldVersion == 1) {
            val valuesToUpdate = mutableMapOf<Int, String>()
            db.rawQuery("select child, value from child_kv where key_name = 'diaper'", arrayOf()).use { cursor ->
                while (cursor.moveToNext()) {
                    try {
                        val child = cursor.getInt(0)
                        var value = cursor.getString(1)
                        value = addJsonFieldValue(value, "extra_options_open", "false")
                        value = addJsonFieldValue(value, "color", "null")
                        value = addJsonFieldValue(value, "amount", "null")
                        valuesToUpdate[child] = value
                    } catch (e: Exception) {
                        GlobalDebugObject.log("Failed to update diaper value for child ${cursor.getInt(0)}")
                        // Ignore, better ignore than crash
                    }
                }
                cursor.close()
            }
            for ((child, value) in valuesToUpdate) {
                db.execSQL(
                    "update child_kv set value = ? where child = ? and key_name = 'diaper'",
                    arrayOf(value, child.toString())
                )
            }
        }
    }
}

class ActivityStore(context: Context) {
    val openHelper = ActivityDatabaseV2(context)
    val database = openHelper.writableDatabase
    val jackOM = jacksonObjectMapper()

    inline fun <reified K> genSet(table: String, value: K?, selectors: Map<String, String>) {
        if (value == null) {
            val query = "delete from $table where " + selectors.keys.joinToString(" and ") { "$it = ?" }
            val args = selectors.values.toTypedArray()
            database.execSQL(query, args)
        } else {
            val query = "insert or replace into $table (" +
                    selectors.keys.joinToString(", ") + ", value) values (" +
                    selectors.keys.joinToString(", ") { "?" } + ", ?)"
            val valueString = jackOM.writeValueAsString(value)
            database.execSQL(query, selectors.values.toTypedArray() + valueString)
        }
    }

    inline fun <reified K> genGet(table: String, selectors: Map<String, String>): K? {
        val query = "select value from $table where " + selectors.keys.joinToString(" and ") { "$it = ?" }
        val cursor = database.rawQuery(query, selectors.values.toTypedArray())
        var value = ""
        try {
            if (cursor.moveToNext()) {
                value = cursor.getString(0)
                cursor.close()
                return jackOM.readValue(value, K::class.java)
            }
        }
        catch (e: IOException) {
            GlobalDebugObject.log("Failed to deserialize value from table $table: '$value'")
        }
        finally {
            cursor.close()
        }
        return null
    }

    inline fun <reified K> globals(key: String): K? {
        return genGet("global_kv", mapOf("key_name" to key))
    }

    inline fun <reified K> globals(key: String, value: K) {
        genSet("global_kv", value, mapOf("key_name" to key))
    }

    inline fun <reified K> login(key: String): K? {
        return genGet("login_kv", mapOf("key_name" to key))
    }

    inline fun <reified K> login(key: String, value: K) {
        genSet("login_kv", value, mapOf("key_name" to key))
    }

    inline fun <reified K> child(child: Int, key: String): K? {
        return genGet("child_kv", mapOf("child" to child.toString(), "key_name" to key))
    }

    inline fun <reified K> child(child: Int, key: String, value: K) {
        genSet("child_kv", value, mapOf("child" to child.toString(), "key_name" to key))
    }

    fun close() {
        database.close()
        openHelper.close()
    }

    fun deleteAllData() {
        database.delete("global_kv", "1", arrayOf())
        database.delete("login_kv", "1", arrayOf())
        database.delete("child_kv", "1", arrayOf())
    }
}