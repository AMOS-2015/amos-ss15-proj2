package org.croudtrip.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;

import org.croudtrip.R;
import org.croudtrip.account.AccountManager;
import org.croudtrip.api.account.User;
import org.croudtrip.api.account.UserDescription;
import org.croudtrip.utils.DefaultTransformer;
import org.croudtrip.api.UsersResource;

import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;



import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import timber.log.Timber;


/**
 * This fragment allows the user to edit their profile information (e.g. name, profile picture, address,
 * etc.).
 * @author Nazeeh Ammari
 */
public class EditProfileFragment extends roboguice.fragment.provided.RoboFragment {

    @Inject
    private UsersResource usersResource;


    //************************* Variables ***************************//
    private String email, password = null, newFirstName, newLastName, newNumber, newAddress;
    String tempFirstName, tempLastName, tempNumber, tempAddress;
    Boolean newGenderIsMale, tempGender;
    Integer newYearOfBirth, tempYearOfBirth;
    Long newBirthDay;
    String profileImageUrl, tempUrl;

    ImageView profilePicture;
    EditText  firstNameEdit, lastNameEdit, phoneNumberEdit, addressEdit;
    RadioGroup genderRadio;
    Button yearPickerButton, save, discard;
    FloatingActionButton editProfileImage;

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

        //Get EditTexts & Buttons
        firstNameEdit = (EditText)view.findViewById(R.id.first_name);
        lastNameEdit = (EditText)view.findViewById(R.id.last_name);
        phoneNumberEdit = (EditText)view.findViewById(R.id.edit_profile_phone);
        addressEdit = (EditText)view.findViewById(R.id.edit_profile_address);
        yearPickerButton=(Button)view.findViewById(R.id.year_picker_button);
        discard = (Button) view.findViewById(R.id.discard);
        save = (Button) view.findViewById(R.id.save);
        editProfileImage = (FloatingActionButton) view.findViewById(R.id.btn_edit_profile_image);
        genderRadio = (RadioGroup) view.findViewById(R.id.radioGender);

        //Get the ImageView and fill it with the profile picture from SharedPrefs (Uri)
        // TODO
        profilePicture = (ImageView)view.findViewById(R.id.profile_picture_edit);
        /*
        if (prefs.getString(Constants.SHARED_PREF_KEY_PROFILE_IMAGE_URI,null) != null) {
            profileImageUri = Uri.parse(prefs.getString(Constants.SHARED_PREF_KEY_PROFILE_IMAGE_URI,null));
        }
        */

        final Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        // Restore user from SharedPref file

        this.user = AccountManager.getLoggedInUser(this.getActivity().getApplicationContext());

