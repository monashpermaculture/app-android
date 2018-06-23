package com.example.adamjamesjudd.permacultureapp;

import android.support.v7.app.AppCompatActivity;

public class VirtualGardenActivity extends AppCompatActivity implements ApplicationConstants {

    //two-fragment layout, switching between the two

    //one fragment represents MUCF and one for MPG
    //https://stackoverflow.com/questions/22713128/how-can-i-switch-between-two-fragments-without-recreating-the-fragments-each-ti/22714222\

    //utilising an OnGestureListener to measure swipes. Swipes will switch between the two fragments
    //ViewPager class can be used to facilitate this functionalty
    //https://developer.android.com/training/animation/screen-slide

    //another fragment can be created at the bottom when the user swipes up from the bottom of the screen
    //this fragment handles asset creation

    //using a custom view as a canvas will be the method of creating the shapes, with a generalised method used for
    //creating specific assets (such as beds) - they will have predefined sizes.
    //Firebase Database will be used to store the positions and sizes of the shapes in the canvas, such as the method provided
}
