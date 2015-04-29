package org.croudtrip.activities;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.fragments.JoinTripFragment;
import org.croudtrip.fragments.NavigationFragment;
import org.croudtrip.fragments.OfferTripFragment;
import org.croudtrip.fragments.ProfileFragment;
import org.croudtrip.fragments.SettingsFragment;
import org.croudtrip.utils.DefaultTransformer;

import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialAccount;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import timber.log.Timber;

/**
 * We will probably use fragments, so this activity works as a container for all these fragments and will probably do
 * some initialization and stuff
 */
public class MainActivity extends MaterialNavigationDrawer<Fragment> {

    @Override
    public void init(Bundle savedInstanceState) {

        this.disableLearningPattern();
        this.allowArrowAnimation();
        this.setBackPattern(MaterialNavigationDrawer.BACKPATTERN_BACK_TO_FIRST);
        this.setDrawerHeaderImage(R.drawable.background_drawer);

        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREF_FILE_USER, Context.MODE_PRIVATE);
        String firstName = prefs.getString(Constants.SHARED_PREF_KEY_FIRSTNAME, "");
        String lastName = prefs.getString(Constants.SHARED_PREF_KEY_LASTNAME, "");
        String email = prefs.getString(Constants.SHARED_PREF_KEY_EMAIL, "");
        final String avatarUrl = prefs.getString(Constants.SHARED_PREF_KEY_AVATAR_URL, null);

        final MaterialAccount account = new MaterialAccount(this.getResources(),firstName+ " " + lastName,email,R.drawable.profile, R.drawable.background_drawer);
        this.addAccount(account);


                        // create sections
        this.addSection(newSection(getString(R.string.menu_join_trip), R.drawable.hitchhiker, new JoinTripFragment()));
        this.addSection(newSection(getString(R.string.menu_offer_trip), R.drawable.ic_directions_car_white, new OfferTripFragment()));
        if(LoginActivity.isUserLoggedIn(this)) {
            // only logged-in users can view their profile
            this.addSection(newSection(getString(R.string.menu_profile), R.drawable.profile_icon, new ProfileFragment()));
        }
        this.addSection(newSection(getString(R.string.navigation), R.drawable.distance, new NavigationFragment()));

        ((MaterialSection) getSectionList().get(0)).setNotifications(3);

        // create bottom section
        this.addBottomSection(newSection(getString(R.string.menu_settings), R.drawable.ic_settings, new SettingsFragment()));

        // download avatar
        if (avatarUrl == null) return;
        Observable
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
                        Timber.d("avatar is null " + (avatar == null));
                        Timber.d("" + avatar.getWidth());
                        account.setPhoto(avatar);
                        notifyAccountDataChanged();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.e(throwable, "failed to download avatar");
                    }
                });
    }

}
