package ru.euphoriadev.vk;


import android.os.Bundle;
import android.view.MenuItem;

import ru.euphoriadev.vk.util.ViewUtil;

/**
 * Created by Igor on 09.02.15.
 */
public class PrefActivity extends BaseThemedActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setTitle(R.string.prefs);

//        TextView tvTitleToolbar = (TextView) findViewById(R.id.toolbarTitle);
//        tvTitleToolbar.setText(R.string.prefs);

        ViewUtil.setTypeface(getToolbar());



        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(true);

        getFragmentManager().beginTransaction()
                .replace(R.id.container_prefs, new SettingsFragment()).commit();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
