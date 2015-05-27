package org.croudtrip.fragments;

import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SVBar;

import org.croudtrip.account.VehicleManager;
import org.croudtrip.R;
import org.croudtrip.api.VehicleResource;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.account.VehicleDescription;
import org.croudtrip.utils.DataHolder;
import org.croudtrip.utils.DefaultTransformer;


import javax.inject.Inject;

import retrofit.client.Response;
import rx.Subscription;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * This fragment allows the user to enter the vehicle information and uploads this information to the server
 * @author nazeehammari
 */
public class VehicleInfoFragment extends SubscriptionFragment {

    @Inject VehicleResource vehicleResource;
    private String newCarType, newCarPlate, newColor;
    private Integer newCarCapacity;

    private EditText carTypeEdit, carPlateEdit;
    private Button colorPickerButton, capacityPickerButton, updateInfo, deleteVehicle;
    private int vehicleId = DataHolder.getInstance().getVehicle_id();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_vehicle_info, container, false);

        if (vehicleId != -1)
            getVehicle(vehicleId);   //Fetches vehicle info from the server and updates the corresponding local variables

        carTypeEdit = (EditText) view.findViewById(R.id.car_type);
        carPlateEdit = (EditText) view.findViewById(R.id.car_plate);
        capacityPickerButton = (Button) view.findViewById(R.id.capacity_picker_button);
        colorPickerButton = (Button) view.findViewById(R.id.color_picker_button);
        updateInfo = (Button) view.findViewById(R.id.update_info);
        deleteVehicle = (Button) view.findViewById(R.id.delete_vehicle);

        if (vehicleId == -1 || vehicleId == -2)
            updateInfo.setText(getString(R.string.add_vehicle));
        else
            updateInfo.setText(getString(R.string.save_changes));

        if (vehicleId == -2)
            carPlateEdit.requestFocus();


        if (vehicleId !=-1 && vehicleId != -2)
            deleteVehicle.setVisibility(View.VISIBLE);


        setFields();
        updateInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                saveCarChanges(vehicleId);
            }
        });
        deleteVehicle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                vehicleId = DataHolder.getInstance().getVehicle_id();
                removeVehicle(vehicleId);
                deleteVehicle.setVisibility(View.INVISIBLE);
                vehicleId = -1;
                updateInfo.setText(getString(R.string.add_vehicle));

            }
        });
        capacityPickerButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                showCapacityPicker();
            }
        });
        colorPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPicker();
            }
        });


        /*
        carTypeEdit.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        ((keyCode == KeyEvent.KEYCODE_ENTER))) {
                    newCarType = carTypeEdit.getText().toString();
                    return true;
                }
                return false;
            }
        });
        carTypeEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    newCarType = carTypeEdit.getText().toString();
                }
            }
        });
        */

        carTypeEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                newCarType = carTypeEdit.getText().toString();
            }
        });

        /*
        carPlateEdit.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        ((keyCode == KeyEvent.KEYCODE_ENTER))) {
                    newCarPlate = carPlateEdit.getText().toString();
                    return true;
                }
                return false;
            }
        });
        carPlateEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    newCarPlate = carPlateEdit.getText().toString();
                }
            }
        });
        */
        carPlateEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                newCarPlate = carPlateEdit.getText().toString();
            }
        });


        return view;
    }

    public void showColorPicker()
    {
        final Dialog colorDialog = new Dialog(getActivity());
        colorDialog.setTitle(Html.fromHtml("<font color='#388e3c'>Select car color</font>"));
        colorDialog.setContentView(R.layout.color_picker_dialog);
        Button set = (Button) colorDialog.findViewById(R.id.set);
        Button cancel = (Button) colorDialog.findViewById(R.id.cancel);
        final ColorPicker colorPicker = (ColorPicker) colorDialog.findViewById(R.id.color_picker);
        SVBar saturationBar = (SVBar) colorDialog.findViewById(R.id.saturation_bar);
        colorPicker.addSVBar(saturationBar);
        colorDialog.show();

        //Change divider line color
        int titleDividerId = getResources().getIdentifier("titleDivider", "id", "android");
        View titleDivider = colorDialog.findViewById(titleDividerId);
        if (titleDivider != null)
            titleDivider.setBackgroundColor(getResources().getColor(R.color.primary));

        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                colorPickerButton.setBackgroundColor(colorPicker.getColor());
                newColor = String.valueOf(colorPicker.getColor());
                colorDialog.hide();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                colorDialog.hide();
            }
        });
    }

    public void showCapacityPicker() {

        final Dialog capacityDialog = new Dialog(getActivity());
        capacityDialog.setTitle(Html.fromHtml("<font color='#388e3c'>Car Capacity</font>"));
        capacityDialog.setContentView(R.layout.capacity_picker_dialog);
        Button set = (Button) capacityDialog.findViewById(R.id.set);
        Button cancel = (Button) capacityDialog.findViewById(R.id.cancel);
        final NumberPicker capacityPicker = (NumberPicker) capacityDialog.findViewById(R.id.capacity_picker);
        capacityPicker.setMaxValue(8);
        capacityPicker.setMinValue(1);
        capacityPicker.setWrapSelectorWheel(false);
        capacityPicker.setValue(Integer.parseInt(capacityPickerButton.getText().toString()));
        setDividerColor(capacityPicker, getResources().getColor(R.color.primary));
        capacityDialog.show();

        //Change divider line color
        int titleDividerId = getResources().getIdentifier("titleDivider", "id", "android");
        View titleDivider = capacityDialog.findViewById(titleDividerId);
        if (titleDivider != null)
            titleDivider.setBackgroundColor(getResources().getColor(R.color.primary));
        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newCarCapacity = capacityPicker.getValue();
                capacityPickerButton.setText(newCarCapacity.toString());
                capacityDialog.hide();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capacityDialog.hide();
            }
        });

    }

    private void setDividerColor(NumberPicker picker, int color) {

        java.lang.reflect.Field[] pickerFields = NumberPicker.class.getDeclaredFields();
        for (java.lang.reflect.Field pf : pickerFields) {
            if (pf.getName().equals("mSelectionDivider")) {
                pf.setAccessible(true);
                try {
                    ColorDrawable colorDrawable = new ColorDrawable(color);
                    pf.set(picker, colorDrawable);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (Resources.NotFoundException e) {
                    e.printStackTrace();
                }
                catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    /*
    public void getVehicles() {
        Subscription subscription = vehicleResource.getVehicles()
                .compose(new DefaultTransformer<List<Vehicle>>())
                .subscribe(new Action1<List<Vehicle>>() {
                    @Override
                    public void call(List<Vehicle> vehicles) {
                        if (vehicles.size() > 0) {
                            Vehicle vehicle = vehicles.get(0);
                            newCarPlate = vehicle.getLicensePlate();
                            newColor = vehicle.getColor();
                            newCarCapacity = vehicle.getCapacity();
                            newCarType = vehicle.getType();
                            //Set fields to values fetched from the server
                            setFields();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Response response = ((RetrofitError) throwable).getResponse();
                        if (response != null && response.getStatus() == 401) {  // Not Authorized
                        } else {
                            Timber.e("error" + throwable.getMessage());
                        }
                        Timber.e("Couldn't get data" + throwable.getMessage());
                    }
                });

        subscriptions.add(subscription);
    }
    */


    public void getVehicle(int id) {
        Subscription subscription = vehicleResource.getVehicle(id)
                .compose(new DefaultTransformer<Vehicle>())
                .subscribe(new Action1<Vehicle>() {
                    @Override
                    public void call(Vehicle vehicle) {
                        newCarPlate = vehicle.getLicensePlate();
                        newColor = vehicle.getColor();
                        newCarCapacity = vehicle.getCapacity();
                        newCarType = vehicle.getType();
                        //Set fields to values fetched from the server
                        setFields();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        //Response response = ((RetrofitError) throwable).getResponse();
                        Timber.e("Failed to fetch with error:\n" + throwable.getMessage());
                    }
                });
        subscriptions.add(subscription);
    }


    public void addVehicle(final VehicleDescription vehicleDescription) {
        Subscription subscription = vehicleResource.addVehicle(vehicleDescription)
                .compose(new DefaultTransformer<Vehicle>())
                .subscribe(new Action1<Vehicle>() {
                    @Override
                    public void call(Vehicle vehicle) {
                            Toast.makeText(getActivity(), "New vehicle added!", Toast.LENGTH_SHORT).show();
                            Timber.v("New vehicle added!");
                        if (VehicleManager.getDefaultVehicleId(getActivity()) == -3)
                            VehicleManager.saveDefaultVehicle(getActivity(), vehicle.getId());
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        //Response response = ((RetrofitError) throwable).getResponse();
                        Timber.e("Creation failed with error:\n" + throwable.getMessage());
                    }
                });
        subscriptions.add(subscription);
    }


    public void removeVehicle(int id) {
        Subscription subscription = vehicleResource.removeVehicle(id)
                .compose(new DefaultTransformer<Response>())
                .subscribe(new Action1<Response>() {
                    @Override
                    public void call(Response response) {
                        Toast.makeText(getActivity(), "Vehicle removed!", Toast.LENGTH_SHORT).show();
                        //Set default to -3 if the user deletes the last available car
                        if (DataHolder.getInstance().getIsLast() == true)
                            VehicleManager.saveDefaultVehicle(getActivity(),-3);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        //Response response = ((RetrofitError) throwable).getResponse();
                        Timber.e("Removal failed with error:\n" + throwable.getMessage());
                    }
                });
        subscriptions.add(subscription);
    }

    public void updateVehicle(int id, VehicleDescription vehicleDescription) {
        Subscription subscription = vehicleResource.updateVehicle(id, vehicleDescription)
                .compose(new DefaultTransformer<Vehicle>())
                .subscribe(new Action1<Vehicle>() {
                    @Override
                    public void call(Vehicle vehicle) {
                        Toast.makeText(getActivity(), "Updated vehicle info", Toast.LENGTH_SHORT).show();
                        Timber.v("Updated vehicle info");
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        //Response response = ((RetrofitError) throwable).getResponse();
                        Timber.e("Update failed with error:\n" + throwable.getMessage());
                    }
                });
        subscriptions.add(subscription);
    }

    public void saveCarChanges(int vehicleId) {
        VehicleDescription vehicleDescription = new VehicleDescription(newCarPlate, newColor, newCarType, newCarCapacity);
        if (carPlateEdit.getText() != null && carPlateEdit.length() > 0) {
            if (vehicleId == -1 || vehicleId == -2)
                addVehicle(vehicleDescription);
            else if (vehicleId != 0)
                updateVehicle(vehicleId, vehicleDescription);
        }
        else
            Toast.makeText(getActivity(), "Car Plate field is mandatory", Toast.LENGTH_SHORT).show();
    }
    public void setFields() {
        if (newCarPlate!=null)
            carPlateEdit.setText(newCarPlate);
        else
        {
            newCarPlate = ("e.g 123456");
            carPlateEdit.setHint(R.string.car_plate_hint);
        }

        if (newCarType!=null)
            carTypeEdit.setText(newCarType);
        else
        {
            carTypeEdit.setHint(R.string.car_type_hint);
            newCarType="Porsche 911";
        }

        if (newColor != null)
            colorPickerButton.setBackgroundColor(Integer.parseInt(newColor));
        else
        {
            colorPickerButton.setBackgroundColor(Color.WHITE);
            newColor= String.valueOf(Color.WHITE);
        }

        if (newCarCapacity!=null)
            capacityPickerButton.setText(String.valueOf(newCarCapacity));
        else
        {
            capacityPickerButton.setText("1");
            newCarCapacity = 1;
        }
    }
    }

