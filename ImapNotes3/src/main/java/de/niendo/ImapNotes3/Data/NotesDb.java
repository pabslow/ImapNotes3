/*
 * Copyright (C) 2022-2023 - Peter Korf <peter@niendo.de>
 * Copyright (C) ?   -2016 - Martin Carpella
 * Copyright (C) ?   -2015 - nb
 * and Contributors.
 *
 * This file is part of ImapNotes3.
 *
 * ImapNotes3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.niendo.ImapNotes3.Data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import de.niendo.ImapNotes3.Miscs.Utilities;

public class NotesDb extends SQLiteOpenHelper {

    private static final String TAG = "IN_NotesDb";


    private static final String COL_TITLE = "title";
    private static final String COL_DATE = "date";
    private static final String COL_NUMBER = "number";
    private static final String COL_ACCOUNT_NAME = "accountname";
    private static final String COL_BGCOLOR = "bgcolor";
    private static final String TABLE_NAME = "notesTable";

    public static final String CREATE_NOTES_DB = "CREATE TABLE IF NOT EXISTS "
            + TABLE_NAME
            + " (pk integer primary key autoincrement, "
            + COL_TITLE + " text not null, "
            + COL_DATE + " text not null, "
            + COL_NUMBER + " text not null, "
            + COL_BGCOLOR + " text not null, "
            + COL_ACCOUNT_NAME + " text not null);";


    private static final int NOTES_VERSION = 3;
    private static final String DATABASE_NAME = "NotesDb";

    private static NotesDb instance = null;

    private NotesDb(@NonNull Context context) {
        super(context, DATABASE_NAME, null, NOTES_VERSION);
    }

    public static NotesDb getInstance(Context context) {
        if (instance == null && context != null) {
            instance = new NotesDb(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_NOTES_DB);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            //SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL("Drop table notesTable;");
            db.execSQL(CREATE_NOTES_DB);
        }
    }

    public synchronized void InsertANoteInDb(@NonNull OneNote noteElement) {
        SQLiteDatabase db = this.getWritableDatabase();

        // delete DS with TempNumber
        db.execSQL("delete from notesTable where number = '" + noteElement.GetUid() +
                "' and accountname = '" + noteElement.GetAccount() + "' and title = 'tmp'");

        ContentValues tableRow = new ContentValues();
        tableRow.put(COL_TITLE, noteElement.GetTitle());
        tableRow.put(COL_DATE, noteElement.GetDate());
        tableRow.put(COL_NUMBER, noteElement.GetUid());
        tableRow.put(COL_BGCOLOR, noteElement.GetBgColor());
        tableRow.put(COL_ACCOUNT_NAME, noteElement.GetAccount());
        db.insert(TABLE_NAME, null, tableRow);


        //Log.d(TAG, "note inserted");
        db.close();
    }

    public synchronized void DeleteANote(@NonNull String number,
                                         @NonNull String accountname) {

        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from notesTable where number = '" + number +
                "' and accountname = '" + accountname + "'");
        db.close();
    }

    public synchronized void UpdateANote(@NonNull String tmpuid,
                                         @NonNull String newuid,
                                         @NonNull String accountname) {
        /* TODO: use sql template and placeholders instead of string concatenation.
         */

        SQLiteDatabase db = this.getWritableDatabase();

        String req = "update notesTable set number='" + newuid + "' where number='-" + tmpuid + "' and accountname='" + accountname + "'";
        db.execSQL(req);
        db.close();
    }

    public synchronized String GetDate(@NonNull String uid,
                                       @NonNull String accountname) {
       /* Returns a string representing the modification time of the note.
          TODO: use date class.
        */
        SQLiteDatabase db = this.getWritableDatabase();
        String RetValue = "";
        String selectQuery = "select date from notesTable where number = '" + uid + "' and accountname='" + accountname + "'";
        try (Cursor c = db.rawQuery(selectQuery, null)) {
            if (c.moveToFirst()) {
                RetValue = c.getString(0);
            }
        }

        db.close();
        return RetValue;
    }

    public synchronized String GetTempNumber(@NonNull String accountname) {
        String RetValue = "-1";
        String selectQuery = "select case when cast(max(abs(number)+2) as int) > 0 then cast(max(abs(number)+1) as int)*-1 else '-1' end from notesTable where number < '0' and accountname='" + accountname + "'";

        SQLiteDatabase db = this.getWritableDatabase();
        try (Cursor c = db.rawQuery(selectQuery, null)) {
            if (c.moveToFirst()) {
                RetValue = c.getString(0);
            }
        }
        // Create DS with TempNumber, so it can not be given two times
        ContentValues tableRow = new ContentValues();
        tableRow.put(COL_TITLE, "tmp");
        tableRow.put(COL_DATE, "");
        tableRow.put(COL_NUMBER, RetValue);
        tableRow.put(COL_ACCOUNT_NAME, accountname);
        tableRow.put(COL_BGCOLOR, "");
        db.insert(TABLE_NAME, null, tableRow);
        db.close();
        return (RetValue);
    }

    public synchronized void GetStoredNotes(@NonNull ArrayList<OneNote> noteList,
                                            @NonNull String accountName,
                                            @NonNull String sortOrder) {
        noteList.clear();
        Date date = null;
        SQLiteDatabase db = this.getWritableDatabase();
        String selection = "";
        String[] selectionArgs = new String[]{};
        if (!(accountName.isEmpty())) {
            selection = COL_ACCOUNT_NAME + " = ?";
            selectionArgs = new String[]{accountName};
        }

        try (Cursor resultPointer = db.query(TABLE_NAME, null, selection,
                selectionArgs, null, null, sortOrder)) {

            if (resultPointer.moveToFirst()) {
                int titleIndex = resultPointer.getColumnIndex(COL_TITLE);
                //int bodyIndex = resultPointer.getColumnIndex("body");
                int AccountNameIndex = resultPointer.getColumnIndex(COL_ACCOUNT_NAME);
                int dateIndex = resultPointer.getColumnIndex(COL_DATE);
                int numberIndex = resultPointer.getColumnIndex(COL_NUMBER);
                int bgColorIndex = resultPointer.getColumnIndex(COL_BGCOLOR);
                //int positionIndex = resultPointer.getColumnIndex("position");
                //int colorIndex = resultPointer.getColumnIndex("color");
                do {
                    //String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
                    //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.ROOT);
                    try {
                        date = Utilities.internalDateFormat.parse(resultPointer.getString(dateIndex));
                    } catch (ParseException e) {
                        Log.d(TAG, "Parsing data from database failed: " + e.getMessage());
                    }
                    //DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(this.ctx);
                    //String sdate = dateFormat.format(date);
                    String sdate = DateFormat.getDateTimeInstance().format(date);
                    if (accountName.isEmpty()) {
                        sdate = sdate + "  (" + resultPointer.getString(AccountNameIndex) + ")";
                    }
                    noteList.add(new OneNote(resultPointer.getString(titleIndex),
                            sdate,
                            resultPointer.getString(numberIndex),
                            resultPointer.getString(AccountNameIndex),
                            resultPointer.getString(bgColorIndex)));

                } while (resultPointer.moveToNext());
                resultPointer.close();
            }
        }
        db.close();
    }

    public synchronized void ClearDb(@NonNull String accountname) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from notesTable where accountname = '" + accountname + "'");
        db.close();
    }
}
