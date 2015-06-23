package org.croudtrip;

import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import org.croudtrip.account.AccountManager;
import org.croudtrip.activities.LoginActivity;
import org.croudtrip.api.account.User;

import java.util.ArrayList;
import java.util.Date;
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

    public void testRegisterButton() {

        // Show correct register view after clicking "Register"
        onView(withId(R.id.btn_register_email)).perform(click());
        onView(withId(R.id.btn_register)).check(matches(withText(R.string.register_button)));
    }


    public void testRegisterEditTexts() throws InterruptedException {

        // Change to register view (not login view)
        onView(withId(R.id.btn_register_email)).perform(click());

        // Enter user data
        String firstName = "first name with spaces";
        String lastName = "last name with spaces";
        String email = "email@anything.something";
        String password = "quite some password, might I say";

        // Use closeSoftKeyboard and Thread.sleep because Espresso is buggy
        // and crashes if the EditText is not visible due to the keyboard (and it doesn't close
        // fast enough -> sleep necessary :( )
        onView(withId(R.id.et_email)).perform(typeText(email));
        closeSoftKeyboard();
        Thread.sleep(1000);
        onView(withId(R.id.et_email)).check(matches(withText(email)));

        onView(withId(R.id.et_firstName)).perform(typeText(firstName));
        closeSoftKeyboard();
        Thread.sleep(1000);
        onView(withId(R.id.et_firstName)).check(matches(withText(firstName)));

        onView(withId(R.id.et_lastName)).perform(typeText(lastName));
        closeSoftKeyboard();
        Thread.sleep(1000);
        onView(withId(R.id.et_lastName)).check(matches(withText(lastName)));

        onView(withId(R.id.et_password)).perform(typeText(password));
        closeSoftKeyboard();
        Thread.sleep(1000);
        onView(withId(R.id.et_password)).check(matches(withText(password)));
    }


    public void testLocalLogin() {

        String email = "email@anything.something";
        String firstName = "first name";
        String lastName = "last name";
        String phone = "123456789";
        Boolean male = true;
        Date date = null;
        String address = "address 12 addressi";
        String avatar = null;
        long lastModified = 0;

        User user = new User(1, email, firstName, lastName, phone,
                male, date, address, avatar, lastModified);

        // User must be logged in, first logout
        AccountManager.logout(loginActivity, false);
        AccountManager.login(loginActivity, user, "some password");
        assertTrue("User logged in on device", AccountManager.isUserLoggedIn(loginActivity));

        // Logged-in user must be correct
        user = AccountManager.getLoggedInUser(loginActivity);
        assertEquals("Email correct", user.getEmail(), email);
        assertEquals("First name correct", user.getFirstName(), firstName);
        assertEquals("Last name correct", user.getLastName(), lastName);
        assertEquals("Phone correct", user.getPhoneNumber(), phone);
        assertEquals("Gender correct", user.getIsMale(), male);
        assertEquals("Birth date correct", user.getBirthday(), date);
        assertEquals("Address correct", user.getAddress(), address);
        assertEquals("Avatar correct", user.getAvatarUrl(), avatar);
        assertEquals("Last modified correct", user.getLastModified(), lastModified);
    }


    public void testLogout() {
        AccountManager.logout(loginActivity, false);
        assertTrue("User logged out (device)", !AccountManager.isUserLoggedIn(loginActivity));
    }


    private String generateRandomString(int length, String allowedChars, String mustHaveChars) {

        if (mustHaveChars.length() > length) {
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
        for (int i = 0; i < mustHaveChars.length(); i++) {
            char mustHave = mustHaveChars.charAt(i);

            int index = -1;
            do {
                index = random.nextInt(length);
            } while (mustHaveCharsIndices.contains(index));

            mustHaveCharsIndices.add(index);
            builder.setCharAt(index, mustHave);
        }

        return builder.toString();
    }
}
