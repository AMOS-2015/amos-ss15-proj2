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
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.TripOffer;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * This adapter manages all drivers for a super trip in a list such that the passenger can scroll
 * through them in his "My Trip" view.
 *
 * @author Vanessa Lange
 */
public class MyTripPassengerDriversAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    //************************** Variables ***************************//

    private View header;    // map and buttons etc. are in the header view

    private static final int TYPE_HEADER = 0;           // header element
    private static final int TYPE_ITEM = 1;             // normal driver element

    private List<JoinTripRequest> drivers;


    //************************** Constructors ***************************//

    public MyTripPassengerDriversAdapter(View header) {
        this.drivers = new ArrayList<JoinTripRequest>();
        this.header = header;
    }


    //**************************** Methods *****************************//

    public View getHeader() {
        return header;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == TYPE_ITEM) {
            // Inflate item layout and pass it to view holder
            return new ItemViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cardview_my_trip_passenger_driver, parent, false));

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
            TripOffer offer = joinRequest.getOffer();

            // Driver name
            User driver = offer.getDriver();
            holder.tvDriverName.setText(driver.getFirstName() + " " + driver.getLastName());

            // Driver image/avatar
            String avatarURL = driver.getAvatarUrl();
            if (avatarURL != null) {
                Context context = holder.ivAvatar.getContext();
                Picasso.with(context).load(avatarURL).into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.profile);
            }

            // Driver car
            Vehicle vehicle = offer.getVehicle();
            holder.tvDriverCar.setText(vehicle.getType() + " " + vehicle.getLicensePlate());

            // Price to pay for this driver
            showPrice(holder.price, joinRequest.getTotalPriceInCents());


            // Change background color if driver has accepted
            int color = 0;
            if (joinRequest.getStatus() == JoinTripStatus.DRIVER_ACCEPTED
                    || joinRequest.getStatus() == JoinTripStatus.PASSENGER_IN_CAR
                    || joinRequest.getStatus() == JoinTripStatus.PASSENGER_AT_DESTINATION) {

                color = R.color.my_trip_passenger_driver_accepted;

            } else if (joinRequest.getStatus() == JoinTripStatus.DRIVER_DECLINED) {
                color = R.color.my_trip_passenger_driver_declined;
            } else {
                color = R.color.my_trip_passenger_driver_default;
            }

            if (joinRequest.getStatus() == JoinTripStatus.PASSENGER_AT_DESTINATION) {
                holder.checkmark.setVisibility(View.VISIBLE);
            } else {
                holder.checkmark.setVisibility(View.GONE);
            }

            color = holder.card.getContext().getResources().getColor(color);
            holder.card.setCardBackgroundColor(color);

        } else if (h instanceof MyTripPassengerDriversAdapter.HeaderViewHolder) {
            MyTripPassengerDriversAdapter.HeaderViewHolder holder = (MyTripPassengerDriversAdapter.HeaderViewHolder) h;
            holder.view = header;
        }
    }


    private void showPrice(TextView textView, int priceInCents) {

        String pEuros = (priceInCents / 100) + "";
        String pCents;

        // Format cents correctly
        int cents = (priceInCents % 100);

        if (cents == 0) {
            pCents = "00";
        } else if (cents < 10) {
            pCents = "0" + cents;
        } else {
            pCents = cents + "";
        }

        textView.setText(textView.getContext().getString(R.string.join_trip_results_price,
                pEuros, pCents));
    }


    @Override
    public int getItemCount() {

        if (drivers == null) {
            return 0;
        }

        return drivers.size() + 1;   // don't forget the header
    }

    public int getNumDrivers() {

        if (drivers == null) {
            return 0;
        }

        return drivers.size();
    }


    private JoinTripRequest getRequest(int position) {

        int realPosition = position - 1;

        if (realPosition < 0 || realPosition >= drivers.size()) {
            return null;
        }

        return drivers.get(realPosition);
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

        drivers.addAll(additionalRequests);
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

        drivers.add(additionalRequest);
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

        for (JoinTripRequest request : drivers) {
            if (request.getId() == requestID) {
                foundRequest = true;
                break;
            }
            index++;
        }

        if (foundRequest) {
            drivers.remove(index);
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
        return drivers.contains(joinTripRequest);
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
            Timber.e("Request is null");
            return;
        }

        boolean requestFound = false;
        for (int i = 0; i < drivers.size(); i++) {
            JoinTripRequest r = drivers.get(i);

            if (r.getId() == request.getId()) {
                drivers.set(i, request);
                requestFound = true;
                break;
            }
        }

        if (!requestFound) {
            drivers.add(request);
            requestFound = true;
        }

        if (requestFound) {
            this.notifyDataSetChanged();
        }
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

        protected TextView tvDriverName;
        protected TextView tvDriverCar;
        protected TextView price;
        protected ImageView ivAvatar;
        protected ImageView checkmark;

        protected CardView card;


        public ItemViewHolder(View view) {
            super(view);
            this.tvDriverName = (TextView)
                    view.findViewById(R.id.card_name);
            this.tvDriverCar = (TextView)
                    view.findViewById(R.id.card_car);
            this.price = (TextView)
                    view.findViewById(R.id.card_price);
            this.ivAvatar = (ImageView)
                    view.findViewById(R.id.card_icon);
            this.checkmark = (ImageView)
                    view.findViewById(R.id.iv_my_trip_passenger_drivers_left_car);

            this.card = (CardView)
                    view.findViewById(R.id.cv_my_trip_passenger_drivers);
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
