package com.yfjin.client.v;

import android.content.Context;
import android.opengl.GLES20;

import com.yfjin.client.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;


public class CameraFilter  {


    public CameraFilter(Context context) {
        this(context, R.raw.camera_vert, R.raw.camera_frag);
    }

    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int DATA_XY_LENGHT = 2;
    private static final int DATA_UV_LENGHT = 2;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = (DATA_XY_LENGHT + DATA_UV_LENGHT) * FLOAT_SIZE_BYTES;


    private final float[] TRIANFLE_VERTICES_DATA = {
            // X, Y
            1, -1
            //UV
            , 1, 1
            // X, Y
            , 1, 1
            //UV
            , 1, 0
            // X, Y
            , -1, 1
            //UV
            , 0, 0
            // X, Y
            , -1, -1
            //UV
            , 0, 1
    };

    private static final int SHORT_SIZE_BYTES = 2;
    private final short[] INDICES_DATA = {
            0, 1, 2,
            2, 3, 0
    };

    //这个必须全局，否则局部变量会被回收
    private FloatBuffer mTriangleVertices;
    private ShortBuffer mIndices;

    protected int mProgram;

    private int vPositionHandler;
    private int vTextureCoordHandler;
    private int fSampleYHandler;
    private int fSampleUVHandler;

    private int mTextureIdY;
    private int mTextureIdUV;


    private ByteBuffer mBuffer;

    private CameraFilter(Context context, int vertexShaderId, int fragShaderId) {

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_DITHER);
        GLES20.glDisable(GLES20.GL_STENCIL_TEST);
        GLES20.glDisable(GL10.GL_DITHER);


        String vertexShader = OpenGLUtils.readRawTextFile(context, vertexShaderId);
        String fragShader = OpenGLUtils.readRawTextFile(context, fragShaderId);
        //编译-》连接-》运行
        mProgram = OpenGLUtils.loadProgram(vertexShader, fragShader);
        GLES20.glUseProgram(mProgram);

        initVert();
        initFrag();

        mIndices = ByteBuffer.allocateDirect(
                INDICES_DATA.length
                        * SHORT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asShortBuffer();
        mIndices.put(INDICES_DATA).position(0);

    }


    private void initVert() {

        vPositionHandler = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mTriangleVertices = ByteBuffer.allocateDirect(TRIANFLE_VERTICES_DATA.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTriangleVertices.put(TRIANFLE_VERTICES_DATA);
        //从索引0的位置取3个数据
        mTriangleVertices.position(0);
        GLES20.glVertexAttribPointer(vPositionHandler
                , 2
                , GLES20.GL_FLOAT
                , false
                , TRIANGLE_VERTICES_DATA_STRIDE_BYTES
                , mTriangleVertices
        );
        GLES20.glEnableVertexAttribArray(vPositionHandler);


        vTextureCoordHandler = GLES20.glGetAttribLocation(mProgram, "vCoord");
        //从索引3的位置取2个数据
        mTriangleVertices.position(DATA_XY_LENGHT);
        GLES20.glVertexAttribPointer(vTextureCoordHandler
                , 2
                , GLES20.GL_FLOAT
                , false
                , TRIANGLE_VERTICES_DATA_STRIDE_BYTES
                , mTriangleVertices
        );
        GLES20.glEnableVertexAttribArray(vTextureCoordHandler);


    }


    private void initFrag() {

        fSampleYHandler = GLES20.glGetUniformLocation(mProgram, "fSampleY");
        fSampleUVHandler = GLES20.glGetUniformLocation(mProgram, "fSampleUV");

        int[] textureIds = new int[2];
        GLES20.glGenTextures(2, textureIds, 0);
        mTextureIdY = textureIds[0];
        mTextureIdUV = textureIds[1];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIdY);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIdUV);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

    }

    public void setSize(int width, int height) {
        //设置长宽
        GLES20.glViewport(0, 0, width, height);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

    }

    public void onDraw(byte[] y, byte[] uv, int width, int height) {
        if (mBuffer == null) {
            mBuffer = ByteBuffer.allocateDirect(width * height * 3 / 2);
        }
        mBuffer.rewind();
        mBuffer.put(y);
        mBuffer.put(uv);
        playBuffer(width,height);
    }

    public void onDraw(byte[] yuv, int width, int height) {
        if (mBuffer == null) {
            mBuffer = ByteBuffer.allocateDirect(width * height * 3 / 2);
        }
        mBuffer.rewind();
        mBuffer.put(yuv);
        playBuffer(width,height);
    }

    private void playBuffer(int width, int height) {
        //upload Y plane data
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIdY);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE
                , width, height
                , 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE
                , mBuffer.position(0));
        GLES20.glUniform1i(fSampleYHandler, 0);

        //update UV plane data
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIdUV);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA
                , width >> 1, height >> 1
                , 0, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE
                , mBuffer.position(width * height));
        GLES20.glUniform1i(fSampleUVHandler, 1);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, INDICES_DATA.length, GLES20.GL_UNSIGNED_SHORT, mIndices);
    }

    public void release() {
        GLES20.glDeleteProgram(mProgram);
    }

}
