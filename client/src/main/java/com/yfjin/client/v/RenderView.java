package com.yfjin.client.v;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

public class RenderView extends GLSurfaceView {

    public RenderView(Context context) {
        super(context);
        init(context);
    }


    public RenderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private CameraRender render;

    private void init(Context context) {
        setEGLContextClientVersion(2);
//        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        render = new CameraRender(this);
        setRenderer(render);
//        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_GPU);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }


    public void setRenderData(byte[] yuv, int width, int height) {
        render.setRenderData(yuv, width, height);
    }

}
