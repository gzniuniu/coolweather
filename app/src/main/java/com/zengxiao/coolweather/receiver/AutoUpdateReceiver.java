package com.zengxiao.coolweather.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.zengxiao.coolweather.service.AutoUpdateService;

/**
 * Created by zx on 2016/3/24.
 */
public class AutoUpdateReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Intent i=new Intent(context, AutoUpdateService.class);
        context.startService(i);
    }
}
