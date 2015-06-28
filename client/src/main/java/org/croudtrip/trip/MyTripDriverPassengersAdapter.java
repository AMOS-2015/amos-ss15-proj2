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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.croudtrip.R;
import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.fragments.offer.MyTripDriverFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/**
 * This adapter manages all accepted passengers in a list such that the driver can scroll
 * through them in his "My Trip" view.
 *
 * @author Vanessa Lange
 */
public class MyTripDriverPassengersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements OnDiversionUpdateListener{

    //************************** Variables ***************************//

    private View header;    // map and earnings are in the header view

    private static final int TYPE_HEADER = 0;           // header element
    private static final int TYPE_ITEM = 1;             // normal passenger element
    private static final int TYPE_PENDING_ITEM = 2;     // pending passenger element

    private List<JoinTripRequest> passengers;
    private List<JoinMatch> pendingPassengers;

    protected OnRequestAcceptDeclineListener listener;
    private MyTripDriverFragment fragment;  // needs to be informed after swipe


    //************************** Constructors ***************************//

    public MyTripDriverPassengersAdapter(MyTripDriverFragment fragment, View header) {
        this.passengers = new ArrayList<JoinTripRequest>();
        this.pendingPassengers = new ArrayList<JoinMatch>();
        this.header = header;
        this.fragment = fragment;

        updateEarnings();
    }


    //**************************** Methods *****************************//