        if(user != null) {
            if (user.getEmail() != null) {
                email=user.getEmail();
            }
            // download avatar
            if (user.getAvatarUrl() != null) {
                profileImageUrl = user.getAvatarUrl();
                tempUrl = user.getAvatarUrl();
                Observable
                        .defer(new Func0<Observable<Bitmap>>() {
                            @Override
                            public Observable<Bitmap> call() {
                                try {
                                    URL url = new URL(user.getAvatarUrl());
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
                                    profilePicture.setImageBitmap(avatar);
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
            }
            else
            {
            }
            //Fill EditText fields
            if (user.getFirstName() != null) {
                firstNameEdit.setText(user.getFirstName());
                tempFirstName = user.getFirstName();
                newFirstName = user.getFirstName();
            }
            else
            {
                firstNameEdit.setText("Unknown");
                tempFirstName = "Unknown";
                newFirstName = "Unknown";
            }

            if (user.getLastName() != null) {
                lastNameEdit.setText(user.getLastName());
                tempLastName = user.getLastName();
                newLastName = user.getLastName();
            }
            else
            {
                lastNameEdit.setText("Unknown");
                tempLastName = "Unknown";
                newLastName="Unknown";
            }

            if (user.getPhoneNumber() != null) {
                phoneNumberEdit.setText(user.getPhoneNumber());
                tempNumber = user.getPhoneNumber();
                newNumber=user.getPhoneNumber();
            }
            else
            {
                phoneNumberEdit.setText("0");
                tempNumber = "0";
                newNumber="0";
            }

            if (user.getAddress()!=null) {
                addressEdit.setText(user.getAddress());
                tempAddress = user.getAddress();
                newAddress=user.getAddress();
            }
            else {
                addressEdit.setText("Unknown");
                tempAddress = "Unknown";
                newAddress="Unknown";
            }

            if (user.getIsMale() != null) {
                tempGender = user.getIsMale();
                newGenderIsMale = user.getIsMale();
                if (user.getIsMale()) {
                    genderRadio.check(R.id.radio_male);
                }
                else
                {
                    genderRadio.check(R.id.radio_female);
                }
            }
            else
            {
                genderRadio.check(R.id.radio_male);
                tempGender = true;
                newGenderIsMale = true;
            }

            if (user.getBirthday() != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date(user.getBirthday()));
                yearPickerButton.setText(calendar.get(Calendar.YEAR) + "");
                tempYearOfBirth = calendar.get(Calendar.YEAR);
                newYearOfBirth = calendar.get(Calendar.YEAR);
            }
            else
            {
                yearPickerButton.setText("2015");
                tempYearOfBirth = 2015;
                newYearOfBirth = 2015;
            }
        }

        //Listeners for EditTexts, save the string to a variable when Enter is pressed or focus is changed
        //Name
        firstNameEdit.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    newFirstName = firstNameEdit.getText().toString();
                    return true;
                }
                return false;
            }
        });

        firstNameEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    newFirstName = firstNameEdit.getText().toString();
                }
            }
        });


        lastNameEdit.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    newLastName = lastNameEdit.getText().toString();
                    return true;
                }
                return false;
            }
        });

        lastNameEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    newLastName = lastNameEdit.getText().toString();
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

        genderRadio.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton checkedButton = (RadioButton) group.findViewById(checkedId);
                newGenderIsMale = checkedButton.getText().equals("Male");
            }
        });

    //Year picker button
    yearPickerButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showYearPicker();
        }
    });
        // Discard changes button
        discard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discardProfileChanges();
            }
        });


        // save changes button
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfileChanges();
            }
        });

        // change profile image button
        editProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Save the previous picture Uri in a temporary variable and clear the ImageView while the user selects an image (maybe not the best practice)
                /*
                BitmapDrawable bd = (BitmapDrawable) profilePicture.getDrawable();
                bd.getBitmap().recycle();
                profilePicture.setImageBitmap(null);
                */

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
            //profileImageUri = data.getData();
            //profilePicture.setImageURI(profileImageUri);
        } else {
            //Fill the ImageView with the previous image in case of failure
            //profilePicture.setImageURI(tempUri);
            //Toast.makeText(getActivity(), "An error occurred while getting the picture, please try again", Toast.LENGTH_SHORT)
            //        .show();
        }
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        //inflater.inflate(R.menu.menu_main, menu);
    }

    public void discardProfileChanges() {
        firstNameEdit.setText(tempFirstName);
        lastNameEdit.setText(tempLastName);
        phoneNumberEdit.setText(tempNumber);
        addressEdit.setText(tempAddress);
        if (tempGender)
            genderRadio.check(R.id.radio_male);
        else
            genderRadio.check(R.id.radio_female);

        yearPickerButton.setText(tempYearOfBirth+"");
        Toast.makeText(getActivity(), "Changes discarded", Toast.LENGTH_SHORT)
                .show();
    }
    public void saveProfileChanges() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, newYearOfBirth);
        newBirthDay = calendar.getTime().getTime();

        user = new User(
                user.getId(),
                user.getEmail(),
                newFirstName,
                newLastName,
                newNumber,
                newGenderIsMale,
                newBirthDay,
                newAddress,
                profileImageUrl,
                0
        );

        AccountManager.saveUser(getActivity().getApplicationContext(), user, null);
        Toast.makeText(getActivity(), "Profile Updated!", Toast.LENGTH_SHORT)
                .show();
        UserDescription userDescription = new UserDescription(email, newFirstName, newLastName, password,newNumber,newGenderIsMale,newBirthDay,newAddress,"some url");
        updateUser(userDescription);
    }

    public void showYearPicker() {

        final Dialog yearDialog = new Dialog(getActivity());
        yearDialog.setTitle("Year of Birth");
        yearDialog.setContentView(R.layout.year_picker_dialog);
        Button set = (Button) yearDialog.findViewById(R.id.set);
        Button cancel = (Button) yearDialog.findViewById(R.id.cancel);
        final NumberPicker yearPicker = (NumberPicker) yearDialog.findViewById(R.id.year_picker);
        yearPicker.setMaxValue(2015);
        yearPicker.setMinValue(1920);
        yearPicker.setWrapSelectorWheel(false);
        yearPicker.setValue(Integer.parseInt(yearPickerButton.getText().toString()));
        yearDialog.show();

        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newYearOfBirth = yearPicker.getValue();
                yearPickerButton.setText(newYearOfBirth.toString());
                yearDialog.hide();
            }
        });


        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                yearDialog.hide();
            }
        });

    }

    public void updateUser (final UserDescription userDescription)
    {
        usersResource.updateUser(userDescription)
                .compose(new DefaultTransformer<User>())
                .subscribe(new Action1<User>() {
                    @Override
                    public void call(User user) {
                        Timber.v("Updated user info");
                    }

                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        //Response response = ((RetrofitError) throwable).getResponse();
                        Timber.e("Update failed with error:\n" + throwable.getMessage());
                    }
                });
    }
    public void onDestroy() {
        super.onDestroy();
        /*
        if (profileImageUri != null) {
            //Recycle the bitmap inside the profile picture ImageView to avoid Out of Memory errors
            BitmapDrawable bd = (BitmapDrawable) profilePicture.getDrawable();
            bd.getBitmap().recycle();
            profilePicture.setImageBitmap(null);
        }
        */
    }
}


