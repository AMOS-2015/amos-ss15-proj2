package org.croudtrip.fragments;


import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.croudtrip.DirectionsResource;
import org.croudtrip.R;
import org.croudtrip.directions.Leg;
import org.croudtrip.directions.Location;
import org.croudtrip.directions.Route;
import org.croudtrip.directions.Step;
import org.croudtrip.server.ServerModule;
import org.croudtrip.utils.DefaultTransformer;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.functions.Action1;

/**
 * A simple {@link Fragment} subclass.
 */
public class NavigationFragment extends Fragment {

    @Inject DirectionsResource directionsResource;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        directionsResource = ServerModule.provideDirectionsResource(this.getActivity());

        MapFragment map = (MapFragment) getFragmentManager().findFragmentById(R.id.location_map);
        map.getMapAsync( new MapReady() );
        map.getMap().setOnMapClickListener( new MapListeners( map.getMap()) );

        return view;
    }

    class MapReady implements OnMapReadyCallback {
        @Override
        public void onMapReady(final GoogleMap map) {
            map.setMyLocationEnabled(true);

            // get some test direction from the server
            // TODO: Improve testing route direction
            String from = "Nuremberg, DE";
            String to = "Berlin, DE";

            directionsResource.getDirections(from, to)
                    .compose(new DefaultTransformer<List<Route>>())
                    .subscribe(new Action1<List<Route>>() {

                        @Override
                        public void call(List<Route> routes) {
                            if (routes == null || routes.isEmpty())
                                Toast.makeText(getActivity(), R.string.no_route_found, Toast.LENGTH_SHORT);

                            List<LatLng> points = new ArrayList<LatLng>();
                            for (Route r : routes) {
                                for (Leg l : r.getLegs())
                                    for (Step s : l.getSteps())
                                        points.addAll( PolyUtil.decode( s.getPolyline() ) );
                                map.addPolyline(new PolylineOptions().addAll(points) );
                            }


                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            // on main thread; something went wrong
                            Log.e("COUDTRIP ERROR", throwable.getMessage());
                        }
                    });
        }
    }

    class MapListeners implements GoogleMap.OnMapClickListener {

        GoogleMap googleMap;
        List<LatLng> markers;

        MapListeners( GoogleMap map ) {
            this.googleMap = map;
            this.markers = new ArrayList<LatLng>();
        }


        @Override
        public void onMapClick(LatLng latLng) {

            // clear map if there are more than two markers
            if( markers.size() >= 2 ){
                markers.clear();
                googleMap.clear();
            }

            if( markers.size() > 0 ) {
                googleMap.addMarker(new MarkerOptions().position(latLng)
                         .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            }
            else
            {
                googleMap.addMarker(new MarkerOptions().position(latLng)
                         .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)));
            }
            markers.add(latLng);

            if( markers.size() >= 2 ) {
                Location locFrom = new Location( markers.get(0).latitude, markers.get(0).longitude );
                Location locTo = new Location( markers.get(1).latitude, markers.get(1).longitude );

                Log.d("FROM", markers.get(0).latitude + " " + markers.get(0).longitude);
                Log.d("FROM", locFrom.getLat() + " " + locFrom.getLng());

                Log.d("TO", markers.get(1).latitude + " " + markers.get(1).longitude);
                Log.d("TO", locTo.getLat() + " " + locTo.getLng());

                final LinearLayout layout = (LinearLayout) getActivity().findViewById(R.id.progressLayout);

                layout.setVisibility( View.VISIBLE );
                directionsResource.getDirections(locFrom.getLat(), locFrom.getLng(), locTo.getLat(), locTo.getLng())
                        .compose(new DefaultTransformer<List<Route>>())
                        .subscribe(new Action1<List<Route>>() {
                            @Override
                            public void call(List<Route> routes) {
                                if (routes == null || routes.isEmpty())
                                    Toast.makeText(getActivity(), R.string.no_route_found, Toast.LENGTH_SHORT).show();

                                List<LatLng> points = new ArrayList<LatLng>();
                                for (Route r : routes) {
                                    for (Leg l : r.getLegs())
                                        for (Step s : l.getSteps())
                                            points.addAll(PolyUtil.decode(s.getPolyline()));

                                    Log.d("CROUDTRIP", "Polyline points " + points.size() );
                                    googleMap.addPolyline(new PolylineOptions().addAll(points) );
                                }

                                layout.setVisibility(View.GONE);
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                // on main thread; something went wrong
                                Log.e("COUDTRIP ERROR", throwable.getMessage());
                                Log.e("COUDTRIP ERROR", throwable.getStackTrace().toString());
                                layout.setVisibility(View.GONE);
                            }
                        });
            }

        }
    }
}
