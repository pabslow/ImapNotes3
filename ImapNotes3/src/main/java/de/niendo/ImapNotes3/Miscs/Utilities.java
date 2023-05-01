/*
 * Copyright (C) 2022-2023 - Peter Korf <peter@niendo.de>
 * Copyright (C)         ? - kwhitefoot
 * Copyright (C)      2016 - kj
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
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.niendo.ImapNotes3.BuildConfig;
import de.niendo.ImapNotes3.R;

/**
 * <p>
 * Reduce repetition by providing static fields and methods for common operations.
 */
public final class Utilities {
    @NonNull
    public static final String PackageName = BuildConfig.APPLICATION_ID;
    @NonNull
    public static final String ApplicationName = BuildConfig.APPLICATION_NAME;
    @NonNull
    public static final String FullApplicationName = ApplicationName + " " + BuildConfig.VERSION_NAME;
    @NonNull
    public static String internalDateFormatString = "yyyy-MM-dd HH:mm:ss";
    @NonNull
    public static SimpleDateFormat internalDateFormat = new SimpleDateFormat(internalDateFormatString, Locale.ROOT);
    public static String HASHTAG_PATTERN = "(?<=(\\s|^))#[^\\s\\!\\@\\#\\$\\%\\^\\<\\>\\&\\*\\(\\)]+(?=(\\s|$))";
    /**
     * The notes have a time stamp associated with time and this is stored as a string on the
     * server so we must define a fixed format for it.
     */

    public static int getColorIdByName(String name) {
        int color = R.color.ListBgColor;

        if (name == null || name.isEmpty()) {
            name = "none";
        }

        try {
            Class res = R.color.class;
            Field field = res.getField(name);
            color = field.getInt(null);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return color;
    }

    public static int getColorByName(String name, Context context) {
        // it is already a color like #ffffff
        if (name.contains("#")) {
            return (Integer.parseInt(name.replace(" ", "").replace("#", ""), 16));
        }
        return context.getResources().getColor(getColorIdByName(name), context.getTheme());
    }

    public static String getValueFromJSON(String text, String key) {
        // don't want to add a json lib..just for this case
        // \{\"href\":\"(.*?)\"\}
        Pattern pattern = Pattern.compile("\\{\\\"" + key + "\\\":\\\"(.*?)\\\"\\}", Pattern.CASE_INSENSITIVE);
        Matcher matcherColor = pattern.matcher(text);
        if (matcherColor.find()) {
            return matcherColor.group(1);
        } else {
            return "";
        }
    }

    public static String CheckUrlScheme(String url) {
        Uri uri = Uri.parse(url);
        if (uri.getScheme() == null) return "http://" + url;
        return url;
    }


    public static String getRealSizeFromUri(Context context, Uri uri) {
        // https://stackoverflow.com/questions/45589736/uri-file-size-is-always-0

        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Audio.Media.SIZE};
            cursor = context.getContentResolver().query(uri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

}
