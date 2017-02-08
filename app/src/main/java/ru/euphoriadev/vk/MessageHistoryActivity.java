package ru.euphoriadev.vk;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.euphoriadev.vk.adapter.BaseArrayAdapter;
import ru.euphoriadev.vk.adapter.ChatMember;
import ru.euphoriadev.vk.adapter.ChatMemberAdapter;
import ru.euphoriadev.vk.adapter.MessageAdapter;
import ru.euphoriadev.vk.adapter.MessageCursorAdapter;
import ru.euphoriadev.vk.adapter.MessageItem;
import ru.euphoriadev.vk.api.Api;
import ru.euphoriadev.vk.api.KException;
import ru.euphoriadev.vk.api.model.VKAttachment;
import ru.euphoriadev.vk.api.model.VKFullUser;
import ru.euphoriadev.vk.api.model.VKMessage;
import ru.euphoriadev.vk.api.model.VKUser;
import ru.euphoriadev.vk.async.ThreadExecutor;
import ru.euphoriadev.vk.common.PrefManager;
import ru.euphoriadev.vk.common.ThemeManager;
import ru.euphoriadev.vk.helper.DBHelper;
import ru.euphoriadev.vk.interfaces.RunnableToast;
import ru.euphoriadev.vk.napi.VKApi;
import ru.euphoriadev.vk.service.LongPollService;
import ru.euphoriadev.vk.sqlite.VKInsertHelper;
import ru.euphoriadev.vk.util.AndroidUtils;
import ru.euphoriadev.vk.util.Encrypter;
import ru.euphoriadev.vk.util.ThemeUtils;
import ru.euphoriadev.vk.util.ViewUtil;
import ru.euphoriadev.vk.util.YandexTranslator;
import ru.euphoriadev.vk.view.FixedListView;
import ru.euphoriadev.vk.view.fab.FloatingActionButton;

/**
 * Created by Igor on 15.03.15.
 */

public class MessageHistoryActivity extends BaseThemedActivity {

    private FixedListView lvHistory;
    private EditText etMessageText;
    private LinearLayout attachmentPanel;
    private ImageButton buttonAttachment;
    private View viewShadow;
    private ArrayList<MessageItem> history;
    private MessageCursorAdapter cursorAdapter;
    private MessageAdapter adapter;

    private SQLiteDatabase database;
    private DBHelper helper;
    private ExecutorService executor;

    private Api api;
    private int uid;
    private int chat_id;
    private long lastTypeNotification;
    private boolean forceClose;
    private boolean hideTyping;
    /**
     * can I load old messages, If false, then messages are loading
     */
    private boolean canLoadOldMessages;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_history);
        if (ThemeManager.isLightTheme()) {
//            getWindow().setBackgroundDrawable(
//                    new ColorDrawable(
//                            ResourcesLoader.getColor(R.color.md_grey_200)));

//            ColorDrawable whiteColor = new ColorDrawable(Color.WHITE);
//            ColorDrawable themeColor = new ColorDrawable(ThemeManager.alphaColor(ThemeManager.getThemeColor(this), 0.10f));
//
//            getWindow().setBackgroundDrawable(new LayerDrawable(new Drawable[]{whiteColor, themeColor}));
        }

        String fullName = getIntent().getExtras().getString("fullName");
        uid = getIntent().getExtras().getInt("user_id");
        chat_id = getIntent().getExtras().getInt("chat_id");
        int users_count = getIntent().getExtras().getInt("users_count");
        boolean isOnline = getIntent().getExtras().getBoolean("online");
        final boolean from_saved = getIntent().getExtras().getBoolean("from_saved", false);
        boolean from_service = getIntent().getExtras().getBoolean("from_sarvice", false);

        api = Api.get();
        if (from_service) LongPollService.messageCount = 0;

        SharedPreferences sPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        hideTyping = sPrefs.getBoolean("hide_typing", false);

        lvHistory = (FixedListView) findViewById(R.id.lvHistory);
        lvHistory.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        lvHistory.setStackFromBottom(true);

        etMessageText = (EditText) findViewById(R.id.messageText);
        etMessageText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!hideTyping && System.currentTimeMillis() - lastTypeNotification > 5000)
                    setTyping();
            }
        });
        ViewUtil.setTypeface(etMessageText);

        FloatingActionButton fabSend = (FloatingActionButton) findViewById(R.id.fabMessageSend);
        fabSend.setColorNormal(ThemeUtils.getThemeAttrColor(this, R.attr.colorAccent));
        fabSend.setColorPressed(ViewUtil.getPressedColor(fabSend.getColorNormal()));
        fabSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(etMessageText.getText().toString());
            }
        });

//        ImageView fabIcon = (ImageView) findViewById(R.id.fabIcon);
//        ImageView fabBackground = (ImageView) findViewById(R.id.fabBackground);
        if (PrefManager.getBoolean(SettingsFragment.KEY_USE_CAT_ICON_SEND)) {
            fabSend.setImageResource(R.drawable.ic_pets_white);
        } else {
            fabSend.setImageResource(R.drawable.ic_keyboard_arrow_right);
        }

        fabSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(etMessageText.getText().toString());
            }
        });
        fabSend.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String decryptMessage = encrypt(etMessageText.getText().toString().trim());
                sendMessage(decryptMessage);
                return true;
            }
        });
