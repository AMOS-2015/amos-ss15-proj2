package org.croudtrip.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.croudtrip.BuildConfig;
import org.croudtrip.R;

import timber.log.Timber;

/**
 * Created by alex on 11.06.15.
 */
public class CrashPopup {

    public static void show(Context context, Throwable throwable) {

        //Dont show a debug popup in production mode
        if (!BuildConfig.DEBUG) {
            return;
        }

        AlertDialog.Builder adb = new AlertDialog.Builder(context);
        adb.setTitle(context.getResources().getString(R.string.crash_popup_title));
        String message = throwable.toString();
        message += "\n";

        StackTraceElement[] stackTrace = throwable.getStackTrace();
        for (StackTraceElement element : stackTrace) {
            message += "\n" + element.toString();
        }

        adb.setMessage(message);
        adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        adb.show();
    }
}
