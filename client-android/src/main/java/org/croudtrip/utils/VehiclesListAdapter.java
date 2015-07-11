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

package org.croudtrip.utils;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import org.croudtrip.R;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.fragments.VehicleInfoFragment;

import java.util.ArrayList;
import java.util.List;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import timber.log.Timber;

/**
 * This Adapter is used in the ProfilePageFragment to display the list of user owned cars.
 * Created by Nazeeh Ammari on 11.05.15.
 */
public class VehiclesListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    //************************** Variables ***************************//

    private final static int ITEM_TYPE_HEADER = 0; // avatar etc. view
    private final static int ITEM_TYPE_ITEM = 1;   // cars

    private final Context context;
    private List<Vehicle> vehicles;
    private View header;
    protected OnItemClickListener listener;

    //************************** Inner classes ***************************//

    public interface OnItemClickListener {
        void onItemClicked(View view, int position);
    }


    /**
     * Provides a reference to the car views for each data item.
     */
    public class CarViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        protected TextView carType;
        protected TextView carPlate;
        protected TextView carCapacity;
        protected Button carColor;
        protected ImageButton edit;

        public CarViewHolder(View view) {
            super(view);

            this.carType = (TextView) view.findViewById(R.id.type);
            this.carPlate = (TextView) view.findViewById(R.id.plate);
            this.carCapacity = (TextView) view.findViewById(R.id.capacity);
            this.carColor = (Button) view.findViewById(R.id.car_color);
            this.edit = (ImageButton) view.findViewById(R.id.edit_vehicle);
            edit.setOnClickListener(this);
            view.setOnClickListener(this);
        }

        // Listener to detect to edit button and start the VehicleInfoFragment
        // (after setting vehicleId to the clicked vehicle in DataHolder)
        @Override
        public void onClick(View view) {

            if (listener != null) {
                listener.onItemClicked(view, getPosition());
            }

            Vehicle vehicle = vehicles.get(getPosition());
            long vehicleId = vehicle.getId();

            if (view == edit) {
                DataHolder.getInstance().setVehicle_id((int) vehicleId);
                ((MaterialNavigationDrawer) context).setFragmentChild(new VehicleInfoFragment(), "Edit Vehicle Info");
                Timber.v("Clicked on vehicle " + vehicleId);
            }
        }
    }


    /**
     * Provides a reference to the header view
     */
    public class HeaderViewHolder extends RecyclerView.ViewHolder {

        protected View header;

        public HeaderViewHolder(View header) {
            super(header);

            this.header = header;
        }
    }


    //************************** Constructors ***************************//

    public VehiclesListAdapter(Context context, List<Vehicle> vehicles, View header) {
        this.context = context;
        this.header = header;

        if (vehicles != null) {
            this.vehicles = vehicles;
        } else {
            this.vehicles = new ArrayList<Vehicle>();
        }
    }


    //**************************** Methods *****************************//

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // Create new views (invoked by the layout manager)
        if (viewType == ITEM_TYPE_ITEM) {
            return new CarViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cardview_vehicles_list, parent, false));

        } else if (viewType == ITEM_TYPE_HEADER) {
            return new HeaderViewHolder(header);
        }

        throw new RuntimeException("There is no type that matches the type " + viewType);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder h, int position) {
        Timber.d("FOUND ELEMENT!!!!!!!!!!!!!!");

        if (h instanceof CarViewHolder) {
            Timber.d("Found car element");
            CarViewHolder holder = (CarViewHolder) h;

            //Set each vehicle's data to values fetched from the server
            Vehicle vehicle = vehicles.get(position - 1);   // -1 because of header
            holder.carType.setText(context.getString(R.string.car_type_side) + vehicle.getType());
            holder.carPlate.setText(context.getString(R.string.car_plate_side) + vehicle.getLicensePlate());
            holder.carCapacity.setText(context.getString(R.string.car_capacity_side) + vehicle.getCapacity() + "");
            holder.carColor.setBackgroundColor(Integer.parseInt(vehicle.getColor()));

        } else if (h instanceof VehiclesListAdapter.HeaderViewHolder) {
            Timber.d("Found header element");
            VehiclesListAdapter.HeaderViewHolder holder = (VehiclesListAdapter.HeaderViewHolder) h;
            holder.header = header;
        }
    }


    /**
     * Adds the given items to the adapter.
     *
     * @param additionalVehicles new elements to add to the adapter
     */
    public void addElements(List<Vehicle> additionalVehicles) {

        if (additionalVehicles == null) {
            return;
        }

        if (vehicles == null) {
            vehicles = additionalVehicles;
        } else {
            vehicles.addAll(additionalVehicles);
        }

        this.notifyDataSetChanged();
    }


    @Override
    public int getItemCount() {
        return vehicles.size() + 1; // header
    }


    @Override
    public int getItemViewType(int position) {

        if (position == 0) {
            return ITEM_TYPE_HEADER;

        }

        return ITEM_TYPE_ITEM;
    }

}
