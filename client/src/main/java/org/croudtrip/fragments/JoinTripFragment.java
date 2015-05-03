package org.croudtrip.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import com.rengwuxian.materialedittext.MaterialEditText;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.api.TripsResource;
import org.croudtrip.location.LocationUpdater;
import org.croudtrip.location.MyAutoCompleteTextView;
import org.croudtrip.location.PlaceAutocompleteAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import roboguice.fragment.provided.RoboFragment;
import roboguice.inject.InjectView;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by alex on 22.04.15.
 */
public class JoinTripFragment extends RoboFragment implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks{

    private final int REQUEST_PLACE_PICKER = 122;
    private GoogleApiClient googleApiClient;
    private PlaceAutocompleteAdapter adapter;
    private static final LatLngBounds BOUNDS_ERLANGEN = new LatLngBounds(
            new LatLng(49.5913193, 11.0097178), new LatLng(49.6498992, 10.8655223));


    @InjectView(R.id.name) private TextView tv_name;
    @InjectView(R.id.attributions) private TextView tv_attributions;
    @InjectView(R.id.address) private EditText tv_address;
    @InjectView(R.id.places) private Button btn_destination;
    @InjectView(R.id.destination) private MyAutoCompleteTextView tv_destination;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);

        if (googleApiClient == null) {
            rebuildGoogleApiClient();
        } else if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
        }



        View view = inflater.inflate(R.layout.fragment_join_trip, container, false);
        return view;
    }

    @Override
    public void onViewCreated( View view, Bundle savedInstanceState ) {
        super.onViewCreated(view, savedInstanceState);

        tv_destination.setOnItemClickListener(mAutocompleteClickListener);
        tv_destination.setThreshold(0);
        //LatLngBounds bounds = LatLngBounds.builder().include(new LatLng(locationUpdater.getLastLocation().getLatitude(), locationUpdater.getLastLocation().getLongitude())).build();
        adapter = new PlaceAutocompleteAdapter(getActivity(), android.R.layout.simple_list_item_1, BOUNDS_ERLANGEN, null);
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

        ArrayList<PlaceAutocompleteAdapter.PlaceAutocomplete> history = new ArrayList<>();
        for (int i=0; i<5; i++) {
            PlaceAutocompleteAdapter.PlaceAutocomplete a = adapter.new PlaceAutocomplete(Math.random() + " dasdasda ", Math.random() + " sda sada");
            history.add(a);
        }
        adapter.setHistory(history);

        // insert the last known location as soon as it is known
        ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(getActivity());
        locationProvider.getLastKnownLocation().observeOn(Schedulers.io())
                                               .subscribe(new Action1<Location>() {
                                                   @Override
                                                   public void call(Location location) {
                                                       LatLngBounds bounds = LatLngBounds.builder().include(new LatLng(location.getLatitude(), location.getLongitude())).build();
                                                       adapter.setBounds(bounds);
                                                   }
                                               });

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
                if (currentLocation == null) {
                    Toast.makeText(getActivity().getApplicationContext(), R.string.offer_trip_no_location, Toast.LENGTH_SHORT).show();
                    return;
                }

                // get destination from string
                LatLng destination = null;
                try {
                    List<Address> addresses = geocoder.getFromLocationName(tv_address.getText().toString(), 1);
                    if (addresses.size() > 0)
                        destination = new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
                } catch (IOException e) {
                    Toast.makeText(getActivity().getApplicationContext(), R.string.offer_trip_no_destination, Toast.LENGTH_SHORT).show();
                    return;
                }

                // no destination received
                if (destination == null) {
                    Toast.makeText(getActivity().getApplicationContext(), R.string.offer_trip_no_destination, Toast.LENGTH_SHORT).show();
                    return;
                }

                // Show the results for this search
                Bundle extras = new Bundle();
                extras.putDouble(JoinTripResultsFragment.KEY_CURRENT_LOCATION_LATITUDE,
                        currentLocation.getLatitude());
                extras.putDouble(JoinTripResultsFragment.KEY_CURRENT_LOCATION_LONGITUDE,
                        currentLocation.getLongitude());
                extras.putDouble(JoinTripResultsFragment.KEY_DESTINATION_LATITUDE, destination.latitude);
                extras.putDouble(JoinTripResultsFragment.KEY_DESTINATION_LONGITUDE, destination.longitude);

                Fragment fragment = new JoinTripResultsFragment();
                fragment.setArguments(extras);
                ((MaterialNavigationDrawer) getActivity()).setFragmentChild(
                        fragment,
                        getString(R.string.join_trip));
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


    /* Google places API stuff below here - Who wants to refactor? :D */


    private void rebuildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(getActivity().getApplicationContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Places.GEO_DATA_API)
                .build();

        googleApiClient.connect();
        Log.d("alex", "building client: " + googleApiClient + " - " + googleApiClient.isConnected());
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
            Log.d("alex", "Autocomplete item selected: " + item.description);

            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
              details about the place.
              */
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(googleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);

            Toast.makeText(getActivity().getApplicationContext(), "Clicked: " + item.description, Toast.LENGTH_SHORT).show();
            Log.d("alex", "Called getPlaceById to get Place details for " + item.placeId);
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                // Request did not complete successfully
                Log.e("alex", "Place query did not complete. Error: " + places.getStatus().toString());
                places.release();
                return;
            }
            // Get the Place object from the buffer.
            final Place place = places.get(0);

            // Format details of the place for display and show it in a TextView.
            tv_address.setText(formatPlaceDetails(getResources(), place.getName(),
                    place.getId(), place.getAddress(), place.getPhoneNumber(),
                    place.getWebsiteUri()));


            Log.d("alex", "Place details received: " + place.getName());

            places.release();
        }
    };

    private static Spanned formatPlaceDetails(Resources res, CharSequence name, String id,
                                              CharSequence address, CharSequence phoneNumber, Uri websiteUri) {
        Log.e("alex", res.getString(R.string.place_details, name, id, address, phoneNumber,
                websiteUri));
        return Html.fromHtml(res.getString(R.string.place_details, name, id, address, phoneNumber,
                websiteUri));

    }

    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.d("alex", "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());

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
        Log.d("alex", "GoogleApiClient connected.");

    }

    @Override
    public void onConnectionSuspended(int i) {
        // Connection to the API client has been suspended. Disable API access in the client.
        adapter.setGoogleApiClient(null);
        Log.e("alex", "GoogleApiClient connection suspended.");
    }
}
