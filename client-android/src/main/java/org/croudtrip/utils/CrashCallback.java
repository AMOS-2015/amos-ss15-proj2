package org.croudtrip.utils;

import android.content.Context;

import com.google.common.base.Optional;

import rx.functions.Action1;
import timber.log.Timber;

public class CrashCallback implements Action1<Throwable> {

    private final Context context;
    private final String logMsg;
    private final Optional<Action1<Throwable>> nextAction;

    public CrashCallback(Context context, String logMsg) {
        this(context, logMsg, null);
    }

    public CrashCallback(Context context, String logMsg, Action1<Throwable> nextAction) {
        this.context = context;
        this.logMsg = logMsg;
        this.nextAction = Optional.fromNullable(nextAction);
    }

    @Override
    public void call(Throwable throwable) {
        Timber.e(throwable, logMsg);
        CrashPopup.show(context, throwable);
        if (nextAction.isPresent()) nextAction.get().call(throwable);
    }
}
