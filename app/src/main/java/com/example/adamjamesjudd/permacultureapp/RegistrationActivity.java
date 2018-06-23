package com.example.adamjamesjudd.permacultureapp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthEmailException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegistrationActivity extends AppCompatActivity implements ApplicationConstants{

    //declare an authentication instance to handle user authentication
    private FirebaseAuth firebaseAuth;
    //declare widget fields for the submission/collection of user inputs
    private EditText firstNameInput;
    private EditText lastNameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText passwordConfirmInput;
    private Button registerButton;

    //declare booleans to measure success of asynchronous firebase operations
        //(class level since internal onCompleteListeners cannot access local variables)
    private boolean verificationEmailSuccess;
    private boolean profileUpdateSuccess;
    private boolean registrationSuccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        setTitle(R.string.title_registration_activity);

        //get an instance of the FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();

        //initialise the references to the widgets to retrieve their data
        firstNameInput = (EditText) findViewById(R.id.registration_first_name_editText);
        lastNameInput = (EditText) findViewById(R.id.registration_last_name_editText);
        emailInput = (EditText) findViewById(R.id.registration_email_editText);
        passwordInput = (EditText) findViewById(R.id.registration_password_editText);
        passwordConfirmInput = (EditText) findViewById(R.id.registration_confirm_password_editText);
        registerButton = (Button) findViewById(R.id.registration_register_button);

        //get extra from Home based intent to fill email/password fields for user convenience
        Bundle intentBundle = getIntent().getExtras();
        //TODO: the pass in of the activity identifier does not work
        if (intentBundle != null){ //verify non-null intent bundle
            String activityIdentifier = intentBundle.getString(ACTIVITY_IDENTIFICATION_KEY);
            if (activityIdentifier != null) { //double check non-null activity identifier
                switch (activityIdentifier) {
                    case HOME_IDENTIFIER: //if intent was received from Home (login)
                        //get the passed values (may be empty strings)
                        String passedEmail = intentBundle.getString(CREDENTIAL_EMAIL_KEY);
                        String passedPassword = intentBundle.getString(CREDENTIAL_PASSWORD_KEY);

                        //fill the fields in this activity
                        emailInput.setText(passedEmail);
                        passwordInput.setText(passedPassword); //do not set the confirm pass field since that may confuse
                        break;
                }
            }
        }


        //set up onClickListeners for the buttons as necessary
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegistration();
            }
        });
    }

    private void attemptRegistration(){
        /**
         * attemptRegistration collects user input, validates and either submits the registration via Firebase auth, logs
         * in the new user and updates their profile with the name (minimum information required)
         * or displays appropriate errors to the user on each input field that is invalid
         *
         * :complexity: asynchronous
         *
         * ERROR - Google Play Services version is too low, Firebase Auth functionality does not work
         * Note: upon resolution also delete matching 'to do' in HomeActivity (~line #62)
         * potential cause: need to update google play services on emulator
         *      progress - tried and failed, research how to update the service 01/05/18
         *      progress - ISSUE APPEARS RESOLVED 03/05 - monitor for more issues and test on physical device
         *
         * potential cause: https://github.com/firebase/FirebaseUI-Android/issues/1104
         *      progress - unlikely to be the issue since API 27 Pixel prompts to update GP Services on this method invocation
         *
         * potential cause: FirebaseAuth implementation SDK vers. too low (11.8.0)?
         *      progress - updated (along with Storage SDK) to 15.0.0 - not a solution
         */

        boolean registrationViable = true;
        //measure success of asynchronous operation
        registrationSuccess = false;

        //collect inputs from each of the fields
        final String userFirstName = firstNameInput.getText().toString();
        final String userLastName = lastNameInput.getText().toString();
        final String userEmail = emailInput.getText().toString();
        final String userPassword = passwordInput.getText().toString();
        final String userConfirmedPassword = passwordConfirmInput.getText().toString();

        if (userFirstName.isEmpty()){ //verify firstname
            registrationViable = false;
            firstNameInput.setError(getResources().getString(R.string.error_field_required));
        }

        if (userLastName.isEmpty()){ //verify lastname
            registrationViable = false;
            lastNameInput.setError(getResources().getString(R.string.error_field_required));
        }

        if (!emailIsValid(userEmail)){ //verify email
            registrationViable = false;
            emailInput.setError(getResources().getString(R.string.error_invalid_email));
        }

        if (!passwordIsValid(userPassword)){ //verify password
            registrationViable = false;
            passwordInput.setError(getResources().getString(R.string.error_invalid_password));
            passwordConfirmInput.setText("");
        }

        if (!userPassword.equals(userConfirmedPassword) || userConfirmedPassword.equals("")){ //verify password confirmation
            passwordConfirmInput.setError(userConfirmedPassword.equals("") ?
                    getResources().getString(R.string.error_field_required) :
                    getResources().getString(R.string.error_password_nonmatch));
            registrationViable = false;
        }

        if (registrationViable){
            //name formatting unnecessary, potentially inaccurate
                //method could be improved and deprecated or removed
            //final String formattedFirstName = formatName(userFirstName);
            //final String formattedLastName = formatName(userLastName);

            boolean userInvalid = false;

            //**generate the user
            firebaseAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                HomeActivity.toastNotification("User created successfully",
                                        RegistrationActivity.this, true);
                                //if the task is successful, initiate the asynchronous user update method
                                updateProfile(userFirstName, userLastName);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        /**
                         * handle the errors that can be thrown when the user creation is unsuccessful
                         * found at: https://www.techotopia.com/index.php/Handling_Firebase_Authentication_Errors_and_Failures#FirebaseAuth_Error_Codes
                         * also stored with this file for longevity
                         */
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //TODO: handle specific errors in user creation and toast for user
                            String errorCode = null;

                            if (e instanceof FirebaseAuthInvalidCredentialsException){

                            }
                            else if(e instanceof FirebaseAuthInvalidUserException){
                                errorCode = ((FirebaseAuthInvalidUserException)e).getErrorCode();
                                switch (errorCode){
                                    //cases represent the specific errors (see above)
                                }
                            }
                            else if (e instanceof FirebaseAuthEmailException){

                            }
                        }
                    });
        }
    }

    private void updateProfile(String firstName, String lastName){
        /**
         * this method handles accessing the user's profile and updating their
         * name (this is minimal profile details requirement and display name
         *
         * :complexity: unknown, Firebase internal methods are costly
         */

        //measure success of asynchronous operation
        profileUpdateSuccess = false;

        //generate a request and fill the display name with the names provided
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(firstName + " " + lastName)
                .build();

        //get the current user instance
        FirebaseUser user = firebaseAuth.getCurrentUser();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            HomeActivity.toastNotification("Profile filled successfully - registration complete",
                                    RegistrationActivity.this, true);
                            //if the profile was successfully updated, send the verification email as the
                            //final step in the profile creation
                            sendVerificationEmail();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        HomeActivity.toastNotification("Profile could not be filled",
                                RegistrationActivity.this, true);
                        //in the case where the profile couldn't be updated, we delete the newly created user
                        deleteCurrentUser();
                    }
                });
    }

    private void sendVerificationEmail(){
        /**
         * ERROR: method was broken, always resulted in failure to send verification.
         * Potential issues:
         *      potential cause - https://stackoverflow.com/questions/39970755/firebase-email-verification-mail-not-working-an-internal-error-has-occurred
         *      progress - not the issue, tried to expand list of accepted email domains and test on unused email, failed (09/06/18)
         *
         *      potential cause - user is not being logged in at time of method call
         *      progress - confirmed not the case, Log.e performed on the current user's email with 100% positive results (09/06/18)
         *
         *      potential cause - some other internal error - need to identify
         *      progress - logged the error code using try/catch and the e.getLocalisedMessage() function
         *      progress - failed to log error, no error was thrown (09/06/18)
         *      progress - added an onFailureListener and repeated the process. Resulting message:
         *          "There is no user record corresponding to this identifier. The user may have been deleted."
         *      progress - trying to move the sendVerification and updateProfile outside of the onComplete of createUserWithEAP
         *      progress - sendVerEmail itself crashing, getCurrentUser returns null (09/06/18)
         *
         *      potential cause - async operations were executed sequentially, rather than nested inside onComplete callbacks
         *      progress - ISSUE RESOLVED as of 11/06/18 by nesting async Firebase calls
         *
         *
         * sendVerificationEmail handles sending the email to the user to verify their account
         * it is the final async operation in the chain createUserWithE&P() > updateProfile() > sendVerEmail()
         *
         * :complexity: asynchronous
         */

        final FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        //send the verification email and monitor for a successful email delivery using an OnCompleteListener
        currentUser.sendEmailVerification()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            HomeActivity.toastNotification("Verification email sent to the provided address",
                                    RegistrationActivity.this, true);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        HomeActivity.toastNotification("Could not send verification email - please try again later from the profile page",
                                RegistrationActivity.this, true);
                    }
                });
    }

    private void deleteCurrentUser() {
        /**
         * this method deletes the currently logged in user.
         * This is invoked when the user's profile could not be updated, but the user was created
         * effectively invalidating the registration
         *
         * :complexity: asynchronous
         */
        firebaseAuth.getCurrentUser().delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            HomeActivity.toastNotification("Account could not be created - please try again later",
                                    RegistrationActivity.this, true);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        HomeActivity.toastNotification("Invalid account creation - please contact the system administrator",
                                RegistrationActivity.this, true);
                    }
                });
    }

    private static String formatName(String name){
        /**
         * formatName takes a String reference and transforms it to match typical naming conventions
         * - name begins with capital
         * - all other letters are lowercase
         *
         * :complexity: O(n + n), n = name.length()
         */

        return(name.substring(0, 1).toUpperCase() +
                name.substring(1).toLowerCase());
    }

    //public visibility granted for use in HomeActivity
    public static boolean emailIsValid(String emailAddress){
        /** emailIsValid returns validity of email address
         * due to additional information known about the nature of user email addresses (monash student emails)
         * a specific validation method can be added.
         *
         * NOTE: exceptions may need to apply into the future for community members
         * they can be added manually via firebase console or an exception can be programmatically generated
         *
         * :complexity: O(n + n + n + cn), n = emailAddress.length(), c = total length of all elements in VALID_DOMAINS
         */

        boolean isValid = true;

        final String[] VALID_DOMAINS = {"student.monash.edu", "monash.edu", "monashclubs.org", "gmail.com"}; //TODO: remove last domain at release-time

        int atPosition = -1;
        //find the '@', exactly one should exist
        for (int charIndex = 0; charIndex < emailAddress.length(); charIndex++){
            if (emailAddress.charAt(charIndex) == '@'){
                if (atPosition != -1){ //if an '@' has already been located, email is invalid
                    isValid = false;
                    break;
                }
                else {
                    atPosition = charIndex;
                }
            }
        }

        //validity is now computed based on at position an isValid
        isValid = isValid && (atPosition != -1);

        //proceed only if the email is still valid
        if (isValid) {
            //get the local and domain components of the email address
            String local = emailAddress.substring(0, atPosition);
            String domain = emailAddress.substring(atPosition + 1, emailAddress.length());

            //validate local substring
            isValid = !(numberOfSpecialCharsInString(local) > 1); //only valid email local identifier with a special character has one '.'

            //validate domain substring - only if the local substring was deemed valid
            boolean domainRecognised = false;
            for (String s : VALID_DOMAINS) {
                if (domain.equals(s)) {
                    domainRecognised = true;
                    break;
                }
            }

            isValid = isValid && domainRecognised; //validity retained only if all previous checks are valid and domain also is
        }
        return isValid;
    }

    //public visibility granted for use in HomeActivity
    public static boolean passwordIsValid(String password){
        /**
         * passwordIsValid uses custom requirements to determine password validity
         * current rules:
         * - 10 to 20 characters (balance memorability and security)
         * - upper and lower case mixture
         * - at least one special character
         * - at least one digit
         *
         * NOTE: modify generatePassword if rules change
         * :complexity: O(n + n + n), n = password.length()
         */

        boolean isValid = true;
        int passwordLength = password.length();
        if (    passwordLength < 10 ||
                passwordLength > 20 ||
                !stringContainsCaseMix(password) ||
                numberOfSpecialCharsInString(password) == 0 ||
                numberOfDigitsInString(password) == 0){
            isValid = false;
        }
        return isValid;
    }

    private static boolean stringContainsCaseMix(String testString){
        /*
         * stringContainsCaseMix returns whether a string argument contains both
         * upper and lower case characters
         * :complexity: O(n), n = testString.length() (large comparison count means high hidden constant)
         */

        //constants for unicode boundaries
        final int UNICODE_UPPER_TOP_BOUND = 90;
        final int UNICODE_UPPER_BOTTOM_BOUND = 65;
        final int UNICODE_LOWER_TOP_BOUND = 122;
        final int UNICODE_LOWER_BOTTOM_BOUND = 97;
        //boolean result variables
        boolean containsUpper = false;
        boolean containsLower = false;
        //loop through each character and verify if upper or lower
        for(int charIndex = 0; charIndex < testString.length(); charIndex++){
            int charUnicode = (int)testString.charAt(charIndex);
            //check if upper only if no upper characters have been identified
            if (!containsUpper) {
                if (charUnicode <= UNICODE_UPPER_TOP_BOUND && charUnicode >= UNICODE_UPPER_BOTTOM_BOUND) {
                    containsUpper = true;
                }
            }
            //check if lower only if no lower characters have been identified
            if (!containsLower) {
                if (charUnicode <= UNICODE_LOWER_TOP_BOUND && charUnicode >= UNICODE_LOWER_BOTTOM_BOUND) {
                    containsLower = true;
                }
            }
            //break if both are found
            if (containsLower && containsUpper){
                break;
            }
        }
        return containsLower && containsUpper;
    }

    private static int numberOfSpecialCharsInString(String testString){
        /*
         * numberOfSpecialCharsInString returns (int) number of chars in testString
         * that are not alphanumeric
         * :complexity: O(n), n = testString.length()
         */

        //constants for unicode boundaries
        final int UNICODE_UPPER_TOP_BOUND = 90;
        final int UNICODE_UPPER_BOTTOM_BOUND = 65;
        final int UNICODE_LOWER_TOP_BOUND = 122;
        final int UNICODE_LOWER_BOTTOM_BOUND = 97;
        final int UNICODE_DIGIT_TOP_BOUND = 57;
        final int UNICODE_DIGIT_BOTTOM_BOUND = 48;

        int specialCount = 0;
        //loop through each character and verify if alphanumeric
        for (int charIndex = 0; charIndex < testString.length(); charIndex++){
            int charUnicode = (int)testString.charAt(charIndex);
            if ((charUnicode < UNICODE_DIGIT_BOTTOM_BOUND) || (charUnicode > UNICODE_LOWER_TOP_BOUND) ||
                    (charUnicode > UNICODE_DIGIT_TOP_BOUND && charUnicode < UNICODE_UPPER_BOTTOM_BOUND) ||
                    (charUnicode > UNICODE_UPPER_TOP_BOUND && charUnicode < UNICODE_LOWER_BOTTOM_BOUND)){
                specialCount++;
            }
        }

        return specialCount;
    }

    private static int numberOfDigitsInString(String testString){
        /*
         * numberOfDigitsInString returns (int) number of chars in testString
         * that are digits 0-9
         * :complexity: O(n), n = testString.length()
         */

        //constants for unicode boundaries
        final int UNICODE_DIGIT_TOP_BOUND = 57;
        final int UNICODE_DIGIT_BOTTOM_BOUND = 48;

        int digitCount = 0;
        //loop through each character and verify if alphanumeric
        for (int charIndex = 0; charIndex < testString.length(); charIndex++){
            int charUnicode = (int)testString.charAt(charIndex);
            if (charUnicode >= UNICODE_DIGIT_BOTTOM_BOUND || charUnicode <= UNICODE_DIGIT_TOP_BOUND){
                digitCount++;
            }
        }

        return digitCount;
    }

}