//        fabBackground.setImageDrawable(ViewUtil.createStateDrawable(
//                ViewUtil.getPressedColor(ThemeManager.getThemeColor(this)),
//                ThemeManager.getThemeColor(this)
//        ));

        ViewUtil.setFilter(fabSend, ThemeManager.getPrimaryTextColorOnAccent(this));
        ViewUtil.setFilter(etMessageText, ThemeManager.isDarkTheme() ? MessageAdapter.DEFAULT_COLOR : MessageAdapter.DEFAULT_LIGHT_COLOR);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.setSubtitleTextAppearance(this, android.R.attr.textAppearanceSmall);
        toolbar.setTitleTextColor(ThemeManager.getPrimaryTextColorOnThemeColor(this));
        toolbar.setSubtitleTextColor(ThemeManager.getPrimaryTextColorOnThemeColor(this));

        viewShadow = findViewById(R.id.layoutMessageShadow);
        if (!ThemeManager.isDarkTheme()) {
            viewShadow.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.message_drop_shadow_white));
        }

        View toolbarShadow = findViewById(R.id.toolbarShadow);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbarShadow.setVisibility(View.GONE);
        }

        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(fullName);
        if (chat_id != 0) {
            getSupportActionBar().setSubtitle(users_count + " " + (getResources().getString(R.string.members)));
        } else {
            getSupportActionBar().setSubtitle(isOnline ? getResources().getString(R.string.online) : getResources().getString(R.string.offline));
        }
        ViewUtil.setTypeface(toolbar);

        lvHistory.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (adapter == null) {
                    return;
                }
                if (scrollState == SCROLL_STATE_IDLE) {
                    adapter.isScrolling = false;
                    adapter.notifyDataSetChanged();
                } else {
                    adapter.isScrolling = true;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

//                // Если находимся в конце списка
                if (visibleItemCount > 0 && firstVisibleItem + visibleItemCount == totalItemCount) {
                    // то скрываем тень
//                    if (vshadow.getVisibility() == View.VISIBLE) vshadow.setVisibility(View.GONE);
                    animateShadow(false);
                } else {
                    // а когда начинаем прокручивать, то она появляется
//                    if (vshadow.getVisibility() == View.GONE) vshadow.setVisibility(View.VISIBLE);
                    animateShadow(true);
                }

//                // если находися на 15 position списка - грузим старые сообщеньки
                if (canLoadOldMessages && !from_saved && adapter != null && firstVisibleItem <= 15) {
                    canLoadOldMessages = false;
                    getOldMessages(30, adapter.getCount());
                }
//                Log.w("ListView", "firstVisibleItem - " + firstVisibleItem + ", visibleItemCount " + visibleItemCount);

            }
        });
        lvHistory.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                MessageItem item = (MessageItem) parent.getItemAtPosition(position);
                adapter.toggleSelection(item);
                invalidateOptionsMenu();
                return true;
            }
        });

        lvHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final MessageItem messageItem = (MessageItem) parent.getItemAtPosition(position);

                if (adapter.isInMultiSelectMode()) {
                    adapter.toggleSelection(messageItem);
                    return;
                }

                makeMessageOptionsDialog(messageItem);
            }

        });
        View listViewFooter = new View(this);
        listViewFooter.setVisibility(View.VISIBLE);
        listViewFooter.setBackgroundColor(Color.TRANSPARENT);
        listViewFooter.setVisibility(View.INVISIBLE);
        listViewFooter.setEnabled(false);
        listViewFooter.setClickable(false);
        listViewFooter.setLayoutParams(new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                AndroidUtils.pxFromDp(this, 75)
        ));
        lvHistory.addFooterView(listViewFooter);



        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chat_id != 0) getChatMembers();

            }
        });

        attachmentPanel = (LinearLayout) findViewById(R.id.attachmentPanel);
        attachmentPanel.setVisibility(View.GONE);
        buttonAttachment = (ImageButton) findViewById(R.id.btnMessageAttach);
        buttonAttachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                attachmentPanel.setVisibility(attachmentPanel.getVisibility() == View.VISIBLE
                        ? View.GONE : View.VISIBLE);
            }
        });
        if (!ThemeManager.isDarkTheme()) {
            ViewUtil.setFilter(buttonAttachment, ThemeManager.getPrimaryLightTextColor());
        }

        canLoadOldMessages = false;
        AsyncTask<Void, Void, Void> task = new LoadMessagesTask(30, 0, from_saved);
        AsyncTaskCompat.executeParallel(task);
        loadWallpaperFromSD();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.w("MessageActivity", "requestCode: " + requestCode);
        Log.w("MessageActivity", "resultCode: " + resultCode);

        if (resultCode == RESULT_OK) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(
                    selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String filePath = cursor.getString(columnIndex);
            cursor.close();

            PrefManager.putString(ThemeManager.PREF_KEY_MESSAGE_WALLPAPER_PATH, filePath);
            ThemeManager.updateThemeValues();

            AlertDialog.Builder builder = new AlertDialog.Builder(MessageHistoryActivity.this);
            builder.setTitle(getString(R.string.apply_blur));
            builder.setMessage(getString(R.string.apply_blur_description));
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    PrefManager.putBoolean(SettingsFragment.KEY_BLUR_WALLPAPER, true);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    PrefManager.putBoolean(SettingsFragment.KEY_BLUR_WALLPAPER, false);
                }
            });
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    loadWallpaperFromSD();
                }
            });
            builder.show();


            Log.w("MessageActivity", "image path is " + filePath);
        }
    }

    private void animateShadow(boolean show) {
//        if (mShowShadow == show) {
//            return;
//        }
//        mShowShadow = show;
//
//        if (show) {
//            viewShadow.setVisibility(View.VISIBLE);
//            ViewCompat.animate(viewShadow).setDuration(200).withLayer().alpha(1.0f).start();
//        } else {
//            ViewCompat.animate(viewShadow).setDuration(200).alpha(0).withLayer().withEndAction(new Runnable() {
//                @Override
//                public void run() {
//                    viewShadow.setVisibility(View.GONE);
//                }
//            }).start();
//        }
    }

    @Override
    public void onBackPressed() {
        if (adapter.isInMultiSelectMode()) {
            adapter.disableMultiSelectMode();
            invalidateOptionsMenu();
            return;
        }
        super.onBackPressed();
        this.overridePendingTransition(R.anim.diagonaltranslate_right, R.anim.diagonaltranslate_right2);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (adapter.isInMultiSelectMode()) {
            menu.findItem(R.id.menuStatsMessages).setVisible(false);
            menu.findItem(R.id.menuUpdateMessages).setVisible(false);
            menu.findItem(R.id.menuMessageMaterialsOfDialog).setVisible(false);
            menu.findItem(R.id.menuMessageTranslateAll).setVisible(false);
            menu.findItem(R.id.menuWallpaper).setVisible(false);

            menu.findItem(R.id.menuMessageDelete).setVisible(true);
        } else {
            menu.findItem(R.id.menuStatsMessages).setVisible(true);
            menu.findItem(R.id.menuUpdateMessages).setVisible(true);
            menu.findItem(R.id.menuMessageTranslateAll).setVisible(true);
            menu.findItem(R.id.menuMessageMaterialsOfDialog).setVisible(true);
            menu.findItem(R.id.menuWallpaper).setVisible(true);

            menu.findItem(R.id.menuMessageDelete).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menuStatsMessages:
                getAllMessages();
                break;

            case R.id.menuUpdateMessages:
                adapter.clear();
                adapter.notifyDataSetChanged();
                new LoadMessagesTask(30, 0, false).execute();
                break;

            case R.id.menuMessageMaterialsOfDialog:
                Intent intent = new Intent(getApplicationContext(), DialogMaterialsActivity.class);
                intent.putExtra("user_id", uid);
                intent.putExtra("chat_id", chat_id);
                startActivity(intent);
                break;

            case R.id.menuMessageTranslateAll:
                YandexTranslator.Language[] languages = YandexTranslator.Language.values();
                final CharSequence[] items = new CharSequence[languages.length];
                for (int i = 0; i < languages.length; i++) {
                    items[i] = languages[i].name();
                }


                AlertDialog.Builder builder = new AlertDialog.Builder(MessageHistoryActivity.this);
                builder.setTitle("На какой язык перевести?");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        translateAllMessages(items[which].toString());
                    }
                });
                builder.create().show();
                break;

            case R.id.menuMessageDelete:
                deleteMessages(adapter.getSelectedItems());
                adapter.disableMultiSelectMode();
                break;

            case R.id.menuWallpaper:
                ThemeManager.updateThemeValues();
                if (!TextUtils.isEmpty(ThemeManager.getWallpaperPath(this))) {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MessageHistoryActivity.this);
                    alertBuilder.setTitle(R.string.set_wallpaper);
                    alertBuilder.setItems(new CharSequence[]{
                            getResources().getString(R.string.wallpaper_change),
                            getResources().getString(R.string.wallpaper_remove)

                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0:
                                    pickImageFromGallery();
                                    break;
                                case 1:
                                    PrefManager.putString(SettingsFragment.KEY_WALLPAPER_PATH, null);
                                    ImageView ivWallpaper = (ImageView) findViewById(R.id.ivWallpaper);
                                    ivWallpaper.setImageDrawable(null);
                                    ivWallpaper.setVisibility(View.GONE);
                                    break;

                            }
                        }
                    });
                    alertBuilder.show();
                } else {
                    pickImageFromGallery();
                }
                break;

            case R.id.menuHideShowTime:
                adapter.toggleStateTime();
                break;

            case android.R.id.home:
                finish();
                this.overridePendingTransition(R.anim.diagonaltranslate_right,
                        R.anim.diagonaltranslate_right2);
                break;


        }

        return super.onOptionsItemSelected(item);
    }


    private void makeMessageOptionsDialog(final MessageItem messageItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageHistoryActivity.this);
        builder.setItems(R.array.message_dialog_array, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        // Переслать
                        Toast.makeText(MessageHistoryActivity.this, "Not yet implements", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        // Копировать
                        AndroidUtils.copyTextToClipboard(MessageHistoryActivity.this, messageItem.message.body);
                        Toast.makeText(MessageHistoryActivity.this, R.string.message_copied, Toast.LENGTH_SHORT).show();
                        break;

                    case 2:
                        // Расшифровать
                        String decryptMessage = decryptMessage(messageItem.message.body);
                        // если расшифровывание прошло удачно
                        if (decryptMessage != null) {
                            messageItem.message.body = decryptMessage;
                            adapter.notifyDataSetChanged();
                        } else {
                            // произошла ошибка, либо не та шифровка в настройках, либо это обычное сообщение
                            Toast.makeText(MessageHistoryActivity.this, R.string.error_decrypting, Toast.LENGTH_LONG).show();
                        }
                        break;

                    case 3:
                        final String[] translatedMessage = new String[1];
                        // Переводчик
                        AlertDialog.Builder alerBuilder = new AlertDialog.Builder(MessageHistoryActivity.this);
                        alerBuilder.setTitle(getResources().getString(R.string.translator))
                                .setMessage(getResources().getString(R.string.translation_text))
                                .setPositiveButton("OK", null)
                                .setNegativeButton("Copy", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (!TextUtils.isEmpty(translatedMessage[0])) {
                                            AndroidUtils.copyTextToClipboard(MessageHistoryActivity.this, translatedMessage[0]);
                                            Toast.makeText(MessageHistoryActivity.this, R.string.message_copied, Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                        final AlertDialog alertDialog = alerBuilder.create();
                        alertDialog.show();


                        YandexTranslator translator = new YandexTranslator(MessageHistoryActivity.this);
                        translator.translateAsync(messageItem.message.body, YandexTranslator.Language.ENGLISH.toString(), YandexTranslator.Language.RUSSIAN.toString(), new YandexTranslator.OnCompleteListener() {
                            @Override
                            public void onCompleteTranslate(YandexTranslator translator, String message) {
                                alertDialog.setMessage(message);
                                translatedMessage[0] = message;
                            }
                        });
                        break;

                    case 4:
                        // Удалить
                        ArrayList<MessageItem> messages = new ArrayList<>();
                        messages.add(messageItem);
                        deleteMessages(messages);

                        if (adapter.getMessages().remove(messageItem)) {
                            database.execSQL("DELETE FROM " + DBHelper.MESSAGES_TABLE + " WHERE " + DBHelper.MESSAGE_ID + " = " + messageItem.message.mid);
                            adapter.notifyDataSetChanged();
                        }
                        break;

                }
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void getAllMessages() {

        final boolean[] stop = {false};
        final int[] count = {
                0, // общее кол-во
                0, // отправленные
                0, // полученные сообщения
        };
        final Boolean[] isHideDialog = {false};

        // https://developer.android.com/training/material/shadows-clipping.html
        final AlertDialog alert;
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageHistoryActivity.this);
        builder
                .setTitle(R.string.statistics_messages)
                .setMessage("")
                .setCancelable(false)
                .setNegativeButton("Hide", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isHideDialog[0] = true;
                        Toast.makeText(MessageHistoryActivity.this, "Диалог появиться снова, когда статистика сообщений будет успешно загруженна", Toast.LENGTH_LONG).show();
                    }
                })
                .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stop[0] = true;
                    }
                });

        alert = builder.create();
        alert.show();

        if (AndroidUtils.hasConnection(getApplicationContext()))
            new Thread() {
                @Override
                public void run() {
                    try {


                        String sql;
                        // если зашли в диалог с пользователем
                        if (chat_id == 0) {
                            sql = "SELECT * FROM " + DBHelper.STATS_MESSAGES_TABLE + " WHERE chat_id = 0 AND user_id = " + uid;
                        } else {
                            // если в чат
                            sql = "SELECT * FROM " + DBHelper.STATS_MESSAGES_TABLE + " WHERE chat_id = " + chat_id;
                        }
                        Cursor cursor = database.rawQuery(sql, null);

                        while (cursor.moveToNext()) {
                            int totalCount = cursor.getInt(cursor.getColumnIndex(DBHelper.TOTAL_COUNT));
                            int outCount = cursor.getInt(cursor.getColumnIndex(DBHelper.OUTGOING_COUNT));
                            int inCount = cursor.getInt(cursor.getColumnIndex(DBHelper.INCOMING_COUNT));

                            count[0] = totalCount;
                            count[1] = outCount;
                            count[2] = inCount;
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                alert.setMessage(
                                        getString(R.string.all)
                                                + String.format("%,d", count[0]) + "\n"
                                                + getString(R.string.outgoing) + String.format("%,d", count[1]) + "\n"
                                                + getString(R.string.incoming) + String.format("%,d", count[2]));

                            }
                        });

                        //   final long startTime = System.currentTimeMillis();
                        long wordsCount = 0;
                        while (!stop[0]) {

                            ArrayList<VKMessage> dialogs = api.getMessagesHistoryWithExecute(uid, chat_id, count[0]);

                            if (dialogs.isEmpty()) {
                                stop[0] = true;
                                Log.w("Dialogs", "IS EMPTY");
                                Log.w("Dialogs", "STOPPING...");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MessageHistoryActivity.this, "Done!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                break;
                            }

                            for (VKMessage message : dialogs) {
                                count[0]++;
                                if (message.is_out) {
                                    count[1]++;
                                } else {
                                    count[2]++;
                                }
                                wordsCount = wordsCount + AndroidUtils.getWordsCount(message.body);
                            }
                            dialogs.clear();
                            dialogs.trimToSize();
                            dialogs = null;

                            final long finalWordsCount = wordsCount;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    // Для нахождение процентов надо 100 разделить на (сумма/посчитанное кол-во сообщений)
                                    alert.setMessage(
                                            getString(R.string.all_words) + String.format("%,d", finalWordsCount) + "\n" +
                                                    getString(R.string.all) + String.format("%,d", count[0]) + "\n"
                                                    + getString(R.string.outgoing) + String.format("%,d", count[1]) + "\n"
                                                    + getString(R.string.incoming) + String.format("%,d", count[2]) +
                                                    "\n");

                                }
                            });

                        }

                        ContentValues cv = new ContentValues();
                        cv.put(DBHelper.USER_ID, uid);
                        cv.put(DBHelper.CHAT_ID, chat_id);
                        cv.put(DBHelper.TOTAL_COUNT, count[0]);
                        cv.put(DBHelper.OUTGOING_COUNT, count[1]);
                        cv.put(DBHelper.INCOMING_COUNT, count[2]);

                        if (cursor.getCount() > 0) {
                            if (chat_id == 0)
                                database.update(DBHelper.STATS_MESSAGES_TABLE, cv, "chat_id = 0 AND user_id = " + uid, null);
                            else
                                database.update(DBHelper.STATS_MESSAGES_TABLE, cv, "chat_id = " + chat_id, null);
                        } else {
                            database.insert(DBHelper.STATS_MESSAGES_TABLE, null, cv);
                        }
                        cursor.close();
                        cv.clear();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!alert.isShowing() && isHideDialog[0]) {
                                    alert.show();
                                }
