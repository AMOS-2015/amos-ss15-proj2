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
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.TextView;
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

    @Inject private UsersResource usersResource;
    @Inject private AvatarsUploadResource avatarsUploadResource;

    @InjectView(R.id.pb_edit_profile) private ProgressWheel progressBar;
    @InjectView(R.id.first_name) EditText firstNameEdit;
    @InjectView(R.id.last_name) EditText lastNameEdit;
    @InjectView(R.id.edit_profile_phone) EditText phoneNumberEdit;
    @InjectView(R.id.edit_profile_address) EditText addressEdit;
    @InjectView(R.id.text_year) TextView yearPickerButton;
    @InjectView(R.id.discard) Button discard;
    @InjectView(R.id.save) Button save;
    @InjectView(R.id.btn_edit_profile_image) FloatingActionButton editProfileImage;
    @InjectView(R.id.radioGender) RadioGroup genderRadio;
    @InjectView(R.id.profile_picture_edit) ImageView profilePicture;
    private String profileImageUrl;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // TODO
        /*
        if (prefs.getString(Constants.SHARED_PREF_KEY_PROFILE_IMAGE_URI,null) != null) {
            profileImageUri = Uri.parse(prefs.getString(Constants.SHARED_PREF_KEY_PROFILE_IMAGE_URI,null));
        }
        */

        User user = AccountManager.getLoggedInUser(getActivity());
        if (user == null) return;

		// download avatar
		if (user.getAvatarUrl() != null) {
            profileImageUrl = user.getAvatarUrl();
			Picasso
                    .with(getActivity())
                    .load(user.getAvatarUrl())
                    .error(R.drawable.profile)
                    .into(profilePicture);
			Timber.i("Profile image was downloaded and set");
		}

		// set user details
		if (user.getFirstName() != null) firstNameEdit.setText(user.getFirstName());
		if (user.getLastName() != null) lastNameEdit.setText(user.getLastName());
		if (user.getPhoneNumber() != null) phoneNumberEdit.setText(user.getPhoneNumber());
		if (user.getAddress() != null) addressEdit.setText(user.getAddress());
		if (user.getIsMale() != null) {
			if (user.getIsMale()) {
				genderRadio.check(R.id.radio_male);
			} else {
				genderRadio.check(R.id.radio_female);
			}
		}
		if (user.getBirthday() != null) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(user.getBirthday());
			yearPickerButton.setText(calendar.get(Calendar.YEAR) + "");
		}

        // year picker button
        yearPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showYearPicker();
            }
        });

        // discard changes button
        discard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Changes discarded", Toast.LENGTH_SHORT).show();
                getActivity().onBackPressed();
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
                //Open Gallery
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, 100);
            }
        });
    }


    //Handle the selected image and upload it to the server
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) return;

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
        if (imageBitmap.getHeight() > 512 || imageBitmap.getWidth() > 512) {
            Timber.i("Image height is: " + imageBitmap.getHeight());
            Timber.i("Image width is: " + imageBitmap.getWidth());
            imageBitmap = getResizedBitmap(imageBitmap, 512);
            Timber.i("New image height is: " + imageBitmap.getHeight());
            Timber.i("New image width is: " + imageBitmap.getWidth());
        }

        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        Timber.i("Uncompressed image length is: " + uncompressedImage.length());
        //Check if the selected picture size is larger than 1MB, if yes, compress it until size is less than 1MB
        if (uncompressedImage.length() > 1024 * 1024) {
            Timber.i("Image size is larger than 1MB");
            //start quality with 100 (essentially no compression)
            int quality = 100;
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteOutputStream);
            //Creates an exact copy of the picture in this specific directory (see method saveByteStreamtoFile)
            compressedImage = saveByteStreamtoFile(byteOutputStream);
            while (byteOutputStream.size() > 1024 * 1024) {
                //Decrease quality (increase compression)
                byteOutputStream.reset();
                quality -= 30;
                Timber.i("compressing, current ratio: " + quality);
                imageBitmap = BitmapFactory.decodeFile("/sdcard/pp.jpeg", bmOptions);
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteOutputStream);
            }
            compressedImage.delete();
            compressedImage = saveByteStreamtoFile(byteOutputStream);
        } else {
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteOutputStream);
            compressedImage.delete();
            compressedImage = saveByteStreamtoFile(byteOutputStream);
        }


        Timber.i("old image size = " + Integer.parseInt(String.valueOf(uncompressedImage.length() / 1024)));
        Timber.i("new image size = " + Integer.parseInt(String.valueOf(compressedImage.length() / 1024)));


        //Create the typedFile and upload the compressed image to the server using the Retrofit interface
        Timber.d("Image path is: " + selectedImagePath);
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
                                   Picasso.with(getActivity()).load(profileImageUrl).error(R.drawable.background_drawer).into(profilePicture);
                                   Timber.i("Successfully uploaded a new picture ");
                               }
                           },
                        new CrashCallback(getActivity(), "failed to upload avatar"));
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
    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
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


    public void saveProfileChanges() {
        progressBar.setVisibility(View.VISIBLE);

        User user = AccountManager.getLoggedInUser(getActivity());
        String firstName = (firstNameEdit.getText().toString().isEmpty()) ? user.getFirstName() : firstNameEdit.getText().toString();
        String lastName = (lastNameEdit.getText().toString().isEmpty()) ? user.getLastName() : lastNameEdit.getText().toString();
        String phone = (phoneNumberEdit.getText().toString().isEmpty()) ? user.getPhoneNumber() : phoneNumberEdit.getText().toString();
        String address = (addressEdit.getText().toString().isEmpty()) ? user.getAddress() : addressEdit.getText().toString();
        profileImageUrl = (profileImageUrl == null) ? user.getAvatarUrl() : profileImageUrl;
        Boolean isMale = user.getIsMale();
        if (genderRadio.getCheckedRadioButtonId() != - 1) isMale = (genderRadio.getCheckedRadioButtonId() == R.id.radio_male);
        Date birthday = user.getBirthday();
        if (!yearPickerButton.getText().toString().isEmpty()) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, Integer.valueOf(yearPickerButton.getText().toString()));
            birthday = calendar.getTime();
        }

        user = new User(
                user.getId(),
                user.getEmail(),
                firstName,
                lastName,
                phone,
                isMale,
                birthday,
                address,
                profileImageUrl,
                user.getLastModified());

        AccountManager.saveUser(getActivity(), user, null);
        UserDescription userDescription
                = new UserDescription(user.getEmail(), firstName, lastName, null, phone, isMale, birthday, address, profileImageUrl);
        updateUser(userDescription);
        changeNavigationDrawerImage(profileImageUrl);
    }


    // changes the navigation drawer profile picture after downloading the new picture from the server
    private void changeNavigationDrawerImage(final String avatarUrl) {
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


    private void showYearPicker() {
        final Dialog yearDialog = new Dialog(getActivity());
        yearDialog.setTitle(Html.fromHtml("<font color='#388e3c'>Birth Year</font>"));
        yearDialog.setContentView(R.layout.year_picker_dialog);
        Button set = (Button) yearDialog.findViewById(R.id.set);
        Button cancel = (Button) yearDialog.findViewById(R.id.cancel);
        final NumberPicker yearPicker = (NumberPicker) yearDialog.findViewById(R.id.year_picker);
        yearPicker.setMaxValue(2015);
        yearPicker.setMinValue(1920);
        yearPicker.setWrapSelectorWheel(false);
        if (yearPickerButton.getText() != null && !yearPickerButton.getText().toString().isEmpty())
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
                yearPickerButton.setText(String.valueOf(yearPicker.getValue()));
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


    private void updateUser(final UserDescription userDescription) {
        subscriptions.add(
                usersResource.updateUser(userDescription)
                        .compose(new DefaultTransformer<User>())
                        .subscribe(new Action1<User>() {
                            @Override
                            public void call(User user) {
                                progressBar.setVisibility(View.GONE);
                                getActivity().onBackPressed();
                                Toast.makeText(getActivity(), "Profile saved", Toast.LENGTH_SHORT).show();
                            }

                        }, new CrashCallback(getActivity(), "failed to update user", new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                progressBar.setVisibility(View.GONE);
                                CrashPopup.show(getActivity(), throwable);
                            }
                        })));
    }

}


