package com.zengxiao.coolweather.util;

/**
 * Created by zx on 2016/3/23.
 */
public interface HttpCallbackListener
{
    void onFinish(String response);
    void onError(Exception e);
}
