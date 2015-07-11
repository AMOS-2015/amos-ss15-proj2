/*
 * The CroudTrip! application aims at revolutionizing the car-ride-sharing market with its easy,
 * user-friendly and highly automated way of organizing shared Trips. Copyright (C) 2015  Nazeeh Ammari,
 *  Philipp Eichhorn, Ricarda Hohn, Vanessa Lange, Alexander Popp, Frederik Simon, Michael Weber
 * This program is free software: you can redistribute it and/or modify  it under the terms of the GNU
 *  Affero General Public License as published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *    If not, see http://www.gnu.org/licenses/.
 */

package org.croudtrip.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
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
import com.pnikosis.materialishprogress.ProgressWheel;
import com.squareup.picasso.Picasso;

import org.croudtrip.R;
import org.croudtrip.account.AccountManager;
import org.croudtrip.api.AvatarsUploadResource;
import org.croudtrip.api.UsersResource;
import org.croudtrip.api.account.User;
import org.croudtrip.api.account.UserDescription;
import org.croudtrip.utils.CrashCallback;
import org.croudtrip.utils.CrashPopup;
import org.croudtrip.utils.DefaultTransformer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import retrofit.mime.TypedFile;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import timber.log.Timber;


/**
 * This fragment allows the user to edit their profile information (e.g. name, profile picture, address,
 * etc.).
 *
 * @author Nazeeh Ammari
 */
public class EditProfileFragment extends SubscriptionFragment {

    @Inject
    private UsersResource usersResource;
    @Inject
    private AvatarsUploadResource avatarsUploadResource;

    @InjectView(R.id.pb_edit_profile)
    private ProgressWheel progressBar;


    //************************* Variables ***************************//
    private String email, password = null, newFirstName, newLastName, newNumber, newAddress;
    private String tempFirstName, tempLastName, tempNumber, tempAddress;
    private Boolean newGenderIsMale, tempGender;
    private Integer newYearOfBirth, tempYearOfBirth;
    private Date newBirthDay;
    private String profileImageUrl, tempUrl;

    private ImageView profilePicture;
    private EditText firstNameEdit, lastNameEdit, phoneNumberEdit, addressEdit;
    private RadioGroup genderRadio;
    private Button yearPickerButton, save, discard;
    private FloatingActionButton editProfileImage;

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
        firstNameEdit = (EditText) view.findViewById(R.id.first_name);
        lastNameEdit = (EditText) view.findViewById(R.id.last_name);
        phoneNumberEdit = (EditText) view.findViewById(R.id.edit_profile_phone);
        addressEdit = (EditText) view.findViewById(R.id.edit_profile_address);
        yearPickerButton = (Button) view.findViewById(R.id.year_picker_button);
        discard = (Button) view.findViewById(R.id.discard);
        save = (Button) view.findViewById(R.id.save);
        editProfileImage = (FloatingActionButton) view.findViewById(R.id.btn_edit_profile_image);
        genderRadio = (RadioGroup) view.findViewById(R.id.radioGender);

        //Get the ImageView and fill it with the profile picture from SharedPrefs (Uri)
        // TODO
        profilePicture = (ImageView) view.findViewById(R.id.profile_picture_edit);
        /*
        if (prefs.getString(Constants.SHARED_PREF_KEY_PROFILE_IMAGE_URI,null) != null) {
            profileImageUri = Uri.parse(prefs.getString(Constants.SHARED_PREF_KEY_PROFILE_IMAGE_URI,null));
        }
        */

        final Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        // Restore user from SharedPref file

        this.user = AccountManager.getLoggedInUser(this.getActivity().getApplicationContext());

