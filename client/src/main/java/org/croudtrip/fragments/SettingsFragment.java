package org.croudtrip.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.croudtrip.R;
import org.croudtrip.account.AccountManager;
import org.croudtrip.gcm.GcmManager;
import org.croudtrip.utils.DefaultTransformer;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import roboguice.fragment.provided.RoboFragment;
import rx.Subscription;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by alex on 22.04.15.
 */
public class SettingsFragment extends RoboFragment {

    //********************** Variables ***************************//
    @Inject
    GcmManager gcmManager;


    //************************ Methods ***************************//

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar = ((MaterialNavigationDrawer) this.getActivity()).getToolbar();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        if(AccountManager.isUserLoggedIn(getActivity().getApplicationContext())) {
            Button logoutButton = (Button) view.findViewById(R.id.settings_logout);
            logoutButton.setVisibility(View.VISIBLE);
            logoutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AccountManager.logout(getActivity().getApplicationContext(), true);

                    if (gcmManager.isRegistered()) {
                        Subscription subscription = gcmManager.unregister()
                                .compose(new DefaultTransformer<Void>())
                                .retry(3)
                                .subscribe(new Action1<Void>() {
                                    @Override
                                    public void call(Void aVoid) {
                                        Timber.d("Successfully unregistered");
                                    }
                                }, new Action1<Throwable>() {
                                    @Override
                                    public void call(Throwable throwable) {
                                        Timber.e(throwable.getMessage());
                                    }
                                });
                    }
                }
            });
        }

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        //inflater.inflate(R.menu.menu_main, menu);
    }
}