//                                SimpleDateFormat sdf = new SimpleDateFormat("s");
                                // Toast.makeText(MessageHistoryActivity.this, "Статистика подсчитанна за " + sdf.format(System.currentTimeMillis() - startTime) + " сек.", Toast.LENGTH_LONG).show();
                            }
                        });


                    } catch (Exception e) {
                        e.printStackTrace();
                        stop[0] = true;
                        alert.setMessage("Error...\n" + e.toString());
                    }
                }
            }.start();
        else alert.setMessage(getResources().getString(R.string.check_internet));
    }

    public String encrypt(String message) {
        String encryptMessageValue = PreferenceManager.getDefaultSharedPreferences(MessageHistoryActivity.this).getString("encrypt_messages", "hex");
        String text = message;
        switch (encryptMessageValue.toUpperCase()) {
            case "HEX":
                text = Encrypter.encodeHEX(text.getBytes());
                break;
            case "BASE":
                text = Encrypter.encodeBase64(text.getBytes());
                break;
            case "MD5":
                text = Encrypter.encodeMD5(text);
                break;
            case "BINARY":
                text = Encrypter.encodeBinary(text);
                break;
            case "3DES":
                text = new String(Encrypter.encodeDES3(text), Charset.forName("UTF-8"));
                break;
            case "HASHCODE":
                text = String.valueOf(Encrypter.encodeHashCode(text));
                break;

        }
        return text;
    }

    public String decryptMessage(String message) {
        String encryptMessageValue = PreferenceManager.getDefaultSharedPreferences(MessageHistoryActivity.this).getString("encrypt_messages", "hex");
        String text = message;
        switch (encryptMessageValue.toUpperCase()) {
            case "HEX":
                text = Encrypter.decodeHEX(text);
                break;
            case "BASE":
                text = Encrypter.decodeBase64(text.getBytes());
                break;
            case "MD5":
                text = null;
                break;
            case "BINARY":
                text = Encrypter.decodeBinary(text);
                break;
            case "3DES":
                text = Encrypter.decodeDES3(text.getBytes());
                break;
            case "HASHCODE":
                text = null;
                break;

        }
        return text;
    }

    private void setTyping() {
        if (!AndroidUtils.hasConnection(this)) {
            return;
        }
        this.lastTypeNotification = System.currentTimeMillis();
        VKApi.messages()
                .setActivity()
                .chatId(chat_id)
                .userId(uid)
                .execute(VKApi.DEFAULT_RESPONSE_LISTENER);
    }

    private void getChatMembers() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ArrayList<VKFullUser> chatUsers = api.getChatUsers(chat_id, "photo_50");
                    HashMap<Integer, VKFullUser> mapInvitedBy = new HashMap<>();
                    for (VKFullUser u : chatUsers) {
                        mapInvitedBy.put(u.invited_by, null);
                    }

                    ArrayList<VKFullUser> profiles = api.getProfilesFull(mapInvitedBy.keySet(), null, "photo_50", null, null, null);
                    for (VKFullUser u : profiles) {
                        mapInvitedBy.put(u.uid, u);
                    }

                    ArrayList<ChatMember> members = new ArrayList<>();
                    for (VKFullUser u : chatUsers) {
                        members.add(new ChatMember(u, mapInvitedBy.get(u.invited_by)));
                    }

                    final ChatMemberAdapter chatMemberAdapter = new ChatMemberAdapter(MessageHistoryActivity.this, members);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MessageHistoryActivity.this)
                                    .setTitle(R.string.title_members)
                                    .setPositiveButton("ОК", null)
                                    .setNeutralButton("Покинуть", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            exitFromChat();
                                            dialog.dismiss();
                                        }
                                    })
                                    .setNegativeButton(R.string.add, null)
                                    .setAdapter(chatMemberAdapter, null);
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    });


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void getOldMessages(final int count, final long offset) {
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor();
        }
        Log.w("MessageHistoryActivity", "get old messages, count: " + count + ", offset: " + offset);
        executor.execute(new Runnable() {
            @Override
            public void run() {
//                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);
                try {
                    VKUser emptyUser = VKUser.EMPTY;
                    HashMap<Integer, VKUser> mapUsers = null;
                    ArrayList<VKMessage> oldMessages = api.getMessagesHistory(uid, chat_id, offset, count, false);
                    if (oldMessages.isEmpty()) {
                        canLoadOldMessages = true;
                        return;
                    }

                    // если это чат. то загружаем пользователей
                    if (chat_id > 0) {
                        mapUsers = new HashMap<>();

                        for (int i = 0; i < oldMessages.size(); i++) {
                            VKMessage m = oldMessages.get(i);
                            mapUsers.put(m.uid, null);
                        }

                        ArrayList<VKUser> apiProfiles = api.getProfiles(mapUsers.keySet(), null, null, null, null);
                        for (int i = 0; i < apiProfiles.size(); i++) {
                            VKUser user = apiProfiles.get(i);
                            mapUsers.put(user.user_id, user);
                        }
                    }


                    for (int i = 0; i < oldMessages.size(); i++) {
                        VKMessage message = oldMessages.get(i);
                        history.add(0, new MessageItem(message, mapUsers == null ? emptyUser : mapUsers.get(message.uid)));
                    }


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            canLoadOldMessages = true;
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                                lvHistory.setSelection(lvHistory.getFirstVisiblePosition() + count);
                            }

                        }
                    });
                    VKInsertHelper.insertMessages(database, oldMessages, true);
