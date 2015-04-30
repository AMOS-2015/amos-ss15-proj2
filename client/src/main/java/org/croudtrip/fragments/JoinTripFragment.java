package org.croudtrip.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.croudtrip.Constants;
import org.croudtrip.R;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;

/**
 * Created by alex on 22.04.15.
 */
public class JoinTripFragment extends Fragment {

    private final int REQUEST_PLACE_PICKER = 122;

    private TextView tv_name, tv_attributions;
    private EditText tv_address;
    private Button btn_destination;

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

        tv_name = (TextView) view.findViewById(R.id.name);
        tv_address = (EditText) view.findViewById(R.id.address);
        tv_attributions = (TextView) view.findViewById(R.id.attributions);



        btn_destination = (Button) view.findViewById(R.id.places);
        btn_destination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    PlacePicker.IntentBuilder intentBuilder = new PlacePicker.IntentBuilder();
                    Intent intent = intentBuilder.build(getActivity().getApplicationContext());

                    startActivityForResult(intent, REQUEST_PLACE_PICKER);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                   e.printStackTrace();
                }
            }
        });

        final MaterialEditText maxWaitingTime = (MaterialEditText) view.findViewById(R.id.waitingTime);
        final SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
        int waitingTime = prefs.getInt(Constants.SHARED_PREF_KEY_WAITING_TIME, 10);
        maxWaitingTime.setText("" + waitingTime);


        Button btn_join = (Button) view.findViewById(R.id.join);
        btn_join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(Constants.SHARED_PREF_KEY_WAITING_TIME, Integer.valueOf(maxWaitingTime.getText().toString()));
                editor.apply();
                Toast.makeText(getActivity().getApplicationContext(), "Join Trip", Toast.LENGTH_SHORT).show();
            }
        });


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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PLACE_PICKER && resultCode == Activity.RESULT_OK) {

            // The user has selected a place. Extract the tv_name and tv_address.
            final Place place = PlacePicker.getPlace(data, getActivity());

            String name = place.getName().toString();
            String address = place.getAddress().toString();
            String attributions = PlacePicker.getAttributions(data);
            if (attributions == null) {
                attributions = "";
            }

            if (address.contains(name)) {
                name = "";
            }

            tv_name.setText(name);
            tv_address.setText(address);
            tv_attributions.setText(Html.fromHtml(attributions));
            btn_destination.setText(getResources().getString(R.string.join_change_destination));
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        //inflater.inflate(R.menu.menu_main, menu);
    }
}
