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

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.ImageView;

import com.theta360.pluginlibrary.activity.PluginActivity;
import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.exif.Box;
import com.theta360.pluginlibrary.receiver.KeyReceiver;
import com.theta360.pluginlibrary.values.LedColor;
import com.theta360.pluginlibrary.values.LedTarget;
import com.theta360.pluginlibrary.values.TextArea;
import com.theta360.pluginlibrary.values.ThetaModel;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends PluginActivity implements CameraFragment.CFCallback {
    private String TAG = "Sample";
    private boolean mIsVideo = false;
    private boolean mIsEnded = false;
    private File mRecordMp4File;
    private File mRecordWavFile;
    private CameraFragment mCamera;

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
                    CameraFragment camera = (CameraFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
                    // not during recording video or not during capturing still
                    if (camera.isMediaRecorderNull() && !camera.isCapturing()) {
                        mIsVideo = !mIsVideo;
                        updateUI();
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

        //CameraFragment
        mCamera = (CameraFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);

        //show preview in TextureView
        TextureView texture_view = (TextureView) findViewById(R.id.texture_view);
        texture_view.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                try {
                    mCamera.open(surface);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                //do nothing
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                //do nothing
            }
        });
    }

    @Override
    protected void onResume() {
        Log.i(TAG,"onResume");
        mIsEnded = false;
        setAutoClose(true);      //the flag which does finish plug-in by long-press MODE
        updateUI();
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
        Log.i(TAG,"onShutter");
        notificationAudioShutter();
    }

    @Override
    public void onPictureTaken(String[] fileUrls) {

        //THETA X
        if (ThetaModel.isXModel()) {
            String fileUrl = fileUrls[0];
            notificationDatabaseUpdate(fileUrl);
            updateUI();
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
            updateUI();
        }
    }

    private void takePicture() {
        if (!mCamera.isCapturing()) {
            notificationSensorStart();  //only for THETA V and THETA Z1
            mCamera.takePicture();
            updateUI();
        }
    }

    private boolean takeVideo() {
        boolean result = true;

        //start video recording
        if (mCamera.isMediaRecorderNull()) {
            // Sample: Register callback to CameraFragment
            // to acquire the result of Box data writing
            mCamera.setBoxCallback(mBoxCallBack);
            notificationSensorStart();  //only for THETA V and THETA Z1
            if (result = mCamera.takeVideo()) {
                notificationAudioMovStart();
            }
            updateUI();
        }

        //stop video recording
        else {
            File[] recordFiles = mCamera.getRecordFiles();
            mRecordMp4File = recordFiles[0];
            mRecordWavFile = recordFiles[1];
            if (result = mCamera.takeVideo()) {
                notificationAudioMovStop();
            }
            updateUI();
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

    private void updateUI() {

        //camera fragment
        CameraFragment camera = (CameraFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);

        //THETA X
        if (ThetaModel.isXModel()) {
            ImageView mode_icon = (ImageView) findViewById(R.id.mode_icon);
            boolean isCapturing = camera.isCapturing();
            if (mIsVideo) {
                mode_icon.setImageResource(isCapturing?
                        R.drawable.u_1_1_25_btn_movie_down:
                        R.drawable.u_1_1_25_btn_movie);
            }
            else {
                mode_icon.setImageResource(isCapturing?
                        R.drawable.u_1_1_24_btn_stillcamera_down:
                        R.drawable.u_1_1_24_btn_stillcamera);
            }
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
            //video mode
            if (mIsVideo) {
                notificationLedHide(LedTarget.LED4);
                notificationLedShow(LedTarget.LED5);
                //start video recording
                if (camera.isCapturing()) {
                    notificationLedBlink(LedTarget.LED7, LedColor.RED, 2000);
                }
                //stop video recording
                else {
                    notificationLedHide(LedTarget.LED7);
                }
            }
            //still mode
            else {
                notificationLedHide(LedTarget.LED5);
                notificationLedShow(LedTarget.LED4);
            }
        }
    }

    private void endProcess() {
        if (!mIsEnded) {
            Log.d(TAG, "endProcess");
            if (!mCamera.isMediaRecorderNull()) {
                takeVideo(); // stop recording
            }
            mCamera.close();
            close();
            mIsEnded = true;
        }
    }
}
