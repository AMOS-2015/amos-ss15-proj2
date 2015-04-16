package crowdtrip.com.croudtrip.activities;

import android.app.Activity;
import android.os.Bundle;

import crowdtrip.com.croudtrip.R;

/**
 * Login functionality will not be in a fragment, but in an extra activity, just to have it completely separated to the business logic
 * (recommended by google)
 */
public class LoginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }
}
