package org.croudtrip.activities;

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

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.converter.JacksonConverter;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * This Activity can login a user using his email and a password.
 * Created by Vanessa Lange on 24.04.15.
 */
public class LoginActivity extends Activity {

    //************************** Variables ******************************//

    private final static String SHARED_PREF_FILE_USER = "org.croudtrip.user";
    private final static String SHARED_PREF_KEY_EMAIL = "email";
    private final static String SHARED_PREF_KEY_PWD = "password";


    private Button loginButton;
    private ProgressBar progressBar;
    private TextView errorTextView;


    //************************** Methods ******************************//

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // User is authenticated by email and password
        final EditText email = (EditText) findViewById(R.id.et_email);
        final EditText password = (EditText) findViewById(R.id.et_password);

        // Initialize GUI elements
        errorTextView = (TextView) findViewById(R.id.tv_invalid_login);
        progressBar = (ProgressBar) findViewById( R.id.pb_login);
        loginButton = (Button) findViewById(R.id.btn_login);
        loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                loginUserByEmail( email.getText().toString(), password.getText().toString() );
            }
        });
    }


    private void loginUserByEmail( final String email, final String password ) {
        final String serverAddress = getResources().getString( R.string.server_address );

        // ---- UI ----
        // Show progress bar, disable login button
        loginButton.setSaveEnabled(false);
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
                    loginButton.setSaveEnabled(true);

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

        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREF_FILE_USER, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }


    /**
     * Checks if the user is currently logged in.
     * @param context application context
     * @return true if the user is currently logged in, otherwise false
     */
    public static boolean isUserLoggedIn(Context context){

        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREF_FILE_USER, Context.MODE_PRIVATE);

        if( prefs.contains(SHARED_PREF_KEY_EMAIL) && prefs.contains(SHARED_PREF_KEY_PWD) ) {
            return true;
        } else {
            return false;
        }
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
        request.addHeader( "Authorization", "Basic " + base64EncodedCredentials );
        return true;
    }
}
