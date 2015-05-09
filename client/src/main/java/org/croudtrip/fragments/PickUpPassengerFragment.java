package org.croudtrip.fragments;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.croudtrip.R;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import roboguice.fragment.provided.RoboFragment;
import roboguice.inject.InjectView;

/**
 * This Fragment should be shown when both driver and passenger confirmed a trip and hence the
 * driver should receive a reminder to pick the passenger up at a certain location. Also the
 * price is displayed.
 * Created by Vanessa Lange on 02.05.15.
 */
public class PickUpPassengerFragment extends SubscriptionFragment {

    //************************** Variables ***************************//

    public final static String KEY_PASSENGER_NAME = "passenger_name";
    public final static String KEY_PASSENGER_LATITUDE = "passenger_latitude";
    public final static String KEY_PASSENGER_LONGITUDE = "passenger_longitude";
    public final static String KEY_PASSENGER_PRICE = "passenger_price";

    @InjectView(R.id.tv_pick_up_passenger)          private TextView nameTextView;
    @InjectView(R.id.tv_pick_up_passenger_price)    private TextView priceTextView;


    //************************** Methods *****************************//

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toolbar toolbar = ((MaterialNavigationDrawer) this.getActivity()).getToolbar();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_pick_up_passenger, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        int passengerPrice = args.getInt(KEY_PASSENGER_PRICE);
        String passengerName = args.getString(KEY_PASSENGER_NAME,
                getString(R.string.offer_trip_pick_up_passenger_default));

        String price = passengerPrice / 100 + ","; // euros
        int cents = passengerPrice % 100;

        if(cents == 0){
            price = price + "00";
        }else if(cents < 10){
            price = price + "0" + cents;
        }else{
            price = price + cents;
        }

        String location = args.getDouble(KEY_PASSENGER_LATITUDE, 0) + "/"
                + args.getDouble(KEY_PASSENGER_LONGITUDE, 0);

        nameTextView.setText(getString(R.string.offer_trip_pick_up_passenger, passengerName, location));
        priceTextView.setText(getString(R.string.offer_trip_pick_up_price, price));
    }
}
