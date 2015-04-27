package org.croudtrip.utils;


import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * RxJava {@link rx.Observable.Transformer} which takes care of
 * running the task on an background thread and notifying subscribers
 * on the android main thread.
 */
public class DefaultTransformer<T> implements Observable.Transformer<T, T> {

	@Override
	public Observable<T> call(Observable<T> observable) {
		return observable
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread());
	}

}
