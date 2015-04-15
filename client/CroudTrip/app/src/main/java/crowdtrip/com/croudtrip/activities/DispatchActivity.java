package crowdtrip.com.croudtrip.activities;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Activity without UI. It just redirects to other activities, based on the login status of the current user
 */
public class DispatchActivity extends Activity {

    boolean loggedIn = false; //TODO: Implement functionality to detect, wether the user is currently logged in


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (loggedIn) {
            startActivity(new Intent(this, MainActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

}