package org.croudtrip.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.croudtrip.R;

/**
 * Created by Vanessa Lange on 24.04.15.
 */
public class LoginActivity extends Activity {

    //************************** Variables ******************************//

    private final static String SHARED_PREF_FILE_USER = "org.croudtrip.user";
    private final static String SHARED_PREF_KEY_EMAIL = "email";
    private final static String SHARED_PREF_KEY_PWD = "password";


    //************************** Methods ******************************//

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_login);

        // User is authenticated by email and password
        final EditText email = (EditText) findViewById(R.id.et_email);
        final EditText password = (EditText) findViewById(R.id.et_password);

        Button loginButton = (Button) findViewById(R.id.btn_login);
        loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                loginUserByEmail( email.getText().toString(), password.getText().toString() );
            }
        });
    }


    private void loginUserByEmail( String email, String password ) {
        final String serverAddress = getResources().getString( R.string.server_address );


        // Server authenticates the user
        // TODO
        boolean loginSuccess = true;

        if( !loginSuccess ) {
            // Show an error message
            findViewById( R.id.tv_invalid_login ).setVisibility( View.VISIBLE );

        } else {
            // Remember the login data
            SharedPreferences prefs = this.getSharedPreferences( SHARED_PREF_FILE_USER, Context.MODE_PRIVATE );

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString( SHARED_PREF_KEY_EMAIL, email );
            editor.putString( SHARED_PREF_KEY_PWD, password ); // TODO: save only encrypted passwort
            editor.apply();

            // Redirect the user to the MainActivity
            startActivity(new Intent(this.getApplicationContext(), MainActivity.class));

            // Finish the LoginActivity
            finish();
        }
    }



    /**
     * Checks if the user is currently logged in.
     * @param context application context
     * @return true if the user is currently logged in, otherwise false
     */
    public static boolean isUserLoggedIn(Context context){

        SharedPreferences prefs = context.getSharedPreferences( SHARED_PREF_FILE_USER, Context.MODE_PRIVATE );

        if( prefs.contains(SHARED_PREF_KEY_EMAIL) && prefs.contains(SHARED_PREF_KEY_PWD) ) {
            return true;
        } else {
            return false;
        }
    }
}
