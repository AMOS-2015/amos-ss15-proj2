package org.croudtrip.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/**
 * Created by Frederik Simon on 08.05.2015.
 */
public class LifecycleHandler implements Application.ActivityLifecycleCallbacks {
    private static int resumed, paused, started, stopped;

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        started++;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        resumed++;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        paused++;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        stopped++;
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    public static boolean isApplicationVisible() {
        return started > stopped;
    }

    public static boolean isApplicationInForeground() {
        return resumed > paused;
    }
}
