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
import org.croudtrip.api.trips.JoinTripRequest;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Adapter for the JoinTripRequests-CardView/List
 * Created by Vanessa Lange on 08.05.15.
 */
public class JoinTripRequestsAdapter extends RecyclerView.Adapter<JoinTripRequestsAdapter.ViewHolder>{

    //************************** Variables ***************************//

    private Context context;
    private List<JoinMatch> joinMatches;

    protected OnRequestAcceptDeclineListener listener;

    //private DirectionsResource dirResource;


    //************************** Constructors ***************************//

    public JoinTripRequestsAdapter(Context context/*, DirectionsResource dirResource*/) {

        this.context = context;
        //this.dirResource = dirResource;
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

        // Passenger name
        User passenger = joinMatch.joinRequest.getQuery().getPassenger();
        holder.tvPassengerName.setText(passenger.getFirstName() + " " + passenger.getLastName());

        // Passenger location
        //List<RouteLocation> passengerWayPoints = joinRequest.getQuery().getPassengerRoute().getWayPoints();
        //String passengerLocation = passengerWayPoints.get(0).getLat() + " / " + passengerWayPoints.get(1).getLng();
        holder.tvPassengerLocation.setText("Nuremberg");//TODO

        // Earnings for driver
        showEarning(holder, joinMatch.joinRequest.getTotalPriceInCents());

        // Diversion to pick up passenger
        int diversionInMeters = joinMatch.diversionInMeters;
        if(diversionInMeters == -1){
            // no data yet -> ask server
            // TODO
            showDiversion(holder, 42000);
        }else{
            showDiversion(holder, diversionInMeters);
        }
    }

    private void showEarning(ViewHolder holder, int earningsInCents){

        String pEuros = (earningsInCents / 100) + "";
        String pCents;

        // Format cents correctly
        int cents = (earningsInCents % 100);

        if(cents == 0){
            pCents = "00";
        }else if(cents < 10){
            pCents = "0" + cents;
        }else{
            pCents = cents + "";
        }

        holder.tvEarnings.setText(context.getString(R.string.join_trip_requests_earnings, pEuros, pCents));
    }


    private void showDiversion(ViewHolder holder, int diversionInMeters){

        if(diversionInMeters >= 1000){
            // round to km
            holder.tvDiversion.setText(context.getString(R.string.join_trip_requests_diversion_km,
                    Math.round(diversionInMeters / 1000.0)));
        }else{
            // just m
            holder.tvDiversion.setText(context.getString(R.string.join_trip_requests_diversion_m,
                    diversionInMeters));
        }
    }


    @Override
    public int getItemCount() {

        if(joinMatches == null){
            return 0;
        }

        return joinMatches.size();
    }


    /**
     * Adds the given items to the adapter.
     * @param additionalRequests new elements to add to the adapter
     */
    public void addRequests(List<JoinTripRequest> additionalRequests){

        if(additionalRequests == null){
            return;
        }

        for(JoinTripRequest joinRequest : additionalRequests){
            this.joinMatches.add(new JoinMatch(joinRequest));
        }

        this.notifyDataSetChanged();
    }


    /**
     * Removes the JoinTripRequest at the specific position from the adapter.
     * @param position the position of the JoinTripRequest in the adapter
     * @return the removed JoinTripRequest
     */
    public JoinTripRequest removeRequest(int position){

        if(position < 0 || position >= joinMatches.size()){
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
     * @param position the position in the adapter of the JoinTripRequest to return
     * @return the JoinTripRequest at the specific position
     */
    public JoinTripRequest getRequest(int position){

        if(position < 0 || position >= joinMatches.size()){
            return null;
        }

        return joinMatches.get(position).joinRequest;
    }



    //************************** Inner classes ***************************//

    public interface OnRequestAcceptDeclineListener {
        void onJoinRequestDecline(View view, int position);
        void onJoinRequestAccept(View view, int position);
    }

    /**
     * Provides a reference to the views for each data item.
     */
    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        protected TextView tvPassengerName;
        protected TextView tvPassengerLocation;
        protected TextView tvEarnings;
        protected TextView tvDiversion;


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


    /**
     * A simple class to keep data together
     */
   private class JoinMatch {

       private JoinTripRequest joinRequest;
       private int diversionInMeters;

       public JoinMatch(JoinTripRequest joinRequest){
           this.joinRequest = joinRequest;
           this.diversionInMeters = -1;
       }
   }
}