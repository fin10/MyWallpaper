package com.fin10.android.mywallpaper;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.fin10.android.mywallpaper.model.WallpaperChanger;
import com.fin10.android.mywallpaper.settings.PreferenceModel;
import com.fin10.android.mywallpaper.settings.SettingsActivity;
import com.fin10.android.mywallpaper.tutorial.TutorialActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MainActivity extends AppCompatActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainActivity.class);

    private BroadcastReceiver mReceiver;
    private Snackbar mSnackBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PreferenceModel.isTutorialEnabled(this)) {
            startActivity(new Intent(this, TutorialActivity.class));
            finish();
            return;
        }

        mReceiver = new WallpaperChanger.Receiver();
        registerReceiver(mReceiver, WallpaperChanger.Receiver.getIntentFilter());

        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSnackBar = Snackbar.make(findViewById(R.id.coordinator_layout), R.string.wallpaper_is_changed, Snackbar.LENGTH_SHORT);
        mSnackBar.setActionTextColor(ActivityCompat.getColor(this, R.color.primary));
        mSnackBar.setAction(R.string.close, new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mSnackBar.dismiss();
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_settings: {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (mReceiver != null) unregisterReceiver(mReceiver);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWallpaperChanged(@NonNull WallpaperChanger.ChangeWallpaperEvent event) {
        LOGGER.debug("id:{}", event.id);
        if (!mSnackBar.isShown()) {
            mSnackBar.show();
        }
    }
}
