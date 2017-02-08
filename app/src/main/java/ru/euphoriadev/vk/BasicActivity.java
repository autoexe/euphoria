package ru.euphoriadev.vk;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.internal.NavigationMenuView;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import ru.euphoriadev.vk.api.Api;
import ru.euphoriadev.vk.api.KException;
import ru.euphoriadev.vk.api.model.VKMessage;
import ru.euphoriadev.vk.api.model.VKUser;
import ru.euphoriadev.vk.async.ThreadExecutor;
import ru.euphoriadev.vk.common.AppLoader;
import ru.euphoriadev.vk.common.PrefManager;
import ru.euphoriadev.vk.common.ThemeManager;
import ru.euphoriadev.vk.helper.DBHelper;
import ru.euphoriadev.vk.interfaces.Refreshable;
import ru.euphoriadev.vk.napi.VKApi;
import ru.euphoriadev.vk.service.CrazyTypingService;
import ru.euphoriadev.vk.service.LongPollService;
import ru.euphoriadev.vk.util.Account;
import ru.euphoriadev.vk.util.AndroidUtils;
import ru.euphoriadev.vk.util.ArrayUtil;
import ru.euphoriadev.vk.util.RefreshManager;
import ru.euphoriadev.vk.util.VKUpdateController;
import ru.euphoriadev.vk.util.ViewUtil;


