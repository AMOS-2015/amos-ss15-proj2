package org.croudtrip.trip;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.croudtrip.R;
import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;

import java.util.List;

import timber.log.Timber;

/**
 * Adapter for the JoinTripRequests-CardView/List
 * Created by Vanessa Lange on 08.05.15.
 */
public class JoinTripRequestsAdapter extends RecyclerView.Adapter<JoinTripRequestsAdapter.ViewHolder>{

    //************************** Variables ***************************//

    private Context context;
    private List<JoinTripRequest> joinRequests;

    protected OnRequestAcceptDeclineListener listener;


    //************************** Inner classes ***************************//

    public static interface OnRequestAcceptDeclineListener {
        public void onJoinRequestDecline(View view, int position);
        public void onJoinRequestAccept(View view, int position);
    }

    /**
     * Provides a reference to the views for each data item.
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        protected TextView tvPassengerName;
        protected TextView tvPassengerLocation;
        protected TextView tvEarnings;


        public ViewHolder(View view) {
            super(view);
            this.tvPassengerName = (TextView)
                    view.findViewById(R.id.tv_join_trip_requests_passenger_name);
            this.tvPassengerLocation = (TextView)
                    view.findViewById(R.id.tv_join_trip_requests_passenger_location);
            this.tvEarnings = (TextView)
                    view.findViewById(R.id.tv_join_trip_requests_earnings);

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

            if(view.getId() == R.id.btn_join_trip_request_yes){
                // Accept
                listener.onJoinRequestAccept(view, getPosition());

            }else if(view.getId() == R.id.btn_join_trip_request_no){
                // Decline
                listener.onJoinRequestDecline(view, getPosition());

            }else{
                Timber.e("Received click from unknown View with ID: " + view.getId());
            }
        }
    }


    //************************** Constructors ***************************//

    public JoinTripRequestsAdapter(Context context, List<JoinTripRequest> joinRequests) {
        this.context = context;
        this.joinRequests = joinRequests;
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

        JoinTripRequest joinRequest = joinRequests.get(position);

        // Passenger name
        User passenger = joinRequest.getQuery().getPassenger();
        holder.tvPassengerName.setText(passenger.getFirstName() + " " + passenger.getLastName());

        // Passenger location
        List<RouteLocation> passengerWayPoints = joinRequest.getQuery().getPassengerRoute().getWayPoints();
        String passengerLocation = passengerWayPoints.get(0).getLat() + " / " + passengerWayPoints.get(1).getLng();
        holder.tvPassengerLocation.setText(passengerLocation);

        // Earnings for driver
        String earnings;
        int cents = joinRequest.getTotalPriceInCents();

        if(cents < 100){
            earnings = "0," + cents;
        }else{
            earnings = (cents / 100) + "," + (cents % 100);
        }
        holder.tvEarnings.setText(context.getString(R.string.join_trip_requests_earnings, earnings));
    }

    @Override
    public int getItemCount() {

        if(joinRequests == null){
            return 0;
        }

        return joinRequests.size();
    }


    /**
     * Adds the given items to the adapter.
     * @param additionalRequests new elements to add to the adapter
     */
    public void addRequests(List<JoinTripRequest> additionalRequests){

        if(additionalRequests == null){
            return;
        }

        if(joinRequests == null){
            joinRequests = additionalRequests;
        }else{
            joinRequests.addAll(additionalRequests);
        }

        this.notifyDataSetChanged();
    }


    /**
     * Removes the JoinTripRequest at the specific position from the adapter.
     * @param position the position of the JoinTripRequest in the adapter
     * @return the removed JoinTripRequest
     */
    public JoinTripRequest removeRequest(int position){

        if(position < 0 || position >= joinRequests.size()){
            return null;
        }

        JoinTripRequest request = joinRequests.remove(position);
        this.notifyDataSetChanged();

        return request;
    }


    public void setOnRequestAcceptDeclineListener(OnRequestAcceptDeclineListener listener) {
        this.listener = listener;
    }


    /**
     * Returns the JoinTripRequest at the specific position
     * @param position the position in the adapter of the JoinTripRequest to return
     * @return the JoinTripRequest at the specific position
     */
    public JoinTripRequest getRequest(int position){

        if(position < 0 || position >= joinRequests.size()){
            return null;
        }

        return joinRequests.get(position);
    }

}