        if (user != null) {
            if (user.getEmail() != null) {
                email = user.getEmail();
            }
            // download avatar
            if (user.getAvatarUrl() != null) {
                profileImageUrl = user.getAvatarUrl();
                tempUrl = user.getAvatarUrl();
                Picasso.with(getActivity()).load(profileImageUrl).error(R.drawable.background_drawer).into(profilePicture);
                Timber.i("Profile image was downloaded and set");
                /*
                Observable
                        .defer(new Func0<Observable<Bitmap>>() {
                            @Override
                            public Observable<Bitmap> call() {
                                try {
                                    URL url = new URL(user.getAvatarUrl());
                                    Timber.i("The avatar url is: " + user.getAvatarUrl());
                                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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
                                if (avatar != null) {
                                    profilePicture.setImageBitmap(avatar);
                                } else {
                                    Timber.d("Profile avatar is null");
                                }
                            }
                        }, new CrashCallback(getActivity()));
                        */
            }

            //Fill EditText fields
            if (user.getFirstName() != null) {
                firstNameEdit.setText(user.getFirstName());
                tempFirstName = user.getFirstName();
                newFirstName = user.getFirstName();
            } else {
                firstNameEdit.setText("Unknown");
                tempFirstName = "Unknown";
                newFirstName = "Unknown";
            }

            if (user.getLastName() != null) {
                lastNameEdit.setText(user.getLastName());
                tempLastName = user.getLastName();
                newLastName = user.getLastName();
            } else {
                lastNameEdit.setText("Unknown");
                tempLastName = "Unknown";
                newLastName = "Unknown";
            }

            if (user.getPhoneNumber() != null) {
                phoneNumberEdit.setText(user.getPhoneNumber());
                tempNumber = user.getPhoneNumber();
                newNumber = user.getPhoneNumber();
            } else {
                phoneNumberEdit.setText("0");
                tempNumber = "0";
                newNumber = "0";
            }

            if (user.getAddress() != null) {
                addressEdit.setText(user.getAddress());
                tempAddress = user.getAddress();
                newAddress = user.getAddress();
            } else {
                addressEdit.setText("Unknown");
                tempAddress = "Unknown";
                newAddress = "Unknown";
            }

            if (user.getIsMale() != null) {
                tempGender = user.getIsMale();
                newGenderIsMale = user.getIsMale();
                if (user.getIsMale()) {
                    genderRadio.check(R.id.radio_male);
                } else {
                    genderRadio.check(R.id.radio_female);
                }
            } else {
                genderRadio.check(R.id.radio_male);
                tempGender = true;
                newGenderIsMale = true;
            }

            if (user.getBirthday() != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(user.getBirthday());
                yearPickerButton.setText(calendar.get(Calendar.YEAR) + "");
                tempYearOfBirth = calendar.get(Calendar.YEAR);
                newYearOfBirth = calendar.get(Calendar.YEAR);
            } else {
                yearPickerButton.setText("Unknown");
                tempYearOfBirth = null;
                newYearOfBirth = null;
            }
        }

