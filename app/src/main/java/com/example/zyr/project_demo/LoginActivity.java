package com.example.zyr.project_demo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    private static final String registerSuccess = "Register Successful";
    private static final String nameTaken = "Username already taken";
    private static final String wrongPass = "Sorry! invalid credentials.";
    private NetworkUtility mNetwork;

    // UI references.
    private AutoCompleteTextView mUserIDView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private TextInputLayout userIDWarpper;
    private TextInputLayout passwordWapper;
    private Button mregistButton;

    private Handler loginHandler = new Handler(){
        public void handleMessage(Message msg){
           if(msg.obj.equals(wrongPass)){
                Toast.makeText(getApplicationContext(),"Invalid credentials, Please try again!",Toast.LENGTH_SHORT).show();
            }
           else {
                Toast.makeText(getApplicationContext(),"Login Success", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("URL_user",msg.obj.toString());
                startActivity(intent);
            }
        }
    };

    private Handler registerHandler = new Handler(){
        public void handleMessage(Message msg){
            if (msg.obj.equals(registerSuccess)){
                Toast.makeText(getApplicationContext(), "Register Success, Please Login!", Toast.LENGTH_SHORT).show();
            }
            else if (msg.obj.equals(nameTaken)){
                Toast.makeText(getApplicationContext(), "Username is already taken, please try again", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(getApplicationContext(), "Register failed", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mNetwork = new NetworkUtility();
        // Set up the login form and initialize the layout widget.
        mUserIDView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();

        userIDWarpper = (TextInputLayout)findViewById(R.id.text_input_layout_1);
        passwordWapper = (TextInputLayout)findViewById(R.id.text_input_layout_2);

        mregistButton = (Button)findViewById(R.id.register_button);

        userIDWarpper.setHint("UserID");
        passwordWapper.setHint("Password");

        Button mDBTestButton = (Button)findViewById(R.id.DBtest);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            //When "Enter" is pressed, the method will be executed
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mUserIDSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mUserIDSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                //To Login
                attemptLogin();
            }
        });

        mregistButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //To Register
                attemptRegister();
            }
        });

        mDBTestButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent dbIntent = new Intent(LoginActivity.this, ShowDBDataActivity.class);
                startActivity(dbIntent);
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        /**
         * This part is used for test other activities' function before the loginActivity is fully functional
         */


        /*mtestButton = (Button)findViewById(R.id.test_button);
        mtestButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        */
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mUserIDView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mUserIDView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String userID = mUserIDView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(userID)) {
            mUserIDView.setError(getString(R.string.error_field_required));
            focusView = mUserIDView;
            cancel = true;
        } else if (!isEmailValid(userID)) {
            mUserIDView.setError(getString(R.string.error_invalid_email));
            focusView = mUserIDView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mNetwork.sendLoginRequest(userID, password, new HttpCallbackListener() {
                @Override
                public void onFinish(String response,Message message) {
                    loginHandler.sendMessage(message);
                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }
            });

        }
    }
    private void attemptRegister(){
        //To register here
        // Reset errors.
        mUserIDView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String userID = mUserIDView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(userID)) {
            mUserIDView.setError(getString(R.string.error_field_required));
            focusView = mUserIDView;
            cancel = true;
        } else if (!isEmailValid(userID)) {
            mUserIDView.setError(getString(R.string.error_invalid_email));
            focusView = mUserIDView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.

            //showProgress(true);
            mNetwork.sendRegisterRequest(userID, password, new HttpCallbackListener() {
                @Override
                public void onFinish(String response, Message message) {
                    registerHandler.sendMessage(message);
                }
                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }
            });

        }
    }

    private boolean isEmailValid(String ID) {
        return ID.length() > 1;
    }

    private boolean isPasswordValid(String password) {

        return password.length() > 3;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mUserIDView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

}

