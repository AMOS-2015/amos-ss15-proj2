package org.croudtrip.fragments;



import roboguice.fragment.RoboFragment;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * A base class fragments that extends the {@link RoboFragment} to provide dependency injection
 * and that handles a {@link rx.subscriptions.CompositeSubscription} that can be used in subclasses
 * to unsubscribe all the subscription if the lifecycle of the fragment ends.
 */
public class SubscriptionFragment extends RoboFragment {
    protected CompositeSubscription subscriptions = new CompositeSubscription();


    @Override
    public void onPause() {
        super.onPause();

        Timber.d("OnPause Subscriptions");

        subscriptions.unsubscribe();
        subscriptions.clear();
        subscriptions = new CompositeSubscription();
    }

}
