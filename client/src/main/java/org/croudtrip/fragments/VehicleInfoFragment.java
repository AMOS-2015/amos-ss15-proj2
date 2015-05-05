package org.croudtrip.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.larswerkman.holocolorpicker.ColorPicker;

import org.croudtrip.R;

import roboguice.fragment.provided.RoboFragment;

/**
 * This fragment allows the user to enter the vehicle information and uploads this information to the server
 * @author nazeehammari
 */
public class VehicleInfoFragment extends RoboFragment {

    Button colorPickerButton;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_vehicle_info, container, false);

        colorPickerButton = (Button) view.findViewById(R.id.color_picker_button);

        colorPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPicker();
            }
        });
        colorPickerButton.setBackgroundColor(getResources().getColor(R.color.wallet_holo_blue_light));
        return view;
    }

    public void showColorPicker()
    {
        final Dialog colorDialog = new Dialog(getActivity());
        colorDialog.setTitle("Select car color");
        colorDialog.setContentView(R.layout.color_picker_dialog);
        Button set = (Button) colorDialog.findViewById(R.id.set);
        Button cancel = (Button) colorDialog.findViewById(R.id.cancel);
        final ColorPicker colorPicker = (ColorPicker) colorDialog.findViewById(R.id.color_picker);
        colorDialog.show();

        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });


        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                colorDialog.hide();
            }
        });

    }
    }
