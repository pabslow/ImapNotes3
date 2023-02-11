package de.niendo.ImapNotes3.Miscs;

import android.content.Context;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.niendo.ImapNotes3.BuildConfig;
import de.niendo.ImapNotes3.R;

/**
 * Created by kj on 2016-11-12 17:21.
 * <p>
 * Reduce repetition by providing static fields and methods for common operations.
 */
public final class Utilities {
    @NonNull
    public static final String PackageName = BuildConfig.APPLICATION_ID;
    @NonNull
    public static String internalDateFormatString = "yyyy-MM-dd HH:mm:ss";
    @NonNull
    public static SimpleDateFormat internalDateFormat = new SimpleDateFormat(internalDateFormatString, Locale.ROOT);

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
        return context.getResources().getColor(getColorIdByName(name), context.getTheme());
    }

    public static String getValueFromJSON(String text, String key) {
        // don't want to add a json lib..just for this case
        // \{\"href\":\"(.*?)\"\}
        Pattern pattern = Pattern.compile("\\{\\\""+key+"\\\":\\\"(.*?)\\\"\\}", Pattern.CASE_INSENSITIVE);
        Matcher matcherColor = pattern.matcher(text);
        if (matcherColor.find()) {
            return matcherColor.group(1);
        } else {
            return "";
        }
    }

}
