package com.theta360.pluginsample;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * MainFragment
 */
public class MainFragment extends Fragment {
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private int mCameraId;
    private Camera.Parameters mParameters;
    private Camera.CameraInfo mCameraInfo;

    private boolean isSurface = false;

    public MainFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        SurfaceView surfaceView = (SurfaceView) view.findViewById(R.id.surfaceView);
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(mSurfaceHolderCallback);
    }


    @Override
    public void onStart() {
        super.onStart();

        if (isSurface) {
            open();
            setSurface(mSurfaceHolder);
        }
    }

    @Override
    public void onStop() {
        close();

        super.onStop();
    }

    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            isSurface = true;
            open();
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            setSurface(surfaceHolder);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            isSurface = false;
            close();
        }
    };

    public void takePicture() {
        mParameters.setPictureSize(5376, 2688);
        mParameters.set("RIC_SHOOTING_MODE", "RicStillCaptureStd");
        mParameters.set("RIC_EXPOSURE_MODE", "RicAutoExposureP");
        mParameters.set("recording-hint", "false");
        mParameters.setJpegThumbnailSize(320, 160);
        mCamera.setParameters(mParameters);

        mCamera.takePicture(onShutterCallback, null, onJpegPictureCallback);
    }

    private void open() {
        if (mCamera == null) {
            int numberOfCameras = Camera.getNumberOfCameras();

            for (int i = 0; i < numberOfCameras; i++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);

                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    mCameraInfo = info;
                    mCameraId = i;
                }

                mCamera = Camera.open(mCameraId);
            }
            mCamera.setErrorCallback(mErrorCallback);
            mParameters = mCamera.getParameters();

//            mParameters.set("RIC_EXPOSURE_MODE", 2);
//            mParameters.set("RIC_WB_MODE", "auto");
//            mParameters.set("RIC_MANUAL_EXPOSURE_ISO_REAR", 0);
//            mParameters.set("RIC_MANUAL_EXPOSURE_ISO_FRONT", 0);
//            mParameters.set("RIC_MANUAL_EXPOSURE_TIME_REAR", 0);
//            mParameters.set("RIC_MANUAL_EXPOSURE_TIME_FRONT", 0);
//            mParameters.set("exposure-compensation-step", 0);
//            mParameters.set("RIC_WB_TEMPERATURE", 5000);
            mParameters.set("RIC_SHOOTING_MODE", "RicMonitoring");
            mCamera.setParameters(mParameters);
        }
    }

    public void close() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.setErrorCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private void setSurface(@NonNull SurfaceHolder surfaceHolder) {
        if (mCamera != null) {
            mCamera.stopPreview();

            try {
                mCamera.setPreviewDisplay(surfaceHolder);
                mParameters
                        .setPreviewSize(1024, 512);
                mCamera.setParameters(mParameters);
            } catch (IOException e) {
                e.printStackTrace();
                close();
            }
            mCamera.startPreview();
        }
    }

    private Camera.ErrorCallback mErrorCallback = new Camera.ErrorCallback() {
        @Override
        public void onError(int error, Camera camera) {

        }
    };

    private Camera.ShutterCallback onShutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
        }
    };

    private Camera.PictureCallback onJpegPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            mParameters.set("RIC_PROC_STITCHING", "RicStaticStitching");
            mCamera.setParameters(mParameters);
            mCamera.stopPreview();

            String fileUrl = "/storage/emulated/0/DCIM/plugin.jpg";
            try (FileOutputStream fileOutputStream = new FileOutputStream(fileUrl)) {
                fileOutputStream.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mCamera.startPreview();
        }
    };
}
