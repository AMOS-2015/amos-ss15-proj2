package org.croudtrip.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;

import org.croudtrip.R;
import org.croudtrip.activities.LoginActivity;
import org.croudtrip.auth.User;

import java.util.Calendar;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;

/**
 * This fragment shows the user's profile with the data he has entered (e.g. address, phone number
 * etc.). From here he can also edit his profile (will be transferred to fragment EditProfileFragment)
 * @author Vanessa Lange
 */
public class ProfileFragment extends Fragment {

    //************************* Variables ***************************//

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

        // Restore user from SharedPref file
        User user = LoginActivity.getLoggedInUser(this.getActivity().getApplicationContext());

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
            if(user.getBirthDay() != null){
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(user.getBirthDay());
                birthYear = calendar.get(Calendar.YEAR) + "";
            }

            setTextViewContent((TextView) view.findViewById(R.id.tv_profile_name), name);
            setTextViewContent((TextView) view.findViewById(R.id.tv_profile_email), user.getEmail());
            setTextViewContent((TextView) view.findViewById(R.id.tv_profile_phone), user.getPhoneNumber());
            setTextViewContent((TextView) view.findViewById(R.id.tv_profile_address), user.getAddress());
            setTextViewContent((TextView) view.findViewById(R.id.tv_profile_gender),
                    (user.getIsMale()) ? getString(R.string.profile_male) : getString(R.string.profile_female));
            setTextViewContent((TextView) view.findViewById(R.id.tv_profile_birthyear), birthYear);


            // Edit profile button
            FloatingActionButton editProfile = (FloatingActionButton) view.findViewById(R.id.btn_edit_profile);
            editProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((MaterialNavigationDrawer) _this.getActivity()).setFragmentChild(new EditProfileFragment(), getString(R.string.profile_edit));
                }
            });
        }

        return view;
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
