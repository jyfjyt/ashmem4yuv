package com.yfjin.client.v;

import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraRender implements GLSurfaceView.Renderer {

    private RenderView mCamerView;
    private CameraFilter mCameraFilter;
    private boolean isCreate=false;

    public CameraRender(RenderView camerView) {
        this.mCamerView = camerView;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCameraFilter = new CameraFilter(mCamerView.getContext());
        isCreate=true;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mCameraFilter.setSize(width,height);
    }


    //-------------------------------------------------------------------------------

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!isCreate||mYUV==null){
            return;
        }
        mCameraFilter.onDraw(mYUV,mWidth,mHeight);
    }

    private byte[] mYUV;
    private int mWidth;
    private int mHeight;
    public void setRenderData(byte[] yuv, int width, int height) {
        if (!isCreate){
            return;
        }
        mYUV=yuv;
        mWidth=width;
        mHeight=height;
        mCamerView.requestRender();
    }
}
