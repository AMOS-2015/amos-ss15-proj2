package org.croudtrip.fragments;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;

import org.croudtrip.R;
import org.croudtrip.account.AccountManager;
import org.croudtrip.api.VehicleResource;
import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.trip.JoinTripResultsAdapter;
import org.croudtrip.utils.DataHolder;
import org.croudtrip.utils.DefaultTransformer;
import org.croudtrip.utils.VehiclesListAdapter;

import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import retrofit.RetrofitError;
import retrofit.client.Response;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import timber.log.Timber;

/**
 * This fragment shows the user's profile with the data he has entered (e.g. address, phone number
 * etc.). From here he can also edit his profile (will be transferred to fragment EditProfileFragment)
 * @author Vanessa Lange
 */
public class ProfileFragment extends SubscriptionFragment {

    //************************* Variables ***************************//
    @InjectView(R.id.vehicles_list)          private RecyclerView recyclerView;
    @Inject  VehicleResource vehicleResource;



    private RecyclerView.LayoutManager layoutManager;
    private VehiclesListAdapter adapter;
    //************************* Methods *****************************//

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the navigation drawer
        Toolbar toolbar = ((MaterialNavigationDrawer) this.getActivity()).getToolbar();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setHasOptionsMenu(true);
        final Fragment _this = this;
        final View view = inflater.inflate(R.layout.fragment_profile, container, false);
        Button addNewVehicle = (Button) view.findViewById(R.id.add_new_vehicle);

        // Restore user from SharedPref file
        User user = AccountManager.getLoggedInUser(this.getActivity().getApplicationContext());

        if(user != null) {
            //  Fill in the profile views
            String name = null;
            if (user.getFirstName() != null && user.getLastName() != null) {
                name = user.getFirstName() + " " + user.getLastName();
            } else if (user.getFirstName() != null) {
                name = user.getFirstName();
            } else if (user.getLastName() != null) {
                name = user.getLastName();
            }

            String birthYear = null;
            if(user.getBirthday() != null){
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date(user.getBirthday()));
                birthYear = calendar.get(Calendar.YEAR) + "";
            }

            String gender = null;
            if(user.getIsMale() != null){
                if(user.getIsMale()) {
                    gender = getString(R.string.profile_male);
                }else {
                    gender = getString(R.string.profile_female);
                }
            }

            setTextViewContent((TextView) view.findViewById(R.id.tv_profile_name), name);
            setTextViewContent((TextView) view.findViewById(R.id.tv_profile_email), user.getEmail());
            setTextViewContent((TextView) view.findViewById(R.id.tv_profile_phone), user.getPhoneNumber());
            setTextViewContent((TextView) view.findViewById(R.id.tv_profile_address), user.getAddress());
            setTextViewContent((TextView) view.findViewById(R.id.tv_profile_gender), gender);
            setTextViewContent((TextView) view.findViewById(R.id.tv_profile_birthyear), birthYear);

            addNewVehicle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   // ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setTarget(new VehicleInfoFragment());
                   //((MaterialNavigationDrawer) getActivity()).getCurrentSection().setTitle("Add new vehicle");
                    DataHolder.getInstance().setVehicle_id(0);
                    ((MaterialNavigationDrawer) _this.getActivity()).setFragmentChild(new VehicleInfoFragment(), "Add new vehicle");
                }
            });
            //vehicleListView.setAdapter(vehicleListAdapter);
            // Edit profile button
            FloatingActionButton editProfile = (FloatingActionButton) view.findViewById(R.id.btn_edit_profile);
            editProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((MaterialNavigationDrawer) _this.getActivity()).setFragmentChild(new EditProfileFragment(), getString(R.string.profile_edit));
                }
            });


            // download avatar
            final String avatarUrl = user.getAvatarUrl();
            if (avatarUrl != null) {
                Subscription subscription = Observable
                        .defer(new Func0<Observable<Bitmap>>() {
                    @Override
                    public Observable<Bitmap> call() {
                        try {
                            URL url = new URL(avatarUrl);
                            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                            connection.setDoInput(true);
                            connection.connect();
                            InputStream input = connection.getInputStream();
                            return Observable.just(BitmapFactory.decodeStream(input));
                        } catch (Exception e) {
                            return Observable.error(e);
                        }
                    }
                })
                        .compose(new DefaultTransformer<Bitmap>())
                        .subscribe(new Action1<Bitmap>() {
                            @Override
                            public void call(Bitmap avatar) {
                                if(avatar != null) {
                                    ((ImageView) view.findViewById(R.id.tv_profile_image)).setImageBitmap(avatar);
                                }else{
                                    Timber.d("Profile avatar is null");
                                }
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Timber.e(throwable, "Failed to download avatar");
                            }
                        });

                subscriptions.add(subscription);
            }
        }

        return view;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Use a linear layout manager to use the RecyclerView
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        adapter = new VehiclesListAdapter(getActivity(), null);
        recyclerView.setAdapter(adapter);

        Subscription subscription = vehicleResource.getVehicles()
                .compose(new DefaultTransformer<List<Vehicle>>())
                .subscribe(new Action1<List<Vehicle>>() {
                    @Override
                    public void call(List<Vehicle> vehicles) {
                        if (vehicles.size() > 0) {
                            adapter.addElements(vehicles);
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


    private void setTextViewContent(TextView tv, String content){
        if(content != null && !content.equals("")){
            tv.setText(content);
        }
    }



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        //inflater.inflate(R.menu.menu_main, menu);
    }



}
