package org.croudtrip.utils;

import android.content.Context;
import android.util.Log;

import rx.functions.Action1;

/**
 * Created by alex on 11.06.15.
 */
public class CrashCallback implements Action1<Throwable> {

    private Context context;

    public CrashCallback(Context context) {
        this.context = context;
    }

    @Override
    public void call(Throwable throwable) {
        CrashPopup.show(context, throwable);
    }
}
