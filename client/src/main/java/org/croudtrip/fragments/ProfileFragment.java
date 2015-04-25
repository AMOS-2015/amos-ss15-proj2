package org.croudtrip.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;

import org.croudtrip.R;
import org.croudtrip.UsersResource;
import org.croudtrip.activities.RegistrationActivity;
import org.croudtrip.auth.User;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.JacksonConverter;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by alex on 22.04.15.
 * @author Vanessa Lange
 */
public class ProfileFragment extends Fragment {

    //************************* Variables ***************************//

    private TextView name;
    private TextView email;
    private TextView phone;
    private TextView address;
    private TextView gender;
    private TextView birthyear;
    private ImageView image;

    private ProgressBar progressBar;

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
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        name = (TextView) view.findViewById(R.id.tv_profile_name);
        email = (TextView) view.findViewById(R.id.tv_profile_email);
        phone = (TextView) view.findViewById(R.id.tv_profile_phone);
        address = (TextView) view.findViewById(R.id.tv_profile_address);
        gender = (TextView) view.findViewById(R.id.tv_profile_gender);
        birthyear = (TextView) view.findViewById(R.id.tv_profile_birthyear);
        image = (ImageView) view.findViewById(R.id.tv_profile_image);

        progressBar = (ProgressBar) view.findViewById(R.id.pb_profile);

        // Edit profile button
        FloatingActionButton editProfile = (FloatingActionButton) view.findViewById(R.id.btn_edit_profile);
        editProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MaterialNavigationDrawer) _this.getActivity()).setFragmentChild(new EditProfileFragment(), getString(R.string.profile_edit));
            }
        });

        loadProfile();

        return view;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        //inflater.inflate(R.menu.menu_main, menu);
    }


    private void loadProfile() {
        final String serverAddress = getResources().getString( R.string.server_address );
        Timber.i("Loading profile data from server");

        // Get profile from server
        UsersResource usersResource = new RestAdapter.Builder()
            .setEndpoint(serverAddress)
            .setConverter(new JacksonConverter())
            .setRequestInterceptor(new RequestInterceptor() {

                @Override
                public void intercept(RequestFacade request) {
                    RegistrationActivity.addAuthorizationHeader(getActivity().getApplicationContext(), request);
                }
            })
            .build()
            .create(UsersResource.class);

        usersResource.getUser().subscribeOn( Schedulers.io() )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<User>() {

                @Override
                public void call(User user) {
                    // SUCCESS

                    // ---- UI ----
                    // Hide progress bar
                    progressBar.setVisibility(View.GONE);

                    setTextViewContent(name, user.getFirstName() + " " + user.getLastName());
                    setTextViewContent(email, user.getEmail());
                    setTextViewContent(phone, user.getPhoneNumber());
                    setTextViewContent(address, user.getAddress());
                    setTextViewContent(gender, (user.getIsMale()) ? getString(R.string.profile_male) : getString(R.string.profile_female));
                    setTextViewContent(birthyear, /*user.getBirthDay().getYear()*/ "todo"); // TODO

                    // TODO: image
                }

            }, new Action1<Throwable>() {

                @Override
                public void call(Throwable throwable) {

                    // ---- UI ----
                    // Hide progress bar
                    progressBar.setVisibility(View.GONE);

                    Timber.e(throwable.getMessage());
                    Toast.makeText(getActivity().getApplicationContext(), getString(R.string.profile_error_general), Toast.LENGTH_LONG).show();
                }
            });

    }

    private void setTextViewContent(TextView tv, String content){

        if(content == null || content.equals("")){
            tv.setText(getString(R.string.profile_unknown));
        }else{
            tv.setText(content);
        }
    }

}
