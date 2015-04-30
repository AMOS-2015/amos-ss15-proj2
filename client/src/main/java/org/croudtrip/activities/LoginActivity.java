package org.croudtrip.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.croudtrip.R;
import org.croudtrip.UsersResource;
import org.croudtrip.account.AccountManager;
import org.croudtrip.account.User;
import org.croudtrip.account.UserDescription;
import org.croudtrip.utils.DefaultTransformer;

import javax.inject.Inject;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * This Activity logs in a user by either registering him or simply by registerEmail and password.
 * @author Frederik Simon, Vanessa Lange
 */
@ContentView(R.layout.activity_login)
public class LoginActivity extends RoboActivity {

    @Inject private UsersResource usersResource;

    @InjectView(R.id.layout_choose)         private View loginChoiceView;
    @InjectView(R.id.btn_register_email)    private Button chooseRegisterButton;
    @InjectView(R.id.btn_login_with_email)  private Button chooseLoginButton;

    @InjectView(R.id.layout_register)   private View registerView;
    @InjectView(R.id.btn_register)      private Button registerButton;
    @InjectView(R.id.pb_register)       private ProgressBar registerProgressBar;
    @InjectView(R.id.et_firstName)      private EditText registerFirstName;
    @InjectView(R.id.et_lastName)       private EditText registerLastName;
    @InjectView(R.id.et_password)       private EditText registerPassword;
    @InjectView(R.id.et_email)          private EditText registerEmail;

    @InjectView(R.id.layout_login)      private View loginView;
    @InjectView(R.id.btn_login)         private Button loginButton;
    @InjectView(R.id.pb_login)          private ProgressBar loginProgressBar;
    @InjectView(R.id.tv_invalid_login)  private TextView loginErrorTextView;
    @InjectView(R.id.et_login_email)    private EditText loginEmail;
    @InjectView(R.id.et_login_password) private EditText loginPassword;

    private int animationDuration;
    private View activeView;


    //************************** Methods ******************************//

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Animation to blend over from the login choice view to registerButton or login view
        animationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        // Remember the currently shown layout/view
        activeView = loginChoiceView;

