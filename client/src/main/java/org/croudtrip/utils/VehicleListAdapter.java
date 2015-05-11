package org.croudtrip.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.croudtrip.R;
import org.croudtrip.api.account.Vehicle;

import java.util.List;

/**
 * This Adapter is used in the Profile Page to show information about user's own vehicles.
 * Created by Nazeeh Ammari on 12.05.15.
 */
public class VehicleListAdapter extends ArrayAdapter<List> {

    //************************** Variables ***************************//

    private final Context context;
    private List<Vehicle> vehicles;


    //************************** Constructors ***************************//

    public VehicleListAdapter(Context context, List<Vehicle> vehicles) {
        super(context, R.layout.vehicle_info_row);
        this.context = context;
        this.vehicles = vehicles;
    }

    //**************************** Methods *****************************//

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.vehicle_info_row, parent, false);
        TextView carType = (TextView) view.findViewById(R.id.type);
        carType.setText("some car type");
        return view;
    }

}
