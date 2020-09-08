package com.hf.t2mediademo;

import android.app.Application;

import com.t2m.media.Initializer;
import com.t2m.tts.Tts;


public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Initializer.init(this); // 初始化SDK
        Tts.init(this); // 初始化TTS服务
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Tts.release(this);
    }
}
