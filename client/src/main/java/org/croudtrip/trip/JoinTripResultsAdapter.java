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

package org.croudtrip.trip;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.croudtrip.R;
import org.croudtrip.api.account.User;
import org.croudtrip.api.trips.SuperTripReservation;
import org.croudtrip.api.trips.TripReservation;

import java.util.List;

/**
 * This Adapter is used in the JoinTripResultsFragment to display the results for a join request.
 * Created by Vanessa Lange on 01.05.15.
 */
public class JoinTripResultsAdapter extends RecyclerView.Adapter<JoinTripResultsAdapter.ViewHolder> {

    //************************** Variables ***************************//

    private final Context context;
    private List<SuperTripReservation> reservations;

    protected OnItemClickListener listener;


    //************************** Inner classes ***************************//

    public static interface OnItemClickListener {
        public void onItemClicked(View view, int position);
    }


    /**
     * Provides a reference to the views for each data item.
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{


        protected TextView tvPrice, tvDuration, tvDistance;
        protected Button btJoin;
        protected LinearLayout llDrivers;

        public ViewHolder(View view) {
            super(view);

            this.tvPrice = (TextView) view.findViewById(R.id.trip_price);
            this.tvDuration = (TextView) view.findViewById(R.id.trip_duration);
            this.tvDistance = (TextView) view.findViewById(R.id.trip_distance);
            this.btJoin = (Button) view.findViewById(R.id.trip_join);
            this.llDrivers = (LinearLayout) view.findViewById(R.id.trip_drivers);


            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (listener != null) {
                listener.onItemClicked(view, getPosition());
            }
        }
    }


    //************************** Constructors ***************************//

    public JoinTripResultsAdapter(Context context, List<SuperTripReservation> reservations) {
        this.context = context;
        this.reservations = reservations;
    }


    //**************************** Methods *****************************//

    @Override
    public JoinTripResultsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // Create new views (invoked by the layout manager)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_join_trip_results, parent, false);

        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        SuperTripReservation superTripReservation = reservations.get(position);

        int totalPrice = 0;
        for (TripReservation reservation : superTripReservation.getReservations()) {
            totalPrice += reservation.getTotalPriceInCents();
        }
        // Price info
        String price = totalPrice / 100 + ","; // euros
        int cents = totalPrice  % 100;


        if(cents == 0){
            price = price + "00";
        }else if(cents < 10){
            price = price + "0" + cents;
        }else{
            price = price + cents;
        }

        holder.tvPrice.setText(price + " €");


        // Driver info
        User driver = superTripReservation.getReservations().get(0).getDriver();
        //holder.tvDriverName.setText(driver.getFirstName());


        // Distance information
        long distance = superTripReservation.getQuery().getPassengerRoute().getDistanceInMeters();

        if(distance < 1000){
            holder.tvDistance.setText(context.getString(
                    R.string.join_trip_results_distance_m, distance));
        }else{
            distance = Math.round(distance / 1000.0);
            holder.tvDistance.setText(context.getString(
                    R.string.join_trip_results_distance_km, distance));
        }


        // Duration info
        long timeInMinutes = superTripReservation.getQuery().getPassengerRoute().getDurationInSeconds() / 60;

        if(timeInMinutes < 60){
            holder.tvDuration.setText(context.getString(R.string.join_trip_results_duration_min,
                    timeInMinutes));
        }else{
            holder.tvDuration.setText(context.getString(R.string.join_trip_results_duration_hmin,
                    timeInMinutes / 60, timeInMinutes % 60));
        }
    }


    /**
     * Adds the given items to the adapter.
     * @param additionalReservations new elements to add to the adapter
     */
    public void addElements(List<SuperTripReservation> additionalReservations){

        if(additionalReservations == null){
            return;
        }

        if(reservations == null){
            reservations = additionalReservations;
        }else{
            reservations.addAll(additionalReservations);
        }

        this.notifyDataSetChanged();
    }


    @Override
    public int getItemCount() {

        if (reservations == null) {
            return 0;
        }

        return reservations.size();
    }

    public void setOnItemClickListener(final OnItemClickListener listener) {
        this.listener = listener;
    }


    /**
     * Returns the reservation at the specific position
     * @param position the position in the adapter of the item to return
     * @return the item at the specific position
     */
    public SuperTripReservation getItem(int position){

        if(position < 0 || position >= reservations.size()){
            return null;
        }

        return reservations.get(position);
    }
}
