package org.croudtrip.fragments;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.croudtrip.R;

import roboguice.fragment.provided.RoboFragment;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * A base class fragments that extends the {@link RoboFragment} to provide dependency injection
 * and that handles a {@link rx.subscriptions.CompositeSubscription} that can be used in subclasses
 * to unsubscribe all the subscription if the lifecycle of the fragment ends.
 */
public class SubscriptionFragment extends RoboFragment {
    protected final CompositeSubscription subscriptions = new CompositeSubscription();

    @Override
    public void onPause() {
        super.onPause();

        Timber.d("OnPause Subscriptions");

        subscriptions.unsubscribe();
        subscriptions.clear();
    }


}
