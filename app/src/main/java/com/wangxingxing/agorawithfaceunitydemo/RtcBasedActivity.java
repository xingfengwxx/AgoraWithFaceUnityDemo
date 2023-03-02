package com.wangxingxing.agorawithfaceunitydemo;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import io.agora.rtc2.RtcEngine;

public abstract class RtcBasedActivity extends AppCompatActivity implements RtcEngineEventHandler {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStart() {
        super.onStart();
        addRtcHandler(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeRtcHandler(this);
    }

    protected App application() {
        return (App) getApplication();
    }

    protected RtcEngine rtcEngine() {
        return application().rtcEngine();
    }

    private void addRtcHandler(RtcEngineEventHandler handler) {
        application().addRtcHandler(handler);
    }

    private void removeRtcHandler(RtcEngineEventHandler handler) {
        application().removeRtcHandler(handler);
    }
}
