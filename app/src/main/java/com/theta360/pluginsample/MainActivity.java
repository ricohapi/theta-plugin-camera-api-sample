/**
 * Copyright 2018 Ricoh Company, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.theta360.pluginsample;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.theta360.pluginlibrary.activity.PluginActivity;
import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.exif.Box;
import com.theta360.pluginlibrary.receiver.KeyReceiver;
import com.theta360.pluginlibrary.values.LedColor;
import com.theta360.pluginlibrary.values.LedTarget;
import com.theta360.pluginlibrary.values.TextArea;
import com.theta360.pluginlibrary.values.ThetaModel;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends PluginActivity implements CameraFragment.CFCallback {
    private String TAG = "Sample";
    private boolean mIsVideo = false;
    private boolean mIsEnded = false;
    private File mRecordMp4File;
    private File mRecordWavFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //ActionBar actionBar = getSupportActionBar();
        //if (actionBar != null) {
        //    actionBar.hide();
        //}

        //do not enter to sleep mode.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setAutoClose(false);

        setKeyCallback(new KeyCallback() {
            @Override
            public void onKeyDown(int keyCode, KeyEvent event) {
                if (keyCode == KeyReceiver.KEYCODE_CAMERA) {
                    if (mIsVideo) {
                        if (!takeVideo()) {
                            // Cancel recording
                            notificationAudioWarning();
                        }
                    } else {
                        takePicture();
                    }
                }
            }

            @Override
            public void onKeyUp(int keyCode, KeyEvent event) {
                if (keyCode == KeyReceiver.KEYCODE_MEDIA_RECORD) {
                    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
                    if (fragment != null && fragment instanceof CameraFragment) {
                        if (((CameraFragment) fragment).isMediaRecorderNull()) {
                            if(!(((CameraFragment) fragment).isCapturing())) {
                                // not recording video or capturing still
                                mIsVideo = !mIsVideo;
                                updateLED();
                            }
                        }
                    }
                }
            }

            @Override
            public void onKeyLongPress(int keyCode, KeyEvent event) {
                if (keyCode == KeyReceiver.KEYCODE_MEDIA_RECORD) {
                    endProcess();
                }
            }
        });

        notificationWlanOff();
        notificationCameraClose();  //only for THETA V and THETA Z1
    }

    @Override
    protected void onResume() {
        Log.i(TAG,"onResume");
        setAutoClose(true);      //the flag which does finish plug-in by long-press MODE
        updateLED();
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i(TAG,"onPause");
        setAutoClose(false);     //the flag which does not finish plug-in in onPause
        endProcess();
        super.onPause();
    }

    @Override
    public void onShutter() {
        notificationAudioShutter();
    }

    @Override
    public void onPictureTaken(String[] fileUrls) {

        //THETA X
        if (ThetaModel.isXModel()) {
            String fileUrl = fileUrls[0];
            notificationDatabaseUpdate(fileUrl);
        }

        //THETA V and THETA Z1
        if (ThetaModel.isVCameraModel()) {
            notificationSensorStop();
            /**
             * The file path specified in "notificationDatabaseUpdate"
             * specifies the file path or directory path under the DCIM directory.
             * Replace file path because fileUrls has full path set
             */
            String storagePath = Environment.getExternalStorageDirectory().getPath();
            for(int i = 0; i < fileUrls.length; i++){
                fileUrls[i] = fileUrls[i].replace(storagePath, "");
            }
            notificationDatabaseUpdate(fileUrls);
        }
    }

    private void takePicture() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
        if (fragment != null && fragment instanceof CameraFragment) {
            if (!(((CameraFragment) fragment).isCapturing())) {
                notificationSensorStart();  //only for THETA V and THETA Z1
                ((CameraFragment) fragment).takePicture();
            }
        }
    }

    private boolean takeVideo() {
        boolean result = true;
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
        if (fragment != null && fragment instanceof CameraFragment) {

            //start video recording
            if (((CameraFragment) fragment).isMediaRecorderNull()) {
                if (!(((CameraFragment) fragment).isCapturing())) {
                    notificationAudioMovStart();
                    notificationLedBlink(LedTarget.LED7, LedColor.RED, 2000);  //only for THETA V
                }
                // Sample: Register callback to CameraFragment
                // to acquire the result of Box data writing
                ((CameraFragment) fragment).setBoxCallback(mBoxCallBack);
                notificationSensorStart();  //only for THETA V and THETA Z1
                result = ((CameraFragment) fragment).takeVideo();
            }

            //stop video recording
            else {
                File[] recordFiles = ((CameraFragment) fragment).getRecordFiles();
                mRecordMp4File = recordFiles[0];
                mRecordWavFile = recordFiles[1];
                result = ((CameraFragment) fragment).takeVideo();
                if (result) {
                    notificationAudioMovStop();
                }
                notificationLedHide(LedTarget.LED7);  //only for THETA V
            }
        }

        return result;
    }

    /**
     * CallBack allows you to configure the processing when metadata write succeeds and fails.
     */
    private Box.Callback mBoxCallBack = new Box.Callback() {
        @Override
        /**
         * fileUrls contains full path of mp4 and wav files
         */
        public void onCompleted(String[] fileUrls) {

            //THETA X
            if (ThetaModel.isXModel()) {
                String fileUrl = fileUrls[0];
                notificationDatabaseUpdate(fileUrl);
            }

            //THETA V and THETA Z1
            /**
             * Sample: If writing of Box data is successful,
             * registration of recorded file to database will be executed
             */
            if (ThetaModel.isVCameraModel()) {
                Log.d(TAG, "Success in writing metadata");
                // Delete Wav file if unnecessary
                mRecordWavFile.delete();
                notificationSensorStop();
                /**
                 * The file path specified in "notificationDatabaseUpdate"
                 * specifies the file path or directory path under the DCIM directory.
                 * Replace file path because fileUrls has full path set
                 */
                String storagePath = Environment.getExternalStorageDirectory().getPath();
                for(int i = 0; i < fileUrls.length; i++){
                    fileUrls[i] = fileUrls[i].replace(storagePath, "");
                }
                notificationDatabaseUpdate(fileUrls);
            }
        }

        @Override
        public void onError() {
            /**
             * Sample: If writing of Box data fails,
             * operation will be performed when an error occurs
             */
            if (ThetaModel.isVCameraModel()) {
                Log.d(TAG, "Failed to write metadata");
                // Delete file if unnecessary
                mRecordMp4File.delete();
                mRecordWavFile.delete();
                notificationSensorStop();
            }
            notificationErrorOccured();
        }
    };

    private void updateLED() {

        //THETA X
        if (ThetaModel.isXModel()) {
            //TODO update icon
        }

        //THETA Z1
        if (ThetaModel.isZ1Model()) {
            Map<TextArea, String> textMap = new HashMap<>();
            if (mIsVideo) {
                textMap.put(TextArea.BOTTOM, "video");
            } else {
                textMap.put(TextArea.BOTTOM, "image");
            }
            notificationOledTextShow(textMap);
        }

        //THETA V
        if (ThetaModel.isVModel()) {
            if (mIsVideo) {
                notificationLedHide(LedTarget.LED4);
                notificationLedShow(LedTarget.LED5);
            } else {
                notificationLedHide(LedTarget.LED5);
                notificationLedShow(LedTarget.LED4);
            }
        }
    }

    private void endProcess() {
        if (!mIsEnded) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
            if (fragment != null && fragment instanceof CameraFragment) {
                if (!((CameraFragment) fragment).isMediaRecorderNull()) {
                    takeVideo(); // stop recording
                }
                ((CameraFragment) fragment).close();
            }
            close();
            mIsEnded = true;
        }
    }
}
