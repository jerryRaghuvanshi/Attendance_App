package com.example.attendanceapp;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class AppController extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
