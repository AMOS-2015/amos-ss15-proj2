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

import org.croudtrip.BasicUser;
import org.croudtrip.HelloWorld;
import org.croudtrip.HelloWorldResource;
import org.croudtrip.R;
import org.croudtrip.RegisterUserResource;

import retrofit.RestAdapter;
import retrofit.client.Response;
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
                //TODO
            }
        });


        /*final EditText serverInput = (EditText) findViewById(R.id.input_server);
        findViewById(R.id.hello_world).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getHelloWorld(serverInput.getText().toString());
            }
        });*/
    }

    private void showRegisterView() {
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
                        Log.e("CroudTrip", throwable.getMessage());
                    }
                });
    }

    private void registerUserByEmail( String firstName, String lastName, String email, String password )
    {
        // TODO: Get server's address from global strings.xml -- server must be online to do so.
        final String serverAddress = "";

        // create user
        BasicUser user = new BasicUser(firstName, lastName, email, password);

        RegisterUserResource register = new RestAdapter.Builder().setEndpoint(serverAddress)
                                                                 .setConverter(new JacksonConverter())
                                                                 .build()
                                                                 .create(RegisterUserResource.class);

        register.registerUser(user).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Response>() {
                    @Override
                    public void call(Response response) {
                        Toast.makeText(LoginActivity.this, "registering user successful", Toast.LENGTH_SHORT).show();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Toast.makeText(LoginActivity.this, "Hello world failed", Toast.LENGTH_SHORT).show();
                        Log.e("CroudTrip", throwable.getMessage());
                    }
                });
    }

}
