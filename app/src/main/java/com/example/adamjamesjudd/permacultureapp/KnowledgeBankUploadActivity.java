package com.example.adamjamesjudd.permacultureapp;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.util.Strings;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class KnowledgeBankUploadActivity extends AppCompatActivity implements ApplicationConstants{
    /**
     * note for future developers - MIME (as in: mimeType) is Multipurpose Internet Mail Extensions
     * they are utilised by this class in order to identify doctypes for user uploads
     * some guidance taken from //https://www.simplifiedcoding.net/firebase-storage-uploading-pdf/
     * (the source code from that website also stored in a project with this one, for longevity)
     *
     */

    //declare references to the widgets
    private Button chooseDocButton;
    private Button uploadDocButton;
    private Spinner doctypeSpinner;
    private DatePicker documentChangedDatePicker;
    private TextView currentFileTextView;

    //declare reference to the Firebase Storage accessor
    private StorageReference storageRef;

    //declare a reference to the Firebase database accessor
    private DatabaseReference databaseRef;

    //int code returned from file chooser returned in onActivityResult()
    final static int PICK_DOCUMENT_CODE = 1;
    final static int GET_READ_PERMISSION_CODE = 2;

    //declare booleans to measure success of asynchronous firebase operations
    //(class level since internal onCompleteListeners cannot access local variables)
    private boolean documentUploadByUriSuccess = false;

    //track whether the date has been changed to a valid one
    private boolean dateValid = false;

    //keep a reference to the current doctype = enumerated by the AppConstants interface
        //this variable should be set only by the onItemSelectedListener SpinnerItemSelectionListener
    private String currentDoctype = null;
    //keep a reference to the name of the selected file between when the user chooses it and uploads it
    private String currentFileName = null;
    //keep a reference to the URI of the selected file as well
    private Uri currentUri = null;
    //keep a reference to the current file type (i.e. .pdf, .doc) as well
    private String currentFileType = null;

    //keep a reference to the arraylist of all doctypes (enumerated), must be initialised in onCreate
    private ArrayList<String> doctypes;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_knowledge_bank_upload);
        setTitle(R.string.title_bank_upload_activity);

        //initilise the doctype list
        doctypes = getDoctypes();

        //get references to the widgets
        chooseDocButton = (Button)findViewById(R.id.bank_choose_doc_button);
        uploadDocButton = (Button)findViewById(R.id.bank_confirm_upload_button);
        doctypeSpinner = (Spinner)findViewById(R.id.bank_upload_doctype_spinner);
        currentFileTextView = (TextView)findViewById(R.id.bank_current_file_textView);

        //TODO: use a datePickerDialogue instead of the DatePicker widget
        //documentChangedDatePicker = (DatePicker)findViewById(R.id.bank_document_upload_datePicker);

        //get a reference to the storage
        storageRef = FirebaseStorage.getInstance().getReference();

        //get a reference to the database
        databaseRef = FirebaseDatabase.getInstance().getReference();

        //attach onClickListener to buttons as appropriate
        chooseDocButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //go through the document picking process
                getDocument();
            }
        });

        uploadDocButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //perform the final upload, asking the user if they are certain about the action
                confirmDocumentUpload(currentFileName, currentFileType, currentDoctype);
            }
        });

        //attach adapter to the spinner
        //ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_item, doctypes);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, doctypes);
        //adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        doctypeSpinner.setAdapter(adapter);
        //set the top item to the currently selected item
        doctypeSpinner.setSelection(0);
        //attach the selection listener to the spinner - set currentDoctype
        doctypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentDoctype = doctypes.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentDoctype = null;
            }
        });

        //TODO: set the minimum date that can be attributed to a document being uploaded and initialise the picker to the current date
        //setMinAndMaxDates(documentChangedDatePicker);
        //setCurrentDate(documentChangedDatePicker);


    }

    private void getDocument(){
        /**
         * getDocument will call chooseDocumentUsingPicker if permission is valid, otherwise permission will be requested
         * the onRequestPermissionResult callback also invokes chooseDocumentUsingPicker if successful
         * the overriden onActivityResult callback will capture the file chosen, if any
         */
        //if the build version is running SDK 23 or higher (Lollipop), then we must explicitly confirm
        //external storage read permission - we can gain permission by intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

            //inbuilt method to compute if the user has previously denied permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)){
                //TODO: asynchronously show a message to the user to describe why the access is required

            }
            else{ //no explanation required, request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        GET_READ_PERMISSION_CODE);
            }
        }
        else{ //we have the permission, grab the file
            chooseDocumentUsingPicker();
        }
    }

    private void chooseDocumentUsingPicker(){
        //creating an intent for file chooser
        Intent intent = new Intent();
        //mimeTypes includes extensions (in order at time of writing) .pdf, .doc, .docx
        String[] mimeTypes = {"application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
        //ensure the picker can select from all three mime types
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        //get content is preferable to ACTION_PICK since the latter should utilise a specific URI defining the collection
            //in storage. See: https://stackoverflow.com/questions/17765265/difference-between-intent-action-get-content-and-intent-action-pick
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Document"), PICK_DOCUMENT_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        /**
         * callback is invoked after every call to ActivityCompat.requestPermissions() - in this case getDocument()
         * check requestCode matches to the code from the request call
         */
        //call to super method
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //cases are used to capture the permission code constants passed into the requestPermissions() calls
        switch (requestCode){
            case GET_READ_PERMISSION_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //if the permission is approved by the user, get the user to pick the doc by intent
                    chooseDocumentUsingPicker();
                }
                else {
                    //if the permission is not granted (or the request cancelled), notify the user they cannot perform the upload
                    HomeActivity.toastNotification("Document selection request aborted - access permission denied",
                            this, true);
                }
                return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //call to super method
        super.onActivityResult(requestCode, resultCode, data);

        //ensure the request was valid
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            //cases are used to capture the document code constant passed into the startActivityForResult call
            switch (requestCode) {
                case PICK_DOCUMENT_CODE:
                    //cache the returned document Uri
                    currentUri = data.getData();
                    //perform the upload since the URI is non-null
                    getPickedDocumentMetadata(currentUri);
                    return;
            }
        }
        else if (resultCode == Activity.RESULT_CANCELED){
            //if the document selection was cancelled - show a toast to display that
            HomeActivity.toastNotification("Document selection cancelled", this, true);
        }
    }

    private void getPickedDocumentMetadata(Uri documentUri) {
        /**
         * retreives the metadata of the chosen file and indicates to the user
         * the file they have currently selected for upload
         * the current metadata retrieved includes MIME and file name
         *
         * :complexity: O(1), theoretically, performance of query may vary depending on thread usage
         */
        String mimeType = getMimeTypeFromUri(documentUri);
        //NOTE: could potentially switch to retrieving the
        //extension and executing switch statement on that instead of MIME
            //progress: MIME deemed more secure as it is not modifiable, potential change ignored

        if (!mimeType.equals("")) {
            //get the filename and cache for upload method (filename + extension)
            currentFileName = getFileNameFromUri(documentUri);
            //TODO: get the file date from the user's input
            String fileMonthYear = getFileCreationDate();


            //doc name and path in firebase storage vary based upon the doctype
            switch (mimeType) {
                case "application/msword": //.doc
                    currentFileType = ".doc";
                    break;
                case "application/pdf": //.pdf
                    currentFileType = ".pdf";
                    break;
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document": //.docx
                    currentFileType = ".docx";
                    break;
                default:
                    //the default represents an invalid file type - invalidate all actions up to this point
                    currentUri = null;
                    currentFileType = null;
                    currentFileName = null;
                    return;
            }
            //set the current file textview for the user's benefit to reflect the chosen file
            currentFileTextView.setText(currentFileName);
        }


    }

    //TODO: seperate the upload process into file selection, then another button to actually submit
    //TODO: allow the user to view the file snapshot before they hit the final upload button (once created)
    private void uploadDocumentByUri(Uri documentUri, String fileName, String fileType, String doctype){
        /**
         * this method childs the root storage reference (within the document directory) and
         * adds in the new file with the relevant metadata
         * Upon successful upload, it calls the saveFileReference() method to asynchronously
         * store the downloadURL for the new file for later accessing
         *
         * :complexity: asynchronous
         */

        if (documentUri != null && !currentFileTextView.getText().toString().isEmpty()) {
            //declare the storage reference to point to the file
            final StorageReference ref = storageRef.child(STORAGE_PATH_DOCUMENT_UPLOADS + fileName);

            //perform the upload
            if (ref != null) {
                ref.putFile(documentUri, new StorageMetadata.Builder()
                        .setCustomMetadata(STORAGE_METADATA_DOCTYPE, doctype) //set the doctype (i.e. Plant Profile)
                        //TODO: set custom meta for the date of last modification
                        .setContentType(fileType) //set the file type (i.e. .pdf)
                        .build())
                        .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                if (task.isSuccessful()) {
                                    //notify the user that the document was successfully uploaded
                                    HomeActivity.toastNotification("Document " + currentFileName + " successfully uploaded",
                                            KnowledgeBankUploadActivity.this, true);
                                    //TODO: saveFileReference(ref); , this call creates an error
                                    //the function needs to be repaired in order to cache a reference to the file for later retreival
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                HomeActivity.toastNotification("Document '" + currentFileName + "' could not be uploaded - please try again later",
                                        KnowledgeBankUploadActivity.this, true);
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                //TODO: show upload progress
                            }
                        });
            } else {
                HomeActivity.toastNotification("File storage location could not be made - please contact the system administrator",
                        KnowledgeBankUploadActivity.this, true);
            }
        } else{ //if the document has not been chosen
            HomeActivity.toastNotification("A file must be chosen for upload",
                    KnowledgeBankUploadActivity.this, true);
        }
    }

    private void saveFileReference(StorageReference reference) {
        /**
         * this method gets an instance of Task<Uri> asynchronously
         * from the storage reference representing the new child for a
         * recently uploaded file.
         * Once retrieved successfully, it calls uploadReferenceToDatabase() to
         * asynchronously upload the downloadURL to the database
         *
         * :complexity: asynchronous
         */
        //call the getDownloadUrl() method to asynchronously retrieve the Task<Uri> instance
        reference.getDownloadUrl()
                .addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            HomeActivity.toastNotification("File cloud reference retrieved",
                                    KnowledgeBankUploadActivity.this, true);
                            //get the resulting URI and pass to the upload method for the final upload
                            uploadReferenceToDatabase(task.getResult());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        HomeActivity.toastNotification("Could not retrieve file cloud reference - please contact the system administrator",
                                KnowledgeBankUploadActivity.this, true);
                    }
                });
    }

    private void uploadReferenceToDatabase(Uri uri){
        //add the download URL of the uploaded file to firebase Database
        databaseRef.child(DATABASE_DOCUMENT_ROOT).child(DATABASE_DOCUMENT_IDENTIFIER_KEY).setValue(uri)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            HomeActivity.toastNotification("File cloud reference saved",
                                    KnowledgeBankUploadActivity.this, true);
                        }
                    }
                })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                HomeActivity.toastNotification("Could not save file cloud reference - please contact the system administrator",
                        KnowledgeBankUploadActivity.this, true);
            }
        });
    }

    private String getMimeTypeFromUri(Uri documentUri){
        //content resolver required in order to get the mime type, does not work using only the URI and MimeTypeMap
            //see here: https://stackoverflow.com/questions/12473851/how-i-can-get-the-mime-type-of-a-file-having-its-uri
            //also saved with source code for longevity

        ContentResolver resolver = getContentResolver();

        //OLD METHOD:
        //String mimeType = MimeTypeMap.getSingleton().
        //        getMimeTypeFromExtension(MimeTypeMap.
        //                getFileExtensionFromUrl(documentUri.getPath()));

        String mimeType = resolver.getType(documentUri);

        return mimeType;
    }

    private String getFileNameFromUri(Uri documentUri){
        /**
         * takes a Uri and computes the position of the file extension to substring it
         * and return just the filename
         *
         * :complexity: O(1) theoretically, performance of query may vary depending on thread usage
         */

        String result = null;
        if (documentUri.getScheme().equals("content")) {
            //return the file in a cursor using the URI to identify its address in storage
            Cursor cursor = getContentResolver().query(documentUri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) { //if the file was located
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)); //get the display name
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = documentUri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
    //TODO: complete this method, using a datePickerDialogue instead of the DatePicker widget

    private String getFileCreationDate(){
        /**
         * method to retrieve the date (month/year) of the file's creation (or most relevant date,
         * or most relevant other date - i.e. date of last modification
         *
         *:complexity: O(1)
         */
        String returnDate = "";
        //get the current date from the picker

        return returnDate;
    }


    private ArrayList<String> getDoctypes() {
        /**
         * retrieves as an array list of strings (formatted correctly) the doctypes available
         * doctypes can be added in the application constants interface (not removed).
         * Method reads all doctypes from the enum and formats them
         *
         * :complexity: O(n), where n is the total number of characters in all doctypes combined
         */
        ArrayList<String> dataset = new ArrayList<>();
        //default constructor initialises dataset - DO NOT MODIFY
        ApplicationConstants.DOCTYPE[] doctypes = ApplicationConstants.DOCTYPE.values();
        for (ApplicationConstants.DOCTYPE d : doctypes){
            dataset.add(splitOnUnderscore(d.toString())); //for each DOCTYPE, add a string representation to the dataset
        }
        return dataset;
    }

    private String splitOnUnderscore(String unsplit){
        /**
         * helper method to split the DOCTYPE values into valid English strings
         * :complexity: O(n), n = length of unsplit
         */
        String split = "";
        for (int i = 0; i < unsplit.length(); i ++){
            char currentChar = unsplit.charAt(i);
            if (currentChar == '_'){
                split += " ";
            }
            else{
                split += currentChar;
            }
        }
        return split;
    }

    //TODO: complete this method, using a datePickerDialogue instead of the DatePicker widget
    private boolean setMinAndMaxDates(DatePicker datePicker){
        /**
         * method used to set the minimum date of selection for a datepicker
         * the current earliest date for any club document is assumed to be 1/1/2011
         * as this was effectively the inception of "EatMonash", the previous governing body
         * for MUCF and MPG
         *
         * TODO: handle the case where the min max dates can't be set
         *
         * :complexity: O(1)
         */

        //formatted mindate - do not change
        final String minDateString = "Sat, Jan 1, '11";
        //create a date formatter in the english locale (same date format as Aus)
        final SimpleDateFormat format = new SimpleDateFormat("EEE, MMM d, ''yy", Locale.ENGLISH);

        try{
            //get the start date as a Date instance
            Date minDate = format.parse(minDateString);
            //set the mindate on the picker using the getTime to return millis since 1970
            datePicker.setMinDate(minDate.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }

        //set the max date to the current time
        datePicker.setMaxDate(System.currentTimeMillis());

        return true;
    }

    //TODO: complete this method, using a datePickerDialogue instead of the DatePicker widget
    private boolean setCurrentDate(DatePicker datePicker){
        /**
         * method to set the selected date on a datepicker to the date at the time of access
         * this should be called during a startup callback (i.e. onCreate() so that it appears
         * as the default value).
         *
         * :complexity: O(1)
         */

        //set the date using the System's current time

        return true;

    }

    private boolean confirmDocumentUpload(String fileName, String fileType, String doctype){
        /**
         * method to create a dialogue that asks the user to confirm their document upload after hitting the
         * "Perform Document Upload" button. users may make mistakes
         *
         * based on the result of the dialogue, this method will either notify the user of cancellation or
         * initiate the document upload asynchronously.
         *
         * This method therefore artificially creates a delay in the process of document upload
         *
         * :complexity: O(1), but depends on user's time since the dialogue remains indefinitely
         */

        //generate the message to be displayed to the user for confirmation
        //must provide all details the user has provided about the document
        final String message =
            "Are you sure you want to upload the " + fileType.substring(1) + " file '" + fileName + "'?" +
            "\nIt will be filed in the document category '" + doctype + "'.";

        //the following code taken from Maaalte and Nicholas Betsworth on StackOverflow
            //see: https://stackoverflow.com/questions/5127407/how-to-implement-a-confirmation-yes-no-dialogpreference

        //display the confirmation dialogue to the user
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialogue_document_confirm)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.action_upload_confirm_positive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        uploadDocumentByUri(currentUri, currentFileName, currentFileType, currentDoctype);
                    }})
                .setNegativeButton(R.string.action_upload_confirm_negative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //notify the user that the upload was cancelled
                        HomeActivity.toastNotification("Document upload cancelled",
                                KnowledgeBankUploadActivity.this, true);
                    }}).show();
        return true;
    }

}
