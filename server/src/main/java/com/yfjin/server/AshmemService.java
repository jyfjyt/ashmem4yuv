package com.yfjin.server;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SharedMemory;
import android.system.ErrnoException;
import android.util.Size;

import com.yfjin.server.c.Camera2Helper;

import java.nio.ByteBuffer;


public class AshmemService extends Service {


    private static final String TAG = "AshmemService";
    private static final String ASHM_FILE_NAME = "video_memory";

    private AshmemImpl mAshmemStub;

    @Override
    public IBinder onBind(Intent intent) {
        if (mAshmemStub == null) {
            initAshmem();
        }
        return mAshmemStub;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private void initAshmem() {

        Camera2Helper helper = new Camera2Helper(this);
        helper.setYUVDataCallback((y, uv, vu, width, height, stride) -> {
            mAshmemStub.refreshFrame(y, uv);
        });
        Size size = helper.getConfig();
        mAshmemStub = new AshmemImpl(size.getWidth(), size.getHeight());

        helper.start(null);

    }

    private static class AshmemImpl extends IAshmem.Stub {

        private SharedMemory mSharedMemory;
        private ByteBuffer mMapping;
        private int mWidth;
        private int mHeight;

        private AshmemImpl(int width, int height) {
            mWidth = width;
            mHeight = height;
            initSharedMemory();
        }

        private int getSize() {
            return mWidth * mHeight * 3 / 2;
        }

        private void initSharedMemory() {
            try {
                mSharedMemory = SharedMemory.create(ASHM_FILE_NAME, getSize());
                mMapping = mSharedMemory.mapReadWrite();
            } catch (ErrnoException e) {
                e.printStackTrace();
            }
        }

        public void refreshFrame(byte[] y, byte[] uv) {
            mMapping.position(0);
            mMapping.put(y);
            mMapping.put(uv);
        }

        @Override
        public int getVideoWidth() throws RemoteException {
            return mWidth;
        }

        @Override
        public int getVideoHeight() throws RemoteException {
            return mHeight;
        }

        @Override
        public SharedMemory getVideoMemory() throws RemoteException {
            return mSharedMemory;
        }
    }
}
