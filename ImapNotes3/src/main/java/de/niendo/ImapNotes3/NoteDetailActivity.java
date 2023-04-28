/*
 * Copyright (C) 2022-2023 - Peter Korf <peter@niendo.de>
 * Copyright (C)         ? - kwhitefoot
 * Copyright (C)      2016 - Martin Carpella
 * Copyright (C)      2014 - c0238
 * Copyright (C) 2014-2015 - nb
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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;

import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import de.niendo.ImapNotes3.Data.OneNote;
import de.niendo.ImapNotes3.Miscs.EditorMenuAdapter;
import de.niendo.ImapNotes3.Miscs.HtmlNote;
import de.niendo.ImapNotes3.Miscs.NDSpinner;
import de.niendo.ImapNotes3.Miscs.StickyNote;
import de.niendo.ImapNotes3.Miscs.Utilities;
import de.niendo.ImapNotes3.Sync.SyncUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashMap;

import javax.mail.Message;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Check;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import jp.wasabeef.richeditor.RichEditor;
import eltos.simpledialogfragment.color.SimpleColorDialog;


public class NoteDetailActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, SimpleDialog.OnDialogResultListener {

    //region Intent item names
    public static final String useSticky = "useSticky";
    public static final String selectedNote = "selectedNote";
    public static final String ActivityType = "ActivityType";
    public static final String ActivityTypeEdit = "ActivityTypeEdit";
    public static final String ActivityTypeAdd = "ActivityTypeAdd";
    public static final String ActivityTypeAddShare = "ActivityTypeAddShare";
    public static final String ActivityTypeProcessed = "ActivityTypeProcessed";
    public static final String DLG_TABLE_DIMENSION = "DLG_TABLE_DIMENSION";
    public static final String DLG_TABLE_DIMENSION_ROW = "DLG_TABLE_DIMENSION_ROW";
    public static final String DLG_TABLE_DIMENSION_COL = "DLG_TABLE_DIMENSION_COL";
    public static final String DLG_HTML_TXT_COLOR = "DLG_HTML_TXT_COLOR";
    public static final String DLG_HTML_BG_COLOR = "DLG_HTML_BG_COLOR";
    public static final String DLG_INSERT_LINK = "DLG_INSERT_LINK";

    public static final String DLG_INSERT_LINK_URL = "DLG_INSERT_LINK_URL";
    public static final String DLG_INSERT_LINK_TEXT = "DLG_INSERT_LINK_TEXT";
    public static final String DLG_INSERT_LINK_TITLE = "DLG_INSERT_LINK_TITLE";
    public static final String DLG_INSERT_LINK_IMAGE = "DLG_INSERT_LINK_IMAGE";
    public static final String DLG_INSERT_LINK_IMAGE_URL = "DLG_INSERT_LINK_IMAGE_URL";
    public static final String DLG_INSERT_LINK_IMAGE_ALT = "DLG_INSERT_LINK_IMAGE_ALT";
    public static final String DLG_INSERT_LINK_IMAGE_WIDTH = "DLG_INSERT_LINK_IMAGE_WIDTH";
    public static final String DLG_INSERT_LINK_IMAGE_HEIGHT = "DLG_INSERT_LINK_IMAGE_HEIGHT";
    public static final String DLG_INSERT_LINK_IMAGE_RELATIVE = "DLG_INSERT_LINK_IMAGE_RELATIVE";

    private static final int EDIT_BUTTON = 6;
    private static final String TAG = "IN_NoteDetailActivity";
    private boolean textChanged = false;
    private boolean textChangedShare = false;
    @NonNull
    private String bgColor = "none";
    private String accountName = "";
    private String suid; // uid as string
    private RichEditor editText;
    private @ColorInt int lastTxtColor = 0x80e9a11d;
    private @ColorInt int lastBgColor = 0x80e9a11d;
    //endregion

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setElevation(0); // or other
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getColor(R.color.ActionBgColor)));
        // Don't display keyboard when on note detail, only if user touches the screen
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );
        editText = findViewById(R.id.bodyView);

        Intent intent = getIntent();
        String stringres;
        Log.d(TAG, "Check_Action_Send");
        // Get intent, action and MIME type
        String action = intent.getAction();
        String ChangeNote = intent.getStringExtra(ActivityType);
        if (ChangeNote == null)
            ChangeNote = "";
        if (action == null)
            action = "";


        if (action.equals(Intent.ACTION_SEND) && !ChangeNote.equals(ActivityTypeAddShare) && !intent.getBooleanExtra(NoteDetailActivity.ActivityTypeProcessed, false)) {
            ImapNotes3.ShowAction(editText, R.string.insert_in_note, R.string.ok, 60,
                    () -> {
                        processShareIntent(intent);
                    });
        }
        if (ChangeNote.equals(ActivityTypeEdit)) {
            HashMap hm = (HashMap) intent.getSerializableExtra(selectedNote);
            boolean usesticky = intent.getBooleanExtra(useSticky, false);

            if (hm != null) {
                suid = hm.get(OneNote.UID).toString();
                accountName = hm.get(OneNote.ACCOUNT).toString();
                File rootDir = ImapNotes3.GetAccountDir(accountName);
                Message message = SyncUtils.ReadMailFromFileRootAndNew(suid, rootDir);
                Log.d(TAG, "rootDir: " + rootDir);
                if (message != null) {
                    if (usesticky) {
                        StickyNote stickyNote = StickyNote.GetStickyFromMessage(message);
                        stringres = stickyNote.text;
                        //String position = sticky.position;
                        bgColor = stickyNote.color;
                    } else {
                        HtmlNote htmlNote = HtmlNote.GetNoteFromMessage(message);
                        stringres = htmlNote.text;
                        bgColor = htmlNote.color;
                    }
                    SetupRichEditor();
                    editText.setHtml(stringres);
                } else {
                    // Entry can not opened..
                    ImapNotes3.ShowMessage(R.string.sync_wait_necessary, null, 3);
                    finish();
                    return;
                }
            } else { // Entry can not opened..
                ImapNotes3.ShowMessage(R.string.Invalid_Message, null, 3);
                finish();
                return;
            }
        } else if (ChangeNote.equals(ActivityTypeAdd)) {   // new entry
            SetupRichEditor();
        } else if (ChangeNote.equals(ActivityTypeAddShare)) {   // new Entry from Share
            SetupRichEditor();
            processShareIntent(intent);
        }
        ResetColors();
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if (which == BUTTON_POSITIVE) {
            switch (dialogTag) {
                case DLG_HTML_TXT_COLOR:
                    lastTxtColor = extras.getInt(SimpleColorDialog.COLOR);
                    editText.setTextColor(lastTxtColor);
                    return true;
                case DLG_HTML_BG_COLOR:
                    lastBgColor = extras.getInt(SimpleColorDialog.COLOR);
                    editText.setTextBackgroundColor(lastBgColor);
                    return true;
                case DLG_TABLE_DIMENSION:
                    editText.insertTable(Integer.valueOf(extras.getString(DLG_TABLE_DIMENSION_COL)), Integer.valueOf(extras.getString(DLG_TABLE_DIMENSION_ROW)));
                    return true;
                case DLG_INSERT_LINK:
                    editText.insertLink(Utilities.CheckUrlScheme(extras.getString(DLG_INSERT_LINK_URL)), extras.getString(DLG_INSERT_LINK_TEXT), extras.getString(DLG_INSERT_LINK_TITLE));
                    return true;
                case DLG_INSERT_LINK_IMAGE:
                    Boolean relative = extras.getBoolean(DLG_INSERT_LINK_IMAGE_RELATIVE);
                    editText.insertImage(Utilities.CheckUrlScheme(extras.getString(DLG_INSERT_LINK_IMAGE_URL)),
                            extras.getString(DLG_INSERT_LINK_IMAGE_ALT), extras.getString(DLG_INSERT_LINK_IMAGE_WIDTH),
                            extras.getString(DLG_INSERT_LINK_IMAGE_HEIGHT), relative);
                    return true;
            }
        }
        return false;
    }

    private void SetupRichEditor() {
        editText.setPadding(10, 10, 10, 10);
        //    editText.setBackground("https://raw.githubusercontent.com/wasabeef/art/master/chip.jpg");
        editText.setPlaceholder(getString(R.string.placeholder));
        editText.LoadFont("Alita Brush", "Alita Brush.ttf");

        editText.setOnTextChangeListener(text -> {
            if (text.contains("loaded"))
                textChanged = textChangedShare;
            if (text.contains("input"))
                textChanged = true;
        });

        editText.setOnClickListener(new RichEditor.onClickListener() {
            @Override
            public void onClick(String text) {
                String url = Utilities.getValueFromJSON(text, "href");
                ImapNotes3.ShowAction(editText, R.string.open_link, R.string.ok, 3,
                        () -> {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            try {
                                startActivity(browserIntent);
                            } catch (ActivityNotFoundException e) {
                                ImapNotes3.ShowMessage(R.string.no_program_found, editText, 3);
                            }

                        });
            }
        });

        NDSpinner formatSpinner = findViewById(R.id.action_format);
        formatSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[11], R.id.action_format, this));
        formatSpinner.setOnItemSelectedListener(this);

        NDSpinner insertSpinner = findViewById(R.id.action_insert);
        insertSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[12], R.id.action_insert, this));
        insertSpinner.setOnItemSelectedListener(this);

        NDSpinner headingSpinner = findViewById(R.id.action_heading);
        headingSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[8], R.id.action_heading, this));
        headingSpinner.setOnItemSelectedListener(this);

        NDSpinner alignmentSpinner = findViewById(R.id.action_alignment);
        alignmentSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[9], R.id.action_alignment, this));
        alignmentSpinner.setOnItemSelectedListener(this);

        NDSpinner tableSpinner = findViewById(R.id.action_table);
        tableSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[5], R.id.action_table, this));
        tableSpinner.setOnItemSelectedListener(this);

        findViewById(R.id.action_undo).setOnClickListener(v -> editText.undo());
        findViewById(R.id.action_redo).setOnClickListener(v -> editText.redo());

    }

/*
    // TODO: delete this?
    public void onClick(View view) {
        Log.d(TAG, "onClick");
        //Boolean isClicked = true;
    }
*/

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        NDSpinner spinner = (NDSpinner) parent;
        if ((view == null) || (!spinner.initIsDone)) return;
        switch (view.getId()) {
            case R.id.action_removeFormat:
                editText.removeFormat();
                break;
            case R.id.action_bold:
                editText.toggleBold();
                break;
            case R.id.action_italic:
                editText.toggleItalic();
                break;
            case R.id.action_subscript:
                editText.setSubscript();
                break;
            case R.id.action_superscript:
                editText.setSuperscript();
                break;
            case R.id.action_strikethrough:
                editText.toggleStrikeThrough();
                break;
            case R.id.action_underline:
                editText.toggleUnderline();
                break;
            case R.id.action_heading1:
                editText.setHeading(1);
                break;
            case R.id.action_heading2:
                editText.setHeading(2);
                break;
            case R.id.action_heading3:
                editText.setHeading(3);
                break;
            case R.id.action_heading4:
                editText.setHeading(4);
                break;
            case R.id.action_heading5:
                editText.setHeading(5);
                break;
            case R.id.action_heading6:
                editText.setHeading(6);
                break;
            case R.id.action_txt_color:
                SimpleColorDialog.build()
                        .colors(this, SimpleColorDialog.MATERIAL_COLOR_PALLET_DARK)
                        .title(getString(R.string.selectTextColor))
                        .colorPreset(lastTxtColor)
                        .setupColorWheelAlpha(false)
                        .allowCustom(true)
                        .neg(R.string.cancel)
                        .show(this, DLG_HTML_TXT_COLOR);
                break;
            case R.id.action_bg_color:
                SimpleColorDialog.build()
                        .colors(this, SimpleColorDialog.MATERIAL_COLOR_PALLET_LIGHT)
                        .title(getString(R.string.selectBgColor))
                        .colorPreset(lastBgColor)
                        .setupColorWheelAlpha(false)
                        .allowCustom(true)
                        .neg(R.string.cancel)
                        .show(this, DLG_HTML_BG_COLOR);
                break;
            case R.id.action_font_size_1:
                editText.setFontSize(1);
                break;
            case R.id.action_font_size_2:
                editText.setFontSize(2);
                break;
            case R.id.action_font_size_3:
                editText.setFontSize(3);
                break;
            case R.id.action_font_size_4:
                editText.setFontSize(4);
                break;
            case R.id.action_font_size_5:
                editText.setFontSize(5);
                break;
            case R.id.action_font_size_6:
                editText.setFontSize(6);
                break;
            case R.id.action_font_size_7:
                editText.setFontSize(7);
                break;
            case R.id.action_font_serif:
                editText.setFontFamily("serif");
                break;
            case R.id.action_font_sansserif:
                editText.setFontFamily("sans-serif");
                break;
            case R.id.action_font_monospace:
                editText.setFontFamily("monospace");
                break;
            case R.id.action_font_cursive:
                editText.setFontFamily("cursive");
                break;
            case R.id.action_font_fantasy:
                editText.setFontFamily("Alita Brush");
                break;
            case R.id.action_indent:
                editText.setIndent();
                break;
            case R.id.action_outdent:
                editText.setOutdent();
                break;
            case R.id.action_align_left:
                editText.setAlignLeft();
                break;
            case R.id.action_align_center:
                editText.setAlignCenter();
                break;
            case R.id.action_align_right:
                editText.setAlignRight();
                break;
            case R.id.action_blockquote:
                editText.setBlockquote();
                break;
            case R.id.action_insert_bullets:
                editText.setUnorderedList();
                break;
            case R.id.action_insert_numbers:
                editText.setOrderedList();
                break;
            case R.id.action_code_off_html:
                editText.setOnJSDataListener(value -> {
                    editText.insertHTML(Html.escapeHtml(value));
                });
                editText.getSelectedHtml();
                break;
            case R.id.action_code_html:
                editText.setOnJSDataListener(value -> {
                    editText.insertHTML(value);
                });
                editText.getSelectedText();
                break;
            case R.id.action_pre:
                editText.setPre();
                break;
            case R.id.action_insert_image:
                // 1. get the selected text via callback
                // 2. make the Image
                editText.setOnJSDataListener(value -> {
                    SimpleFormDialog.build()
                            .title(R.string.insert_link_image)
                            //.msg(R.string.please_fill_in_form)
                            .fields(
                                    Input.plain(DLG_INSERT_LINK_IMAGE_URL)
                                            .required()
                                            .hint(R.string.link_image_url)
                                            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL)
                                            .text(value),
                                    Check.box(DLG_INSERT_LINK_IMAGE_RELATIVE)
                                            .check(true)
                                            .label(R.string.image_size_relative),
                                    Input.plain(DLG_INSERT_LINK_IMAGE_WIDTH)
                                            .hint(R.string.link_image_width)
                                            .inputType(InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_CLASS_NUMBER)
                                            .text("100"),
                                    Input.plain(DLG_INSERT_LINK_IMAGE_HEIGHT)
                                            .hint(R.string.link_image_height)
                                            .inputType(InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_CLASS_NUMBER)
                                            .text(""),
                                    Input.plain(DLG_INSERT_LINK_IMAGE_ALT)
                                            .hint(R.string.link_alt_text)
                                            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL)
                                            .text("")
                            )
                            .neg(R.string.cancel)
                            .show(this, DLG_INSERT_LINK_IMAGE);
                });
                editText.getSelectedText();
                break;
            case R.id.action_insert_audio:
                // 1. get the selected text via callback
                // 2. make the Image
                editText.setOnJSDataListener(value -> {
                    if (!value.isEmpty()) {
                        editText.insertAudio(value);
                    } else
                        ImapNotes3.ShowMessage(R.string.select_link_audio, editText, 1);
                });
                editText.getSelectedText();
                break;
            case R.id.action_insert_video:
                // 1. get the selected text via callback
                // 2. make the Image
                editText.setOnJSDataListener(value -> {
                    if (!value.isEmpty()) {
                        if (value.startsWith("https://www.youtube.com"))
                            value = value.replace("watch?v=", "embed/");
                        // https://www.youtube.com/watch?v=3AeYHDZ2riI
                        // https://www.youtube.com/embed/3AeYHDZ2riI
                        editText.insertVideo(value, "video", "auto", "");
                    } else
                        ImapNotes3.ShowMessage(R.string.select_link_video, editText, 3);
                });
                editText.getSelectedText();
                break;
            case R.id.action_insert_youtube:
                // 1. get the selected text via callback
                // 2. make the Image
                editText.setOnJSDataListener(value -> {
                    if (!value.isEmpty()) {
                        if (value.startsWith("https://www.youtube.com"))
                            value = value.replace("watch?v=", "embed/");

                        // https://www.youtube.com/watch?v=3AeYHDZ2riI
                        // https://www.youtube.com/embed/3AeYHDZ2riI

                        editText.insertYoutubeVideo(value);
                    } else
                        ImapNotes3.ShowMessage(R.string.select_link_youtube, editText, 3);
                });
                editText.getSelectedText();
                break;
            case R.id.action_insert_link:
                // 1. get the selected text via callback
                // 2. make the Link
                editText.setOnJSDataListener(value -> {
                    String url = "";
                    String text = "";
                    String title = "";
                    if (!value.isEmpty()) {
                        String[] values = value.split(" ", 2);
                        if (values.length == 2) {
                            url = Utilities.CheckUrlScheme(values[0]);
                            title = values[0];
                            text = values[1];
                        } else {
                            url = Utilities.CheckUrlScheme(value);
                            text = value;
                            title = value;
                        }
                    }
                    SimpleFormDialog.build()
                            .title(R.string.insert_link)
                            //.msg(R.string.please_fill_in_form)
                            .fields(
                                    Input.plain(DLG_INSERT_LINK_URL)
                                            .required()
                                            .hint(R.string.link_url)
                                            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL)
                                            .text(url),
                                    Input.plain(DLG_INSERT_LINK_TEXT)
                                            .hint(R.string.link_text)
                                            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL)
                                            .text(text),
                                    Input.plain(DLG_INSERT_LINK_TITLE)
                                            .required()
                                            .hint(R.string.link_title)
                                            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL)
                                            .text(title)
                            )
                            .neg(R.string.cancel)
                            .show(this, DLG_INSERT_LINK);
                });
                editText.getSelectedText();
                break;
            case R.id.action_insert_checkbox:
                editText.insertCheckbox();
                break;
            case R.id.action_insert_star:
                editText.insertHTML("&#11088;");
                break;
            case R.id.action_insert_question:
                editText.insertHTML("&#10067;");
                break;
            case R.id.action_insert_datetime:
                //String date = Utilities.internalDateFormat.format(Calendar.getInstance().getTime());
                String date = Calendar.getInstance().getTime().toLocaleString();
                editText.insertHTML(date);
                break;
            case R.id.action_insert_exclamation:
                editText.insertHTML("&#10071;");
                break;
            case R.id.action_insert_hline:
                editText.insertHR_Line();
                break;
            case R.id.action_insert_section:
                editText.insertCollapsibleSection(getString(R.string.section), getString(R.string.content));
                break;
            case R.id.action_insert_table:
                SimpleFormDialog.build()
                        .title(R.string.enter_table_dimension)
                        //.msg(R.string.please_fill_in_form)
                        .fields(
                                Input.plain(DLG_TABLE_DIMENSION_COL).required().hint(R.string.count_table_col)
                                        .inputType(InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_CLASS_NUMBER)
                                        .text("3"),
                                Input.plain(DLG_TABLE_DIMENSION_ROW).required().hint(R.string.count_table_row)
                                        .inputType(InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_CLASS_NUMBER)
                                        .text("5")
                        )
                        .neg(R.string.cancel)
                        .show(this, DLG_TABLE_DIMENSION);

                break;
            case R.id.action_insert_column:
                editText.addColumnToTable();
                break;
            case R.id.action_insert_row:
                editText.addRowToTable();
                break;
            case R.id.action_delete_column:
                editText.deleteColumnFromTable();
                break;
            case R.id.action_delete_row:
                editText.deleteRowFromTable();
                break;

        }

        // for color selection, it does not closes by itself
        NDSpinner formatSpinner = findViewById(R.id.action_format);
        formatSpinner.onDetachedFromWindow();

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    // realColor is misnamed.  It is the ID of the radio button widget that chooses the background
    // colour.
    private void ResetColors() {
        editText.setEditorBackgroundColor(Utilities.getColorByName(bgColor, getApplicationContext()));
        editText.setEditorFontColor(getColor(R.color.EditorTxtColor));
        (findViewById(R.id.scrollView)).setBackgroundColor(Utilities.getColorByName(bgColor, getApplicationContext()));
        lastTxtColor = getColor(R.color.EditorTxtColor);
        lastBgColor = Utilities.getColorByName(bgColor, getApplicationContext());
    }

    @SuppressLint("RestrictedApi")
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail, menu);
        MenuBuilder m = (MenuBuilder) menu;
        m.setOptionalIconsVisible(true);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        //MenuItem item = menu.findItem(R.id.color);
        super.onPrepareOptionsMenu(menu);
        //depending on your conditions, either enable/disable
        //item.setVisible(usesticky);
        //menu.findItem(color.id).setChecked(true);
        return true;
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final Intent intent = new Intent();
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.delete:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.delete_note)
                        .setMessage(R.string.are_you_sure_you_wish_to_delete_the_note)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                            //Log.d(TAG,"We ask to delete Message #"+this.currentNote.get("number"));
                            intent.putExtra("DELETE_ITEM_NUM_IMAP", suid);
                            setResult(ListActivity.DELETE_BUTTON, intent);
                            finish();//finishing activity
                        })
                        .setNegativeButton(R.string.no, null).show();
                return true;
            case R.id.save:
                Save(true);
                return true;
            case R.id.share:
                Share();
                return true;
            case android.R.id.home:
                onBackPressed();
                //NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.none:
                item.setChecked(true);
                bgColor = "none";
                ResetColors();
                return true;
            case R.id.blue:
                item.setChecked(true);
                bgColor = "blue";
                ResetColors();
                return true;
            case R.id.white:
                item.setChecked(true);
                bgColor = "white";
                ResetColors();
                return true;
            case R.id.black:
                item.setChecked(true);
                bgColor = "black";
                ResetColors();
                return true;
            case R.id.yellow:
                item.setChecked(true);
                bgColor = "yellow";
                ResetColors();
                return true;
            case R.id.pink:
                item.setChecked(true);
                bgColor = "pink";
                ResetColors();
                return true;
            case R.id.green:
                item.setChecked(true);
                bgColor = "green";
                ResetColors();
                return true;
            case R.id.brown:
                item.setChecked(true);
                bgColor = "brown";
                ResetColors();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Note that this function does not save the note to permanent storage it just passes it back to
     * the calling activity to be saved in whatever fashion it that activity wishes.
     */
    private void Save(boolean finish) {
        Log.d(TAG, "Save");
        editText.setOnJSDataListener(value -> {
            Intent intent = new Intent();
            intent.putExtra(ListActivity.EDIT_ITEM_NUM_IMAP, suid);
            intent.putExtra(ListActivity.EDIT_ITEM_ACCOUNTNAME, accountName);
            intent.putExtra(ListActivity.EDIT_ITEM_TXT, value);
            intent.putExtra(ListActivity.EDIT_ITEM_COLOR, bgColor);
            setResult(NoteDetailActivity.EDIT_BUTTON, intent);
            textChanged = false;
            if (finish) finish(); //finishing activity
        });
        // data comes via callback
        editText.getHtml();
    }

    private void Share() {
        Log.d(TAG, "Share");
        editText.setOnJSDataListener(value -> {
            Intent sendIntent = new Intent();
            Spanned html = Html.fromHtml(value, Html.FROM_HTML_MODE_COMPACT);
            String[] tok = html.toString().split("\n", 2);
            String title = tok[0];

            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.setType("text/html");
            sendIntent.putExtra(Intent.EXTRA_TEXT, html);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.shared_note_from) + Utilities.ApplicationName + ": " + title);

            String directory = getApplicationContext().getCacheDir().toString();
            File outfile = new File(directory, title.replaceAll("[#:/]", "") + ".html");
            Log.d(TAG, "Share Note: " + outfile);
            try (OutputStream str = new FileOutputStream(outfile)) {
                str.write(value.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                ImapNotes3.ShowMessage(R.string.share_file_error + e.toString(), editText, 2);
                e.printStackTrace();
            }

            Uri logUri =
                    FileProvider.getUriForFile(
                            getApplicationContext(),
                            Utilities.PackageName, outfile);
            sendIntent.putExtra(Intent.EXTRA_STREAM, logUri);

            Intent shareIntent = Intent.createChooser(sendIntent, title);
            startActivity(shareIntent);
        });
        // data comes via callback
        editText.getHtml();

    }

    private void processShareIntent(Intent intent) {
        String sharedData = "";
        // Share: Receive Data as new message
        String strAction = intent.getAction();
        if (!editText.hasFocus()) editText.focusEditor();
        if ((strAction != null) && strAction.equals(Intent.ACTION_SEND)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            String type = intent.getType();
            String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);

            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            Log.d(TAG, "Share 1");
            if (uri != null) {
                if (type.startsWith("image/")) {
                    //    File mediaFile = new File(uri.getPath());
                    //    long fileSizeInKB = mediaFile.length() / 1024;
                    //    if (fileSizeInKB > 500) {
                    //        ImapNotes3.ShowMessage(R.string.image_size_to_large, editText,2);
                    //    } else {
                    editText.insertHTML(uri.getPath());
                    editText.insertImageAsBase64(uri, uri.getPath(), "100", "", true);
                    //    }
                } else {
                    BufferedInputStream bufferedInputStream;
                    try {
                        bufferedInputStream =
                                new BufferedInputStream(getContentResolver().openInputStream(uri));
                        byte[] contents = new byte[1024];
                        int bytesRead = 0;
                        while ((bytesRead = bufferedInputStream.read(contents)) != -1) {
                            sharedData += new String(contents, 0, bytesRead);
                        }
                        bufferedInputStream.close();
                        editText.insertHTML(sharedData);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (subject != null) {
                    subject = "<b>" + subject + "</b><br>";
                } else subject = "";
                if (sharedText != null) {
                    if (type.equals("text/html")) {
                        editText.insertHTML(subject + sharedText);
                    } else if (type.startsWith("text/")) {
                        editText.insertHTML(Html.escapeHtml(subject + sharedText));
                    } else if (type.startsWith("image/")) {
                        editText.insertImage(sharedText, "shared image", "100", "", true);
                    }
                }
            }
            textChangedShare = true;
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        if (textChanged) {
            new AlertDialog.Builder(this)
                    .setIcon(R.mipmap.ic_launcher)
                    .setTitle(R.string.made_changes)
                    .setMessage(R.string.save_changes)
                    .setNegativeButton(R.string.no, (arg0, arg1) -> NoteDetailActivity.super.onBackPressed())
                    .setNeutralButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.yes, (arg0, arg1) -> {
                        Save(true);
                    }).create().show();
        } else {
            NoteDetailActivity.super.onBackPressed();
        }
    }


}
