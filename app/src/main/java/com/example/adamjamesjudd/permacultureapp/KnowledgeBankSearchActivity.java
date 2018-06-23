package com.example.adamjamesjudd.permacultureapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class KnowledgeBankSearchActivity extends AppCompatActivity implements ApplicationConstants{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_knowledge_bank_search);
        setTitle(R.string.title_bank_search_activity);
        //get the
        StorageReference sRef = FirebaseStorage.getInstance().getReference();
    }

    //get the type(s) of documents required by the user from a set of radiobuttons (or another option)
    //each of the documents is stored on FireBase Storage using metadata. This can be sourced with a call to
    //file.getMetadata
}
