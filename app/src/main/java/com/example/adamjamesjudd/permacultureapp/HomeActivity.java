package com.example.adamjamesjudd.permacultureapp;

/**
this activity serves as a login screen for users. Firebase Auth will provide the
credential authentication service
 */
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity implements ApplicationConstants{
    //declare a static variable to handle passing of activity identifier strings in intents
    public static final String ACTIVITY_IDENTIFICATION_KEY = "From activity: ";

    //declare an authentication instance to handle user authentication
    private FirebaseAuth firebaseAuth;
    //declare widget fields for the email, password and credential memorisation for authentication purposes and submission
    private EditText emailInput;
    private EditText passwordInput;
    private CheckBox rememberDetails;
    private Button loginButton;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setTitle(R.string.title_home_activity);

        //get the default application FirebaseAuth instance
        firebaseAuth = FirebaseAuth.getInstance();

        //TODO: the below call should be replaced - function should be that if logged in, skip login screen
        firebaseAuth.signOut();

        //initialise the references to the widgets to retrieve their data
        emailInput = (EditText) findViewById(R.id.home_email_editText);
        passwordInput = (EditText) findViewById(R.id.home_password_editText);
        rememberDetails = (CheckBox) findViewById(R.id.home_checkbox);
        loginButton = (Button) findViewById(R.id.home_login_button);
        registerButton = (Button) findViewById(R.id.home_register_button);

        //set up onClickListeners for the buttons as necessary
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loginUser()) {
                    memoriseDetails(!rememberDetails.isChecked(),
                            emailInput.getText().toString(),
                            passwordInput.getText().toString());
                }
            }
        });
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToRegister();
            }
        });

        //set up textWatcher listeners on the EditTexts as necessary
        emailInput.addTextChangedListener(new SimpleTextWatcher(){
            @Override
            public void afterTextChanged(Editable email) {
                super.afterTextChanged(email);
                /**
                 * afterTextChanged override handles disabling error message if email is valid
                 * compexity: equal to inner complexity of RegistrationActivity.emailIsValid()
                 */

                //set the email error to null if valid
                if (RegistrationActivity.emailIsValid(email.toString())){
                    emailInput.setError(null);
                }


            }
        });
        passwordInput.addTextChangedListener(new SimpleTextWatcher(){
            @Override
            public void afterTextChanged(Editable email) {
                /**
                 * afterTextChanged override handles disabling error message if password is valid
                 * :complexity: equal to inner complexity of RegistrationActivity.passwordIsValid()
                 */

                super.afterTextChanged(email);
                //set the password error to null if valid
                if (RegistrationActivity.passwordIsValid(email.toString())){
                    passwordInput.setError(null);
                }
            }
        });
    }

    //TODO: Google Play Services outdated preventing Auth functionality - see same TODO in RegistrationActivity

    private boolean loginUser(){
        /**
         * loginUser handles attempting a logon using the credentials supplied by the user
         * as well as displaying errors to the user as necessary
         *
         * :complexity: O(1), FirebaseAuth abstracted methods carry large overheads, however, and string matching is likely
         * best case O(n + m), n = userEmail.length(), m = userPassword.length()
         */

        String userEmail = emailInput.getText().toString();
        String userPassword = passwordInput.getText().toString();

        boolean loginViable = true;

        if (!RegistrationActivity.emailIsValid(userEmail)){
            loginViable = false;
            emailInput.setError(getResources().getString(R.string.error_invalid_email));
        }

        if (!RegistrationActivity.passwordIsValid(userPassword)){
            loginViable = false;
            passwordInput.setError(getResources().getString(R.string.error_invalid_password));
        }

        //if the loginViable flag is now false (login invalidated), skip to return statement
        if (loginViable) {

            try {
                //attempt sign-in via Firebase
                firebaseAuth.signInWithEmailAndPassword(userEmail, userPassword).addOnCompleteListener(
                        new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    //successful login will automatically transfer the user to the virtual garden
                                    //as this is the most commonly relevant activity
                                    goToVirtualGarden();
                                } else {
                                    //inform the user of general invalid inputs
                                    toastNotification("Could not sign in account, credentials invalid",
                                            HomeActivity.this,true);
                                }
                            }
                        });
            } catch (Exception e) {
                //handle displaying error
            }

            loginViable = firebaseAuth.getCurrentUser() != null;

            if (loginViable) {
                //pass by intent to the homepage for the knowledge bank component
                Intent intent = new Intent(this, KnowledgeBankHomeActivity.class);
                intent.putExtra(ACTIVITY_IDENTIFICATION_KEY, HOME_IDENTIFIER);
                startActivity(intent);
            }
        }

        return loginViable;
    }

    private void goToRegister(){
        /**
         * goToRegister handles passing by intent to the registration activity
         * where the user can create an account
         * should also handle passing filled email/password fields in register activity if possible
         *
         * :complexity: O(1)
         */
        Intent intent = new Intent(this, RegistrationActivity.class);
        intent.putExtra(ACTIVITY_IDENTIFICATION_KEY, HOME_IDENTIFIER);

        //put extra data to fill email and password for user convenience
        intent.putExtra(CREDENTIAL_EMAIL_KEY, emailInput.getText().toString());
        intent.putExtra(CREDENTIAL_PASSWORD_KEY, passwordInput.getText().toString());

        startActivity(intent);
    }

    private void goToVirtualGarden(){
        /**
         * goToVirtualGarden handles passing by intent to the virtual garden activity
         * should also handle passing filled email/password fields in register activity if possible
         *
         * :complexity: O(1)
         */
        /*
        Intent intent = new Intent(this, VirtualGardenActivity.class);
        */
        Intent intent = new Intent(this, KnowledgeBankUploadActivity.class); //remove later
        intent.putExtra(ACTIVITY_IDENTIFICATION_KEY, HOME_IDENTIFIER);

        startActivity(intent);
    }

    //TODO: test memoriseDetails
    private boolean memoriseDetails(boolean deletion, String validatedEmail, String validatedPassword){
        /**
         * memoriseDetails should be called in login button's onclick.
         * this method handles saving the user's login details to persistent storage and returns success of operation
         * NOTE: current method is sharedPref. More secure variants may be available
         * NOTE: do not call this method until details are validated
         *
         * email/password strings should be recorded, if deletion then any stored
         * data should be removed
         *
         * :complexity: O(1)
         */

        //get the general sharedpref file using default mode
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);

        //get the handle to the sharedPref editor
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (deletion){
            //remove from the preferences file the email and password
            editor.remove(CREDENTIAL_EMAIL_KEY).remove(CREDENTIAL_PASSWORD_KEY);
        }
        else {
            //insert into the preferences file the email and password
            editor.putString(CREDENTIAL_EMAIL_KEY, validatedEmail).
                    putString(CREDENTIAL_PASSWORD_KEY, validatedPassword);
        }

        return editor.commit();
    }

    //TODO: test fillFields
    private void fillFieldsIfMemorised(){
        /**
         * this method handles filling the edittext input fields from persistent storage
         * if there is no data at the storage location fields cannot be filled - return empty Strings
         * refer to memoriseDetails() for notes
         *
         * :complexity: O(1)
         */

        //get the general sharedPref file using default mode
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);

        //get the stored credentials, if any
        String storedEmail = sharedPreferences.getString(CREDENTIAL_EMAIL_KEY, "");
        String storedPassword = sharedPreferences.getString(CREDENTIAL_PASSWORD_KEY, "");

        //fill the fields with retrieved values (or empty ones)
        emailInput.setText(storedEmail);
        passwordInput.setText(storedPassword);
    }

    private class SimpleTextWatcher implements TextWatcher{
        /**
         * this convenience class is a listener to be assigned to each input field
         * ANONYMOUS IMPLEMENTATION MUST OVERRIDE AT LEAST ONE METHOD TO APPLY FUNCTIONALITY
         */

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {}
    }


    public static void toastNotification(String message, Context activityContext, boolean isLongToast){
        /**
         * utility method to be used anywhere in the project
         * creates a toast of specific length and message from any activity
         * does not have an action
         *
         * :complexity: O(1)
         */
        Toast.makeText(activityContext,
                message,
                isLongToast ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }
}
