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

package org.croudtrip.fragments.join;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gc.materialdesign.views.Slider;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.pnikosis.materialishprogress.ProgressWheel;

import org.croudtrip.Constants;
import org.croudtrip.MainApplication;
import org.croudtrip.R;
import org.croudtrip.db.DatabaseHelper;
import org.croudtrip.fragments.SubscriptionFragment;
import org.croudtrip.fragments.offer.DispatchOfferTripFragment;
import org.croudtrip.location.LocationUpdater;
import org.croudtrip.location.MyAutoCompleteTextView;
import org.croudtrip.location.PlaceAutocompleteAdapter;
import org.croudtrip.utils.CrashPopup;
import org.croudtrip.utils.DefaultTransformer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by alex on 22.04.15.
 */
public class JoinSearchFragment extends SubscriptionFragment implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private final int REQUEST_PLACE_PICKER = 122;
    private DatabaseHelper dbHelper;
    private GoogleApiClient googleApiClient;
    private PlaceAutocompleteAdapter adapter;

    private org.croudtrip.db.Place lastSelected;
    private Location specifiedLocation;

    @InjectView(R.id.address) private TextView tv_address;
    @InjectView(R.id.places) private Button btn_destination;
    @InjectView(R.id.destination) private MyAutoCompleteTextView tv_destination;
    @InjectView(R.id.slider_waitingTime) private Slider slider_waitingTime;
    @InjectView(R.id.waitingTime) private TextView tv_waitingTime;
    @InjectView(R.id.pb_join_trip_destination) private ProgressWheel progressBar;
    @InjectView(R.id.layout_load_location) private LinearLayout loadLocationLayout;
    @InjectView(R.id.join) private Button btn_join;


    @Inject
    LocationUpdater locationUpdater;
    private Geocoder geocoder;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        geocoder = new Geocoder(getActivity());
        dbHelper = ((MainApplication) getActivity().getApplication()).getHelper();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);


        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(getActivity().getApplicationContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Places.GEO_DATA_API)
                    .build();

            googleApiClient.connect();
        } else if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
        }

        View view = inflater.inflate(R.layout.fragment_join_search, container, false);
        return view;
    }

    @Override
    public void onViewCreated( View view, Bundle savedInstanceState ) {
        super.onViewCreated(view, savedInstanceState);

        /*
        Set up the autocomplete textView
         */
        tv_destination.setOnItemClickListener(mAutocompleteClickListener);
        tv_destination.setThreshold(0);
        LatLngBounds bounds = null;
        if (locationUpdater.getLastLocation() != null) {
            bounds = LatLngBounds.builder().include(new LatLng(locationUpdater.getLastLocation().getLatitude(), locationUpdater.getLastLocation().getLongitude())).build();

        }
        adapter = new PlaceAutocompleteAdapter(getActivity(), android.R.layout.simple_list_item_1, bounds, null);
        tv_destination.setAdapter(adapter);
        adapter.setGoogleApiClient(googleApiClient);
        tv_destination.clearFocus();
        tv_destination.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if(hasFocus){
                    tv_destination.showDropDown();
                }
            }
        });

        /*
        Populate the autocomplete textView
         */
        try {
            List<org.croudtrip.db.Place> savedPlaces = dbHelper.getPlaceDao().queryForAll();
            ArrayList<PlaceAutocompleteAdapter.PlaceAutocomplete> history = new ArrayList<>();
            for (int i=savedPlaces.size()-1; i>=0; i--) {
                if (history.size() == 5) {
                    break;
                }
                PlaceAutocompleteAdapter.PlaceAutocomplete a = adapter.new PlaceAutocomplete(savedPlaces.get(i).getId(), savedPlaces.get(i).getDescription());
                history.add(a);
            }
            adapter.setHistory(history);
        } catch (SQLException e) {
            CrashPopup.show(getActivity(), e);
            e.printStackTrace();
        }

        /*
        Insert the last known location as soon as it is known
         */
        ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(getActivity());
        Subscription subscription = locationProvider.getLastKnownLocation().observeOn(Schedulers.io())
                .subscribe(new Action1<Location>() {
                    @Override
                    public void call(Location location) {
                        LatLngBounds bounds = LatLngBounds.builder().include(new LatLng(location.getLatitude(), location.getLongitude())).build();
                        adapter.setBounds(bounds);
                    }
                });
        subscriptions.add(subscription);

        /*
        Open the placepicker on button click
         */
        btn_destination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    PlacePicker.IntentBuilder intentBuilder = new PlacePicker.IntentBuilder();
                    Intent intent = intentBuilder.build(getActivity().getApplicationContext());

                    startActivityForResult(intent, REQUEST_PLACE_PICKER);
                } catch (GooglePlayServicesRepairableException e) {
                    CrashPopup.show(getActivity(), e);
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    CrashPopup.show(getActivity(), e);
                    e.printStackTrace();
                }
            }
        });

        /*
        Load and set the maximum waiting time
         */
        final SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
        int waitingTime = prefs.getInt(Constants.SHARED_PREF_KEY_WAITING_TIME, 10);
        tv_waitingTime.setText(getString(R.string.join_max_waiting) + " " + waitingTime);
        slider_waitingTime.setValue(waitingTime);

        slider_waitingTime.setOnValueChangedListener(new Slider.OnValueChangedListener() {
            @Override
            public void onValueChanged(int i) {
                tv_waitingTime.setText(getString(R.string.join_max_waiting) + " " + i);
            }
        });


        /*
        Retrieve starting position, save destination and try to join a trip
         */
        btn_join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                /*
                Save maximum waiting time
                 */
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(Constants.SHARED_PREF_KEY_WAITING_TIME, slider_waitingTime.getValue());
                editor.apply();

                org.croudtrip.db.Place tempPlace = lastSelected;
                lastSelected = null;

                /*
                Get the starting position either by placepicker or GPS data
                 */
                Location currentLocation;
                if (specifiedLocation == null) {
                    // retrieve current position
                    currentLocation = locationUpdater.getLastLocation();
                    if (currentLocation == null) {
                        Toast.makeText(getActivity().getApplicationContext(), R.string.offer_trip_no_location, Toast.LENGTH_SHORT).show();

                        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
                        adb.setTitle(getResources().getString(R.string.enable_gps_title));
                        adb.setMessage(getResources().getString(R.string.gpd_not_available));
                        adb.setPositiveButton(getResources().getString(R.string.redirect_to_placepicker), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                try {
                                    PlacePicker.IntentBuilder intentBuilder = new PlacePicker.IntentBuilder();
                                    Intent intent = intentBuilder.build(getActivity().getApplicationContext());

                                    startActivityForResult(intent, REQUEST_PLACE_PICKER);
                                } catch (GooglePlayServicesRepairableException e) {
                                    CrashPopup.show(getActivity(), e);
                                    e.printStackTrace();
                                } catch (GooglePlayServicesNotAvailableException e) {
                                    CrashPopup.show(getActivity(), e);
                                    e.printStackTrace();
                                }

                                return;
                            }
                        });
                        adb.show();
                        return;
                    }
                } else {
                    currentLocation = specifiedLocation;
                    specifiedLocation = null;
                }

                // get destination from string
                LatLng destination = null;
                try {
                    List<Address> addresses = null;
                    if (tv_address.getText() == null || tv_address.getText().equals("")) {
                        addresses = geocoder.getFromLocationName(tv_destination.getText().toString(), 1);
                    } else {
                        addresses = geocoder.getFromLocationName(tv_address.getText().toString(), 1);
                    }
                    if (addresses != null && addresses.size() > 0)
                        destination = new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
                } catch (IOException e) {
                    Toast.makeText(getActivity().getApplicationContext(), R.string.join_trip_no_destination, Toast.LENGTH_SHORT).show();
                    return;
                }
                // no destination received
                if (destination == null) {
                    Toast.makeText(getActivity().getApplicationContext(), R.string.offer_trip_no_destination, Toast.LENGTH_SHORT).show();
                    return;
                }

                if( currentLocation == null ) {
                    Toast.makeText(getActivity().getApplicationContext(), R.string.offer_trip_no_location, Toast.LENGTH_SHORT).show();
                    return;
                }

                /*
                Save or update destination in database
                 */
                try {
                    if (tempPlace != null) {
                        dbHelper.getPlaceDao().delete(tempPlace);
                        dbHelper.getPlaceDao().create(tempPlace);
                    } else {
                        tempPlace = new org.croudtrip.db.Place();
                        tempPlace.setId(tv_destination.getText().toString());
                        tempPlace.setDescription(tv_destination.getText().toString());
                        dbHelper.getPlaceDao().delete(tempPlace);
                        dbHelper.getPlaceDao().create(tempPlace);
                    }
                } catch (SQLException e) {
                    CrashPopup.show(getActivity(), e);
                    e.printStackTrace();
                }


                /*
                Set status to "searching"
                 */
                editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, true);
                editor.apply();


                /*
                Add trip information to extras and update UI
                 */
                Bundle extras = new Bundle();
                //extras.putString(JoinDispatchFragment.KEY_ACTION_TO_RUN, JoinDispatchFragment.ACTION_START_BACKGROUND_SEARCH);
                extras.putInt(JoinDispatchFragment.KEY_MAX_WAITING_TIME, slider_waitingTime.getValue() * 60); // max waiting time in seconds
                extras.putDouble(JoinDispatchFragment.KEY_CURRENT_LOCATION_LATITUDE, currentLocation.getLatitude());
                extras.putDouble(JoinDispatchFragment.KEY_CURRENT_LOCATION_LONGITUDE,currentLocation.getLongitude());
                extras.putDouble(JoinDispatchFragment.KEY_DESTINATION_LATITUDE, destination.latitude);
                extras.putDouble(JoinDispatchFragment.KEY_DESTINATION_LONGITUDE, destination.longitude);

                Intent startingIntent = new Intent(Constants.EVENT_CHANGE_JOIN_UI);
                startingIntent.putExtras(extras);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(startingIntent);
            }
        });


        /*
        Open the "Offer Trip" Fragment on click at the floating action button
         */
        FloatingActionButton btn_offer = (FloatingActionButton) view.findViewById(R.id.btn_offer_trip);
        btn_offer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialNavigationDrawer drawer = (MaterialNavigationDrawer) getActivity();
                drawer.setFragment(new DispatchOfferTripFragment(), getString(R.string.menu_offer_trip));
                MaterialSection section = drawer.getSectionByTitle(getString(R.string.menu_offer_trip));

                if (section == null) {
                    drawer.getSectionByTitle(getString(R.string.menu_my_trip));
                }

                drawer.setSection(section);
            }
        });


        // if there is currently no position available disable the offer trip button
        if( locationUpdater.getLastLocation() == null && specifiedLocation == null ) {
            btn_join.setEnabled( false );
            loadLocationLayout.setVisibility(View.VISIBLE);
            Subscription sub = locationProvider.getLastKnownLocation()
                    /* JUST FOR TESTING!!!
                            .observeOn( Schedulers.newThread() )
                            .subscribeOn(Schedulers.newThread())
                            .flatMap(new Func1<Location, Observable<Location>>() {
                                @Override
                                public Observable<Location> call(Location location) {
                                    try {
                                        Thread.sleep(4000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    return Observable.just(location);
                                }
                            })*/
                    .compose(new DefaultTransformer<Location>())
                    .subscribe( new Action1<Location>() {
                        @Override
                        public void call(Location location) {
                            if( location == null )
                                return;

                            locationUpdater.setLastLocation( location );
                            btn_join.setEnabled(true);
                            loadLocationLayout.setVisibility(View.GONE);
                        }
                    });

            subscriptions.add( sub );
        }

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // The user has selected a place. Extract the location
        if (requestCode == REQUEST_PLACE_PICKER && resultCode == Activity.RESULT_OK) {
            Place place = PlacePicker.getPlace(data, getActivity());
            Location l = new Location("placePicker");
            l.setLatitude(place.getLatLng().latitude);
            l.setLongitude(place.getLatLng().longitude);
            specifiedLocation = l;

            btn_join.setEnabled(true);
            //loadLocationLayout.setVisibility(View.VISIBLE);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
    }




    @Override
    public void onConnected(Bundle bundle) {
        adapter.setGoogleApiClient(googleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {
        adapter.setGoogleApiClient(null);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getActivity(),
                "Could not connect to Google API Client: Error " + connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();

        // Disable API access in the adapter because the client was not initialised correctly.
        adapter.setGoogleApiClient(null);
    }


    private AdapterView.OnItemClickListener mAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            /*
             Retrieve the place ID of the selected item from the Adapter.
             The adapter stores each Place suggestion in a PlaceAutocomplete object from which we
             read the place ID.
              */
            final PlaceAutocompleteAdapter.PlaceAutocomplete item = adapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);

            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
              details about the place.
              */
            progressBar.setVisibility(View.VISIBLE);
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(googleApiClient, placeId);
            placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                @Override
                public void onResult(PlaceBuffer places) {
                    if (!places.getStatus().isSuccess()) {
                        // Request did not complete successfully
                        places.release();
                        return;
                    }

                    Place place;
                    try {
                        place = places.get(0);
                    } catch (IllegalStateException e) {
                        places.release();
                        return;
                    }
                    lastSelected = new org.croudtrip.db.Place();
                    lastSelected.setId(place.getId());
                    lastSelected.setDescription(place.getAddress() + "");
                    tv_address.setText(place.getAddress());
                    progressBar.setVisibility(View.GONE);


                    places.release();

                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            });

            Toast.makeText(getActivity().getApplicationContext(), "Clicked: " + item.description, Toast.LENGTH_SHORT).show();
        }
    };
}