    public View getHeader(){
        return header;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // Inflate item layout and pass it to view holder
        if (viewType == TYPE_ITEM) {
            return new ItemViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cardview_my_trip_driver_passengers, parent, false));

        } else if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(header);

        }else if (viewType == TYPE_PENDING_ITEM) {
            return new PendingItemViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cardview_join_trip_requests, parent, false));
        }

        throw new RuntimeException("There is no type that matches the type " + viewType);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder h, int position) {

        if (h instanceof ItemViewHolder) {
            ItemViewHolder holder = (ItemViewHolder) h;

            JoinTripRequest joinRequest = passengers.get(position - 1 - pendingPassengers.size());
            TripQuery query = joinRequest.getSuperTrip().getQuery();

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
            showPassengerLocation(holder.tvPassengerLocation, query.getPassengerRoute().getWayPoints().get(0));

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

        }else if (h instanceof MyTripDriverPassengersAdapter.PendingItemViewHolder) {
            PendingItemViewHolder holder = (PendingItemViewHolder) h;

            JoinMatch joinMatch = pendingPassengers.get(position - 1);  // -1 because of header
            TripQuery query = joinMatch.joinRequest.getSuperTrip().getQuery();

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
            showPassengerLocation(holder.tvPassengerLocation, query.getPassengerRoute().getWayPoints().get(0));

            // Earnings for driver
            showEarning(holder.tvEarnings, joinMatch.joinRequest.getTotalPriceInCents());

            // Diversion to pick up passenger
            int diversionInMinutes = joinMatch.diversionInMinutes;
            if (diversionInMinutes == -1) {
                // no data yet -> ask server
                Timber.d("Asking server for diversion");
                fragment.informAboutDiversion(joinMatch.joinRequest, this, holder.tvDiversion);

            } else {
                Timber.d("Used cached result for diversion");
                showDiversion(holder.tvDiversion, diversionInMinutes);
            }
        }
    }

    private void showPassengerLocation(TextView tvLocation, RouteLocation location) {

        tvLocation.setVisibility(View.VISIBLE);

        // Receive addresses for Latitude/Longitude
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(tvLocation.getContext(), Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(location.getLat(), location.getLng(), 1);

            String city = addresses.get(0).getLocality();
            String street = addresses.get(0).getThoroughfare();

            if (city == null && street == null) {
                // no data -> hide TextView
                tvLocation.setVisibility(View.GONE);

            } else if (city != null && street != null) {
                // both data
                tvLocation.setText(city + ", " + street);

            } else {
                // either only city of street
                String loc = (city != null) ? city : street;
                tvLocation.setText(loc);
            }

        } catch (IOException e) {
            e.printStackTrace();
            tvLocation.setVisibility(View.GONE);
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

    private void showDiversion(TextView textView, int diversionInMinutes) {

        String minutes;

        int min = diversionInMinutes % 60;

        if (diversionInMinutes >= 60) {
            String hours = diversionInMinutes / 60 + "";

            if (min == 0) {
                minutes = "00";
            } else if (min < 10) {
                minutes = "0" + min;
            } else {
                minutes = min + "";
            }

            textView.setText(textView.getContext().getString(R.string.join_trip_requests_diversion_hmin,
                    hours, minutes));
        } else {
            textView.setText(textView.getContext().getString(R.string.join_trip_requests_diversion_min,
                    min));
        }
    }


    @Override
    public void onDiversionUpdate(JoinTripRequest joinRequest, TextView textView, int diversionInMinutes) {
        showDiversion(textView, diversionInMinutes);

        // Cache the result
        for (JoinMatch match : pendingPassengers) {
            if (match.joinRequest.equals(joinRequest)) {
                match.diversionInMinutes = diversionInMinutes;
                break;
            }
        }

        notifyDataSetChanged();
    }


    @Override
    public int getItemCount() {
        return passengers.size() + pendingPassengers.size() + 1;   // 1 for header
    }

    public int getPassengersCount(){
        return passengers.size();
    }

    public int getPendingPassengersCount(){
        return pendingPassengers.size();
    }


    private JoinTripRequest getPassenger(int position) {

        if (position < 0 || position >= passengers.size()) {
            return null;
        }

        return passengers.get(position);
    }

    /**
     * Returns the JoinTripRequest at the specific position
     *
     * @param position the position in the adapter of the JoinTripRequest to return
     * @return the JoinTripRequest at the specific position
     */
    public JoinTripRequest getPendingPassenger(int position) {

        if (position < 0 || position >= pendingPassengers.size()) {
            return null;
        }

        return pendingPassengers.get(position).joinRequest;
    }


    /**
     * Adds the given JoinTripRequest to the adapter.
     *
     * @param passengers new elements to add to the adapter
     */
    public void addPassengers(List<JoinTripRequest> passengers) {

        if (passengers == null) {
            return;
        }

        this.passengers.addAll(passengers);
        this.notifyDataSetChanged();
    }


    /**
     * Adds the given JoinTripRequest to the adapter.
     *
     * @param passenger new element to add to the adapter.
     */
    public void addPassenger(JoinTripRequest passenger) {

        if (passenger == null) {
            return;
        }

        passengers.add(passenger);
        this.notifyDataSetChanged();
    }

    /**
     * Searches for a JoinTripRequest (pending passenger) with the same ID as additionalPendingPassenger
     * and replaces the request with the given one. If the request isn't in the list yet, it is
     * simply added.
     *
     * @param additionalPendingPassenger new elements to add to the adapter
     */
    public void updatePendingPassenger(JoinTripRequest additionalPendingPassenger) {

        if (additionalPendingPassenger == null) {
            return;
        }

        boolean requestFound = false;
        for (int i = 0; i < pendingPassengers.size(); i++) {
            JoinTripRequest r = pendingPassengers.get(i).joinRequest;

            if (r.getId() == additionalPendingPassenger.getId()) {
                if(r.equals(additionalPendingPassenger)){
                    // Nothing changed, no need to update
                    return;
                }

                pendingPassengers.set(i, new JoinMatch(additionalPendingPassenger));
                requestFound = true;
                break;
            }
        }

        if (!requestFound) {
            pendingPassengers.add(new JoinMatch(additionalPendingPassenger));
            requestFound = true;
        }

        if (requestFound) {
            this.notifyDataSetChanged();
        }
    }


    /**
     * Removes the JoinTripRequest with the same requestID from the adapter.
     * @param requestID
     */
    public void removePendingPassenger(long requestID) {

        boolean foundRequest = false;
        int index = 0;

        for (JoinMatch request : pendingPassengers) {
            if (request.joinRequest.getId() == requestID) {
                foundRequest = true;
                break;
            }
            index++;
        }

        if (foundRequest) {
            pendingPassengers.remove(index);
            this.notifyDataSetChanged();
        }
    }


    /**
     * Removes the given JoinTripRequest with the same ID from the adapter.
     *
     * @param requestID the ID of the JoinTripRequest to remove
     */
    public void removePassenger(long requestID) {

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
    public boolean containsPassenger(JoinTripRequest joinTripRequest) {
        return passengers.contains(joinTripRequest);
    }


    /**
     * Searches for a JoinTripRequest with the same ID as the given request
     * and replaces the request with the given one. If the request isn't in the list yet, it is
     * simply added.
     *
     * @param request the request to update
     */
    public void updatePassenger(JoinTripRequest request) {

        if (request == null) {
            return;
        }

        boolean requestFound = false;
        for (int i = 0; i < passengers.size(); i++) {
            JoinTripRequest r = passengers.get(i);

            if (r.getId() == request.getId()) {

                if(r.equals(request)){
                    // Nothing changed, no need to update
                    return;
                }

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

    public boolean isPositionPendingPassenger(int position) {
        return position - 1 >= 0 && position - 1 < pendingPassengers.size();
    }


    @Override
    public int getItemViewType(int position) {

        if (isPositionHeader(position)) {
            return TYPE_HEADER;

        }else if(isPositionPendingPassenger(position)){
            return TYPE_PENDING_ITEM;
        }

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


    public interface OnRequestAcceptDeclineListener {
        void onJoinRequestDecline(View view, int position);
        void onJoinRequestAccept(View view, int position);
    }

    public void setOnRequestAcceptDeclineListener(OnRequestAcceptDeclineListener listener) {
        this.listener = listener;
    }

    /**
     * Provides a reference to a pending passenger
     */
    class PendingItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        protected TextView tvPassengerName;
        protected TextView tvPassengerLocation;
        protected TextView tvEarnings;
        protected TextView tvDiversion;
        protected ImageView ivAvatar;


        public PendingItemViewHolder(View view) {
            super(view);
            this.tvPassengerName = (TextView)
                    view.findViewById(R.id.tv_join_trip_requests_passenger_name);
            this.tvPassengerLocation = (TextView)
                    view.findViewById(R.id.tv_join_trip_requests_passenger_location);
            this.tvEarnings = (TextView)
                    view.findViewById(R.id.tv_join_trip_requests_earnings);
            this.tvDiversion = (TextView)
                    view.findViewById(R.id.tv_join_trip_requests_diversion);
            this.ivAvatar = (ImageView)
                    view.findViewById(R.id.iv_join_trip_requests_user_image);

            // Get notified if the user accepts or declines a request
            ImageButton acceptButton = (ImageButton)
                    view.findViewById(R.id.btn_join_trip_request_yes);
            ImageButton declineButton = (ImageButton)
                    view.findViewById(R.id.btn_join_trip_request_no);

            acceptButton.setOnClickListener(this);
            declineButton.setOnClickListener(this);
        }



        @Override
        public void onClick(View view) {

            if (listener == null) {
                return;
            }

            if (view.getId() == R.id.btn_join_trip_request_yes) {
                // Accept
                listener.onJoinRequestAccept(view, getPosition());

            } else if (view.getId() == R.id.btn_join_trip_request_no) {
                // Decline
                listener.onJoinRequestDecline(view, getPosition());

            } else {
                Timber.e("Received click from unknown View with ID: " + view.getId());
            }
        }
    }

    /**
     * A simple class to keep data together
     */
    private class JoinMatch {

        private JoinTripRequest joinRequest;
        private int diversionInMinutes;

        public JoinMatch(JoinTripRequest joinRequest) {
            this.joinRequest = joinRequest;
            this.diversionInMinutes = -1;
        }
    }

}
