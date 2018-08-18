package com.theta360.pluginsample;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.KeyEvent;
import android.view.WindowManager;
import com.theta360.pluginlibrary.activity.PluginActivity;
import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.receiver.KeyReceiver;

public class MainActivity extends PluginActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationCameraClose();
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // ボタン操作イベントを取得した際のコールバックをセットします。
        setKeyCallback(new KeyCallback() {
            @Override
            public void onKeyDown(int keyCode, KeyEvent event) {
                if (keyCode == KeyReceiver.KEYCODE_CAMERA) {
                    takePicture();
                }
            }

            @Override
            public void onKeyUp(int keyCode, KeyEvent event) {
            }

            @Override
            public void onKeyLongPress(int keyCode, KeyEvent event) {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
                if (fragment != null && fragment instanceof MainFragment) {
                    ((MainFragment) fragment).close();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void takePicture() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
        if (fragment != null && fragment instanceof MainFragment) {
            notificationAudioShutter();
            ((MainFragment) fragment).takePicture();
        }
    }
}
