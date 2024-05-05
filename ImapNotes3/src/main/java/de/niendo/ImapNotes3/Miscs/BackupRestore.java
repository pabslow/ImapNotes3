package de.niendo.ImapNotes3.Miscs;

import static android.os.Environment.DIRECTORY_DOCUMENTS;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import de.niendo.ImapNotes3.ImapNotes3;
import de.niendo.ImapNotes3.R;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Check;
import eltos.simpledialogfragment.form.FormElement;
import eltos.simpledialogfragment.form.Hint;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;

public class BackupRestore implements SimpleDialog.OnDialogResultListener {
    public static final String TAG = "IN_BackupDialog";
    private static final String ACCOUNTNAME = "ACCOUNTNAME";
    private static final String BACKUP_RESTORE_DIALOG = "BACKUP_RESTORE_DIALOG";

    static public void CreateArchive(ListView listview, Activity activity, String accountname) {
        Log.d(TAG, "SendArchive");
        String directory;
        String title;
        Context context = ImapNotes3.getAppContext();

        if (accountname.isEmpty()) {
            directory = ImapNotes3.GetRootDir().toString();
            title = Utilities.ApplicationName + "_" + context.getString(R.string.all_accounts);
        } else {
            directory = ImapNotes3.GetAccountDir(accountname).toString();
            title = Utilities.ApplicationName + "_" + accountname;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            title = title + "_" + currentDateTime.format(formatter);
        }
        File extStorage = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS);
        File outfile = new File(extStorage, title + ".zip");

        try {

            if (!ZipUtils.checkPermissionStorage(context)) {
                ZipUtils.requestPermission(activity);
            }
            ZipUtils.zipDirectory(directory, outfile.toString());
            ImapNotes3.ShowMessage(context.getResources().getString(R.string.archive_created) + outfile, listview, 15);
        } catch (IOException e) {
            ImapNotes3.ShowMessage(context.getResources().getString(R.string.archive_not_created) + e.getMessage(), listview, 5);
        }
    }

    public static boolean RestoreArchive(FragmentActivity activity, Uri uri) {

        try {

            List<String> directories = ZipUtils.listDirectories(activity, uri);
            for (String dir : directories) {
                List<String> files = ZipUtils.listFilesInDirectory(activity, uri, dir);

                int i = files.size();
                FormElement[] formElements = new FormElement[i + 2];
                i = 0;
                formElements[i++] = Input.spinner(ACCOUNTNAME, "Account 1", "Account 2")
                        .hint(R.string.account_name_restore)
                        .required(false);
                formElements[i++] = Hint.plain("R.string.import from: " + dir);


                for (String file : files) {
                    formElements[i++] = Check.box(file).label(file);
                    // ZipUtils.extractFile(zipFilePath, file, destDirectory);
                }


                SimpleFormDialog.build()
                        .fullscreen()
                        .title("R.string.select_notes_for_restore")
                        .msg("R.string.please_fill_in_form")
                        .fields(formElements)
                        .show(activity, BACKUP_RESTORE_DIALOG);


            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    ;

    @Override
    public boolean onResult(@NonNull String s, int i, @NonNull Bundle bundle) {
        return false;
    }


}
