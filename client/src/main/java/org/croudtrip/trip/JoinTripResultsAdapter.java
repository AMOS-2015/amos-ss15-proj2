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
import android.widget.TextView;

import org.croudtrip.R;
import org.croudtrip.api.account.User;
import org.croudtrip.api.trips.TripReservation;

import java.util.List;

/**
 * This Adapter is used in the JoinTripResultsFragment to display the results for a join request.
 * Created by Vanessa Lange on 01.05.15.
 */
public class JoinTripResultsAdapter extends RecyclerView.Adapter<JoinTripResultsAdapter.ViewHolder> {

    //************************** Variables ***************************//

    private final Context context;
    private List<TripReservation> reservations;

    protected OnItemClickListener listener;


    //************************** Inner classes ***************************//

    public static interface OnItemClickListener {
        public void onItemClicked(View view, int position);
    }


    /**
     * Provides a reference to the views for each data item.
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{


        protected TextView tvPrice;
        protected TextView tvDriverName;
        protected TextView tvDistance;
        protected TextView tvDuration;

        public ViewHolder(View view) {
            super(view);

            this.tvPrice = (TextView)
                    view.findViewById(R.id.tv_join_trip_results_price);
            this.tvDriverName = (TextView)
                    view.findViewById(R.id.tv_join_trip_results_driver_name);
            this.tvDistance = (TextView)
                    view.findViewById(R.id.tv_join_trip_results_distance);
            this.tvDuration = (TextView)
                    view.findViewById(R.id.tv_join_trip_results_duration);

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

    public JoinTripResultsAdapter(Context context, List<TripReservation> reservations) {
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

        TripReservation reservation = reservations.get(position);

        // Price info
        String price = reservation.getTotalPriceInCents() / 100 + ","; // euros
        int cents = reservation.getTotalPriceInCents()  % 100;

        if(cents == 0){
            price = price + "00";
        }else if(cents < 10){
            price = price + "0" + cents;
        }else{
            price = price + cents;
        }

        holder.tvPrice.setText(price + " €");


        // Driver info
        User driver = reservation.getDriver();
        holder.tvDriverName.setText(driver.getFirstName());


        // Distance information
        long distance = reservation.getQuery().getPassengerRoute().getDistanceInMeters();

        if(distance < 1000){
            holder.tvDistance.setText(context.getString(
                    R.string.join_trip_results_distance_m, distance));
        }else{
            distance = Math.round(distance / 1000.0);
            holder.tvDistance.setText(context.getString(
                    R.string.join_trip_results_distance_km, distance));
        }


        // Duration info
        long timeInMinutes = reservation.getQuery().getPassengerRoute().getDurationInSeconds() / 60;

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
    public void addElements(List<TripReservation> additionalReservations){

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
     * Returns the TripReservation at the specific position
     * @param position the position in the adapter of the item to return
     * @return the item at the specific position
     */
    public TripReservation getItem(int position){

        if(position < 0 || position >= reservations.size()){
            return null;
        }

        return reservations.get(position);
    }
}
