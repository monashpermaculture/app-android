package com.example.adamjamesjudd.permacultureapp;

import java.util.ArrayList;

/**
 * all activities must implement this interface
 * contains constants used by more than one activity class
 */
public interface ApplicationConstants {
    //activity identification keys
    String ACTIVITY_IDENTIFICATION_KEY = "from_activity:";
    String HOME_IDENTIFIER = "home";
    String REGISTRATION_IDENTIFIER = "register";
    String BANK_HOME_IDENTIFIER = "knowledge_bank_home";
    String BANK_SEARCH_IDENTIFIER = "knowledge_bank_search";
    String BANK_UPLOAD_IDENTIFIER = "knowledge_bank_upload";
    String VIRTUAL_GARDEN_IDENTIFIER = "virtual_garden";


    //firebase constants
    String STORAGE_PATH_DOCUMENT_UPLOADS = "documents/";
    String STORAGE_METADATA_DOCTYPE = "Doctype"; //metadata keys should begin with a capital
        //constants to define database JSON hierarchy - nodes should be all lowercase
    String DATABASE_DOCUMENT_ROOT = "documents";
    String DATABASE_DOCUMENT_IDENTIFIER_KEY = "doc_reference";
    String DATABASE_MPG_ASSETS_ROOT = "mpg_assets";
    String DATABASE_MUCFARM_ASSETS_ROOT = "mucfarm_assets";


    //credential constants
    String CREDENTIAL_EMAIL_KEY = "stored_email:"; //used for both sharedpref and intent bundle
    String CREDENTIAL_PASSWORD_KEY = "stored_password:"; //used for both sharedpref and intent bundle

    //document types enumeration
    enum DOCTYPE {
        //DO NOT MODIFY THESE VALUES WITHOUT CONSULTING ORIGINAL AUTHOR
            //values should be listed in order of their precedence/use frequency, ordering is inconsequential
        Standard_Operating_Procedures,
        Administrative_Procedures,
        Induction_Handout,
        Club_Constitution,
        Meeting_Minutes,
        Financial_Audits,
        Infrastructure_Issue_Reports,
        Plant_Issue_Reports,
        Plant_Profiles;
    }

}
