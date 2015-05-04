package org.croudtrip.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.croudtrip.R;
import org.croudtrip.gcm.GcmManager;
import org.croudtrip.utils.DefaultTransformer;

import javax.inject.Inject;

import roboguice.fragment.provided.RoboFragment;
import roboguice.inject.InjectView;
import rx.functions.Action1;

/**
 * Quick and simple GCM testing going on here ...
 */
public class GcmTestFragment extends RoboFragment {

    @InjectView(R.id.status) private TextView statusView;
    @InjectView(R.id.register) private Button registerButton;
    @InjectView(R.id.unregister) private Button unregisterButton;

    @Inject GcmManager gcmManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_gcm_test, container, false);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        toggleRegistration();
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerButton.setEnabled(false);
                statusView.setText("registering ...");
                gcmManager.register()
                        .compose(new DefaultTransformer<Void>())
                        .subscribe(new SuccessAction(), new ErrorAction());
            }
        });
        unregisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unregisterButton.setEnabled(false);
                statusView.setText("unregistering ...");
                gcmManager.unregister()
                        .compose(new DefaultTransformer<Void>())
                        .subscribe(new SuccessAction(), new ErrorAction());
            }
        });
    }


    private void toggleRegistration() {
        if (gcmManager.isRegistered()) {
            statusView.setText("Registered");
            registerButton.setEnabled(false);
            unregisterButton.setEnabled(true);
        } else {
            statusView.setText("Unregistered");
            registerButton.setEnabled(true);
            unregisterButton.setEnabled(false);
        }
    }


    private final class SuccessAction implements Action1<Void> {
        @Override
        public void call(Void nothing) {
            toggleRegistration();
        }
    }


    private final class ErrorAction implements Action1<Throwable> {
        @Override
        public void call(Throwable throwable) {
            toggleRegistration();
            statusView.setText(throwable.getMessage());
        }
    }

}