public class BasicActivity extends BaseThemedActivity implements
        NavigationView.OnNavigationItemSelectedListener, Refreshable {
    static final int OFFICIAL_GROUP_ID = 59383198;

    private AppLoader appLoader;
    private Fragment currentFragment;
    private NavigationView navigationView;
    private DrawerLayout drawer;
    private Toolbar toolbar;
    private Api api;
    private Account account;
    private long backPressedTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        appLoader = AppLoader.getLoader(getApplicationContext());
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        if (checkNotLogged()) {
            return;
        }
//        circularRevealAnimation();

        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.setBackgroundDrawable(null);
        toolbar.setTitleTextColor(ThemeManager.getPrimaryTextColorOnThemeColor(this));
        toolbar.setSubtitleTextColor(ThemeManager.getPrimaryTextColorOnThemeColor(this));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        setStatusBarColor();
        setTitle(R.string.messages);

        ViewUtil.setTypeface(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawerLayout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(BasicActivity.this,
                drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.navView);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.navMessages);
        applyTypefaces();

        NavigationMenuView navigationMenu = (NavigationMenuView) navigationView.getChildAt(0);
        if (navigationMenu != null) {
            navigationMenu.setVerticalScrollBarEnabled(false);
        }

        account = new Account(this);
        account.restore();

        api = Api.init(account);

        selectItem(R.id.navMessages);
        initDrawerHeader();

        if (AndroidUtils.hasConnection(this)) {
            trackStats();
            joinInGroup();
        }
        RefreshManager.registerForChangePreferences(this, SettingsFragment.KEY_BLUR_RADIUS);
        RefreshManager.registerForChangePreferences(this, SettingsFragment.KEY_MAKING_DRAWER_HEADER);
        RefreshManager.registerForChangePreferences(this, SettingsFragment.KEY_WALLPAPER_PATH);
        RefreshManager.registerForChangePreferences(this, SettingsFragment.KEY_GRAVITY_DRAWER_HEADER);

        startService(new Intent(this, LongPollService.class));
        loadWallpaper();
//        VKSqliteHelper.test();
    }


    private void testLongPoll() {
        VKUpdateController.getInstance().addListener(new VKUpdateController.MessageListener() {
            @Override
            public void onNewMessage(VKMessage message) {
                VKUser user = DBHelper.get(BasicActivity.this).getUserFromDB(message.uid);
                Toast.makeText(BasicActivity.this, "Новое сообщение от "
                        + user.first_name + ", chat id: "
                        + message.chat_id + " с текстом " + message.body, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onReadMessage(int message_id) {
                Toast.makeText(BasicActivity.this, "Сообщение с id " + message_id + " прочтено", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onDeleteMessage(int message_id) {
                Toast.makeText(BasicActivity.this, "Сообщение с id " + message_id + " удалено", Toast.LENGTH_LONG).show();
            }
        });

        VKUpdateController.getInstance().addListener(new VKUpdateController.UserListener() {
            @Override
            public void onOffline(int user_id) {
                VKUser user = DBHelper.get(BasicActivity.this).getUserFromDB(user_id);

                Toast.makeText(BasicActivity.this, "Пользователь " + user.toString() + " стал оффлайн", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onOnline(int user_id) {
                VKUser user = DBHelper.get(BasicActivity.this).getUserFromDB(user_id);

                Toast.makeText(BasicActivity.this, "Пользователь " + user.toString() + " стал онлайн", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onTyping(int user_id, int chat_id) {
                VKUser user = DBHelper.get(BasicActivity.this).getUserFromDB(user_id);

                Toast.makeText(BasicActivity.this, "Пользователь " + user.toString() + " набирает текст" +
                        (chat_id == 0 ? "" : "в чате " + chat_id), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void circularRevealAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final View rootLayout = findViewById(R.id.drawerLayout);
            rootLayout.setVisibility(View.INVISIBLE);

            rootLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    v.removeOnLayoutChangeListener(this);
                    AndroidUtils.circularReveal(rootLayout);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    private boolean checkNotLogged() {

        account = new Account(this);
        account.restore();
        if (account.access_token == null) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return true;
        }
        return false;
    }

    private void applyTypefaces() {
        Menu m = navigationView.getMenu();
        for (int i = 0; i < m.size(); i++) {
            MenuItem item = m.getItem(i);

            //for applying a font to subMenu...
            SubMenu subMenu = item.getSubMenu();
            if (subMenu != null && subMenu.size() > 0) {
                for (int j = 0; j < subMenu.size(); j++) {
                    MenuItem subMenuItem = subMenu.getItem(j);
                    applyTypeface(subMenuItem);
                }
            }

            //the method we have create in activity
            applyTypeface(item);
        }
    }

    private void applyTypeface(MenuItem item) {
        item.setTitle(ViewUtil.createTypefaceSpan(item.getTitle()));
    }

    /**
     * Swaps fragments in the main content view
     */
    private void selectItem(final int id) {
        // Update the main content by replacing fragments
        switch (id) {

            case R.id.navNewsVideos:
                currentFragment = new NewsVideosFragments();
                break;

            case R.id.navFriends:
                currentFragment = new FriendsTabsFragment();
                break;

            case R.id.navMessages:
                currentFragment = new DialogsFragment();
                break;

            case R.id.navAudios:
                currentFragment = new AudioListFragment();
                break;

            case R.id.navGroups:
                currentFragment = new GroupsFragment();
                break;

            case R.id.navVideos:
                currentFragment = new VideosFragment();
                break;

            case R.id.navGifts:
                currentFragment = new GiftsFragment();
                break;

            case R.id.navDocs:
                currentFragment = new DocsTabsFragment();
                break;

            case R.id.navNotes:
                currentFragment = new NotesFragment();
                break;

            case R.id.navPrefs:
                startActivity(new Intent(this, PrefActivity.class));
                break;


            case R.id.navExit:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(BasicActivity.this)
                                .setTitle(R.string.exit_for_account)
                                .setMessage(getResources().getString(R.string.exit_for_account_description))
                                .setNegativeButton("No", null)
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Account account = new Account(BasicActivity.this);
                                        account.clear();

//                                        DBHelper dbHelper = DBHelper.get(BasicActivity.this);
//                                        SQLiteDatabase database = dbHelper.getWritableDatabase();
//                                        dbHelper.dropTables(database);
//                                        dbHelper.close();
                                        finish();
                                    }
                                });

                        AlertDialog dialog = builder.create();
                        dialog.show();

                    }
                });
                break;

        }


        // Insert the fragment by replacing any existing fragment
        if (currentFragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, currentFragment).commit();

        } else {
            // Error
            Log.e(this.getClass().getName(), "Error. Fragment is not created");
        }

    }

    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            int statusBarHeight = AndroidUtils.getStatusBarHeight(this);
//
//            AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appBarLayout);
//            appBarLayout.setBackgroundDrawable(new ColorDrawable(ThemeUtils.getThemeAttrColor(this,R.attr.colorPrimary)));
//            appBarLayout.setPadding(0, statusBarHeight, 0, 0);

            toolbar.setPadding(0, statusBarHeight, 0, 0);
        }
    }

    private void trackStats() {
       VKApi.stats().trackVisitor().execute(null);
    }

    private void loadWallpaper() {
        String wallpaperPath = ThemeManager.getWallpaperPath(this);
        if (TextUtils.isEmpty(wallpaperPath)) {
            return;
        }
        ImageView ivWallpaper = (ImageView) findViewById(R.id.ivWallpaper);
        ivWallpaper.setVisibility(View.VISIBLE);

        boolean applyBlur = PrefManager.getBoolean(SettingsFragment.KEY_BLUR_WALLPAPER);
        RequestCreator creator = Picasso.with(this)
                .load(new File(wallpaperPath));
        if (applyBlur) {
            creator.transform(new AndroidUtils.PicassoBlurTransform(PrefManager.getInt(SettingsFragment.KEY_BLUR_RADIUS)));
        }
        creator.into(ivWallpaper);
    }

    private void joinInGroup() {
        int isJoinGroup = PrefManager.getInt(SettingsFragment.KEY_IS_JOIN_GROUP, 0);

        // Если уже в группе/ откахались вступить
        if (isJoinGroup == -1) {
            return;
        }
        // если еще нет, то увеличиваем счетчик
        if (isJoinGroup <= 5) {
            PrefManager.putInt(SettingsFragment.KEY_IS_JOIN_GROUP, ++isJoinGroup);
            return;
        }
        // Просим вступить в группу, после 5х запуска
        ThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                    final Boolean isMemberGroup = api.isGroupMember(OFFICIAL_GROUP_ID, api.getUserId());
                    // если пользовать не в группе - вызываем диалог

                    if (isMemberGroup) {
                        // если мы уже в группе
                        PrefManager.putInt(SettingsFragment.KEY_IS_JOIN_GROUP, -1);
                        Log.w("BasicActivity", "IsMemberOfGroup");
                    } else
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showDialogJoinGroup();
                            }
                        });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showDialogJoinGroup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(BasicActivity.this);
        builder.setTitle(getResources().getString(R.string.join_in_group_ask))
                .setMessage(getString(R.string.join_in_group_ask_description))
                .setPositiveButton("Join", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        joinInGroup(OFFICIAL_GROUP_ID);
                        Toast.makeText(BasicActivity.this, R.string.thank_you, Toast.LENGTH_LONG).show();
                        PrefManager.putInt(SettingsFragment.KEY_IS_JOIN_GROUP, -1);

                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PrefManager.putInt(SettingsFragment.KEY_IS_JOIN_GROUP, -1);
                    }
                });
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void joinInGroup(final int groupId) {
        ThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    api.joinGroup(groupId, null, null);
                } catch (IOException | JSONException | KException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    private void getUserStatus(final Account account, final TextView textView) {
        if (TextUtils.isEmpty(account.status)) {
            ThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    final VKUser user = Api.get().getProfile((int) account.user_id);
                    if (user != null && !TextUtils.isEmpty(user.status)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(user.status);
                                account.status = user.status;
                                account.save();
                            }
                        });
                    }
                }
            });
            return;
        }
        textView.setText(account.status);

    }

    private void loadHintsFriends() {
        if (!AndroidUtils.hasConnection(this)) {
            return;
        }

        final Menu menuFriends = navigationView.getMenu().addSubMenu("Friends");
        ThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ArrayList<VKUser> friends = Api.get().getFriends(Api.get().getUserId(), "hints", 5, null, null, null);
                    if (ArrayUtil.isEmpty(friends)) {
                        return;
                    }

                    for (int i = 0; i < friends.size(); i++) {
                        final VKUser user = friends.get(i);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Picasso.with(BasicActivity.this)
                                        .load(user.photo_50)
                                        .config(Bitmap.Config.RGB_565)
                                        .into(new Target() {
                                            @Override
                                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                                Log.w("onBitmapLoaded", user.toString());
                                                menuFriends.add(user.toString()).setIcon(new BitmapDrawable(getResources(), bitmap));
                                            }

                                            @Override
                                            public void onBitmapFailed(Drawable errorDrawable) {

                                            }

                                            @Override
                                            public void onPrepareLoad(Drawable placeHolderDrawable) {

                                            }
                                        });
                            }
                        });

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initDrawerHeader() {
        if (account == null) {
            account = new Account(this).restore();
        }

        View headerView = navigationView.getHeaderView(0);
        final ImageView drawerImageView = (ImageView) headerView.findViewById(R.id.drawerIvPhoto);
        final TextView drawerTitle = (TextView) headerView.findViewById(R.id.drawerTitle);
        final TextView drawerStatus = (TextView) headerView.findViewById(R.id.drawerStatus);
        final ImageView drawerBackground = (ImageView) headerView.findViewById(R.id.drawerBackgroundHeader);
        ViewUtil.setTypeface(drawerStatus);
        ViewUtil.setTypeface(drawerTitle);

        final Drawable headerDrawable = ThemeManager.getDrawerHeader(this);
        if (headerDrawable != null) {
            drawerBackground.setImageDrawable(headerDrawable);
        } else {
            drawerBackground.setImageDrawable(null);
            try {
                Picasso.with(this)
                        .load(account.photo)
                        .placeholder(R.drawable.camera_b)
                        .transform(new AndroidUtils.PicassoBlurTransform(PrefManager.getInt(SettingsFragment.KEY_BLUR_RADIUS, 20)))
                        .into(drawerBackground);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        drawerTitle.setText(account.fullName);
        getUserStatus(account, drawerStatus);
        if (PrefManager.getBoolean(SettingsFragment.KEY_GRAVITY_DRAWER_HEADER)) {
            LinearLayout drawerContainer = (LinearLayout) headerView.findViewById(R.id.drawerHeaderContainer);
            drawerContainer.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        }
        try {
            Picasso.with(BasicActivity.this)
                    .load(account.photo)
                    .into(drawerImageView);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ThemeManager.isBlackThemeColor()) {
//            navigationView.setItemIconTintList(AndroidUtils.createColorStateList(Color.WHITE, ThemeManager.getSecondaryTextColor()));
//            navigationView.setItemTextColor(AndroidUtils.createColorStateList(Color.WHITE, ThemeManager.getSecondaryTextColor()));
            navigationView.setItemIconTintList(ColorStateList.valueOf(Color.WHITE));
            navigationView.setItemTextColor(ColorStateList.valueOf(Color.WHITE));

        }

        // does not work as it should
//        loadHintsFriends();
    }


    @Override
    public void onBackPressed() {
        if (currentFragment instanceof GroupsFragment) {
            ((GroupsFragment) currentFragment).onBackPressed();
        }

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
            VKApi.close();
            if (AndroidUtils.serviceIsRunning(CrazyTypingService.class)) {
                stopService(new Intent(this, CrazyTypingService.class));
            }
            if (!PrefManager.getBoolean(SettingsFragment.KEY_ENABLE_NOTIFY, true) || !PrefManager.getBoolean(SettingsFragment.KEY_IS_LIVE_ONLINE_SERVICE)) {
                stopService(new Intent(this, LongPollService.class));
                appLoader.getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        System.exit(0);
                    }
                }, 800);
            }
        } else {
            Toast.makeText(this, R.string.exit_message,
                    Toast.LENGTH_SHORT).show();
            backPressedTime = System.currentTimeMillis();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VKUpdateController.getInstance().cleanup();
        RefreshManager.unregisterForChangePreferences(this);

        DBHelper helper = DBHelper.get(this);
        helper.close();
        helper = null;

        System.gc();
        RefreshManager.unregisterForChangePreferences(this);
        if (!PrefManager.getBoolean(SettingsFragment.KEY_ENABLE_NOTIFY, true))
            stopService(new Intent(this, LongPollService.class));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (!drawer.isDrawerOpen(GravityCompat.START)) drawer.openDrawer(GravityCompat.START);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public boolean onNavigationItemSelected(final MenuItem item) {
        if (item.getItemId() != R.id.navPrefs) setTitle(item.getTitle());

        drawer.closeDrawers();
        if (currentFragment != null && currentFragment instanceof AbstractFragment) {
            ((AbstractFragment) currentFragment).setRefreshing(false);
        }
        ThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                selectItem(item.getItemId());

            }
        });
        return true;
    }

    @Override
    public void onRefresh(String prefKey) {
        Log.w("BasicActivity", "onRefresh: " + prefKey);
        // Change blur radius of NavigationView....

        ThemeManager.updateThemeValues();
        if (prefKey.equals(SettingsFragment.KEY_BLUR_RADIUS)
                || prefKey.equals(SettingsFragment.KEY_MAKING_DRAWER_HEADER)
                || prefKey.equals(SettingsFragment.KEY_GRAVITY_DRAWER_HEADER)) {
            initDrawerHeader();
        }

        if (prefKey.equals(SettingsFragment.KEY_WALLPAPER_PATH)) {
            loadWallpaper();
        }
    }
}
