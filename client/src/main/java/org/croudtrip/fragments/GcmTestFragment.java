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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import org.croudtrip.R;
import org.croudtrip.gcm.GcmManager;
import org.croudtrip.utils.DefaultTransformer;

import javax.inject.Inject;

import roboguice.fragment.provided.RoboFragment;
import roboguice.inject.InjectView;
import rx.Subscription;
import rx.functions.Action1;

/**
 * Quick and simple GCM testing going on here ...
 */
public class GcmTestFragment extends SubscriptionFragment {

    @InjectView(R.id.status) private TextView statusView;
    @InjectView(R.id.register) private Button registerButton;
    @InjectView(R.id.unregister) private Button unregisterButton;

    @Inject GcmManager gcmManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_gcm_test, container, false);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        toggleRegistration();
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerButton.setEnabled(false);
                statusView.setText("registering ...");
                Subscription subscription = gcmManager.register()
                        .compose(new DefaultTransformer<Void>())
                        .subscribe(new SuccessAction(), new ErrorAction());

                subscriptions.add(subscription);

            }
        });
        unregisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unregisterButton.setEnabled(false);
                statusView.setText("unregistering ...");
                Subscription subscription = gcmManager.unregister()
                        .compose(new DefaultTransformer<Void>())
                        .subscribe(new SuccessAction(), new ErrorAction());

                subscriptions.add(subscription);
            }
        });
    }


    private void toggleRegistration() {
        if (gcmManager.isRegistered()) {
            statusView.setText("Registered");
            registerButton.setEnabled(false);
            unregisterButton.setEnabled(true);
        } else {
            statusView.setText("Unregistered");
            registerButton.setEnabled(true);
            unregisterButton.setEnabled(false);
        }
    }


    private final class SuccessAction implements Action1<Void> {
        @Override
        public void call(Void nothing) {
            toggleRegistration();
        }
    }


    private final class ErrorAction implements Action1<Throwable> {
        @Override
        public void call(Throwable throwable) {
            toggleRegistration();
            statusView.setText(throwable.getMessage());
        }
    }

}
