package org.croudtrip.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.croudtrip.R;

/**
 * This fragment allows the user to enter the vehicle information and uploads this information to the server
 * @author nazeehammari
 */
public class VehicleInfoFragment extends Fragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_vehicle_info, container, false);
        return view;
    }
    }
