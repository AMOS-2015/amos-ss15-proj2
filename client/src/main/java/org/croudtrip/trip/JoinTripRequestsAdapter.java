package org.croudtrip.trip;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.croudtrip.R;
import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.trips.JoinTripRequest;

import java.util.List;

/**
 * Created by Vanessa Lange on 08.05.15.
 */
public class JoinTripRequestsAdapter extends RecyclerView.Adapter<JoinTripRequestsAdapter.ViewHolder> {

    //************************** Variables ***************************//

    private List<JoinTripRequest> joinRequests;


    //************************** Inner classes ***************************//

    /**
     * Provides a reference to the views for each data item.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        protected TextView tvPassengerName;
        protected TextView tvPassengerLocation;

        public ViewHolder(View view) {
            super(view);
            this.tvPassengerName = (TextView)
                    view.findViewById(R.id.tv_join_trip_requests_passenger_name);
            this.tvPassengerLocation = (TextView)
                    view.findViewById(R.id.tv_join_trip_requests_passenger_location);
        }
    }


    //************************** Constructors ***************************//

    public JoinTripRequestsAdapter(List<JoinTripRequest> joinRequests) {
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

        User passenger = joinRequest.getQuery().getPassenger();
        holder.tvPassengerName.setText(passenger.getFullName());

        Route passengerRoute = joinRequest.getQuery().getPassengerRoute();
        String passengerLocation = passengerRoute.getStart().getLat() + " / " + passengerRoute.getStart().getLng();
        holder.tvPassengerLocation.setText(passengerLocation);
    }


    /**
     * Adds the given items to the adapter.
     * @param additionalRequests new elements to add to the adapter
     */
    public void addElements(List<JoinTripRequest> additionalRequests){

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

    @Override
    public int getItemCount() {

        if(joinRequests == null){
            return 0;
        }

        return joinRequests.size();
    }
}