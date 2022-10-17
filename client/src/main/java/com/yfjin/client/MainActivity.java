package com.yfjin.client;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.yfjin.client.databinding.ActivityMainBinding;
import com.yfjin.server.IAshmem;

import java.nio.ByteBuffer;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("123123", "client main:" + android.os.Process.myPid());
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.de1.setOnClickListener(v -> bind());
    }


    private boolean isConnect = false;
    private IAshmem mAshmem = null;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mAshmem = IAshmem.Stub.asInterface(service);
            try {
                mAshmem.asBinder().linkToDeath(mDeathRecipient, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            runOnUiThread(() -> {
                binding.de1.setText("Connected");
            });
            isConnect = true;
            new Thread(() -> doAction()).start();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isConnect = false;
            runOnUiThread(() -> {
                binding.de1.setText("Dis Connect");
            });
        }
    };


    // 失效重联机制, 当Binder死亡时, 重新连接
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            runOnUiThread(() -> {
                binding.de1.setText("ERR DEATH");
            });
        }
    };

    private void bind() {
        Intent intent = new Intent();
        intent.setAction("com.yfjin.action");
        //android5.0之后都要设置
        intent.setPackage("com.yfjin.server");
        bindService(intent, mConnection, BIND_AUTO_CREATE);
    }


    private ByteBuffer mBuffer;
    private byte[] mContent;
    private int mWidth;
    private int mHeight;

    private void doAction() {
        try {
            mWidth = mAshmem.getVideoWidth();
            mHeight = mAshmem.getVideoHeight();
            mContent = new byte[mWidth * mHeight * 3 / 2];
            mBuffer = mAshmem.getVideoMemory().mapReadOnly();
            while (isConnect) {
                Thread.sleep(32);
                mBuffer.position(0);
                mBuffer.get(mContent);
                binding.renderView.setRenderData(mContent, mWidth, mHeight);
            }
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                binding.de1.setText("ERR");
            });
        }
    }
}