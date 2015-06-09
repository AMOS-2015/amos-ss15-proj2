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
import android.location.Address;
import android.location.Geocoder;
import android.support.v7.widget.CardView;
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
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.TripQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This adapter manages all accepted passengers in a list such that the driver can scroll
 * through them in his "My Trip" view.
 *
 * @author Vanessa Lange
 */
public class MyTripDriverPassengersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    //************************** Variables ***************************//

    private View header;    // map and earnings are in the header view

    private static final int TYPE_HEADER = 0;           // header element
    private static final int TYPE_ITEM = 1;             // normal passenger element

    private List<JoinTripRequest> passengers;


    //************************** Constructors ***************************//

    public MyTripDriverPassengersAdapter(View header) {
        this.passengers = new ArrayList<JoinTripRequest>();
        this.header = header;

        updateEarnings();
    }


    //**************************** Methods *****************************//

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == TYPE_ITEM) {
            // Inflate item layout and pass it to view holder
            return new ItemViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cardview_my_trip_driver_passengers, parent, false));

        } else if (viewType == TYPE_HEADER) {
            //inflate your layout and pass it to view holder
            return new HeaderViewHolder(header);
        }

        throw new RuntimeException("There is no type that matches the type " + viewType);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder h, int position) {

        if (h instanceof ItemViewHolder) {
            ItemViewHolder holder = (ItemViewHolder) h;

            JoinTripRequest joinRequest = getRequest(position);
            TripQuery query = joinRequest.getQuery();

            // Passenger name
            User passenger = query.getPassenger();
            holder.tvPassengerName.setText(passenger.getFirstName() + " " + passenger.getLastName());

            // Passenger image/avatar
            String avatarURL = passenger.getAvatarUrl();
            if (avatarURL != null) {
                Context context = holder.ivAvatar.getContext();
                Picasso.with(context).load(avatarURL).into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.profile);
            }

            // Passenger location
            showPassengerLocation(holder, query.getPassengerRoute().getWayPoints().get(0));

            // Earnings for driver
            showEarning(holder.tvEarnings, joinRequest.getTotalPriceInCents());

            // Change background color and show green check mark if destination reached
            int color = 0;
            if (joinRequest.getStatus() == JoinTripStatus.PASSENGER_AT_DESTINATION) {
                color = R.color.my_trip_driver_passenger_destination_reached;
                holder.checkmark.setVisibility(View.VISIBLE);
            } else {
                color = R.color.my_trip_driver_passenger;
                holder.checkmark.setVisibility(View.GONE);
            }

            color = holder.card.getContext().getResources().getColor(color);
            holder.card.setCardBackgroundColor(color);

        } else if (h instanceof MyTripDriverPassengersAdapter.HeaderViewHolder) {
            MyTripDriverPassengersAdapter.HeaderViewHolder holder = (MyTripDriverPassengersAdapter.HeaderViewHolder) h;
            holder.view = header;
        }


    }

    private void showPassengerLocation(ItemViewHolder holder, RouteLocation location) {

        holder.tvPassengerLocation.setVisibility(View.VISIBLE);

        // Receive addresses for Latitude/Longitude
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(holder.tvPassengerLocation.getContext(), Locale.getDefault());

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


    private void showEarning(TextView textView, int earningsInCents) {

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

        textView.setText(textView.getContext().getString(R.string.my_trip_driver_my_earnings,
                pEuros, pCents));
    }


    @Override
    public int getItemCount() {

        if (passengers == null) {
            return 0;
        }

        return passengers.size() + 1;   // don't forget the header
    }


    private JoinTripRequest getRequest(int position) {

        int realPosition = position - 1;

        if (realPosition < 0 || realPosition >= passengers.size()) {
            return null;
        }

        return passengers.get(realPosition);
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
     * Adds the given JoinTripRequest to the adapter.
     *
     * @param additionalRequest new element to add to the adapter.
     */
    public void addRequest(JoinTripRequest additionalRequest) {

        if (additionalRequest == null) {
            return;
        }

        passengers.add(additionalRequest);
        this.notifyDataSetChanged();
    }


    /**
     * Removes the given JoinTripRequest with the same ID from the adapter.
     *
     * @param requestID the ID of the JoinTripRequest to remove
     */
    public void removeRequest(long requestID) {

        boolean foundRequest = false;
        int index = 0;

        for (JoinTripRequest request : passengers) {
            if (request.getId() == requestID) {
                foundRequest = true;
                break;
            }
            index++;
        }

        if (foundRequest) {
            passengers.remove(index);
            this.notifyDataSetChanged();
        }
    }


    /**
     * Checks whether the given JoinTripRequest is in the adapter
     *
     * @param joinTripRequest the JoinTripRequest to search for
     * @return true if the request is in the adapter, otherwise false
     */
    public boolean contains(JoinTripRequest joinTripRequest) {
        return passengers.contains(joinTripRequest);
    }


    /**
     * Searches for a JoinTripRequest with the same ID as the given request
     * and replaces the request with the given one. If the request isn't in the list yet, it is
     * simply added.
     *
     * @param request the request to update
     */
    public void updateRequest(JoinTripRequest request) {

        if (request == null) {
            return;
        }

        boolean requestFound = false;
        for (int i = 0; i < passengers.size(); i++) {
            JoinTripRequest r = passengers.get(i);

            if (r.getId() == request.getId()) {
                passengers.set(i, request);
                requestFound = true;
                break;
            }
        }

        if (!requestFound) {
            passengers.add(request);
            requestFound = true;
        }

        if (requestFound) {
            this.notifyDataSetChanged();
        }
    }


    /**
     * Shows the total earnings to the driver
     *
     * @param totalEarningsInCent
     */
    private void setTotalEarnings(int totalEarningsInCent) {
        TextView earnings = (TextView) header.findViewById(R.id.tv_my_trip_driver_earnings);
        showEarning(earnings, totalEarningsInCent);
        notifyDataSetChanged();
    }


    public void updateEarnings() {

        int totalEarnings = 0;

        if (passengers != null) {
            for (JoinTripRequest request : passengers) {
                totalEarnings += request.getTotalPriceInCents();
            }
        }

        setTotalEarnings(totalEarnings);
    }


    private boolean isPositionHeader(int position) {
        return position == 0;
    }


    @Override
    public int getItemViewType(int position) {

        if (isPositionHeader(position))
            return TYPE_HEADER;

        return TYPE_ITEM;
    }


    //************************** Inner classes ***************************//


    /**
     * Provides a reference to the views for each data item.
     */
    class ItemViewHolder extends RecyclerView.ViewHolder {

        protected TextView tvPassengerName;
        protected TextView tvPassengerLocation;
        protected TextView tvEarnings;
        protected ImageView ivAvatar;

        protected CardView card;
        protected ImageView checkmark;


        public ItemViewHolder(View view) {
            super(view);
            this.tvPassengerName = (TextView)
                    view.findViewById(R.id.tv_my_trip_driver_passengers_passenger_name);
            this.tvPassengerLocation = (TextView)
                    view.findViewById(R.id.tv_my_trip_driver_passengers_passenger_location);
            this.tvEarnings = (TextView)
                    view.findViewById(R.id.tv_my_trip_driver_passengers_passenger_earnings);
            this.ivAvatar = (ImageView)
                    view.findViewById(R.id.iv_my_trip_driver_passengers_user_image);

            this.card = (CardView)
                    view.findViewById(R.id.cv_my_trip_driver_passengers);
            this.checkmark = (ImageView)
                    view.findViewById(R.id.iv_my_trip_driver_passengers_reached_destination);
        }
    }

    /**
     * Provides a reference to the header view
     */
    class HeaderViewHolder extends RecyclerView.ViewHolder {

        protected View view;

        public HeaderViewHolder(View view) {
            super(view);
            this.view = view;
        }
    }
}
