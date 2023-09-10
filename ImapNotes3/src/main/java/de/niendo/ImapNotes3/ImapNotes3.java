/*
 * Copyright (C) 2022-2023 - Peter Korf <peter@niendo.de>
 * Copyright (C)         ? - kwhitefoot
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

package de.niendo.ImapNotes3;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.StrictMode;
import android.view.View;

import androidx.annotation.StringRes;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import de.niendo.ImapNotes3.Miscs.Imaper;

import java.io.File;

public class ImapNotes3 extends Application {
    private static Context mContext;
    private static View mContent;
    private Imaper thisSessionImapFolder;
    public static Intent intent; // For Data-Exchange SyncAdapater
    private static final String ReservedChars = "[\\\\/:*?\"<>|'!]";

    public static Context getAppContext() {
        return mContext;
    }

    public static File GetRootDir() {
        return mContext.getFilesDir();
    }

    public static File GetAccountDir(String account) {
        return new File(GetRootDir(), RemoveReservedChars(account));
    }

    public static String RemoveReservedChars(String data) {
        return data.replaceAll(ReservedChars, "_");
    }

    public static File GetSharedPrefsDir() {
        return new File(mContext.getFilesDir().getParent(), "shared_prefs");
    }

    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public static void setContent(View content) {
        mContent = content;
    }

    // ?
    public void SetImaper(Imaper currentImaper) {
        this.thisSessionImapFolder = currentImaper;
    }

    // ?
    public Imaper GetImaper() {
        return this.thisSessionImapFolder;
    }

    public ImapNotes3() {
        if (BuildConfig.DEBUG)
//            StrictMode.enableDefaults();
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
//                    .penaltyDeath()
                    .build());
    }

    public static Snackbar ShowAction(
            View view,
            @StringRes int actionTextId,
            @StringRes int actionButtonId,
            int durationSeconds,
            Runnable actionCallback) {

        if (view == null)
            view = mContent;

        if (durationSeconds == 0)
            durationSeconds = BaseTransientBottomBar.LENGTH_INDEFINITE;
        else
            durationSeconds = durationSeconds * 1000;
        Snackbar snackbar =
                Snackbar.make(view, actionTextId, durationSeconds)
                        .setAction(actionButtonId, v -> actionCallback.run());

        snackbar
                .getView()
                .setBackgroundColor(mContext.getColor(R.color.ShareActionBgColor));
        snackbar.setTextColor(mContext.getColor(R.color.ShareActionTxtColor));
        snackbar.setActionTextColor(mContext.getColor(R.color.ShareActionTxtColor));
        snackbar.show();
        return snackbar;
    }

    public static String AvoidLargeBundle;
    public static void ShowMessage(@StringRes int resId, View view, int durationSeconds) {
        ShowMessage(mContext.getResources().getString(resId), view, durationSeconds);
    }

    public static void ShowMessage(String message, View view, int durationSeconds) {

        if (view == null)
            view = mContent;

        Snackbar snackbar =
                Snackbar.make(view, message, durationSeconds * 1000);
        snackbar
                .getView()
                .setBackgroundColor(mContext.getColor(R.color.ShareActionBgColor));
        snackbar.setTextColor(mContext.getColor(R.color.ShareActionTxtColor));
        snackbar.setActionTextColor(mContext.getColor(R.color.ShareActionTxtColor));

        snackbar.show();
    }

}
