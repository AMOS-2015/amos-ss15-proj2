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

package org.croudtrip.fragments.offer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gc.materialdesign.views.Slider;
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
import org.croudtrip.account.VehicleManager;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.VehicleResource;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.db.DatabaseHelper;
import org.croudtrip.fragments.SubscriptionFragment;
import org.croudtrip.fragments.VehicleInfoFragment;
import org.croudtrip.location.LocationUpdater;
import org.croudtrip.location.MyAutoCompleteTextView;
import org.croudtrip.location.PlaceAutocompleteAdapter;
import org.croudtrip.utils.DataHolder;
import org.croudtrip.utils.DefaultTransformer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import retrofit.RetrofitError;
import retrofit.client.Response;
import roboguice.inject.InjectView;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by alex on 22.04.15.
 */
public class OfferTripFragment extends SubscriptionFragment implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private static final String
            SHARED_PREF_KEY_RESOLVED_ADDRESS = "resolvedAddress",
            SHARED_PREF_KEY_ADDRESS = "address";

    private static OfferTripFragment instance;

    private final int REQUEST_PLACE_PICKER = 122;
    private DatabaseHelper dbHelper;
    private GoogleApiClient googleApiClient;
    private PlaceAutocompleteAdapter adapter;
    private Location specifiedLocation;


    private org.croudtrip.db.Place lastSelected;

    @InjectView(R.id.address) private TextView tv_address;
    @InjectView(R.id.destination) private MyAutoCompleteTextView tv_destination;
    @InjectView(R.id.slider_diversion) private Slider slider_diversion;
    @InjectView(R.id.slider_price) private Slider slider_price;
    @InjectView(R.id.diversion) private TextView tv_diversion;
    @InjectView(R.id.price) private TextView tv_price;
    @InjectView(R.id.my_car) private Button myCar;
    @InjectView(R.id.pb_offer_trip_destination) private ProgressWheel progressBar;
    @InjectView(R.id.layout_load_location) private LinearLayout loadLocationLayout;
    @InjectView(R.id.offer) private Button btn_offer;



    @Inject LocationUpdater locationUpdater;

    @Inject TripsResource tripsResource;
    @Inject VehicleResource vehicleResource;
    private Geocoder geocoder;

    private CarSelectDialogFragment myCarSelectDialogFragment = new CarSelectDialogFragment();
    private static List<String> carArrayList = new ArrayList<String>();
    private static List<Integer> carIdsArray = new ArrayList<Integer>();
    private static int numberOfCars = 0;
    private static int tempDefaultCarId = 0;
    private static int defaultVehicleId = 0;

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

        geocoder = new Geocoder(getActivity());
        dbHelper = ((MainApplication) getActivity().getApplication()).getHelper();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

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


        return inflater.inflate(R.layout.fragment_offer_trip, container, false);
    }

    @Override
    public void onViewCreated( View view, Bundle savedInstanceState ) {
        super.onViewCreated(view, savedInstanceState);


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
            e.printStackTrace();
        }

        // insert the last known location as soon as it is known
        ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(getActivity());
        Subscription subscription =  locationProvider.getLastKnownLocation().observeOn(Schedulers.io())
                .subscribe(new Action1<Location>() {
                    @Override
                    public void call(Location location) {
                        LatLngBounds bounds = LatLngBounds.builder().include(new LatLng(location.getLatitude(), location.getLongitude())).build();
                        adapter.setBounds(bounds);
                    }
                });

        subscriptions.add(subscription);


        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);

        // get max diversion from shared preferences and update textview as well as the slider
        int savedMaxDiversion = prefs.getInt(Constants.SHARED_PREF_KEY_DIVERSION, 3);
        tv_diversion.setText(getString(R.string.offer_max_diversion) + " " + savedMaxDiversion);
        slider_diversion.setValue(savedMaxDiversion);

        // get price per km from shared preferences and update textview as well as the slider
        int savedPrice = prefs.getInt(Constants.SHARED_PREF_KEY_PRICE, 26);
        tv_price.setText(getString(R.string.price) + " " + savedPrice);
        slider_price.setValue(savedPrice);

        // restore previous address
        String address = prefs.getString(SHARED_PREF_KEY_ADDRESS, "");
        String resolvedAddress = prefs.getString(SHARED_PREF_KEY_RESOLVED_ADDRESS, "");
        tv_destination.setText(address);
        tv_address.setText(resolvedAddress);
        tv_destination.setSelectAllOnFocus(true);

        slider_diversion.setOnValueChangedListener(new Slider.OnValueChangedListener() {
            @Override
            public void onValueChanged(int i) {
                tv_diversion.setText(getString(R.string.offer_max_diversion) + " " + i);
            }
        });
        slider_price.setOnValueChangedListener(new Slider.OnValueChangedListener() {
            @Override
            public void onValueChanged(int i) {
                tv_price.setText(getString(R.string.price) + " " + i);
            }
        });


        if( locationUpdater == null )
            Timber.d("Location Updater is null");


        //Get default car's type from server and set the button text accordingly
        Subscription Vsubscription = vehicleResource.getVehicle(VehicleManager.getDefaultVehicleId(getActivity()))
                .compose(new DefaultTransformer<Vehicle>())
                .subscribe(new Action1<Vehicle>() {
                    @Override
                    public void call(Vehicle vehicle) {
                        if (vehicle != null) {
                            if (vehicle.getType() != null)
                                myCar.setText(vehicle.getType());
                        }
                        else
                            myCar.setText("My Cars");
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        //Response response = ((RetrofitError) throwable).getResponse();
                        Timber.e("Failed to fetch with error:\n" + throwable.getMessage());
                    }
                });
        subscriptions.add(Vsubscription);

        myCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Get a list of user vehicles and add it to the ArrayList
                Subscription vListSubscription = vehicleResource.getVehicles()
                        .compose(new DefaultTransformer<List<Vehicle>>())
                        .subscribe(new Action1<List<Vehicle>>() {
                            @Override
                            public void call(List<Vehicle> vehicles) {
                                carArrayList.clear();
                                carIdsArray.clear();
                                if (vehicles.size() > 0) {
                                    for (int i = 0; i < vehicles.size(); i++) {
                                        carArrayList.add(vehicles.get(i).getType());
                                        carIdsArray.add((int) vehicles.get(i).getId());
                                        Timber.i("Added " + carArrayList.get(i) + " with id: " + (int) vehicles.get(i).getId());
                                        numberOfCars = vehicles.size();
                                    }
                                    myCarSelectDialogFragment.show(getFragmentManager(), "Car Select");
                                }
                                else
                                    showCarPlateDialog();
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Response response = ((RetrofitError) throwable).getResponse();
                                if (response != null && response.getStatus() == 401) {  // Not Authorized
                                } else {
                                    Timber.e("error" + throwable.getMessage());
                                }
                                Timber.e("Couldn't get data" + throwable.getMessage());
                            }
                        });

                subscriptions.add(vListSubscription);
                //((MaterialNavigationDrawer) getActivity()).setFragmentChild(new VehicleSelectionFragment(), "Select a car as default");
                //showCarSelectionDialog();
            }
        });




        // By clicking on the offer-trip-button the driver makes his choice public
        btn_offer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveState();

                org.croudtrip.db.Place tempPlace = lastSelected;
                lastSelected = null;

                Location currentLocation;
                if (specifiedLocation == null) {
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
                                    e.printStackTrace();
                                } catch (GooglePlayServicesNotAvailableException e) {
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
                    Timber.d("Destination Extraction: " + e.getMessage());
                    Toast.makeText(getActivity().getApplicationContext(), R.string.offer_trip_no_destination, Toast.LENGTH_SHORT).show();
                    return;
                }

                // no destination received
                if (destination == null) {
                    Toast.makeText(getActivity().getApplicationContext(), R.string.offer_trip_no_destination, Toast.LENGTH_SHORT).show();
                    return;
                }

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
                    e.printStackTrace();
                }


                if (VehicleManager.getDefaultVehicleId(getActivity()) == -3) {
                    //Get a list of user vehicles and add it to the ArrayList
                    Subscription vListSubscription = vehicleResource.getVehicles()
                            .compose(new DefaultTransformer<List<Vehicle>>())
                            .subscribe(new Action1<List<Vehicle>>() {
                                @Override
                                public void call(List<Vehicle> vehicles) {
                                    carArrayList.clear();
                                    carIdsArray.clear();
                                    if (vehicles.size() > 0) {
                                        for (int i = 0; i < vehicles.size(); i++) {
                                            carArrayList.add(vehicles.get(i).getType());
                                            carIdsArray.add((int) vehicles.get(i).getId());
                                            Timber.i("Added " + carArrayList.get(i) + " with id: " + (int) vehicles.get(i).getId());
                                            numberOfCars = vehicles.size();
                                        }
                                        myCarSelectDialogFragment.show(getFragmentManager(), "Car Select");
                                    }
                                    else
                                        showCarPlateDialog();
                                }
                            }, new Action1<Throwable>() {
                                @Override
                                public void call(Throwable throwable) {
                                    Response response = ((RetrofitError) throwable).getResponse();
                                    if (response != null && response.getStatus() == 401) {  // Not Authorized
                                    } else {
                                        Timber.e("error" + throwable.getMessage());
                                    }
                                    Timber.e("Couldn't get data" + throwable.getMessage());
                                }
                            });

                    subscriptions.add(vListSubscription);
                }else {
                    if( currentLocation == null )
                    {
                        return;
                    }

                    // Start the My Trip view for the driver
                    final Bundle b = new Bundle();
                    b.putString(MyTripDriverFragment.ARG_ACTION, MyTripDriverFragment.ACTION_CREATE);
                    b.putInt("maxDiversion", Integer.valueOf(slider_diversion.getValue() + ""));
                    b.putInt("pricePerKilometer", Integer.valueOf(slider_price.getValue() + ""));
                    b.putDouble("fromLat", currentLocation.getLatitude());
                    b.putDouble("fromLng", currentLocation.getLongitude());
                    b.putDouble("toLat", destination.latitude);
                    b.putDouble("toLng", destination.longitude);
                    final MyTripDriverFragment myTripDriverFragment = new MyTripDriverFragment();

                    b.putLong("vehicle_id", VehicleManager.getDefaultVehicleId(getActivity()));
                    myTripDriverFragment.setArguments(b);

                    // Change "Offer Trip" to "My Trip" in navigation drawer
                    MaterialNavigationDrawer drawer = ((MaterialNavigationDrawer) getActivity());
                    MaterialSection section = drawer.getSectionByTitle(getString(R.string.menu_offer_trip));
                    section.setTitle(getString(R.string.menu_my_trip));

                    // The next fragment shows the "My Trip screen"
                    drawer.setFragment(myTripDriverFragment, getString(R.string.menu_my_trip));


                    Toast.makeText(getActivity().getApplicationContext(), R.string.offer_trip, Toast.LENGTH_SHORT).show();
                }

            }
        });

        // if there is currently no position available disable the offer trip button
        if( locationUpdater.getLastLocation() == null && specifiedLocation == null ) {
            btn_offer.setEnabled( false );
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
                                    btn_offer.setEnabled(true);
                                    loadLocationLayout.setVisibility(View.GONE);
                                }
                            });

            subscriptions.add(sub);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PLACE_PICKER && resultCode == Activity.RESULT_OK) {
            final Place place = PlacePicker.getPlace(data, getActivity());
            Location l = new Location("placePicker");
            l.setLatitude(place.getLatLng().latitude);
            l.setLongitude(place.getLatLng().longitude);
            specifiedLocation = l;
            btn_offer.setEnabled(true);
            loadLocationLayout.setVisibility(View.VISIBLE);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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

    public void onConnectionFailed(ConnectionResult connectionResult) {
        // TODO(Developer): Check error code and notify the user of error state and resolution.
        Toast.makeText(getActivity(),
                "Could not connect to Google API Client: Error " + connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();

        // Disable API access in the adapter because the client was not initialised correctly.
        adapter.setGoogleApiClient(null);

    }

    @Override
    public void onConnected(Bundle bundle) {
        // Successfully connected to the API client. Pass it to the adapter to enable API access.
        adapter.setGoogleApiClient(googleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Connection to the API client has been suspended. Disable API access in the client.
        adapter.setGoogleApiClient(null);
    }

    private void showCarPlateDialog () {
        AlertDialog.Builder carPlateDialog = new AlertDialog.Builder(getActivity());
        carPlateDialog.setTitle(R.string.no_car_plate_title);
        carPlateDialog.setMessage(R.string.no_car_plate_message);
        carPlateDialog.setCancelable(true);
        carPlateDialog.setPositiveButton(getString(R.string.yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        DataHolder.getInstance().setVehicle_id(-2);
                        saveState();
                        ((MaterialNavigationDrawer) getActivity()).setFragmentChild(new VehicleInfoFragment(), getString(R.string.new_car));
                    }
                });
        carPlateDialog.setNegativeButton(getString(R.string.no),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert11 = carPlateDialog.create();
        alert11.show();
    }

    private void saveState() {
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(Constants.SHARED_PREF_KEY_DIVERSION, slider_diversion.getValue());
        editor.putInt(Constants.SHARED_PREF_KEY_PRICE, slider_price.getValue());
        editor.putString(SHARED_PREF_KEY_ADDRESS, tv_destination.getText().toString());
        editor.putString(SHARED_PREF_KEY_RESOLVED_ADDRESS, tv_address.getText().toString());
        editor.apply();
    }

    //This class is used to show a list of available cars and enable the user to choose the default one
    @SuppressLint("ValidFragment")
    private class CarSelectDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            //Get the default vehicle id from shared prefs
            defaultVehicleId = (int) VehicleManager.getDefaultVehicleId(getActivity());
            final String[] carList = new String[numberOfCars];
            final int checkedCarIndex = carIdsArray.indexOf(defaultVehicleId);
            Timber.i("Default car id is: " + defaultVehicleId);
            Timber.i("Checked car index is: "+ checkedCarIndex);

            //Fill the array with data from the ArrayList that was obtained previously
            for (int i=0;i<numberOfCars;i++) {
                carList[i] = carArrayList.get(i);
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Set the dialog title
            builder.setTitle(R.string.select_car)
                    .setSingleChoiceItems(carList, checkedCarIndex, new DialogInterface.OnClickListener() {
                        //Save the checked car in a temporary variable
                        @Override
                        public void onClick(DialogInterface arg0, int selectedId) {
                            tempDefaultCarId = selectedId;
                            DataHolder.getInstance().setVehicle_type(carList[selectedId]);
                        }
                    })
                    //Save the selected car and set the button text
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            VehicleManager.saveDefaultVehicle(getActivity(), carIdsArray.get(tempDefaultCarId));
                            Timber.i("Default car set to: " + carIdsArray.get(tempDefaultCarId));
                            Toast.makeText(getActivity(), "Default car set!", Toast.LENGTH_SHORT).show();
                            myCar.setText(DataHolder.getInstance().getVehicle_type());
                        }
            })

                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            return builder.create();
        }

        @Override
        public void onStart() {
            super.onStart();
        }
    }

}