        //Listeners for EditTexts, save the string to a variable when Enter is pressed or focus is changed
        //Name
        firstNameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                newFirstName = firstNameEdit.getText().toString();
            }
        });


        lastNameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                newLastName = lastNameEdit.getText().toString();
            }
        });


        phoneNumberEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                newNumber = phoneNumberEdit.getText().toString();
            }
        });


        //Address
        addressEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                newAddress = addressEdit.getText().toString();
            }
        });


        genderRadio.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
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

    //Handle the selected image and upload it to the server
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            //Get the image path from the intent result
            String selectedImagePath = null;
            Uri selectedImageUri = data.getData();
            Cursor cursor = getActivity().getContentResolver().query(
                    selectedImageUri, null, null, null, null);
            if (cursor == null) {
                selectedImagePath = selectedImageUri.getPath();
            } else {
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                selectedImagePath = cursor.getString(idx);
            }


            File uncompressedImage = new File(selectedImagePath);
            File compressedImage = new File("/sdcard/pp.jpeg");
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Bitmap imageBitmap = BitmapFactory.decodeFile(selectedImagePath, bmOptions);

            //Reduce the image resolution in case it's higher than 512x512
            if (imageBitmap.getHeight() > 512 || imageBitmap.getWidth() > 512)
            {
                Timber.i("Image height is: "+imageBitmap.getHeight());
                Timber.i("Image width is: "+imageBitmap.getWidth());
                imageBitmap = getResizedBitmap(imageBitmap, 512);
                Timber.i("New image height is: "+ imageBitmap.getHeight());
                Timber.i("New image width is: "+ imageBitmap.getWidth());
            }

            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            Timber.i("Uncompressed image length is: " + uncompressedImage.length());
            //Check if the selected picture size is larger than 1MB, if yes, compress it until size is less than 1MB
            if (uncompressedImage.length() > 1024*1024)
            {
                Timber.i("Image size is larger than 1MB");
                //start quality with 100 (essentially no compression)
                int quality = 100;
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteOutputStream);
                //Creates an exact copy of the picture in this specific directory (see method saveByteStreamtoFile)
                compressedImage = saveByteStreamtoFile(byteOutputStream);
                while (byteOutputStream.size() > 1024*1024)
                {
                    //Decrease quality (increase compression)
                    byteOutputStream.reset();
                    quality -= 30;
                    Timber.i("compressing, current ratio: " + quality);
                    imageBitmap = BitmapFactory.decodeFile("/sdcard/pp.jpeg", bmOptions);
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteOutputStream);
                }
                compressedImage.delete();
                compressedImage = saveByteStreamtoFile(byteOutputStream);
            }
            else
            {
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteOutputStream);
                compressedImage.delete();
                compressedImage = saveByteStreamtoFile(byteOutputStream);
            }


            Timber.i("old image size = " + Integer.parseInt(String.valueOf(uncompressedImage.length() / 1024)));
            Timber.i("new image size = " + Integer.parseInt(String.valueOf(compressedImage.length()/ 1024)));


            //Create the typedFile and upload the compressed image to the server using the Retrofit interface
            Timber.d("Image path is: "+selectedImagePath);
            TypedFile typedFile = new TypedFile("multipart/form-data", compressedImage);
            avatarsUploadResource.uploadFile(typedFile)
            .compose(new DefaultTransformer<User>())
                    .subscribe(new Action1<User>() {
                                   @Override
                                   public void call(User user) {
                                       profileImageUrl = user.getAvatarUrl();
                                       //Add s after http, for some reason fetching the image with http does not work
                                       profileImageUrl = profileImageUrl.substring(0, 4) + "s" + profileImageUrl.substring(4, profileImageUrl.length());
                                       Timber.i(user.getAvatarUrl());
                                       Uri profileImageUri = data.getData();
                                       //profilePicture.setImageURI(profileImageUri);
                                       Picasso.with(getActivity()).load(profileImageUrl).error(R.drawable.background_drawer).into(profilePicture);
                                       Timber.i("Successfully uploaded a new picture ");
                                        }
                               },
                            new CrashCallback(getActivity(), "failed to upload avatar"));
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

    //This method saves the compressed image from the byteStream to a file that can be used
    //to upload the picture to the server
    private File saveByteStreamtoFile(ByteArrayOutputStream os){
        FileOutputStream fos;
        File compressedImage = null;
        try {
            fos = new FileOutputStream("/sdcard/pp.jpeg");
            os.writeTo(fos);
            os.flush();
            fos.flush();
            os.close();
            fos.close();
            compressedImage = new File("/sdcard/pp.jpeg");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return compressedImage;
    }

    //This method resizes the image while keeping the same aspect ratio
    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 0) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
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

        if (tempYearOfBirth == null)
            yearPickerButton.setText("Unknown");
        else
            yearPickerButton.setText(tempYearOfBirth + "");

        Toast.makeText(getActivity(), "Changes discarded", Toast.LENGTH_SHORT)
                .show();
    }

    public void saveProfileChanges() {

        progressBar.setVisibility(View.VISIBLE);

        Calendar calendar = Calendar.getInstance();
        if (newYearOfBirth != null) {
            calendar.set(Calendar.YEAR, newYearOfBirth);
            newBirthDay = calendar.getTime();
        } else
            newBirthDay = null;


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
        UserDescription userDescription = new UserDescription(email, newFirstName, newLastName, password, newNumber, newGenderIsMale, newBirthDay, newAddress, profileImageUrl);
        updateUser(userDescription);
        changeNavigationDrawerImage(profileImageUrl);

    }

    //This method changes the navigation drawer profile picture after downloading the new picture from the server
    public void changeNavigationDrawerImage(final String avatarUrl) {
        final MaterialNavigationDrawer drawer = ((MaterialNavigationDrawer) getActivity());
        if (avatarUrl != null) {
            //Download the new profile picture
            Observable.defer(new Func0<Observable<Bitmap>>() {
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
            }).compose(new DefaultTransformer<Bitmap>())
                    .subscribe(new Action1<Bitmap>() {
                        @Override
                        public void call(Bitmap avatar) {
                            //Set the profile picture if download was successful
                            drawer.getCurrentAccount().setPhoto(avatar);
                            drawer.notifyAccountDataChanged();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Timber.e(throwable, "failed to download avatar");
                        }
                    });
        }
    }
    public void showYearPicker() {

        final Dialog yearDialog = new Dialog(getActivity());
        yearDialog.setTitle(Html.fromHtml("<font color='#388e3c'>Birth Year</font>"));
        yearDialog.setContentView(R.layout.year_picker_dialog);
        Button set = (Button) yearDialog.findViewById(R.id.set);
        Button cancel = (Button) yearDialog.findViewById(R.id.cancel);
        final NumberPicker yearPicker = (NumberPicker) yearDialog.findViewById(R.id.year_picker);
        yearPicker.setMaxValue(2015);
        yearPicker.setMinValue(1920);
        yearPicker.setWrapSelectorWheel(false);
        if (yearPickerButton.getText() != null && !yearPickerButton.getText().equals("Unknown"))
            yearPicker.setValue(Integer.parseInt(yearPickerButton.getText().toString()));
        else
            yearPicker.setValue(2015);

        setDividerColor(yearPicker, getResources().getColor(R.color.primary));
        yearDialog.show();

        //Change divider line color
        int titleDividerId = getResources().getIdentifier("titleDivider", "id", "android");
        View titleDivider = yearDialog.findViewById(titleDividerId);
        if (titleDivider != null)
            titleDivider.setBackgroundColor(getResources().getColor(R.color.primary));

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
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    public void updateUser(final UserDescription userDescription) {
        subscriptions.add(
                usersResource.updateUser(userDescription)
                        .compose(new DefaultTransformer<User>())
                        .subscribe(new Action1<User>() {
                            @Override
                            public void call(User user) {
                                Timber.v("Updated user info");
                                progressBar.setVisibility(View.GONE);
                                EditProfileFragment.this.getActivity().onBackPressed();
                            }

                        }, new CrashCallback(getActivity(), "failed to update user", new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                progressBar.setVisibility(View.GONE);
                                CrashPopup.show(getActivity(), throwable);
                            }
                        })));
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


