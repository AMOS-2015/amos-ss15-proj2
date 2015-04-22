package org.croudtrip.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.croudtrip.R;
import org.croudtrip.UserResource;
import org.croudtrip.auth.User;
import org.croudtrip.auth.UserDescription;

import retrofit.RestAdapter;
import retrofit.converter.JacksonConverter;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Login functionality will not be in a fragment, but in an extra activity, just to have it completely separated to the business logic
 * (recommended by google)
 */
public class LoginActivity extends Activity {

    private View layoutRegister, layoutChoose;
    private int animationDuration;

    private boolean registerViewVisible = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        animationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        layoutRegister = findViewById(R.id.layout_register);
        layoutChoose = findViewById(R.id.layout_choose);

        layoutRegister.setVisibility(View.GONE);


        Button loginEmail = (Button) findViewById(R.id.btn_login_email);
        loginEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegisterView();
            }
        });

        final EditText firstName = (EditText) findViewById(R.id.et_firstName);
        final EditText lastName = (EditText) findViewById(R.id.et_lastName);
        final EditText email = (EditText) findViewById(R.id.et_email);

        Button register = (Button) findViewById(R.id.btn_register);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUserByEmail(firstName.getText().toString(), lastName.getText().toString(), email.getText().toString(), "1234");
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (registerViewVisible) {
            showChooseView();
        } else {
            super.onBackPressed();
        }
    }

    private void showChooseView() {
        registerViewVisible = false;
        layoutChoose.setAlpha(0f);
        layoutChoose.setVisibility(View.VISIBLE);

        //layoutRegister.setTranslationY(-100);
        layoutChoose.animate()
                .setStartDelay(animationDuration/2)
                .alpha(1f)
                .translationY(0)
                .setDuration(animationDuration)
                .setListener(null);

        layoutRegister.animate()
                .alpha(0f)
                //.translationY(200)
                .setDuration(animationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        layoutRegister.setVisibility(View.GONE);
                    }
                });
    }

    private void showRegisterView() {
        registerViewVisible = true;
        layoutRegister.setAlpha(0f);
        layoutRegister.setVisibility(View.VISIBLE);

        layoutRegister.setTranslationY(-100);
        layoutRegister.animate()
                .setStartDelay(animationDuration/2)
                .alpha(1f)
                .translationY(100)
                .setDuration(animationDuration)
                .setListener(null);

        layoutChoose.animate()
                .alpha(0f)
                .translationY(200)
                .setDuration(animationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        layoutChoose.setVisibility(View.GONE);
                    }
                });
    }


    private void registerUserByEmail( String firstName, String lastName, String email, String password ) {
        // TODO: Get server's address from global strings.xml -- server must be online to do so.
        final String serverAddress = getResources().getString(R.string.server_address);

        // create user
        UserDescription userDescription = new UserDescription(email, firstName, lastName, password);

        UserResource userResource = new RestAdapter.Builder().setEndpoint(serverAddress)
                                                                 .setConverter(new JacksonConverter())
                                                                 .build()
                                                                 .create(UserResource.class);

        userResource.registerUser(userDescription).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<User>() {
                    @Override
                    public void call(User user) {
                        Toast.makeText(LoginActivity.this, "registering user successful (id " + user.getId() + ")", Toast.LENGTH_SHORT).show();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e("CroudTrip", throwable.getMessage());
                    }
                });
    }

}
