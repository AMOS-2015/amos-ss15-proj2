package org.croudtrip.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.UsersResource;
import org.croudtrip.auth.User;
import org.croudtrip.auth.UserDescription;

import java.util.Date;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.converter.JacksonConverter;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Login functionality will not be in a fragment, but in an extra activity, just to have it completely separated to the business logic
 * (recommended by google)
 * @author Frederik Simon, Vanessa Lange
 */
public class LoginActivity extends Activity {


    private Button loginButton;
    private ProgressBar progressBar;
    private TextView errorTextView;

    private View layoutChoose, layoutRegister, layoutLogin;
    private int animationDuration;

    private boolean registerViewVisible = false;
    private boolean loginViewVisible = false;


    //************************** Methods ******************************//

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        animationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        layoutChoose = findViewById(R.id.layout_choose);
        layoutRegister = findViewById(R.id.layout_register);
        layoutLogin = findViewById(R.id.layout_login);

        Button chooseLogin = (Button) findViewById(R.id.btn_login_with_email);
        chooseLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoginView();
            }
        });

        Button chooseRegister = (Button) findViewById(R.id.btn_register_email);
        chooseRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegisterView();
            }
        });


        // REGISTER LAYOUT
        final EditText registerFirstName = (EditText) findViewById(R.id.et_firstName);
        final EditText registerLastName = (EditText) findViewById(R.id.et_lastName);
        final EditText email = (EditText) findViewById(R.id.et_email);

        Button register = (Button) findViewById(R.id.btn_register);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUserByEmail(registerFirstName.getText().toString(), registerLastName.getText().toString(), email.getText().toString(), "1234");
            }
        });


        // LOGIN LAYOUT
        // User is authenticated by email and password
        final EditText loginEmail = (EditText) findViewById(R.id.et_login_email);
        final EditText loginPassword = (EditText) findViewById(R.id.et_login_password);

        // Initialize GUI elements
        errorTextView = (TextView) findViewById(R.id.tv_invalid_login);
        progressBar = (ProgressBar) findViewById( R.id.pb_login);


        loginButton = (Button) findViewById(R.id.btn_login);
        loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                loginUserByEmail(loginEmail.getText().toString(), loginPassword.getText().toString());
            }
        });


        // SKIP
        findViewById(R.id.skip).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (registerViewVisible || loginViewVisible) {
            showChooseView();
        } else {
            super.onBackPressed();
        }
    }

    private void showChooseView() {

        layoutChoose.setAlpha(0f);
        layoutChoose.setVisibility(View.VISIBLE);

        //layoutRegister.setTranslationY(-100);
        layoutChoose.animate()
                .setStartDelay(animationDuration / 2)
                .alpha(1f)
                .translationY(0)
                .setDuration(animationDuration)
                .setListener(null);

        if(registerViewVisible) {
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
            registerViewVisible = false;

        }else if(loginViewVisible){
            layoutLogin.animate()
                    .alpha(0f)
                        //.translationY(200)
                    .setDuration(animationDuration)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            layoutLogin.setVisibility(View.GONE);
                        }
                    });
            loginViewVisible = false;
        }
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

    private void showLoginView() {
        loginViewVisible = true;
        layoutLogin.setAlpha(0f);
        layoutLogin.setVisibility(View.VISIBLE);

        layoutLogin.setTranslationY(-100);
        layoutLogin.animate()
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


    private void registerUserByEmail( final String firstName, final String lastName, final String email, final String password ) {
        final String serverAddress = getResources().getString(R.string.server_address);

        // create user
        UserDescription userDescription = new UserDescription(email, firstName, lastName, password);

        UsersResource usersResource = new RestAdapter.Builder().setEndpoint(serverAddress)
                                                                 .setConverter(new JacksonConverter())
                                                                 .build()
                                                                 .create(UsersResource.class);

        usersResource.registerUser(userDescription).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<User>() {
                    @Override
                    public void call(User user) {
                        SharedPreferences prefs = LoginActivity.this.getSharedPreferences(Constants.SHARED_PREF_FILE_USER, Context.MODE_PRIVATE);

                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(Constants.SHARED_PREF_KEY_FIRSTNAME, firstName);
                        editor.putString(Constants.SHARED_PREF_KEY_LASTNAME, lastName);
                        editor.apply();

                        login(user, password);
                        Toast.makeText(LoginActivity.this, getString(R.string.registration_success), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, DummyActivity.class));
                        finish();

                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        RetrofitError retrofitError = (RetrofitError) throwable;
                        String message;
                        if (retrofitError.getResponse() != null && retrofitError.getResponse().getStatus() == 409) {
                            message = getString(R.string.registration_error_conflict);
                        } else {
                            message = getString(R.string.registration_error_general);
                        }
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                        Timber.e(throwable.getMessage());
                    }
                });
    }

    private void loginUserByEmail( final String email, final String password ) {
        final String serverAddress = getResources().getString( R.string.server_address );

        // ---- UI ----
        // Show progress bar, disable login button
        loginButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        errorTextView.setVisibility(View.GONE);


        // Server authenticates the user
        UsersResource usersResource = new RestAdapter.Builder()
                .setEndpoint(serverAddress)
                .setConverter(new JacksonConverter())
                .setRequestInterceptor(new RequestInterceptor() {

                    @Override
                    public void intercept(RequestFacade request) {
                        // Put email and password in header for authorization
                        addAuthorizationHeader(email, password, request);
                    }
                })
                .build()
                .create(UsersResource.class);

        usersResource.getUser().subscribeOn( Schedulers.io() )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<User>() {

                @Override
                public void call(User user) {
                    // LOGIN SUCCESS

                    // ---- UI ----
                    // Hide progress bar
                    progressBar.setVisibility(View.GONE);

                    // Remember the login data = login
                    login(user, password);

                    // Redirect the user to the MainActivity and finish the LoginActivity
                    startActivity(new Intent(LoginActivity.this.getApplicationContext(), MainActivity.class));
                    finish();
                }

            }, new Action1<Throwable>() {

                @Override
                public void call(Throwable throwable) {

                    // ---- UI ----
                    // Hide progress bar, enable login button
                    progressBar.setVisibility(View.GONE);
                    loginButton.setEnabled(true);

                    Timber.e(throwable.getMessage());
                    RetrofitError retrofitError = (RetrofitError) throwable;

                    if (retrofitError.getResponse() != null && retrofitError.getResponse().getStatus() == 401) {
                        // Show error message for invalid login
                        errorTextView.setVisibility(View.VISIBLE);
                    } else {
                        // Show an error for general errors e.g. connection issues
                        Toast.makeText(LoginActivity.this, LoginActivity.this.getString(R.string.login_error_general), Toast.LENGTH_LONG).show();
                    }
                }
            });

    }


    /**
     * Deletes any authorization data of the currently logged-in user from the device. Afterwards,
     * the user can be regarded as "logged out".
     * @param context application context
     */
    public static void logout(Context context){

        // remove any saved login data
        SharedPreferences prefs = context.getSharedPreferences(Constants.SHARED_PREF_FILE_USER, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        // redirect to login screen and delete any activities "before"
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }


    /**
     * Remember the user and his password
     */
    private void login(User user, String password){

        SharedPreferences prefs = LoginActivity.this.getSharedPreferences(Constants.SHARED_PREF_FILE_USER, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(Constants.SHARED_PREF_KEY_PWD, password); // TODO: save only encrypted password?
        editor.putLong(Constants.SHARED_PREF_KEY_ID, user.getId());

        if(user.getEmail() != null) editor.putString(Constants.SHARED_PREF_KEY_EMAIL, user.getEmail());
        if(user.getAddress() != null) editor.putString(Constants.SHARED_PREF_KEY_ADDRESS, user.getAddress());
        if(user.getFirstName() != null) editor.putString(Constants.SHARED_PREF_KEY_FIRSTNAME, user.getFirstName());
        if(user.getLastName() != null) editor.putString(Constants.SHARED_PREF_KEY_LASTNAME, user.getLastName());
        if(user.getPhoneNumber() != null) editor.putString(Constants.SHARED_PREF_KEY_PHONE, user.getPhoneNumber());
        if(user.getBirthDay() != null) editor.putLong(Constants.SHARED_PREF_KEY_BIRTHDAY, user.getBirthDay().getTime());
        if(user.getIsMale() != null) editor.putBoolean(Constants.SHARED_PREF_KEY_MALE, user.getIsMale());
        if(user.getAvatarUrl() != null) editor.putString(Constants.SHARED_PREF_KEY_AVATAR_URL, user.getAvatarUrl());

        editor.apply();
    }


    /**
     * Returns the currently logged-in user. If no user is logged in, null is returned.
     * Every attribute not filled in by the user is null or -1 for numbers, default gender is male.
     * @param context application context
     * @return the currently logged-in user or null
     */
    public static User getLoggedInUser(Context context){
        SharedPreferences prefs = context.getSharedPreferences(Constants.SHARED_PREF_FILE_USER, Context.MODE_PRIVATE);

        if(!prefs.contains(Constants.SHARED_PREF_KEY_PWD)){
            return null;
        }

        Date birthday = null;
        if(prefs.contains(Constants.SHARED_PREF_KEY_BIRTHDAY)){
            new Date(prefs.getLong(Constants.SHARED_PREF_KEY_BIRTHDAY, 0));
        }

        User user = new User(
                prefs.getLong(Constants.SHARED_PREF_KEY_ID, -1),
                prefs.getString(Constants.SHARED_PREF_KEY_EMAIL, null),
                prefs.getString(Constants.SHARED_PREF_KEY_FIRSTNAME, null),
                prefs.getString(Constants.SHARED_PREF_KEY_LASTNAME, null),
                prefs.getString(Constants.SHARED_PREF_KEY_PHONE, null),
                prefs.getBoolean(Constants.SHARED_PREF_KEY_MALE, true),
                birthday,
                prefs.getString(Constants.SHARED_PREF_KEY_ADDRESS, null),
                prefs.getString(Constants.SHARED_PREF_KEY_AVATAR_URL, null)
        );
        return user;
    }

    public static boolean isUserLoggedIn(Context context){
        return LoginActivity.getLoggedInUser(context) != null;
    }


    /**
     * Adds the authorization header to a request to the server such that the server can authorize
     * the user.
     * @param context application context
     * @param request the request the header should be added to.
     * @return true if adding the header was successful, false otherwise (e.g. User is not logged in)
     */
    public static boolean addAuthorizationHeader( Context context, RequestInterceptor.RequestFacade request ) {

        SharedPreferences prefs = context.getSharedPreferences(Constants.SHARED_PREF_FILE_USER, Context.MODE_PRIVATE);
        String email = prefs.getString(Constants.SHARED_PREF_KEY_EMAIL, null);
        String password = prefs.getString(Constants.SHARED_PREF_KEY_PWD, null);

        return addAuthorizationHeader( email, password, request );
    }


    /**
     * Adds the authorization header to a request to the server such that the server can authorize
     * the user. Use {@link #addAuthorizationHeader(Context, RequestInterceptor.RequestFacade)
     * addAuthorizationHeader( Context context, RequestInterceptor.RequestFacade request )} if email
     * or password are unknown
     * @param email email address of the user
     * @param password password of the user
     * @param request the request the header should be added to
     * @return true if adding the header was successful, false otherwise (e.g. User is not logged in)
     */
    public static boolean addAuthorizationHeader( String email, String password, RequestInterceptor.RequestFacade request ) {

        if ( email == null || password == null ) {
            return false;
        }

        // Put email and password in header
        String credentials = email + ":" + password;
        String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        request.addHeader("Authorization", "Basic " + base64EncodedCredentials);
        return true;
    }

}
