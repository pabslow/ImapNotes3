package de.niendo.ImapNotes3.Miscs;

import static android.os.Environment.DIRECTORY_DOCUMENTS;

import android.app.Activity;
import android.app.backup.BackupDataOutput;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import de.niendo.ImapNotes3.ImapNotes3;
import de.niendo.ImapNotes3.ListActivity;
import de.niendo.ImapNotes3.NoteDetailActivity;
import de.niendo.ImapNotes3.R;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Check;
import eltos.simpledialogfragment.form.FormElement;
import eltos.simpledialogfragment.form.Hint;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;

public class BackupDialog extends AppCompatActivity implements SimpleDialog.OnDialogResultListener {
    public static final String TAG = "IN_BackupDialog";
    private static final String ACCOUNTNAME = "ACCOUNTNAME";
    private static final String BACKUP_RESTORE_DIALOG = "BACKUP_RESTORE_DIALOG";
    private static final int FILE_SELECT_CODE = 0;
    private View view;

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

        /*
        Uri logUri =
                FileProvider.getUriForFile(
                        getApplicationContext(),
                        Utilities.PackageName, outfile);

        Intent sendIntent = new Intent();

        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.setType("application/zip");
        sendIntent.putExtra(Intent.EXTRA_TEXT, title);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        sendIntent.putExtra(Intent.EXTRA_STREAM, logUri);

        Intent shareIntent = Intent.createChooser(sendIntent, title);
        startActivity(shareIntent);
*/
    }

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_main); // Uncomment this line if you have a layout file
    }

    public void openFileSelector(View view) {
        this.view = view;
        //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        Intent intent = new Intent(view.getContext(), BackupDialog.class);
        intent.setType("application/zip");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a ZIP file"),
                    FILE_SELECT_CODE);
        } catch (ActivityNotFoundException ex) {
            ImapNotes3.ShowMessage("Please install a file manager.", view, 3000);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_SELECT_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    String path = uri.getPath();
                    if (path != null) {
                        File file = new File(path);
                        if (file.exists()) {
                            RestoreArchive(this, file.toString());
                            // Do something with the selected file
                            // For example, you can get the file path like this: file.getAbsolutePath()
                            //Toast.makeText(this, "Selected file: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        }
    }

    public boolean RestoreArchive(FragmentActivity activity, String zipFilePath) {

        try {

            List<String> directories = ZipUtils.listDirectories(zipFilePath);
            for (String dir : directories) {
                List<String> files = ZipUtils.listFilesInDirectory(zipFilePath, dir);


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
