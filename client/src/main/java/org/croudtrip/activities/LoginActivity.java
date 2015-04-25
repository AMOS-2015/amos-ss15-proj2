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

import org.croudtrip.R;
import org.croudtrip.UsersResource;
import org.croudtrip.auth.User;
import org.croudtrip.auth.UserDescription;

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

    //************************** Variables ******************************//

    private final static String SHARED_PREF_FILE_USER = "org.croudtrip.user";
    private final static String SHARED_PREF_KEY_EMAIL = "email";
    private final static String SHARED_PREF_KEY_PWD = "password";


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
                startActivity(new Intent(LoginActivity.this, DummyActivity.class));
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
                .setStartDelay(animationDuration/2)
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


    private void registerUserByEmail( String firstName, String lastName, String email, String password ) {
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
                        Toast.makeText(LoginActivity.this, getString(R.string.registration_success), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, DummyActivity.class));
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

                // Remember the login data
                SharedPreferences prefs = LoginActivity.this.getSharedPreferences(SHARED_PREF_FILE_USER, Context.MODE_PRIVATE);

                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(SHARED_PREF_KEY_EMAIL, email);
                editor.putString(SHARED_PREF_KEY_PWD, password); // TODO: save only encrypted passwort?
                editor.apply();

                // Redirect the user to the MainActivity
                startActivity(new Intent(LoginActivity.this.getApplicationContext(), MainActivity.class));

                // Finish the LoginActivity
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
    /*
    public static void logout(Context context){

        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREF_FILE_USER, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
    */

    /**
     * Checks if the user is currently logged in.
     * @param context application context
     * @return true if the user is currently logged in, otherwise false
     */
    public static boolean isUserLoggedIn(Context context){
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREF_FILE_USER, Context.MODE_PRIVATE);
        return prefs.contains(SHARED_PREF_KEY_EMAIL) && prefs.contains(SHARED_PREF_KEY_PWD);
    }


    /**
     * Adds the authorization header to a request to the server such that the server can authorize
     * the user.
     * @param context application context
     * @param request the request the header should be added to.
     * @return true if adding the header was successful, false otherwise (e.g. User is not logged in)
     */
    public static boolean addAuthorizationHeader( Context context, RequestInterceptor.RequestFacade request ) {

        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREF_FILE_USER, Context.MODE_PRIVATE);
        String email = prefs.getString(SHARED_PREF_KEY_EMAIL, null);
        String password = prefs.getString(SHARED_PREF_KEY_PWD, null);

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
