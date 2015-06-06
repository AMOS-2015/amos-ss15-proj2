package org.croudtrip;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;


import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.test.ActivityInstrumentationTestCase2;

import android.test.suitebuilder.annotation.LargeTest;

import org.croudtrip.activities.LoginActivity;

/**
 * Created by alex on 06.06.15.
 */
@LargeTest
public class LoginTests extends ActivityInstrumentationTestCase2<LoginActivity> {

    private LoginActivity loginActivity;

    public LoginTests(Class<LoginActivity> activityClass) {
        super(activityClass);
    }

    public LoginTests() {
        super(LoginActivity.class);
    }


    @Override
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        // Espresso will not launch our activity for us, we must launch it via getActivity().
        loginActivity = getActivity();
    }

    public void testCreateAccount() {
        onView(withId(R.id.btn_register_email)).perform(click());
        //onView(withId(R.id.btn_register)).perform(click());

        onView(withId(R.id.btn_register))
                .check(matches(withText(R.string.register_button)));
    }

}
