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

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;

/**
 * Created by alex on 22.04.15.
 */
public class SettingsFragment extends Fragment {

    //********************** Variables ***************************//



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
