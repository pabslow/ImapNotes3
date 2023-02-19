/*
 * Copyright (C) 2022-2023 - Peter Korf <peter@niendo.de>
 * Copyright (C)      2016 - Martin Carpella
 * Copyright (C)      2015 - nb
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
package de.niendo.ImapNotes3.Miscs;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import de.niendo.ImapNotes3.Data.NotesDb;
import de.niendo.ImapNotes3.Data.OneNote;
import de.niendo.ImapNotes3.NotesListAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class SyncThread extends AsyncTask<Object, Void, Boolean> {
    // --Commented out by Inspection (11/26/16 11:48 PM):boolean bool_to_return;
// --Commented out by Inspection START (11/26/16 11:48 PM):
//    @NonNull
//    ImapNotesResult res = new ImapNotesResult();
// --Commented out by Inspection STOP (11/26/16 11:48 PM)
    private static final String TAG = "SyncThread";
    private final @StringRes
    int resId;
    private final NotesListAdapter adapter;
    private final ArrayList<OneNote> notesList;
    private final String sortOrder;
    private final String imapNotesAccountName;
    //private final WeakReference<Context> applicationContextRef;

    /**
     * SQLite database that holds status information about the notes.
     */
    // TODO: NoteDb should probably never be null.
    @NonNull
    private final NotesDb storedNotes;

    // TODO: remove unused arguments.
    public SyncThread(String imapNotesAccountName,
                      ArrayList<OneNote> noteList,
                      NotesListAdapter listToView,
                      @StringRes int resId,
                      String sortOrder,
                      Context context) {
        //this.imapFolder = imapFolder;
        this.imapNotesAccountName = imapNotesAccountName;
        this.notesList = noteList;
        this.adapter = listToView;
        this.resId = resId;
        this.sortOrder = sortOrder;
        //Notifier.Show(resId, applicationContext, 1);
        this.storedNotes = NotesDb.getInstance(context);
        //this applicationContextRef= new WeakReference<>(context);;
    }

    // Do not pass arguments via execute; the object is never reused so it is quite safe to pass
    // the arguments in the constructor.
    @NonNull
    @Override
    protected Boolean doInBackground(Object... stuffs) {
        /*String username = null;
        String password = null;
        String server = null;
        String portnum = null;
        String security = null;
        String usesticky = null;
*/
         /*       this.adapter = ((NotesListAdapter) stuffs[3]);
        this.notesList = ((ArrayList<OneNote>) stuffs[2]);
        this.storedNotes = ((NotesDb) stuffs[5]);
        this.ctx = (Context) stuffs[6];
 */
        //username = ((ImapNotesAccount) stuffs[1]).GetUsername();
        //password = ((ImapNotesAccount) stuffs[1]).GetPassword();
        //server = ((ImapNotesAccount) stuffs[1]).GetServer();
        //portnum = ((ImapNotesAccount) stuffs[1]).GetPortnum();
        //security = ((ImapNotesAccount) stuffs[1]).GetSecurity();
        //usesticky = ((ImapNotesAccount) stuffs[1]).GetUsesticky();

        storedNotes.GetStoredNotes(this.notesList, imapNotesAccountName, sortOrder);
        return true;
    }

    protected void onPostExecute(Boolean result) {
        if (result) {
            this.adapter.notifyDataSetChanged();
        }
    }
}
