package org.croudtrip.fragments;

import android.app.Activity;
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

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.TripsResource;
import org.croudtrip.location.LocationUpdater;
import org.croudtrip.trips.TripOffer;
import org.croudtrip.trips.TripOfferDescription;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import roboguice.fragment.provided.RoboFragment;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by alex on 22.04.15.
 */
public class OfferTripFragment extends RoboFragment {

    private static OfferTripFragment instance;

    private final int REQUEST_PLACE_PICKER = 122;

    private TextView tv_name, tv_attributions;
    private EditText tv_address;
    private Button btn_destination;

    @Inject LocationUpdater locationUpdater;

    @Inject TripsResource tripsResource;

    private Geocoder geocoder;

    public static OfferTripFragment get() {
        synchronized (OfferTripFragment.class) {
            if (instance == null) {
                instance = new OfferTripFragment();
            }
            return instance;
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar = ((MaterialNavigationDrawer) this.getActivity()).getToolbar();
        geocoder = new Geocoder(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_offer_trip, container, false);

        tv_name = (TextView) view.findViewById(R.id.name);
        tv_address = (EditText) view.findViewById(R.id.address);
        tv_attributions = (TextView) view.findViewById(R.id.attributions);

        // choose a destination by using the place picker
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

        // define maximum waiting time
        final MaterialEditText maxDiversion = (MaterialEditText) view.findViewById(R.id.diversion);
        final SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);

        int waitingTime = prefs.getInt(Constants.SHARED_PREF_KEY_DIVERSION, 3);
        maxDiversion.setText("" + waitingTime);

        // define maximum price per kilometer that offers the driver
        final MaterialEditText pricePerKm = (MaterialEditText) view.findViewById(R.id.price);
        int price = prefs.getInt(Constants.SHARED_PREF_KEY_PRICE, 26);
        pricePerKm.setText("" + price);

        if( locationUpdater == null )
            Timber.d("Location Updater is null");

        // By clicking on the offer-trip-button the driver makes his choice public
        Button btn_join = (Button) view.findViewById(R.id.offer);
        btn_join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(Constants.SHARED_PREF_KEY_DIVERSION, Integer.valueOf(maxDiversion.getText().toString()));
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

                TripOfferDescription tripOffer = new TripOfferDescription(
                                                        new org.croudtrip.directions.Location( currentLocation.getLatitude(), currentLocation.getLongitude() ),
                                                        new org.croudtrip.directions.Location( destination.latitude, destination.longitude ),
                                                        Integer.valueOf(maxDiversion.getText().toString()) );

                tripsResource.addOffer( tripOffer ).subscribe(new Action1<TripOffer>() {
                    @Override
                    public void call(TripOffer routeNavigations) {
                        Timber.d("Your offer was successfully sent to the server");
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        // on main thread; something went wrong
                        Timber.e(throwable.getMessage());
                    }
                });

                Toast.makeText(getActivity().getApplicationContext(), R.string.offer_trip, Toast.LENGTH_SHORT).show();
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
            btn_destination.setText(getResources().getString(R.string.offer_change_destination));
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
