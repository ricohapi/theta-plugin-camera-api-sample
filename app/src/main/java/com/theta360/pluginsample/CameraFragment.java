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

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.theta360.pluginlibrary.activity.ThetaInfo;
import com.theta360.pluginlibrary.exif.Box;
import com.theta360.pluginlibrary.exif.CameraAttitude;
import com.theta360.pluginlibrary.exif.CameraSettings;
import com.theta360.pluginlibrary.exif.DngExif;
import com.theta360.pluginlibrary.exif.Exif;
import com.theta360.pluginlibrary.exif.GpsInfo;
import com.theta360.pluginlibrary.exif.SensorValues;
import com.theta360.pluginlibrary.exif.Xmp;
import com.theta360.pluginlibrary.exif.values.SphereType;
import com.theta360.pluginlibrary.factory.Camera;
import com.theta360.pluginlibrary.factory.FactoryBase;
import com.theta360.pluginlibrary.factory.MediaRecorder;
import com.theta360.pluginlibrary.values.ThetaModel;

import org.apache.sanselan.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.security.KeyStore.getApplicationContext;

/**
 * CameraFragment
 */
public class CameraFragment extends Fragment {

    public CameraFragment() {
    }

    private FactoryBase factory;
    private Camera mCamera;
    private int mCameraId;
    private Camera.Parameters mParameters;
    private CFCallback mCallback;
    private Box.Callback mBoxCallback;
    private AudioManager mAudioManager;//for video
    private MediaRecorder mMediaRecorder;//for video
    //private boolean mIsSurface = false;
    private boolean mIsCapturing = false;
    private boolean mIsDuringExposure = false;
    private File mInstanceRecordMP4;
    private File mInstanceRecordWAV;
    private String mMp4filePath;
    private String mWavfilePath;
    private CameraAttitude mCameraAttitude = null;

    private String TAG = "CameraFragment";

