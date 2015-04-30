package org.croudtrip.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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
import com.google.android.gms.maps.model.LatLng;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.TripsResource;
import org.croudtrip.directions.RouteLocation;
import org.croudtrip.location.LocationUpdater;
import org.croudtrip.trips.TripMatch;
import org.croudtrip.trips.TripRequestDescription;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import roboguice.fragment.provided.RoboFragment;
import roboguice.inject.InjectView;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by alex on 22.04.15.
 */
public class JoinTripFragment extends RoboFragment {

    private final int REQUEST_PLACE_PICKER = 122;

    @InjectView(R.id.name) private TextView tv_name;
    @InjectView(R.id.attributions) private TextView tv_attributions;
    @InjectView(R.id.address) private EditText tv_address;
    @InjectView(R.id.places) private Button btn_destination;

    @Inject LocationUpdater locationUpdater;
    @Inject TripsResource tripsResource;
    private Geocoder geocoder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        geocoder = new Geocoder(getActivity());

        Toolbar toolbar = ((MaterialNavigationDrawer) this.getActivity()).getToolbar();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_join_trip, container, false);
        return view;
    }

    @Override
    public void onViewCreated( View view, Bundle savedInstanceState )
    {
        super.onViewCreated(view, savedInstanceState);

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

                // retrieve current position
                Location currentLocation = locationUpdater.getLastLocation();
                if( currentLocation == null ) {
                    Toast.makeText(getActivity().getApplicationContext(), R.string.offer_trip_no_location, Toast.LENGTH_SHORT).show();
                    return;
                }

                // get destination from string
                LatLng destination = null;
                try {
                    List<Address> addresses = geocoder.getFromLocationName( tv_address.getText().toString(), 1 );
                    if( addresses.size() > 0 )
                        destination = new LatLng( addresses.get(0).getLatitude(), addresses.get(0).getLongitude() );
                } catch (IOException e) {
                    Toast.makeText(getActivity().getApplicationContext(), R.string.offer_trip_no_destination, Toast.LENGTH_SHORT).show();
                    return;
                }

                // no destination received
                if( destination == null ) {
                    Toast.makeText(getActivity().getApplicationContext(), R.string.offer_trip_no_destination, Toast.LENGTH_SHORT).show();
                    return;
                }

                TripRequestDescription tripRequestDescription = new TripRequestDescription( new RouteLocation( currentLocation.getLatitude(), currentLocation.getLongitude() ),
                                                                                            new RouteLocation( destination.latitude, destination.longitude ));

                tripsResource.findMatches( tripRequestDescription ).subscribeOn(Schedulers.io())
                                                                   .observeOn(AndroidSchedulers.mainThread())
                                                                   .subscribe( new Action1<List<TripMatch>>() {
                                                                       @Override
                                                                       public void call(List<TripMatch> tripMatches) {

                                                                           if( tripMatches == null || tripMatches.size() == 0 ) {
                                                                                Toast.makeText(getActivity().getApplicationContext(), R.string.join_trip_no_matches, Toast.LENGTH_SHORT).show();
                                                                                return;
                                                                           }
                                                                           Toast.makeText(getActivity().getApplicationContext(), "Found " + tripMatches.size() + " matches", Toast.LENGTH_SHORT).show();
                                                                           TripMatch tripMatch = tripMatches.get(0);

                                                                       }
                                                                   }, new Action1<Throwable>() {
                                                                       @Override
                                                                       public void call(Throwable throwable) {
                                                                           // on main thread; something went wrong
                                                                           Timber.e(throwable.getMessage());
                                                                       }
                                                                   });

                Toast.makeText(getActivity().getApplicationContext(), "Join Trip", Toast.LENGTH_SHORT).show();
            }
        });


        FloatingActionButton btn_offer = (FloatingActionButton) view.findViewById(R.id.btn_offer_trip);
        final Fragment _this = this;
        btn_offer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialNavigationDrawer drawer = (MaterialNavigationDrawer) _this.getActivity();
                drawer.setFragment(OfferTripFragment.get(), getString(R.string.menu_offer_trip));
                MaterialSection section = drawer.getSectionByTitle(getString(R.string.menu_offer_trip));
                drawer.setSection(section);

                /*
                Use something like this to navigate to activities at a lower hierarchy, like the details of a route, driver etc
                ((MaterialNavigationDrawer)_this.getActivity()).setFragmentChild(OfferTripFragment.get(), getString(R.string.menu_offer_trip));
                 */
            }
        });

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