//                    adapter.getMessages().trimToSize();
                    if (oldMessages != null) {
                        oldMessages.clear();
                        oldMessages.trimToSize();
                        oldMessages = null;
                    }
//                    if (tempMessageList != null) {
//                        tempMessageList.clear();
//                        tempMessageList.trimToSize();
//                        tempMessageList = null;
//                    }
                    if (mapUsers != null) {
                        mapUsers.clear();
                        mapUsers = null;
                    }
                    System.gc();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                canLoadOldMessages = true;
            }
        });
    }

    private void sendMessage(String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        if (text.equalsIgnoreCase("КЕК") || text.equalsIgnoreCase("ЛОЛ")) {
            Log.w("MessageHistoryActivity", "an attempt to send word KEK");
            Toast.makeText(this, "Сообщение с данным текстом нельзя отправить", Toast.LENGTH_LONG).show();
            return;
        }
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor();
        }
        final String finalText = text;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final VKMessage message = new VKMessage();
                message.body = finalText;
                message.uid = api.getUserId();
                message.chat_id = chat_id;
                message.is_out = true;
                message.date = System.currentTimeMillis() / 1000;

                VKUser user = new VKUser();
                final MessageItem item = new MessageItem(message, user, MessageItem.Status.SENDING);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        etMessageText.setText("");
                        adapter.getMessages().add(item);
                        adapter.notifyDataSetChanged();
                        lvHistory.smoothScrollToPosition(adapter.getCount());
                    }
                });

                try {
                    int mid = api.sendMessage(uid, chat_id, message.body, null, null, null, null, null, null, null, null);
                    item.status = MessageItem.Status.SENT;
                    item.message.mid = mid;

                    VKInsertHelper.sValues.clear();
                    VKInsertHelper.insertMessage(database, message);
                    VKInsertHelper.sValues.clear();
                    VKInsertHelper.insertDialog(database, message);
                } catch (Exception e) {
                    e.printStackTrace();
                    item.setStatus(MessageItem.Status.ERROR);
                    if (PrefManager.getBoolean("resend_failed_msg", true)) {
                        ContentValues cv = new ContentValues();
                        cv.put(DBHelper.USER_ID, uid);
                        cv.put(DBHelper.CHAT_ID, chat_id);
                        cv.put(DBHelper.BODY, message.body);
                        database.insert(DBHelper.FAILED_MESSAGES_TABLE, null, cv);
                    }
                } finally {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });
    }

    private void deleteMessages(final ArrayList<MessageItem> messages) {
        final ArrayList<MessageItem> items = new ArrayList<>(messages);
        ThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ArrayList<Integer> mids = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) {
                        mids.add(items.get(i).message.mid);
                    }
                    api.deleteMessage(mids);

                    mids.clear();
                    mids.trimToSize();

                    history.removeAll(items);