    private static final String DCIM =
            (ThetaModel.isVCameraModel()) ? Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM).getPath() : "/DCIM/";
    private String FOLDER = "/DCIM/100_TEST/";
    private String PATH_INTERNAL = "/storage/emulated/0";
    private String PATH = PATH_INTERNAL;
    private String mPath = "";
    private String mFilepath = "";

    public boolean isMediaRecorderNull() {
        return mMediaRecorder == null;
    }

    public boolean isCapturing() {
        return mIsCapturing;
    }

    /**
     * Sample: Returns the MP4 file and WAV file that it holds
     *
     * @return recordFiles [0]:MP4 file [1]:WAV file
     */
    public File[] getRecordFiles() {
        File[] recordFiles = {mInstanceRecordMP4, mInstanceRecordWAV};
        return recordFiles;
    }

    public interface CFCallback {
        void onShutter();
        void onPictureTaken(String[] fileUrls);
    }

    /**
     * CallBack allows you to configure the processing
     * when metadata write succeeds and fails.
     */
    public void setBoxCallback(@NonNull Box.Callback callback) {
        mBoxCallback = callback;
    }

    private Camera.ErrorCallback mErrorCallback = new Camera.ErrorCallback() {
        @Override
        public void onError(int var1, theta360.hardware.Camera var2) {
        }

        @Override
        public void onError(int var1, android.hardware.Camera var2) {
        }
    };

    private MediaRecorder.OnInfoListener onInfoListener = new MediaRecorder.OnInfoListener() {
        @Override
        public void onInfo(android.media.MediaRecorder mediaRecorder, int what, int extra) {
        }

        @Override
        public void onInfo(theta360.media.MediaRecorder mediaRecorder, int what, int extra) {
        }
    };

    private MediaRecorder.OnErrorListener onErrorListener = new MediaRecorder.OnErrorListener() {
        @Override
        public void onError(android.media.MediaRecorder mediaRecorder, int what, int extra) {
        }

        @Override
        public void onError(theta360.media.MediaRecorder mediaRecorder, int what, int extra) {
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        factory = new FactoryBase();

        //THETA X
        if (ThetaModel.isXModel()) {
            mCamera = factory.abstractCamera(FactoryBase.CameraModel.XCamera);
        }

        //THETA V and THETA Z1
        if (ThetaModel.isVCameraModel()) {
            mCamera = factory.abstractCamera(FactoryBase.CameraModel.VCamera);
        }

        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated");
        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);//for video
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof CFCallback) {
            mCallback = (CFCallback) context;
        }

        /*
         * Attitude sensor start
         */
        mCameraAttitude = new CameraAttitude(context);
        mCameraAttitude.register();
    }

    @Override
    public void onStart() {
        super.onStart();
        //startWatching(); // for debug
    }

    @Override
    public void onStop() {
        //stopWatching(); // for debug
        super.onStop();
    }

    @Override
    public void onDetach() {
        mCallback = null;

        /*
         * Attitude sensor stop
         */
        mCameraAttitude.unregister();

        super.onDetach();
    }

    public void open(SurfaceTexture surface) throws IOException {

        //THETA X
        if (ThetaModel.isXModel()) {
            mCameraId = theta360.hardware.Camera.CameraInfo.CAMERA_FACING_DOUBLE;
            mCamera.open(getContext(), mCameraId);
            mCamera.setPreviewTexture(surface);
            mCamera.setErrorCallback(mErrorCallback);
            mParameters = mCamera.getParameters();

            /**
             * Initialize CameraSettings for Metadata
             */
            CameraSettings.initialize();

            mParameters.set("RIC_SHOOTING_MODE", "RicPreview1024");
            mParameters.setPreviewSize(1024, 512);
            mCamera.setParameters();
            mCamera.startPreview();
        }

        //THETA V and Z1
        if (ThetaModel.isVCameraModel()) {
            mCameraId = android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
            mCamera.open(mCameraId);
            mCamera.setPreviewTexture(surface);
            mCamera.setErrorCallback(mErrorCallback);
            mParameters = mCamera.getParameters();

            /**
             * Initialize CameraSettings for Metadata
             */
            CameraSettings.initialize();

            mParameters.set("RIC_SHOOTING_MODE", "RicMonitoring");
            mParameters.setPreviewSize(1920, 960);
            mCamera.setParameters();
            mCamera.startPreview();
        }
    }

    public void close() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.setErrorCallback(null);
            mCamera.release();
            mCamera = null;
            mIsCapturing = false;
            mIsDuringExposure = false;
        }
        releaseMediaRecorder();
    }

    private void releaseMediaRecorder () {
        if (mMediaRecorder != null) {
            //mMediaRecorder.reset();
            mMediaRecorder.setOnInfoListener(null);
            mMediaRecorder.setOnErrorListener(null);
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock();
            mIsCapturing = false;
        }
    }

    private Camera.ShutterCallback onShutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {

            //THETA X
            if (!ThetaModel.isVCameraModel()) {
                if (mIsCapturing) {
                    mIsCapturing = false;
                    mCallback.onShutter();
                } else {
                    mParameters = mCamera.getParameters();
                    CameraSettings.setCameraParameters(mParameters);
                }
            }

            //THETA V and THETA Z1
            if (ThetaModel.isVCameraModel()) {
                if (mIsCapturing && !mIsDuringExposure) {
                    mIsDuringExposure = true;
                    mIsCapturing = false;

                    /*
                     * Hold the current value of the attitude sensor
                     * - It will be used later as setting value for Metadata.
                     */
                    mCameraAttitude.snapshot();

                    if (mCallback != null) {
                        mCallback.onShutter();
                    }
                } else if (!mIsCapturing && mIsDuringExposure) {
                    mIsDuringExposure = false;

                    /*
                     * Acquire the camera parameters for metadata at the completion of exposure
                     */
                    mParameters = mCamera.getParameters();
                    CameraSettings.setCameraParameters(mParameters);
                } else {
                    mIsCapturing = false;
                    mIsDuringExposure = false;
                }
            }
        }

        @Override
        public void onLongShutter() {
            //TODO
        }

        @Override
        public void onShutterend() {
            //TODO
        }
    };

    //
    // folder path and file path related functions (THETA X)
    //
    Boolean setFolderPath() {
        mPath = PATH;
        //check whether SD card is inserted
        File dir = new File("/storage/");
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].toString().endsWith("emulated") || files[i].toString().endsWith("self")) {
                    //ignored
                } else {
                    mPath = files[i].toString();
                    break;
                }
            }
        }
        //directory check : DCIM
        File dcim = new File(mPath + DCIM);
        if (!dcim.exists()) {
            dcim.mkdir();
            mPath = PATH_INTERNAL;
            return false;
        }
        //directory check : FOLDER
        File folder = new File(mPath + FOLDER);
        if (!folder.exists()) {
            folder.mkdir();
            mPath = PATH_INTERNAL;
            return false;
        }
        mPath += FOLDER;
        return true;
    }

    private File getOutputMediaFile() {
        if (!setFolderPath()) {
            return null;
        }
        mFilepath = mPath + "PC" + (new SimpleDateFormat("HHmmss")).format(new Date()) + ".JPG";
        Log.i(TAG, "JPEG file path = " + mFilepath);
        return (new File(mFilepath));
    }

    private String getDateTime() {
        Date date = new Date(System.currentTimeMillis());
        String format = "yyyyMMddHHmmss";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        String text = sdf.format(date);
        return text;
    }

    private Camera.PictureCallback onJpegPictureCallback = new Camera.PictureCallback() {

        //THETA X
        @Override
        public void onPictureTaken(byte[] bytes, theta360.hardware.Camera camera) {

            //filename
            File pictureFile = getOutputMediaFile();
            if (pictureFile == null) {
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(bytes);
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            List<String> fileUrls = new ArrayList<String>();
            fileUrls.add(mFilepath);
            mCallback.onPictureTaken(fileUrls.toArray(new String[fileUrls.size()]));

            mCamera.startPreview();
            mIsCapturing = false;
        }

        //THETA V and THETA Z1
        @Override
        public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
            mParameters.set("RIC_PROC_STITCHING", "RicStaticStitching");
            mCamera.setParameters();
            mCamera.stopPreview();

            /*
             * Create Exif object with image data holding incomplete Metadeta
             */
            Exif exif = new Exif(data, true);

            /*
             * Set Sphere type
             */
            CameraSettings.setSphereType(SphereType.EQUIRECTANGULAR);

            /*
             * Set Date, Time, and TimeZone to CameraSettings
             * - In fact, please use `camera.getOptions` of `RICOH THETA API v2.1` to obtain `dateTimeZone`
             *   from 'THETA shooting application' beforehand.
             *   The operating system's time zone is always UTC (+00:00).
             *
             *   See. https://developers.theta360.com/ja/docs/v2.1/api_reference/
             */
            long sysTime = System.currentTimeMillis();
            int timeZoneOffset = 32400000;  // TimeZone offset JST (+09:00)
            CameraSettings.setDateTime(sysTime);
            CameraSettings.setTimeZone(timeZoneOffset);

            /*
             * Set attitude sensor value
             * - Get the sensor value already held and sets it to the camera settings.
             */
            SensorValues sensorValues = new SensorValues();
            sensorValues.setAttitudeRadian(mCameraAttitude.getAttitudeRadianSnapshot());
            sensorValues.setCompassAccuracy(mCameraAttitude.getAccuracySnapshot());
            CameraSettings.setSensorValues(sensorValues);

            /*
             * Confirm setting value (for debug)
             */
            Log.d("CameraSettings", "Confirm CameraSettings");
            Log.d("CameraSettings", "  ExposureProgram: " + CameraSettings.getExposureProgram());
            Log.d("CameraSettings", "  Aperture: " + CameraSettings.getAperture());
            Log.d("CameraSettings", "  ShutterSpeed: " + CameraSettings.getShutterSpeed());
            Log.d("CameraSettings", "  Iso: " + CameraSettings.getIso());
            Log.d("CameraSettings", "  ExposureCompensation: " + CameraSettings.getExposureCompensation());
            Log.d("CameraSettings", "  WhiteBalance: " + CameraSettings.getWhiteBalance());
            Log.d("CameraSettings", "  ColorTemperature: " + CameraSettings.getColorTemperature());
            Log.d("CameraSettings", "  Filter: " + CameraSettings.getFilter());
            Log.d("CameraSettings", "  SphereType: " + CameraSettings.getSphereType());
            Log.d("CameraSettings", "  DateTimzeZone: " + CameraSettings.getDateTimeZone());
            Log.d("CameraSettings", "  DateTimze: " + CameraSettings.getDateTime());
            if (!CameraSettings.isEmptyTimeZone()) {
                Log.d("CameraSettings", "  TimeZone: " + CameraSettings.getTimeZone());
            } else {
                Log.d("CameraSettings", "  TimeZone: (Not set yet)");
            }

            /*
             * Confirm sensor value (for debug)
             */
            Log.d("SensorValues", "Confirm SensorValues:");
            Log.d("SensorValues", "  AttitudeRadian: "
                    + sensorValues.getAttitudeRadian()[0] + ", "
                    + sensorValues.getAttitudeRadian()[1] + ", "
                    + sensorValues.getAttitudeRadian()[2]);
            Log.d("SensorValues", "  CompassAccuracy: " + sensorValues.getCompassAccuracy());

            /*
             * Set correct value for Receptor-IFD in MakerNote.
             */
            exif.setExifSphere();

            /*
             * Set GPS information
             * - In fact, please use `camera.getOptions` of `RICOH THETA API v2.1` to obtain `gpsInfo`
             *   from 'THETA shooting application' beforehand.
             *
             *   See. https://developers.theta360.com/ja/docs/v2.1/api_reference/
             */
            GpsInfo gpsInfo = new GpsInfo();
            // Sample: Asia/Tokyo
            gpsInfo.setLat(35.6580); // North latitude 35 degree 39 minutes 29 seconds
            gpsInfo.setLng(139.7377);  // East longitude 139 degree 44 minutes 28 seconds
            gpsInfo.setAltitude(40.0); // Altitude 40.0m
            gpsInfo.setDatum(GpsInfo.DATUM);
            gpsInfo.setDateTimeZone(CameraSettings.getDateTimeZone());
            CameraSettings.setGpsInfo(gpsInfo);

            /*
             * Confirm GPS Infomation (for debug)
             */
            Log.d("GpsInfo", "Confirm GpsInfo:");
            Log.d("GpsInfo", "  Latitude: " + gpsInfo.getLat());
            Log.d("GpsInfo", "  Longitude: " + gpsInfo.getLng());
            Log.d("GpsInfo", "  Altitude: " + gpsInfo.getAltitude());
            Log.d("GpsInfo", "  Datum: " + gpsInfo.getDatum());
            Log.d("GpsInfo", "  DateTimeZone: " + gpsInfo.getDateTimeZone());

            /*
             * Set correct value for GPS-IFD
             */
            exif.setExifGPS();

            /*
             * Set correct value for MakerNote.
             */
            exif.setExifMaker();

            /*
             * Get Exif data
             * - Acquires the image data in which the correct Metadata has been set.
             */
            byte[] exifData = exif.getExif();

            String fileUrl = String.format("%s/plugin_%s.JPG", DCIM, getDateTime());
            Log.i(TAG, "JPEG file path = " + fileUrl);
            List<String> fileUrls = new ArrayList<String>();
            fileUrls.add(fileUrl);
            try (FileOutputStream fileOutputStream = new FileOutputStream(fileUrl)) {
                // Get Image size
                int width = mParameters.getPictureSizeWidth();
                int height = mParameters.getPictureSizeHeight();

                int pitch = 0;
                int roll = 0;
                if (!CameraSettings.isZenith()) {
                    pitch = exif.calcPitch();
                    roll = exif.calcRoll();
                }

                /*
                 * Add XMP and write image data to a file.
                 */
                Xmp.setXmp(exifData, fileOutputStream, width, height, pitch, roll);
            } catch (IOException e) {
                e.printStackTrace();
            }

            /**
             * If DNG output is enabled, a DNG file is output by the following process.
             */
            if (!CameraSettings.isDngOutput()) {
                /**
                 * The following DNG file is output by enabling DNG output and executing still image shooting.
                 */
                File dngTempFile = new File(
                        Environment.getExternalStorageDirectory().getPath() + "/temp.dng");
                String dngFileUrl = fileUrl.replace(".JPG", ".DNG");
                fileUrls.add(dngFileUrl);
                /**
                 * Copy temp.dng and give the same metadata as the jpeg file.
                 */

                try (FileInputStream dngInputStream = new FileInputStream(dngTempFile);
                     FileOutputStream dngOutputStream = new FileOutputStream(dngFileUrl)) {
                    byte[] dngData = IOUtils.getInputStreamBytes(dngInputStream);
                    DngExif dngExif = new DngExif(dngData, false);
                    dngExif.setExifGPS();
                    dngExif.setExifSphere();
                    dngExif.setExifMaker();
                    dngExif.replaceJpeg(exifData);

                    dngOutputStream.write(dngExif.getExif());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            mCallback.onPictureTaken(fileUrls.toArray(new String[fileUrls.size()]));

            mCamera.startPreview();
            mIsCapturing = false;

            // After shooting, set the shooting mode to monitoring mode.
            mParameters.set("RIC_SHOOTING_MODE", "RicMonitoring");
            mCamera.setParameters();
        }
    };

    public void takePicture() {
        if (!mIsCapturing) {
            mIsCapturing = true;

            /**
             * Set Manufacturer name, Serial number, Version name, Model name
             */
            CameraSettings.setManufacturer("RICOH");
            CameraSettings.setThetaSerialNumber(ThetaInfo.getThetaSerialNumber());
            CameraSettings.setThetaFirmwareVersion(ThetaInfo.getThetaFirmwareVersion(getContext()));
            CameraSettings.setThetaModel(ThetaModel.getValue(ThetaInfo.getThetaModelName()));

            //THETA X
            if (ThetaModel.isXModel()) {
                mParameters.setPictureSize(11008, 5504);
            }
            //THETA Z1
            if (ThetaModel.isZ1Model()) {
                mParameters.setPictureSize(6720, 3360);
            }
            //THETA V
            if (ThetaModel.isVModel()) {
                mParameters.setPictureSize(5376, 2688);
            }
            mParameters.set("RIC_SHOOTING_MODE", "RicStillCaptureStd");
            mParameters.set("RIC_EXPOSURE_MODE", "RicAutoExposureP");
            mParameters.set("RIC_MANUAL_EXPOSURE_TIME_REAR", -1);
            mParameters.set("RIC_MANUAL_EXPOSURE_TIME_FRONT", -1);
            mParameters.set("RIC_MANUAL_EXPOSURE_ISO_REAR", -1);
            mParameters.set("RIC_MANUAL_EXPOSURE_ISO_FRONT", -1);
            mParameters.set("exposure-compensation-step", "0.333333333");   // 1/3 step
            mParameters.setExposureCompensation(0);
            if (ThetaModel.isZ1Model()) {
                mParameters.set("RIC_MANUAL_EXPOSURE_AV_REAR", 0);
                mParameters.set("RIC_MANUAL_EXPOSURE_AV_FRONT", 0);
            }
            mParameters.set("RIC_WB_MODE", "RicWbAuto");
            mParameters.set("RIC_WB_TEMPERATURE", 2500);
            mParameters.set("RIC_PROC_STITCHING", "RicDynamicStitchingAuto");
            mParameters.set("RIC_PROC_ZENITH_CORRECTION", "RicZenithCorrectionOnAuto");
            if (ThetaModel.isZ1Model()) {
                mParameters.set("RIC_DNG_OUTPUT_ENABLED", 1); //DNG output is enabled only on Z1
            }
            mParameters.set("recording-hint", "false");
            mParameters.setJpegThumbnailSize(320, 160);
            mCamera.setParameters();

            mCamera.takePicture(onShutterCallback, null, onJpegPictureCallback);
            Log.d(TAG, "mCamera.takePicture()");
        }
    }

    public boolean takeVideo() {
        boolean result = true;

        //THETA X
        if (ThetaModel.isXModel()) {
            //start video recording
            if (mMediaRecorder == null) {
                mMediaRecorder = factory.abstractMediaRecorder(FactoryBase.CameraModel.XCamera);
                mMediaRecorder.newMediaRecorder();

                //Context
                mMediaRecorder.setMediaRecorderContext(getApplicationContext());
                //Audio Setting
                mMediaRecorder.setAudioSource(theta360.media.MediaRecorder.AudioSource.DEFAULT);
                //mMediaRecorder.setMicDeviceId(mMediaRecorder.getExternalDeviceId());
                //setExternalMicDevVendorName(NULL)
                mMediaRecorder.setMicGain(1);          //microphone gain
                mMediaRecorder.setMicSamplingFormat(theta360.media.MediaRecorder.MEDIA_FORMAT_PCM_I16);
                mMediaRecorder.setAudioChannels(1);
                mMediaRecorder.setAudioSamplingRate(48_000);
                mMediaRecorder.setAudioEncodingBitRate(128_000);

                //Video Setting
                mMediaRecorder.setVideoSource(theta360.media.MediaRecorder.VideoSource.CAMERA);
                mMediaRecorder.setOutputFormat(theta360.media.MediaRecorder.OutputFormat.MPEG_4);
                //mMediaRecorder.setVideoFrameRate(30);
                //mMediaRecorder.setVideoSize(3840, 1920);
                //mMediaRecorder.setVideoEncodingBitRate(64_000_000);
                mMediaRecorder.setVideoEncoder(theta360.media.MediaRecorder.VideoEncoder.H264);
                mMediaRecorder.setVideoEncodingProfileLevel(0x7F000001,0x8000);   //BaseLine@L5.1 0x7F000001

                //Camcorder Profile
                mMediaRecorder.setProfile(theta360.media.CamcorderProfile.get(theta360.media.CamcorderProfile.QUALITY_4K));

                //GOP
                mMediaRecorder.setVideoEncodingIFrameInterval(1.0f);

                //filename
                setFolderPath();
                mFilepath = mPath + "VD" + (new SimpleDateFormat("HHmmss")).format(new Date()) + ".MP4";
                Log.i(TAG, "MP4 file path = " + mFilepath);
                mMediaRecorder.setOutputFile(mFilepath);

                // Sample:Set up 4K Equi videos
                mParameters.set("RIC_SHOOTING_MODE", "RicMovieRecording3840");
                mParameters.set("RIC_PROC_STITCHING", "RicDynamicStitchingAuto");
                mParameters.set("RIC_PROC_ZENITH_CORRECTION", "RicZenithCorrectionOnAuto");
                mParameters.set("RIC_EXPOSURE_MODE", "RicAutoExposureP");
                mParameters.setPreviewFrameRate(30);
                mCamera.setParameters();

                CameraSettings.setCameraParameters(mParameters);
                CameraSettings.setSphereType(SphereType.EQUIRECTANGULAR);

                //mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
                mMediaRecorder.setOnErrorListener(onErrorListener);
                mMediaRecorder.setOnInfoListener(onInfoListener);

                try {
                    mMediaRecorder.prepare();
                    mMediaRecorder.start();
                    mIsCapturing = true;
                    Log.d(TAG, "mMediaRecorder.start()");
                } catch (IOException | RuntimeException e) {
                    e.printStackTrace();
                    releaseMediaRecorder();
                    result = false;
                }
            }
            //stop video recording
            else {
                try {
                    mMediaRecorder.stop();
                    mIsCapturing = false;
                    Log.d(TAG, "mMediaRecorder.stop()");
                } catch (RuntimeException e) {
                    result = false;
                } finally {
                    releaseMediaRecorder();
                }

                List<String> fileUrls = new ArrayList<String>();
                fileUrls.add(mFilepath);
                mBoxCallback.onCompleted(fileUrls.toArray(new String[fileUrls.size()]));

                //takeVideoView.setVisibility(View.GONE);
            }
            return result;
        }

        // THETA V and THETA Z1
        if(ThetaModel.isVCameraModel()) {
            //start video recording
            if (mMediaRecorder == null) {
                // Audio Manager settings
                mMediaRecorder = factory.abstractMediaRecorder(FactoryBase.CameraModel.VCamera);
                mMediaRecorder.newMediaRecorder();

                mAudioManager.setParameters("RicUseBFormat=true");
                mAudioManager.setParameters("RicMicSelect=RicMicSelectAuto");
                mAudioManager.setParameters("RicMicSurroundVolumeLevel=RicMicSurroundVolumeLevelNormal");

                // Sample:Set up 4K Equi videos
                mParameters.set("RIC_PROC_STITCHING", "RicStaticStitching");
                mParameters.set("RIC_SHOOTING_MODE", "RicMovieRecording4kEqui");

                CamcorderProfile camcorderProfile = CamcorderProfile.get(mCameraId, 10013);

                mParameters.set("video-size", "3840x1920");
                mParameters.set("recording-hint", "true");

                mCamera.setParameters();

                mCamera.unlock();

                mMediaRecorder.setCamera(mCamera);
                mMediaRecorder.setVideoSource(android.media.MediaRecorder.VideoSource.CAMERA);
                mMediaRecorder.setAudioSource(android.media.MediaRecorder.AudioSource.UNPROCESSED);

                camcorderProfile.videoCodec = android.media.MediaRecorder.VideoEncoder.H264;
                camcorderProfile.audioCodec = android.media.MediaRecorder.AudioEncoder.AAC;
                camcorderProfile.audioChannels = 1;

                mMediaRecorder.setProfile(camcorderProfile);
                mMediaRecorder.setVideoEncodingBitRate(56000000); // 56 Mbps
                mMediaRecorder.setVideoFrameRate(30); // 30 fps
                mMediaRecorder.setMaxDuration(1500000); // max: 25 min
                mMediaRecorder.setMaxFileSize(20401094656L); // max: 19 GB

                //mMediaRecorder.setVideoSize(3840, 1920);

                String dateTime = getDateTime();
                mMp4filePath = String.format("%s/plugin_%s.MP4", DCIM, dateTime);
                mWavfilePath = String.format("%s/plugin_%s.WAV", DCIM, dateTime);
                Log.i(TAG, "MP4 file path = " + mMp4filePath);
                String videoWavFile = String.format("%s,%s", mMp4filePath, mWavfilePath);
                mMediaRecorder.setOutputFile(videoWavFile);
                //mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
                mMediaRecorder.setOnErrorListener(onErrorListener);
                mMediaRecorder.setOnInfoListener(onInfoListener);

                try {
                    /**
                     * Set parameters to be given to metadata before starting recording of video
                     */
                    CameraSettings.setCameraParameters(mParameters);

                    /*
                     * Set Sphere type
                     */
                    CameraSettings.setSphereType(SphereType.EQUIRECTANGULAR);

                    /*
                     * Set Date, Time, and TimeZone to CameraSettings
                     * - In fact, please use `camera.getOptions` of `RICOH THETA API v2.1` to obtain `dateTimeZone`
                     *   from 'THETA shooting application' beforehand.
                     *   The operating system's time zone is always UTC (+00:00).
                     *
                     *   See. https://developers.theta360.com/ja/docs/v2.1/api_reference/
                     */
                    long sysTime = System.currentTimeMillis();
                    int timeZoneOffset = 32400000;  // TimeZone offset JST (+09:00)
                    CameraSettings.setDateTime(sysTime);
                    CameraSettings.setTimeZone(timeZoneOffset);

                    /**
                     * Set Microphone Gain
                     */
                    CameraSettings.setGain("RicMicSurroundVolumeLevel=RicMicSurroundVolumeLevelNormal");

                    /**
                     * Set the parameter to be set to movie metadata to MovieSettings
                     * If you want to add metadata to the video file,
                     * please set the parameter to MovieSettings as below
                     */

                    /**
                     * Set Manufacturer name, Serial number, Version name, Model name
                     */
                    CameraSettings.setManufacturer("RICOH");
                    CameraSettings.setThetaSerialNumber(ThetaInfo.getThetaSerialNumber());
                    CameraSettings.setThetaFirmwareVersion(ThetaInfo.getThetaFirmwareVersion(getContext()));
                    CameraSettings.setThetaModel(ThetaModel.getValue(ThetaInfo.getThetaModelName()));

                    /*
                     * Set GPS information
                     * - In fact, please use `camera.getOptions` of `RICOH THETA API v2.1` to obtain `gpsInfo`
                     *   from 'THETA shooting application' beforehand.
                     *
                     *   See. https://developers.theta360.com/ja/docs/v2.1/api_reference/
                     */
                    GpsInfo gpsInfo = new GpsInfo();
                    // Sample: Asia/Tokyo
                    gpsInfo.setLat(35.6580); // North latitude 35 degree 39 minutes 29 seconds
                    gpsInfo.setLng(139.7377);  // East longitude 139 degree 44 minutes 28 seconds
                    gpsInfo.setAltitude(40.0); // Altitude 40.0m
                    gpsInfo.setDatum(GpsInfo.DATUM);
                    gpsInfo.setDateTimeZone(CameraSettings.getDateTimeZone());
                    CameraSettings.setGpsInfo(gpsInfo);

                    /*
                     * Set attitude sensor value
                     */
                    mCameraAttitude.snapshot();
                    SensorValues sensorValues = new SensorValues();
                    sensorValues.setAttitudeRadian(mCameraAttitude.getAttitudeRadianSnapshot());
                    sensorValues.setCompassAccuracy(mCameraAttitude.getAccuracySnapshot());
                    CameraSettings.setSensorValues(sensorValues);

                    mMediaRecorder.prepare();
                    mMediaRecorder.start();
                    Log.d(TAG, "mMediaRecorder.start()");

                    mInstanceRecordMP4 = new File(mMp4filePath);
                    mInstanceRecordWAV = new File(mWavfilePath);
                } catch (IOException | RuntimeException e) {
                    e.printStackTrace();
                    releaseMediaRecorder();
                    result = false;
                }
            }
            //stop video recording
            else {
                try {
                    mMediaRecorder.stop();
                    Log.d(TAG, "mMediaRecorder.stop()");
                    /**
                     * Metadata is written to the movie file
                     * by specifying mp4 file path and wav file path in form box of BoxClass
                     */
                    Box box = new Box();
                    box.formBox(mMp4filePath, mWavfilePath, mBoxCallback);

                    // After shooting, set the shooting mode to monitoring mode.
                    mParameters.set("RIC_SHOOTING_MODE", "RicMonitoring");
                    mCamera.setParameters();
                } catch (RuntimeException e) {
                    // cancel recording
                    mInstanceRecordMP4.delete();
                    mInstanceRecordWAV.delete();
                    result = false;
                } finally {
                    releaseMediaRecorder();
                }
            }
            return result;
        }
        return result;
    }
}
