package de.niendo.ImapNotes3.Miscs;

import static android.os.Environment.DIRECTORY_DOCUMENTS;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;

import de.niendo.ImapNotes3.ImapNotes3;
import de.niendo.ImapNotes3.R;
import de.niendo.ImapNotes3.Sync.SyncUtils;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Check;
import eltos.simpledialogfragment.form.FormElement;
import eltos.simpledialogfragment.form.Hint;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;

public class BackupRestore extends DialogFragment implements SimpleDialog.OnDialogResultListener {
    public static final String TAG = "IN_BackupDialog";
    private static final String ACCOUNTNAME = "ACCOUNTNAME";
    private static final String BACKUP_RESTORE_DIALOG = "BACKUP_RESTORE_DIALOG";
    private static final String BACKUP_RESTORE_DIALOG_ACCOUNT = "BACKUP_RESTORE_DIALOG_ACCOUNT";
    private static final String DLG_BACKUP_RESTORE_DIALOG_DEST_DIR = "DLG_BACKUP_RESTORE_DIALOG_DEST_DIR";
    private final Context context;
    private final Uri uri;
    private final List<String> accountList;
    private List<String> allNotes;

    public BackupRestore(Uri uri, List<String> accountList) {
        this.context = ImapNotes3.getAppContext();
        this.uri = uri;
        accountList.remove(0);
        this.accountList = accountList;
    }


    @NonNull
    //@Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        RestoreArchive();
        return builder.create();
    }

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
            title = Utilities.ApplicationName + "_" + ImapNotes3.RemoveReservedChars(accountname);
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
            String basePath = accountname.isEmpty() ? "" : ImapNotes3.RemoveReservedChars(accountname) + "/";
            ZipUtils.zipDirectory(directory, outfile.toString(), basePath);
            ImapNotes3.ShowMessage(context.getResources().getString(R.string.archive_created) + outfile, listview, 15);
        } catch (IOException e) {
            ImapNotes3.ShowMessage(context.getResources().getString(R.string.archive_not_created) + e.getMessage(), listview, 5);
        }
    }


    public void RestoreArchive() {
        try {
            List<String> dirsInZip = ZipUtils.listDirectories(context, uri);
            if (dirsInZip.isEmpty()) dirsInZip.add(""); // old zip format, notes in root
            if (dirsInZip.size() == 1) {
                SelectNotesDialog("");
            } else {
                SimpleFormDialog.build()
                        //.fullscreen(true) //theme is broken

                        .title("R.string.Restore_from_Backup")
                        .msg("R.string.more_then_one_account_found")
                        .fields(
                                Input.spinner(ACCOUNTNAME, (ArrayList<String>) dirsInZip)
                                        .hint("R.string.account_name_restore_import")
                                        .required(true))
                        .neg(R.string.cancel)
                        .pos(R.string.ok)
                        .show(this, BACKUP_RESTORE_DIALOG_ACCOUNT);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private INotesRestore mCallback;

    private void SelectNotesDialog(String dir) {
        //for (String dir : dirsInZip) {
        Bundle extra = new Bundle();
        try {
            allNotes = ZipUtils.listFilesInDirectory(context, uri, dir);
            int i = allNotes.size();
            FormElement<?, ?>[] formElements = new FormElement[2 * i + 2];
            i = 0;
            formElements[i++] = Input.spinner(ACCOUNTNAME, (ArrayList<String>) accountList)
                    .hint(R.string.account_name_restore)
                    .required(true);
            formElements[i++] = Hint.plain("R.string.import from: " + dir);

            String destDirectory = context.getCacheDir().toString() + "/Import/" + dir + "/";

            for (String file : allNotes) {

                try {
                    Message message = SyncUtils.ReadMailFromFile(new File(ZipUtils.extractFile(context, uri, file, destDirectory)));
                    if (!(message == null)) {
                        formElements[i++] = Check.box(file)
                                .label(message.getSubject())
                                .check(false);
                        formElements[i++] = Hint.plain(DateFormat.getDateTimeInstance().format(message.getSentDate()));
                    }
                } catch (IOException | MessagingException e) {
                    throw new RuntimeException(e);
                }


            }

            extra.putString(DLG_BACKUP_RESTORE_DIALOG_DEST_DIR, destDirectory);
            SimpleFormDialog.build()
                    //.fullscreen(true) //theme is broken
                    .title("R.string.select_notes_for_restore")
                    .msg("R.string.please_fill_in_form")
                    .fields(formElements)
                    .extra(extra)
                    .neg(R.string.cancel)
                    .neut(R.string.select_all_notes_for_restore)
                    .show(this, BACKUP_RESTORE_DIALOG);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle bundle) {
        if (which == BUTTON_NEGATIVE) return false;
        switch (dialogTag) {
            case BACKUP_RESTORE_DIALOG_ACCOUNT:
                String dir = bundle.getString(ACCOUNTNAME);
                SelectNotesDialog(dir);
                break;
            case BACKUP_RESTORE_DIALOG:
                String accountName = bundle.getString(ACCOUNTNAME);
                ArrayList<Uri> messageUris = new ArrayList<>();
                for (String file : allNotes) {
                    if (bundle.getBoolean(file) || which == BUTTON_NEUTRAL) {
                        String destDirectory = bundle.getString(DLG_BACKUP_RESTORE_DIALOG_DEST_DIR);
                        messageUris.add(Uri.fromFile(new File(destDirectory + file)));
                    }
                }
                if (!messageUris.isEmpty()) mCallback.onSelectedData(messageUris, accountName);
                break;
        }
        return false;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            mCallback = (INotesRestore) context;
        } catch (ClassCastException e) {
            Log.d(TAG, "Activity doesn't implement the INotesRestore interface");
        }
    }

    public interface INotesRestore {
        void onSelectedData(ArrayList<Uri> messageUris, String accountName);
    }

}
