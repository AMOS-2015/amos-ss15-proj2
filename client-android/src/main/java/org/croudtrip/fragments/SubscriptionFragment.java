/*
 * The CroudTrip! application aims at revolutionizing the car-ride-sharing market with its easy,
 * user-friendly and highly automated way of organizing shared Trips. Copyright (C) 2015  Nazeeh Ammari,
 *  Philipp Eichhorn, Ricarda Hohn, Vanessa Lange, Alexander Popp, Frederik Simon, Michael Weber
 * This program is free software: you can redistribute it and/or modify  it under the terms of the GNU
 *  Affero General Public License as published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *    If not, see http://www.gnu.org/licenses/.
 */

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
