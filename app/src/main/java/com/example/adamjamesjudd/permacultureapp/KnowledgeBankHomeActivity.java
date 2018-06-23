package com.example.adamjamesjudd.permacultureapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class KnowledgeBankHomeActivity extends AppCompatActivity implements ApplicationConstants{

    //declare widget fields for the submission/collection of user inputs
    private Button documentUploadButton;
    private Button documentSearchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_knowledge_bank_home);
        setTitle(R.string.title_bank_home_activity);

        //initialise the references to the widgets to retrieve their data
        documentSearchButton = findViewById(R.id.bank_search_button);
        documentUploadButton = findViewById(R.id.bank_upload_button);

        //set up onClickListeners for the buttons as necessary
        documentUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToUpload();
            }
        });

        documentSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HomeActivity.toastNotification("This feature is currently unavailable",
                        KnowledgeBankHomeActivity.this, true);
                //TODO: goToSearch(); , once the functionality is implemented
            }
        });
    }

    private void goToUpload(){
        /**
         * this method handles pass by intent to the Bank's Document Uploader
         * selection by picker is performed and the upload can be finalised before
         * returning to this activity
         *
         * :complexity: O(1)
         */

        Intent intent = new Intent(this, KnowledgeBankUploadActivity.class);
        intent.putExtra(ACTIVITY_IDENTIFICATION_KEY, BANK_HOME_IDENTIFIER);
        startActivity(intent);
    }

    //TODO: finish goToSearch to pass by intent to search activity
    private void goToSearch(){
        /**
         * this method handles pass by intent to the Bank's Document Searcher
         * from there searching can be narrowed
         *
         * :complexity: O(1)
         */
    }
}
