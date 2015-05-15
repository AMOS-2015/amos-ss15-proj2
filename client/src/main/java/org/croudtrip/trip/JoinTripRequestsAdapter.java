package org.croudtrip.trip;

import android.location.Address;
import android.location.Geocoder;
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
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.fragments.JoinTripRequestsFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/**
 * Adapter for the JoinTripRequests-CardView/List
 * Created by Vanessa Lange on 08.05.15.
 */
public class JoinTripRequestsAdapter extends RecyclerView.Adapter<JoinTripRequestsAdapter.ViewHolder>
        implements OnDiversionUpdateListener {

    //************************** Variables ***************************//

    private JoinTripRequestsFragment fragment;
    private List<JoinMatch> joinMatches;

    protected OnRequestAcceptDeclineListener listener;


    //************************** Constructors ***************************//

    public JoinTripRequestsAdapter(JoinTripRequestsFragment fragment) {
        this.fragment = fragment;
        this.joinMatches = new ArrayList<JoinMatch>();
    }


    //**************************** Methods *****************************//

    @Override
    public JoinTripRequestsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // Create new views (invoked by the layout manager)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_join_trip_requests, parent, false);

        ViewHolder vh = new ViewHolder(view);
        return vh;
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        JoinMatch joinMatch = joinMatches.get(position);
        TripQuery query = joinMatch.joinRequest.getQuery();

        // Passenger name
        User passenger = query.getPassenger();
        //holder.tvPassengerName.setText(passenger.getFirstName() + " " + passenger.getLastName());
/*
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
        showEarning(holder, joinMatch.joinRequest.getTotalPriceInCents());

        // Diversion to pick up passenger
        int diversionInMinutes = joinMatch.diversionInMinutes;
        if (diversionInMinutes == -1) {
            // no data yet -> ask server
            Timber.d("Asking server for diversion");
            fragment.informAboutDiversion(joinMatch.joinRequest, this, holder.tvDiversion);

        } else {
            Timber.d("Used cached result for diversion");
            showDiversion(holder.tvDiversion, diversionInMinutes);
        }*/
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
            String featureName = addresses.get(0).getFeatureName();

            if (city == null && featureName == null) {
                // no data -> hide TextView
                holder.tvPassengerLocation.setVisibility(View.GONE);

            } else if (city != null && featureName != null && !featureName.equals(city)) {
                // both data
                holder.tvPassengerLocation.setText(city + ", " + featureName);

            } else {
                // either only city of featureName (or both the same)
                String loc = (city != null) ? city : featureName;
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

        holder.tvEarnings.setText(fragment.getActivity().getString(R.string.join_trip_requests_earnings,
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

            textView.setText(fragment.getActivity().getString(R.string.join_trip_requests_diversion_hmin,
                    hours, minutes));
        } else {
            textView.setText(fragment.getActivity().getString(R.string.join_trip_requests_diversion_min,
                    min));
        }
    }


    @Override
    public int getItemCount() {

        if (joinMatches == null) {
            return 0;
        }

        return joinMatches.size();
    }


    /**
     * Adds the given items to the adapter.
     *
     * @param additionalRequests new elements to add to the adapter
     */
    public void addRequests(List<JoinTripRequest> additionalRequests) {

        if (additionalRequests == null) {
            return;
        }

        for (JoinTripRequest joinRequest : additionalRequests) {
            this.joinMatches.add(new JoinMatch(joinRequest));
        }

        this.notifyDataSetChanged();
    }


    /**
     * Removes the JoinTripRequest at the specific position from the adapter.
     *
     * @param position the position of the JoinTripRequest in the adapter
     * @return the removed JoinTripRequest
     */
    public JoinTripRequest removeRequest(int position) {

        if (position < 0 || position >= joinMatches.size()) {
            return null;
        }

        JoinMatch match = joinMatches.remove(position);
        this.notifyDataSetChanged();

        return match.joinRequest;
    }


    public void setOnRequestAcceptDeclineListener(OnRequestAcceptDeclineListener listener) {
        this.listener = listener;
    }


    /**
     * Returns the JoinTripRequest at the specific position
     *
     * @param position the position in the adapter of the JoinTripRequest to return
     * @return the JoinTripRequest at the specific position
     */
    public JoinTripRequest getRequest(int position) {

        if (position < 0 || position >= joinMatches.size()) {
            return null;
        }

        return joinMatches.get(position).joinRequest;
    }


    @Override
    public void onDiversionUpdate(JoinTripRequest joinRequest, TextView textView, int diversionInMinutes) {
        showDiversion(textView, diversionInMinutes);

        // Cache the result
        for (JoinMatch match : joinMatches) {
            if (match.joinRequest.equals(joinRequest)) {
                match.diversionInMinutes = diversionInMinutes;
            }
        }
    }


    //************************** Inner classes ***************************//

    public interface OnRequestAcceptDeclineListener {
        void onJoinRequestDecline(View view, int position);

        void onJoinRequestAccept(View view, int position);
    }


    /**
     * Provides a reference to the views for each data item.
     */
    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        protected TextView tvPassengerName;
        protected TextView tvPassengerLocation;
        protected TextView tvEarnings;
        protected TextView tvDiversion;
        protected ImageView ivAvatar;


        public ViewHolder(View view) {
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