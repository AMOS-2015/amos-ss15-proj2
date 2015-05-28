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

import android.location.Address;
import android.location.Geocoder;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.croudtrip.R;
import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.TripQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This adapter manages all accepted passengers in a list such that the driver can scroll
 * through them in his "My Trip" view.
 * @author Vanessa Lange
 */
public class MyTripDriverPassengersAdapter extends RecyclerView.Adapter<MyTripDriverPassengersAdapter.ViewHolder>{

    //************************** Variables ***************************//

    private Fragment fragment;
    private List<JoinTripRequest> passengers;


    //************************** Constructors ***************************//

    public MyTripDriverPassengersAdapter(Fragment fragment) {
        this.fragment = fragment;
        this.passengers = new ArrayList<JoinTripRequest>();
    }


    //**************************** Methods *****************************//

    @Override
    public MyTripDriverPassengersAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // Create new views (invoked by the layout manager)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_my_trip_driver_passengers, parent, false);

        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        JoinTripRequest joinRequest = passengers.get(position);
        TripQuery query = joinRequest.getQuery();

        // Passenger name
        User passenger = query.getPassenger();
        holder.tvPassengerName.setText(passenger.getFirstName() + " " + passenger.getLastName());

        // Passenger image/avatar
        String avatarURL = passenger.getAvatarUrl();
        if (avatarURL != null) {
            Picasso.with(fragment.getActivity()).load(avatarURL).into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.profile);
        }

        // Passenger location
        showPassengerLocation(holder, query.getPassengerRoute().getWayPoints().get(0));

        // Earnings for driver
        showEarning(holder, joinRequest.getTotalPriceInCents());
    }

    private void showPassengerLocation(ViewHolder holder, RouteLocation location) {

        holder.tvPassengerLocation.setVisibility(View.VISIBLE);

        // Receive addresses for Latitude/Longitude
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(fragment.getActivity(), Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(location.getLat(), location.getLng(), 1);

            String city = addresses.get(0).getLocality();
            String street = addresses.get(0).getThoroughfare();

            if (city == null && street == null) {
                // no data -> hide TextView
                holder.tvPassengerLocation.setVisibility(View.GONE);

            } else if (city != null && street != null) {
                // both data
                holder.tvPassengerLocation.setText(city + ", " + street);

            } else {
                // either only city of street
                String loc = (city != null) ? city : street;
                holder.tvPassengerLocation.setText(loc);
            }

        } catch (IOException e) {
            e.printStackTrace();
            holder.tvPassengerLocation.setVisibility(View.GONE);
        }
    }


    private void showEarning(ViewHolder holder, int earningsInCents) {

        String pEuros = (earningsInCents / 100) + "";
        String pCents;

        // Format cents correctly
        int cents = (earningsInCents % 100);

        if (cents == 0) {
            pCents = "00";
        } else if (cents < 10) {
            pCents = "0" + cents;
        } else {
            pCents = cents + "";
        }

        holder.tvEarnings.setText(fragment.getActivity().getString(R.string.my_trip_driver_my_earnings,
                pEuros, pCents));
    }


    @Override
    public int getItemCount() {

        if (passengers == null) {
            return 0;
        }

        return passengers.size();
    }


    /**
     * Adds the given JoinTripRequest to the adapter.
     *
     * @param additionalRequests new elements to add to the adapter
     */
    public void addRequests(List<JoinTripRequest> additionalRequests) {

        if (additionalRequests == null) {
            return;
        }

        passengers.addAll(additionalRequests);
        this.notifyDataSetChanged();
    }



    /**
     * Returns the JoinTripRequest at the specific position
     *
     * @param position the position in the adapter of the JoinTripRequest to return
     * @return the JoinTripRequest at the specific position
     */
    public JoinTripRequest getRequest(int position) {

        if (position < 0 || position >= passengers.size()) {
            return null;
        }

        return passengers.get(position);
    }


    //************************** Inner classes ***************************//


    /**
     * Provides a reference to the views for each data item.
     */
    class ViewHolder extends RecyclerView.ViewHolder{

        protected TextView tvPassengerName;
        protected TextView tvPassengerLocation;
        protected TextView tvEarnings;
        protected ImageView ivAvatar;


        public ViewHolder(View view) {
            super(view);
            this.tvPassengerName = (TextView)
                    view.findViewById(R.id.tv_my_trip_driver_passengers_passenger_name);
            this.tvPassengerLocation = (TextView)
                    view.findViewById(R.id.tv_my_trip_driver_passengers_passenger_location);
            this.tvEarnings = (TextView)
                    view.findViewById(R.id.tv_my_trip_driver_passengers_passenger_earnings);
            this.ivAvatar = (ImageView)
                    view.findViewById(R.id.iv_my_trip_driver_passengers_user_image);
        }
    }
}
