package crowdtrip.com.croudtrip.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.crowdtrip.HelloWorld;
import org.crowdtrip.HelloWorldResource;

import crowdtrip.com.croudtrip.R;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        final EditText serverInput = (EditText) findViewById(R.id.input_server);
        findViewById(R.id.hello_world).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getHelloWorld(serverInput.getText().toString());
            }
        });
    }


    private void getHelloWorld(String serverAddress) {
        HelloWorldResource resource = new RestAdapter.Builder()
                .setEndpoint(serverAddress)
                .setConverter(new JacksonConverter())
                .build()
                .create(HelloWorldResource.class);

        resource.getHelloWorld()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<HelloWorld>() {
                    @Override
                    public void call(HelloWorld helloWorld) {
                        Toast.makeText(LoginActivity.this, helloWorld.getGreeting() + " " + helloWorld.getTarget(), Toast.LENGTH_SHORT).show();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Toast.makeText(LoginActivity.this, "Hello world failed", Toast.LENGTH_SHORT).show();
                        Log.e("CrowdTrip", throwable.getMessage());
                    }
                });
    }

}
