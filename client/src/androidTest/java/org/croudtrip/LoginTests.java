package org.croudtrip;

import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import org.croudtrip.account.AccountManager;
import org.croudtrip.activities.LoginActivity;
import org.croudtrip.api.account.User;

import java.util.ArrayList;
import java.util.Random;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

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

    public void testCreateValidAccount() throws InterruptedException{

        AccountManager.logout(loginActivity, false);

        // Show correct register view after clicking "Register"
        onView(withId(R.id.btn_register_email)).perform(click());
        onView(withId(R.id.btn_register)).check(matches(withText(R.string.register_button)));

        // Register a new random user
        final String numbers = "0123456789";
        final String ABC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String firstName = generateRandomString(20, numbers+ABC, "");
        String lastName = generateRandomString(20, numbers+ABC, "");
        String email = generateRandomString(10, numbers+ABC, "") + "@" + generateRandomString(6, ABC, "")
                + "." + generateRandomString(6, ABC, "");
        String password = generateRandomString(20, numbers+ABC, "");

        // Use closeSoftKeyboard and Thread.sleep because Espresso is buggy
        // and crashes if the EditText is not visible due to the keyboard (and it doesn't close
        // fast enough -> sleep necessary :( )
        onView(withId(R.id.et_email)).perform(typeText(email));
        closeSoftKeyboard(); Thread.sleep(1000);

        onView(withId(R.id.et_firstName)).perform(typeText(firstName));
        closeSoftKeyboard(); Thread.sleep(1000);

        onView(withId(R.id.et_lastName)).perform(typeText(lastName));
        closeSoftKeyboard(); Thread.sleep(1000);

        onView(withId(R.id.et_password)).perform(typeText(password));
        closeSoftKeyboard(); Thread.sleep(1000);

        onView(withId(R.id.btn_register)).perform(click());
        Thread.sleep(1000);

        assertTrue("User logged in (device)", AccountManager.isUserLoggedIn(loginActivity));

        User user = AccountManager.getLoggedInUser(loginActivity);
        assertTrue("User first name correct", user.getFirstName().equals(firstName));
        assertTrue("User last name correct", user.getLastName().equals(lastName));
        assertTrue("User email correct", user.getEmail().equals(email));
    }


    public void testLogout(){
        AccountManager.logout(loginActivity, false);
        assertTrue("User logged out (device)", !AccountManager.isUserLoggedIn(loginActivity));
    }


    private String generateRandomString(int length, String allowedChars, String mustHaveChars){

        if(mustHaveChars.length() > length){
            return null;
        }

        // Build a random string from the allowed chars
        Random random = new Random();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(allowedChars.charAt(random.nextInt(allowedChars.length())));
        }

        // Insert must-have characters at different positions in the string
        ArrayList<Integer> mustHaveCharsIndices = new ArrayList<Integer>();
        for(int i = 0; i < mustHaveChars.length(); i++){
            char mustHave = mustHaveChars.charAt(i);

            int index = -1;
            do{
                index = random.nextInt(length);
            }while(mustHaveCharsIndices.contains(index));

            mustHaveCharsIndices.add(index);
            builder.setCharAt(index, mustHave);
        }

        return builder.toString();
    }
}
