package org.croudtrip.utils;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.croudtrip.R;
import org.croudtrip.api.account.Vehicle;

import java.util.List;


/**
 * This Adapter is used in the ProfilePageFragment to display the list of user owned cars.
 * Created by Nazeeh Ammari on 11.05.15.
 */
public class VehiclesListSelectAdapter extends RecyclerView.Adapter<VehiclesListSelectAdapter.ViewHolder> {

    //************************** Variables ***************************//
    private final Context context;
    private List<Vehicle> vehicles;
    protected OnItemClickListener listener;

    //************************** Inner classes ***************************//

    public static interface OnItemClickListener {
        public void onItemClicked(View view, int position);
    }


    /**
     * Provides a reference to the views for each data item.
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        protected TextView carType;
        protected TextView carPlate;

        protected Button selectButton;
        public ViewHolder(View view) {
            super(view);

            this.carType = (TextView) view.findViewById(R.id.type);
            this.carPlate = (TextView) view.findViewById(R.id.plate);
            this.selectButton = (RadioButton) view.findViewById(R.id.select_vehicle);
            selectButton.setOnClickListener(this);
            view.setOnClickListener(this);
        }

        //Listener to detect to edit button and start the VehicleInfoFragment (after setting vehicleId to the clicked vehicle in DataHolder)
        @Override
        public void onClick(View view) {
            if (listener != null) {
                listener.onItemClicked(view, getPosition());
            }
            Vehicle vehicle=vehicles.get(getPosition());
            long vehicleId = vehicle.getId();
            if (view == selectButton) {
                DataHolder.getInstance().setVehicle_id((int) vehicleId);
                Toast.makeText(context, vehicleId + " was checked", Toast.LENGTH_SHORT).show();
            }
        }
    }


    //************************** Constructors ***************************//

    public VehiclesListSelectAdapter(Context context, List<Vehicle> vehicles) {
        this.context = context;
        this.vehicles = vehicles;
    }


    //**************************** Methods *****************************//

    @Override
    public VehiclesListSelectAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // Create new views (invoked by the layout manager)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_vehicles_select, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        //Set each vehicle's data to values fetched from the server
        Vehicle vehicle = vehicles.get(position);
        holder.carType.setText(context.getString(R.string.car_type_side) + vehicle.getType());
        holder.carPlate.setText(context.getString(R.string.car_plate_side) + vehicle.getLicensePlate());


        //Toast.makeText(context, holder.getPosition()+"", Toast.LENGTH_SHORT).show();
    }



    /**
     * Adds the given items to the adapter.
     * @param additionalVehicles new elements to add to the adapter
     */
    public void addElements(List<Vehicle> additionalVehicles){

        if(additionalVehicles == null){
            return;
        }

        if(vehicles == null){
            vehicles = additionalVehicles;
        }else{
            vehicles.addAll(additionalVehicles);
        }

        this.notifyDataSetChanged();
    }




    @Override
    public int getItemCount() {

        if (vehicles == null) {
            return 0;
        }

        return vehicles.size();
    }

    public void setOnItemClickListener(final OnItemClickListener listener) {
        this.listener = listener;
    }



    public Vehicle getItem(int position){

        if(position < 0 || position >= vehicles.size()){
            return null;
        }

        return vehicles.get(position);
    }
}
