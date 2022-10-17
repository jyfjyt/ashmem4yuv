package com.yfjin.server.c;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;

public class Camera2Helper {

    private Context mContext;
    private String mCameraId;
    private Size mBestPreviewSize;

    private Handler mCameraHandler;
    private HandlerThread mCameraHandlerThread;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;

    //1路 UI显示
//    private static SurfaceTexture mSurfaceTexture;
    //2路 原始数据
    private Handler mDataHandler;
    private HandlerThread mDataHandlerThread;
    private ImageReader mDataImageReader;


    public Camera2Helper(Context context) {
        mContext = context.getApplicationContext();
    }

    private boolean isStarting = false;

    public Size getConfig() {
        if (mBestPreviewSize != null) {
            return mBestPreviewSize;
        }
        //摄像头的管理类
        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraList = cameraManager.getCameraIdList();
//            mCameraId = cameraList[cameraList.length - 1];
            mCameraId = cameraList[0];
            //这个摄像头的配置信息
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(mCameraId);
            //以及获取图片输出的尺寸和预览画面输出的尺寸、支持哪些格式
            //获取到的、像预览尺寸
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            //寻找一个 最合适的尺寸
            mBestPreviewSize = Camera2Util.getBestSupportedSize(null, new ArrayList<>(Arrays.asList(map.getOutputSizes(SurfaceTexture.class))));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return mBestPreviewSize;
    }

    //开启摄像头
    @SuppressLint("MissingPermission")
    public synchronized void start(SurfaceTexture surfaceTexture) {
        if (isStarting){
            return;
        }
        isStarting = true;
        //摄像头的管理类

        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraList = cameraManager.getCameraIdList();
            String cameraId = cameraList[cameraList.length - 1];
            //这个摄像头的配置信息
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            //以及获取图片输出的尺寸和预览画面输出的尺寸、支持哪些格式
            //获取到的、像预览尺寸
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            //寻找一个 最合适的尺寸
            if (mBestPreviewSize == null) {
                mBestPreviewSize = Camera2Util.getBestSupportedSize(null, new ArrayList<>(Arrays.asList(map.getOutputSizes(SurfaceTexture.class))));
            }
            //不单独HandlerThread的话，默认是主线程
            mCameraHandlerThread = new HandlerThread("CameraBackground");
            mCameraHandlerThread.start();
            mCameraHandler = new Handler(mCameraHandlerThread.getLooper());

            initUpload();
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    mCameraDevice = cameraDevice;
                    //建立绘画
                    createCameraSession(surfaceTexture);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    cameraDevice.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    mCameraDevice = null;
                }
            }, mCameraHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void initUpload() {
        mDataImageReader = ImageReader.newInstance(
                mBestPreviewSize.getWidth(), mBestPreviewSize.getHeight(),
                ImageFormat.YUV_420_888, 2
        );
        mDataImageReader.setOnImageAvailableListener(new OnUploadImageAvailable(), mDataHandler);
        mDataHandlerThread = new HandlerThread("UploadNV21Background");
        mDataHandlerThread.start();
        mDataHandler = new Handler(mDataHandlerThread.getLooper());
    }


    private void createCameraSession(SurfaceTexture texture) {
        try {
            CaptureRequest.Builder requestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            List<Surface> surfaceList = createSurfaceList(texture,requestBuilder);
            //建立 链接     目的  几路 数据出口
            mCameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    // 系统的相机
                    // The camera is already closed
                    if (null == mCameraDevice) {
                        return;
                    }
                    try {
                        cameraCaptureSession.setRepeatingRequest(requestBuilder.build()
                                , new CameraCaptureSession.CaptureCallback() {
                                }
                                , mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    mCaptureSession = cameraCaptureSession;
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private List<Surface> createSurfaceList(SurfaceTexture texture,CaptureRequest.Builder requestBuilder) {
        List<Surface> surfaceList = new ArrayList<>();
        if (texture != null) {
            //设置预览宽高
            texture.setDefaultBufferSize(mBestPreviewSize.getWidth(), mBestPreviewSize.getHeight());
            //创建有一个Surface 画面1
            Surface surface = new Surface(texture);
            requestBuilder.addTarget(surface);
            surfaceList.add(surface);
        }
        if (mDataImageReader != null) {
            requestBuilder.addTarget(mDataImageReader.getSurface());
            surfaceList.add(mDataImageReader.getSurface());
        }

        return surfaceList;
    }


    public void stop() {
        mYUVCallback = null;

        if (mCaptureSession!=null){
            mCaptureSession.close();
            mCaptureSession=null;
        }

        if (mCameraDevice!=null){
            mCameraDevice.close();
            mCameraDevice=null;
        }
        mCameraHandlerThread.quitSafely();
        mCameraHandler.removeCallbacksAndMessages(null);

        if (mDataImageReader != null) {
            mDataImageReader.close();
            mDataImageReader = null;
        }
        if (mDataHandlerThread != null) {
            mDataHandlerThread.quitSafely();
            mDataHandlerThread = null;
        }
        if (mDataHandler != null) {
            mDataHandler.removeCallbacksAndMessages(null);
            mDataHandler = null;
        }

//        releaseSurfaceTexture();

        isStarting = false;

    }


    private class OnUploadImageAvailable implements ImageReader.OnImageAvailableListener {

        private byte[] y;
        private byte[] uv;
        private byte[] vu;

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            if (image == null) {
                return;
            }
            Image.Plane[] planes = image.getPlanes();
            if (y == null) {
                y = new byte[planes[0].getBuffer().limit() - planes[0].getBuffer().position()];
                uv = new byte[planes[1].getBuffer().limit() - planes[1].getBuffer().position()];
                vu = new byte[planes[2].getBuffer().limit() - planes[2].getBuffer().position()];
            }
            if (image.getPlanes()[0].getBuffer().remaining() == y.length) {
                planes[0].getBuffer().get(y);
                planes[1].getBuffer().get(uv);
                planes[2].getBuffer().get(vu);
            }
            int stride = planes[0].getRowStride();

            if (mYUVCallback != null) {
                mYUVCallback.onPreview(y, uv, vu, mBestPreviewSize.getWidth(), mBestPreviewSize.getHeight(), stride);
            }

            image.close();
        }
    }

    private IYUVCallback mYUVCallback;

    public void setYUVDataCallback(IYUVCallback callback) {
        mYUVCallback = callback;
    }

    public interface IYUVCallback {
        void onPreview(byte[] y, byte[] uv, byte[] vu, int width, int height, int stride);
    }

}
