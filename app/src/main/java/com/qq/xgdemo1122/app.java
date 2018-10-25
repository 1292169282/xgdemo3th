package com.qq.xgdemo1122;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.tencent.android.otherPush.StubAppUtils;



/**
 * Created by admin on 2017/2/13.
 */

public class app extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

		StubAppUtils.attachBaseContext(base);
        Log.d("初始化了呀压压", "app");
    }
    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
    }

}