        // The user can decide to login (with registerEmail and password)...
        chooseLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showView(loginView);
            }
        });

        // ... or to registerButton with a new account
        chooseRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showView(registerView);
            }
        });

        // REGISTER LAYOUT
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerUser(
                        registerFirstName.getText().toString(),
                        registerLastName.getText().toString(),
                        registerEmail.getText().toString(),
                        registerPassword.getText().toString()
                );
            }
        });

        // LOGIN LAYOUT
        loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                loginUser(loginEmail.getText().toString(), loginPassword.getText().toString());
            }
        });

        // SKIP login
        findViewById(R.id.skip).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            }
        });
    }

    @Override
    public void onBackPressed() {

        if (activeView.equals(loginChoiceView)) {
            // Perform a normal back
            super.onBackPressed();
        } else {
            // If the user decided to register or login before, animate now back to
            // the choice screen
            showView(loginChoiceView);
        }
    }


    private void showView(final View view) {

        // Make the new view visible
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);

        // Put views slightly below screen to let them "pop up" afterwards (not the choice screen)
        float animatedTranslationY = (view.equals(loginChoiceView)) ? 0 : 100;
        view.setTranslationY(-animatedTranslationY);

        // Show the new view
        view.animate()
                .setStartDelay(animationDuration / 2)
                .alpha(1f)
                .translationY(animatedTranslationY)
                .setDuration(animationDuration)
                .setListener(null);

        // Hide the current view
        activeView.animate()
                .alpha(0f)
                .setDuration(animationDuration)
                .translationY((activeView.equals(loginChoiceView)) ? 200 : 0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        activeView.setVisibility(View.GONE);
                        activeView = view;
                    }
                });
    }


    /**
     * Registers the user with the given data at the server
     * @param firstName the user's first name
     * @param lastName the user's last name
     * @param email the user's registerEmail address
     * @param password the user's password
     */
    private void registerUser(final String firstName, final String lastName, final String email,
                              final String password) {

        // UI: Disable register button and show progress bar
        registerButton.setEnabled(false);
        registerProgressBar.setVisibility(View.VISIBLE);

        // Create a new user on the server
        UserDescription userDescription = new UserDescription(email, firstName, lastName, password);
        usersResource.registerUser(userDescription)
                .compose(new DefaultTransformer<User>())
                .subscribe(new Action1<User>() {

                    @Override
                    public void call(User user) {
                        // REGISTER SUCCESS

                        // UI: Hide progress bar
                        registerProgressBar.setVisibility(View.GONE);

                        Timber.i("Successfully registered user with registerEmail " + user.getEmail());

                        // Finally log the user in locally and start the main app
                        loginAndRedirect(user, password);
                    }

                }, new Action1<Throwable>() {

                    @Override
                    public void call(Throwable throwable) {
                        // ERROR

                        // UI: Re-enable register button and hide progress bar
                        registerButton.setEnabled(true);
                        registerProgressBar.setVisibility(View.GONE);

                        Response response = ((RetrofitError) throwable).getResponse();

                        String message;
                        if (response != null && response.getStatus() == 409) {  // Conflict
                            message = getString(R.string.registration_error_conflict);
                        } else {
                            message = getString(R.string.registration_error_general);
                        }

                        // Show an error message
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                        Timber.e("Registration failed with error:\n" + throwable.getMessage());
                    }
                });
    }


    /**
     * Logs a user in by connecting to the server and checking the given registerEmail and password
     * @param email the user's registerEmail address
     * @param password the user's password
     */
    private void loginUser(final String email, final String password) {

        // UI: Show progress bar, disable login button
        loginButton.setEnabled(false);
        loginProgressBar.setVisibility(View.VISIBLE);
        loginErrorTextView.setVisibility(View.GONE);

        // Create new adapter to set "custom" auth header (user not officially logged in yet)
        UsersResource usersResource = new RestAdapter.Builder()
                .setEndpoint(getString(R.string.server_address))
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        AccountManager.addAuthorizationHeader(email, password, request);
                    }
                })
                .build()
                .create(UsersResource.class);


        // Make the actual call to the server to receive the correct user object
        usersResource.getUser()
                .compose(new DefaultTransformer<User>())
                .subscribe(new Action1<User>() {

                    @Override
                    public void call(User user) {
                        // LOGIN SUCCESS

                        // UI: Hide progress bar
                        loginProgressBar.setVisibility(View.GONE);

                        // Finally log the user in locally and start the main app
                        loginAndRedirect(user, password);
                    }

                }, new Action1<Throwable>() {

                    @Override
                    public void call(Throwable throwable) {
                        // ERROR

                        // UI: Hide progress bar, enable login button again
                        loginProgressBar.setVisibility(View.GONE);
                        loginButton.setEnabled(true);

                        Response response = ((RetrofitError) throwable).getResponse();

                        if (response != null && response.getStatus() == 401) {  // Not Authorized
                            // Show error message for invalid login
                            loginErrorTextView.setVisibility(View.VISIBLE);
                        } else {
                            // Show an error for general errors e.g. connection issues
                            String errorMessage =  LoginActivity.this.getString(R.string.login_error_general);
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }

                        Timber.e("Logging the user in failed with error:\n" + throwable.getMessage());
                    }
                });

    }


    /**
     * After having been authorized by the server, this method logs the user in locally and
     * redirects him to the main app.
     * @param user the user to log in
     * @param password the user's password
     */
    private void loginAndRedirect(User user, String password){
        Context appContext = LoginActivity.this.getApplicationContext();
        AccountManager.login(appContext, user, password);
        Intent intent = new Intent(appContext, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
