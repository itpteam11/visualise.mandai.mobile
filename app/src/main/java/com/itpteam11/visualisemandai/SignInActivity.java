package com.itpteam11.visualisemandai;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

/**
 * This activity allows user to sign in into the application
 */

public class SignInActivity extends AppCompatActivity implements OnClickListener {
    private static final String TAG = "SignInActivity";

    //Declaring activity's widgets
    private Button btnSignIn;
    private EditText etEmail, etPassword;

    //Firebase Authentication
    private FirebaseAuth authentication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        //Initialising widgets
        btnSignIn = (Button) findViewById(R.id.signin_activity_button_signin);
        etEmail = (EditText) findViewById(R.id.signin_activity_edittext_email);
        etPassword = (EditText) findViewById(R.id.signin_activity_edittext_password);

        //Assign on click listener to button
        btnSignIn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Button button = (Button) v;

        //Run respective switch statement accordingly to the pressed button
        switch(button.getId()) {
            case R.id.signin_activity_button_signin:
                if (!(etEmail.getText().toString().equals("")) && !(etPassword.getText().toString().equals(""))) {
                    // Signing in using Firebase Authentication with given email address and password
                    authentication.getInstance().signInWithEmailAndPassword(etEmail.getText().toString(), etPassword.getText().toString())
                            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (!task.isSuccessful()) {
                                        Log.d(TAG, "Authentication Unsuccessful " + task.getException());

                                        //Display toast when sign in unsuccessful
                                        Toast.makeText(getApplicationContext(), "Oops, sign in failed!\nPlease enter valid email or password", Toast.LENGTH_LONG).show();
                                    } else {
                                        Log.d(TAG, "Authentication Successful");

                                        //Intent to MainActivity when sign in successful
                                        Intent mainActivity = new Intent(SignInActivity.this, MainActivity.class);
                                        mainActivity.putExtra("userID", FirebaseAuth.getInstance().getCurrentUser().getUid().toString());
                                        startActivity(mainActivity);
                                    }
                                }
                            });
                } else {
                    Toast.makeText(getApplicationContext(), "Oops, sign in failed!\nPlease enter valid email or password", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
}
