package org.croudtrip.fragments;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.croudtrip.DirectionsResource;
import org.croudtrip.R;
import org.croudtrip.UsersResource;
import org.croudtrip.directions.Route;

import java.util.ArrayList;
import java.util.List;

import retrofit.RestAdapter;
import retrofit.converter.JacksonConverter;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * A simple {@link Fragment} subclass.
 */
public class NavigationFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        MapFragment map = (MapFragment) getFragmentManager().findFragmentById(R.id.location_map);
        map.getMapAsync( new MapReady() );

        return view;
    }

    class MapReady implements OnMapReadyCallback {

        @Override
        public void onMapReady(GoogleMap map) {
            map.setMyLocationEnabled(true);

            // get some test direction from the server
            String from = "Nuremberg, DE";
            String to = "Erlangen, DE";

            // TODO: call the server and do a directions request and read the polyline out of it and draw it on the map
            DirectionsResource directionsResource = new RestAdapter.Builder().setEndpoint(getResources().getString( R.string.server_address ))
                    .setConverter(new JacksonConverter())
                    .build()
                    .create(DirectionsResource.class);


            final GoogleMap googleMap = map;
            directionsResource.getDirections(from, to)
                              .subscribeOn(Schedulers.io())
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribe( new Action1<List<Route>>() {

                                  @Override
                                  public void call(List<Route> routes) {
                                    if( routes == null || routes.isEmpty() )
                                        Toast.makeText(getActivity(), R.string.no_route_found, Toast.LENGTH_SHORT);

                                    for( Route r : routes ) {
                                        googleMap.addPolyline( new PolylineOptions().addAll( PolyUtil.decode( r.getPolyline() ) ) );
                                    }
                                  }
                              });
        }
    }
}
