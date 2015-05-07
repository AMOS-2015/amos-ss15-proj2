package org.croudtrip.trip;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.croudtrip.R;
import org.croudtrip.api.account.User;
import org.croudtrip.api.trips.TripReservation;

import java.util.List;

/**
 * This Adapter is used in the JoinTripResultsActivity to display the results for a join request.
 * Created by Vanessa Lange on 01.05.15.
 */
public class JoinTripResultsAdapter extends ArrayAdapter<TripReservation>{

    public JoinTripResultsAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public JoinTripResultsAdapter(Context context, int resource, List<TripReservation> items) {
        super(context, resource, items);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;

        if(view == null){
            // Set row layout
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(R.layout.listview_row_join_trip_results, null);
        }

        TripReservation reservation = getItem(position);
        if(reservation == null){
            return view;
        }


        // Insert price information
        TextView priceTextView = (TextView) view.findViewById(R.id.tv_join_trip_results_price);

        String price = reservation.getTotalPriceInCents() / 100 + ","; // euros
        int cents = reservation.getTotalPriceInCents()  % 100;

        if(cents == 0){
            price = price + "00";
        }else if(cents < 10){
            price = price + "0" + cents;
        }else{
            price = price + cents;
        }

        priceTextView.setText(getContext().getString(R.string.join_trip_results_costs, price));


        // Insert driver information
        TextView driverNameTextView = (TextView) view.findViewById(R.id.tv_join_trip_results_driver_name);
        User driver = reservation.getDriver();

        String driverName = null;
        if (driver.getFirstName() != null && driver.getLastName() != null) {
            driverName = driver.getFirstName() + " " + driver.getLastName();
        } else if (driver.getFirstName() != null) {
            driverName = driver.getFirstName();
        } else if (driver.getLastName() != null) {
            driverName = driver.getLastName();
        }

        driverNameTextView.setText(driverName);


        // Insert distance information
        TextView distanceTextView = (TextView) view.findViewById(R.id.tv_join_trip_results_distance);
        long distance = reservation.getQuery().getRoute().getDistanceInMeters();

        if(distance < 1000){
            distanceTextView.setText(getContext().getString(
                    R.string.join_trip_results_distance_m, distance));
        }else{

            distance = Math.round(distance / 1000.0);
            distanceTextView.setText(getContext().getString(
                    R.string.join_trip_results_distance_km, distance));
        }


        // Insert time information
        TextView timeTextView = (TextView) view.findViewById(R.id.tv_join_trip_results_time);
        long timeInMinutes = reservation.getQuery().getRoute().getDurationInSeconds() / 60;

        if(timeInMinutes < 60){
            timeTextView.setText(getContext().getString(R.string.join_trip_results_duration_min,
                    timeInMinutes));
        }else{
            timeTextView.setText(getContext().getString(R.string.join_trip_results_duration_hmin,
                    timeInMinutes / 60, timeInMinutes % 60));
        }


        return view;
    }
}
