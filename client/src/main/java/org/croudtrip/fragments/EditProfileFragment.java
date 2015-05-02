package org.croudtrip.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.account.AccountManager;
import org.croudtrip.account.User;


/**
 * This fragment allows the user to edit their profile information (e.g. name, profile picture, address,
 * etc.).
 * @author Nazeeh Ammari
 */
public class EditProfileFragment extends Fragment {


    //************************* Variables ***************************//
    ImageView profilePicture;
    EditText  profileNameEdit, phoneNumberEdit, addressEdit;
    String newName, newNumber, newAddress;
    String tempName, tempNumber, tempAddress;
    Uri profileImageUri, tempUri;

    private User user;


    //************************* Methods *****************************//
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        //Get EditTexts
        profileNameEdit = (EditText)view.findViewById(R.id.edit_profile_name);
        phoneNumberEdit = (EditText)view.findViewById(R.id.edit_profile_phone);
        addressEdit = (EditText)view.findViewById(R.id.edit_profile_address);

        //Get the ImageView and fill it with the profile picture from SharedPrefs (Uri)
        // TODO
        profilePicture = (ImageView)view.findViewById(R.id.profile_picture_edit);
        /*
        if (prefs.getString(Constants.SHARED_PREF_KEY_PROFILE_IMAGE_URI,null) != null) {
            profileImageUri = Uri.parse(prefs.getString(Constants.SHARED_PREF_KEY_PROFILE_IMAGE_URI,null));
        }
        */
        if (profileImageUri != null) {
            profilePicture.setImageURI(profileImageUri);
            tempUri = profileImageUri;
        }
        else
        {
            profilePicture.setImageResource(R.drawable.background_drawer);
        }
        final Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        // Restore user from SharedPref file

        this.user = AccountManager.getLoggedInUser(this.getActivity().getApplicationContext());


        if(user != null) {
            String name = null;
            if (user.getFirstName() != null && user.getLastName() != null) {
                name = user.getFirstName() + " " + user.getLastName();
            } else if (user.getFirstName() != null) {
                name = user.getFirstName();
            } else if (user.getLastName() != null) {
                name = user.getLastName();
            }

            //Fill EditText fields
            if (name != null) {
                profileNameEdit.setHint(name);
                tempName = name;
            }
            else
            {
                profileNameEdit.setHint("Unknown");
                tempName = "Unknown";
            }

            if (user.getPhoneNumber() != null) {
                phoneNumberEdit.setHint(user.getPhoneNumber());
                tempNumber = user.getPhoneNumber();
            }
            else
            {
                phoneNumberEdit.setHint("Unknown");
                tempNumber = "Unknown";
            }

            if (user.getAddress()!=null) {
                addressEdit.setHint(user.getAddress());
                tempAddress = user.getAddress();
            }
            else {
                addressEdit.setHint("Unknown");
                tempAddress = "Unknown";
            }

            //Prepare variables to save them in SharedPrefs
            newName = name;
            newNumber = user.getPhoneNumber();
            newAddress = user.getAddress();
        }

        //Listeners for EditTexts, save the string to a variable when Enter is pressed or focus is changed
        //Name
        profileNameEdit.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    newName = profileNameEdit.getText().toString();
                    return true;
                }
                return false;
            }
        });

        profileNameEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    newName = profileNameEdit.getText().toString();
                }
            }
        });


        //Phone Number
        phoneNumberEdit.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    newNumber = phoneNumberEdit.getText().toString();
                    return true;
                }
                return false;
            }
        });
        phoneNumberEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    newNumber = phoneNumberEdit.getText().toString();
                }
            }
        });

        //Address
        addressEdit.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    newAddress = addressEdit.getText().toString();
                    return true;
                }
                return false;
            }
        });
        addressEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    newAddress = addressEdit.getText().toString();
                }
            }
        });

        // Discard changes button
        Button discard = (Button) view.findViewById(R.id.discard);
        discard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileNameEdit.setHint(tempName);
                profileNameEdit.setText("");
                phoneNumberEdit.setHint(tempNumber);
                phoneNumberEdit.setText("");
                addressEdit.setHint(tempAddress);
                addressEdit.setText("");
                Toast.makeText(getActivity(), "Changes discarded", Toast.LENGTH_SHORT)
                        .show();
            }
        });


        // save changes button
        Button save = (Button) view.findViewById(R.id.save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfileChanges();
            }
        });

        // change profile image button
        FloatingActionButton editProfileImage = (FloatingActionButton) view.findViewById(R.id.btn_edit_profile_image);
        editProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Save the previous picture Uri in a temporary variable and clear the ImageView while the user selects an image (maybe not the best practice)

                tempUri = profileImageUri;
                BitmapDrawable bd = (BitmapDrawable) profilePicture.getDrawable();
                bd.getBitmap().recycle();
                profilePicture.setImageBitmap(null);


                //Open Gallery
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, 100);
            }
        });

        return view;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            //Fill the ImageView
            profileImageUri = data.getData();
            profilePicture.setImageURI(profileImageUri);
        } else {
            //Fill the ImageView with the previous image in case of failure
            profilePicture.setImageURI(tempUri);
            Toast.makeText(getActivity(), "An error occurred while getting the picture, please try again", Toast.LENGTH_SHORT)
                    .show();
        }
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        //inflater.inflate(R.menu.menu_main, menu);
    }

    public void saveProfileChanges() {

        // TODO: put all changes into the user object properly
        user = new User(
                user.getId(),
                user.getEmail(),
                newName,
                null,
                newNumber,
                user.getIsMale(),
                user.getBirthDay(),
                newAddress,
                null,
                0 // TODO get actual last modified timestamp
        );

        //TODO: maybe pass a new password to the method
        AccountManager.saveUser(getActivity().getApplicationContext(), user, null);

        SharedPreferences prefs = this.getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (profileImageUri != null) {
            editor.putString(Constants.SHARED_PREF_KEY_PROFILE_IMAGE_URI, profileImageUri.toString());
        }
        editor.apply();
        Toast.makeText(getActivity(), "Profile Updated!", Toast.LENGTH_SHORT)
                .show();
    }

    public void onDestroy() {
        super.onDestroy();
        if (profileImageUri != null) {
            //Recycle the bitmap inside the profile picture ImageView to avoid Out of Memory errors
            BitmapDrawable bd = (BitmapDrawable) profilePicture.getDrawable();
            bd.getBitmap().recycle();
            profilePicture.setImageBitmap(null);
        }
    }
}