//                    for (int i = 0; i < messages.size(); i++) {
//                        MessageItem deletedMessage = messages.get(i);
//
//                        for (int j = 0; j < history.size(); j++) {
//                            MessageItem historyItem = history.get(i);
//                            if (historyItem.message.mid == deletedMessage.message.mid) {
//                                history.remove(j);
//                            }
//                        }
//                    }

                    items.clear();
                    items.trimToSize();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void exitFromChat() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    api.removeUserFromChat(chat_id, Api.get().getUserId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        finish();
    }

    private void translateAllMessages(final String languageTo) {
        final String translateTextWait = getResources().getString(R.string.translate_text_please_wait);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.translator);
        builder.setCancelable(false);
        builder.setMessage(translateTextWait);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                forceClose = true;
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();


        ThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                YandexTranslator translator = new YandexTranslator(MessageHistoryActivity.this);
                int count = 0;
                for (int i = 0; i < history.size(); i++) {
                    if (forceClose) {
                        break;
                    }
                    MessageItem item = history.get(i);
                    final String text = translator.translate(item.message.body,
                            YandexTranslator.Language.AUTO_DETECT.toString(),
                            languageTo);

                    if (text.equals("[error]")) {
                        // error. stopping translate
                        forceClose = true;
                        AndroidUtils.post(new RunnableToast(MessageHistoryActivity.this, R.string.check_internet, true));
                        continue;
                    }
                    if (!TextUtils.isEmpty(text)) {
                        item.message.body = text;
                    }
                    count++;
                    final int finalCount = count;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setMessage(translateTextWait + "\n" + "Переведенно " + finalCount + " сообщений из " + history.size());
                        }
                    });
                    translateAttachMessages(item.message, translator, languageTo);
                }
                translator.close();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        if (adapter != null)
                            adapter.notifyDataSetChanged();
                    }
                });

            }
        });
    }

    private void translateAttachMessages(VKMessage message, YandexTranslator translator, String languageTo) {
        if (message.attachments.isEmpty()) {
            return;
        }

        for (int i = 0; i < message.attachments.size(); i++) {
            // если вложение не содержит пересланного сообщения
            final VKAttachment attachment = message.attachments.get(i);
            if (!attachment.type.equals(VKAttachment.TYPE_MESSAGE)) {
                continue;
            }

            VKMessage item = attachment.message;
            String text = translator.translate(item.body,
                    YandexTranslator.Language.AUTO_DETECT.toString(),
                    languageTo);

            if (text != null) {
                item.body = text;
            }
        }
    }

    private void pickImageFromGallery() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, 100);
    }

    private void loadWallpaperFromSD() {
        String path = ThemeManager.getWallpaperPath(this);
        ImageView ivWallpaper = (ImageView) findViewById(R.id.ivWallpaper);
        if (!TextUtils.isEmpty(path)) {

            int blurRadius = PrefManager.getInt(SettingsFragment.KEY_BLUR_RADIUS);
            boolean applyBlur = PrefManager.getBoolean(SettingsFragment.KEY_BLUR_WALLPAPER);
            ivWallpaper.setVisibility(View.VISIBLE);
            final RequestCreator creator = Picasso.with(this)
                    .load(new File(path));
            if (applyBlur) {
                creator.transform(new AndroidUtils.PicassoBlurTransform(blurRadius));
            }
            creator.into(ivWallpaper);
        } else {
            ivWallpaper.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        if (cursorAdapter != null) {
            cursorAdapter.close();
            cursorAdapter = null;
        }
        if (adapter != null) {
            adapter.unregisterLongPoll();
            adapter.clear();
            adapter = null;
        }
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
//        VKLongPoll.get(this).unregister("MessageHistory");
        System.gc();
        super.onDestroy();

    }

    /**
     * Async task for load messages
     */
    private class LoadMessagesTask extends AsyncTask<Void, Void, Void> {
        private int count;
        private int offset;
        private boolean fromSaved;

        /**
         * Create a new AsyncTask
         *
         * @param count     Number of messages to return, max value 200
         * @param offset    needed to return a specific subset of messages
         * @param fromSaved true, if you have to get messages only from database
         */
        public LoadMessagesTask(int count, int offset, boolean fromSaved) {
            this.count = count;
            this.offset = offset;
            this.fromSaved = fromSaved;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            history = new ArrayList<>(30);
            adapter = new MessageAdapter(MessageHistoryActivity.this, history, uid, chat_id);
            adapter.setCloseListener(new BaseArrayAdapter.OnMultiModeCloseListener() {
                @Override
                public void onClose() {
                    Log.w("MessageActivity", "onClose ActionMode");
                    invalidateOptionsMenu();
                }
            });
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            canLoadOldMessages = true;
        }

        @Override
        protected Void doInBackground(Void... params) {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            helper = DBHelper.get(MessageHistoryActivity.this);
            database = helper.getWritableDatabase();

            if (fromSaved) {
                cursorAdapter();
                return null;
            }
            // prepare sql command
            String sql = chat_id != 0 ? ("SELECT * FROM " + DBHelper.MESSAGES_TABLE +
                    " LEFT JOIN " + DBHelper.USERS_TABLE +
                    " ON " + DBHelper.MESSAGES_TABLE + "." + DBHelper.USER_ID +
                    " = " + DBHelper.USERS_TABLE + "." + DBHelper.USER_ID +
                    " WHERE " + DBHelper.MESSAGES_TABLE + "." + DBHelper.CHAT_ID +
                    " = " + chat_id)
                    : ("SELECT * FROM " + DBHelper.MESSAGES_TABLE + " WHERE user_id = " + uid + " AND chat_id = 0");

            Cursor cursor = database.rawQuery(sql, null);
            if (cursor.getCount() > 0) {
                // Start dialog is not first,
                // loaded messages
                getMessagesFrom(cursor);
                publishProgress(null);
            }
            if (!AndroidUtils.hasConnection(MessageHistoryActivity.this)) {
                // if user not have internet connection
                cursor.close();
                AndroidUtils.post(new RunnableToast(MessageHistoryActivity.this, R.string.check_internet, true));
                return null;
            }
            ArrayList<VKMessage> messages = loadMessagesFromNetwork();
            if (messages.isEmpty()) {
                // no messages or error
                return null;
            }
            // cache users
            SparseArray<VKUser> users = new SparseArray<>(30);
            for (int i = 0; i < messages.size(); i++) {
                VKMessage message = messages.get(i);
                users.append(message.uid, VKUser.EMPTY);
            }

            HashSet<Integer> keySet = AndroidUtils.keySet(users);
            ArrayList<VKUser> vkUsers = loadUsersFromNetwork(keySet);

            for (int i = 0; i < vkUsers.size(); i++) {
                VKUser user = vkUsers.get(i);
                users.put(user.user_id, user);
            }

            history.clear();
            // Reverse adding
            for (int i = messages.size() - 1; i >= 0; i--) {
                VKMessage message = messages.get(i);
                history.add(new MessageItem(message, users.get(message.uid)));
            }

            cursor.close();
            publishProgress(null);

            deleteOldMessages(sql);

            VKInsertHelper.insertMessages(database, messages, true);
            VKInsertHelper.updateUsers(database, vkUsers, true);

            messages.clear();
            messages.trimToSize();
            messages = null;
            vkUsers.clear();
            vkUsers.trimToSize();
            vkUsers = null;
            users.clear();
            users = null;
            keySet.clear();
            keySet = null;
            cursor.close();

            Runtime.getRuntime().gc();
            return null;
        }

        @Override
        protected final void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

            // init adapter
            if (adapter == null) {
                adapter = new MessageAdapter(MessageHistoryActivity.this, history, uid, chat_id);

            } else if (adapter != lvHistory.getAdapter()) {
                lvHistory.setAdapter(adapter);
            } else if (!PrefManager.getBoolean(SettingsFragment.KEY_USE_ALTERNATIVE_UPDATE_MESSAGES)) {
                adapter.notifyDataSetChanged();
            } else {
                lvHistory.setAdapter(adapter);
            }
            // sets the currently selected item
            lvHistory.setSelection(lvHistory.getCount());

            // enables fast scroll indicator
            if (adapter.getCount() > 500) {
                lvHistory.setFastScrollEnabled(true);
            }

        }

        /**
         * Download messages.
         * Returns a list of the current user's private messages,
         * that match search criteria
         */
        private ArrayList<VKMessage> loadMessagesFromNetwork() {
            try {
                return Api.get().getMessagesHistory(uid, chat_id, this.offset, this.count, false);
            } catch (Exception e) {
                e.printStackTrace();
                AndroidUtils.runOnUi(new RunnableToast(MessageHistoryActivity.this, R.string.check_internet, true));
            }
            return new ArrayList<>(0);
        }

        /**
         * Load users from network.
         * Returns detailed information on users
         *
         * @param uids the user IDs or screen names (screen_name).
         */
        private ArrayList<VKUser> loadUsersFromNetwork(Collection<Integer> uids) {
            try {
                return Api.get().getProfiles(uids, null, null, null, null);
            } catch (IOException | JSONException | KException e) {
                e.printStackTrace();
            }
            return new ArrayList<>(0);
        }

        private void getMessagesFrom(Cursor cursor) {
            // Reverse getting messages from database
            for (cursor.moveToLast(); !cursor.isBeforeFirst(); cursor.moveToPrevious()) {
                String body = cursor.getString(4);
                String photo = chat_id != 0 ? cursor.getString(cursor.getColumnIndex(DBHelper.PHOTO_50)) : null;
                String firstName = chat_id != 0 ? cursor.getString(cursor.getColumnIndex(DBHelper.FIRST_NAME)) : null;
                String lastName = chat_id != 0 ? cursor.getString(cursor.getColumnIndex(DBHelper.LAST_NAME)) : null;
                int out = cursor.getInt(7);
                int read_state = cursor.getInt(6);
                int date = cursor.getInt(5);
                int mid = cursor.getInt(1);

                VKMessage message = new VKMessage();

                message.mid = mid;
                message.chat_id = chat_id;
                message.date = date;
                message.body = body;
                message.is_out = out == 1;
                message.read_state = read_state == 1;

                VKUser user = null;
                if (firstName != null || lastName != null) {
                    user = new VKUser();
                    user.photo_50 = photo;
                    user.first_name = firstName;
                    user.last_name = lastName;
                }

                history.add(new MessageItem(message, user == null ? VKUser.EMPTY : user));
            }
        }

        /**
         * Delete old messages, that would be in their place to put new
         */
        public void deleteOldMessages(String sql) {
            Cursor c = database.rawQuery(sql, null);
            while (c.moveToNext()) {
                int _id = c.getInt(0);
                //  database.delete(DBHelper.MESSAGES_TABLE, "_id = " + _id, null);
                database.execSQL("DELETE FROM " + DBHelper.MESSAGES_TABLE + " WHERE _id = " + _id);
            }
            c.close();
        }

        private ArrayList<VKUser> getUsersFrom(SQLiteDatabase database) {
            Cursor cursor = database.rawQuery("SELECT * FROM " + DBHelper.USERS_TABLE, null);
            ArrayList<VKUser> users = new ArrayList<>(cursor.getCount());
            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    VKUser user = new VKUser();
                    user.user_id = cursor.getInt(0);
                    user.first_name = cursor.getString(1);
                    user.last_name = cursor.getString(2);
                    user.online = cursor.getInt(4) == 1;
                    user.online_mobile = cursor.getInt(5) == 1;
                    user.status = cursor.getString(6);
                    user.photo_50 = cursor.getString(9);
                    user.photo_100 = cursor.getString(10);
                    user.photo_200 = cursor.getString(11);

                    users.add(user);
                }
                return users;
            }
            cursor.close();
            return users;
        }

        /**
         * Set Cursor adapter to list view,
         * used if user save messages to database
         */
        private void cursorAdapter() {
            final String saveSql = "SELECT * FROM " + DBHelper.SAVED_MESSAGES_TABLE + "_" + uid;
            final Cursor saveCursor = database.rawQuery(saveSql, null);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cursorAdapter = new MessageCursorAdapter(MessageHistoryActivity.this, saveCursor, saveSql, chat_id, uid);
                    lvHistory.setAdapter(cursorAdapter);

                    // sets the currently selected item
                    lvHistory.setSelection(adapter.getCount());

                    // enables fast scroll indicator
                    if (cursorAdapter.getCount() > 500) {
                        lvHistory.setFastScrollEnabled(true);
                    }
                }
            });
        }

    }
}

