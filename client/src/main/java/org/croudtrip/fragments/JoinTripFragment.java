package org.croudtrip.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.getbase.floatingactionbutton.FloatingActionButton;

import org.croudtrip.R;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;

/**
 * Created by alex on 22.04.15.
 */
public class JoinTripFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar = ((MaterialNavigationDrawer) this.getActivity()).getToolbar();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_join_trip, container, false);

        FloatingActionButton btn_offer = (FloatingActionButton) view.findViewById(R.id.btn_offer_trip);
        final Fragment _this = this;
        btn_offer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialNavigationDrawer drawer = (MaterialNavigationDrawer)_this.getActivity();
                drawer.setFragment(OfferTripFragment.get(), getString(R.string.menu_offer_trip));
                MaterialSection section = drawer.getSectionByTitle(getString(R.string.menu_offer_trip));
                drawer.setSection(section);

                /*
                Use something like this to navigate to activities at a lower hierarchy, like the details of a route, driver etc
                ((MaterialNavigationDrawer)_this.getActivity()).setFragmentChild(OfferTripFragment.get(), getString(R.string.menu_offer_trip));
                 */
            }
        });


        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        //inflater.inflate(R.menu.menu_main, menu);
    }
}
