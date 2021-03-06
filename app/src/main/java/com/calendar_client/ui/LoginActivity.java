package com.calendar_client.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.calendar_client.R;
import com.calendar_client.data.User;
import com.calendar_client.utils.ApplicationConstants;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {
    private EditText etPassword;
    private EditText etEmail;
    private User user;

    private SharedPreferences sharedPreferences;
    private Button btnLogin;
    private TextView tvSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        etPassword = (EditText) findViewById(R.id.etPassword);
        etEmail = (EditText) findViewById(R.id.etEmail);


        // get user from shared preference. if doesnt exist return empty string
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String userJSON = sharedPreferences.getString("user", "");

        // if user exist, move on to home screen
        if (userJSON != null && !userJSON.equals("")) {
            Gson gson = new Gson();
            user =  gson.fromJson(userJSON,User.class);
            new RefreshTokenTask().execute();

            finish();
            Intent homeScreen = new Intent(this, CalendarActivity.class);
            startActivity(homeScreen);
        }

        // link layout components
        tvSignUp = (TextView) findViewById(R.id.tvSignUp);
        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean valid = true;

                String password = etPassword.getText().toString();
                if (password.isEmpty()) {
                    etPassword.setError(getString(R.string.login_password_error_empty));
                    valid = false;
                } else if (password.length() < 5) {
                    etPassword.setError(getString(R.string.login_password_error_length));
                    valid = false;
                } else {
                    etPassword.setError(null);
                }

                String email = etEmail.getText().toString();
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    etEmail.setError(getString(R.string.login_email_error_invalid));
                    valid = false;
                } else if (email.isEmpty()) {
                    etEmail.setError(getString(R.string.login_email_error_empty));
                    valid = false;
                } else {
                    etEmail.setError(null);
                }

                if (valid) {
                    btnLogin.setEnabled(false);
                    new LoginTask().execute();
                }
            }
        });

        tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signUpIntent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(signUpIntent);
            }
        });

    }

    private class LoginTask extends AsyncTask<String, Void, String> {
        String email;
        String password;

        // get the email and password - before executing task
        @Override
        protected void onPreExecute() {
            email = etEmail.getText().toString();
            password = etPassword.getText().toString();
        }

        // executing
        // make http request to the server, send email and password for verification of the user
        @Override
        protected String doInBackground(String... strings) {
            StringBuilder response;
            try {
                URL url = new URL(ApplicationConstants.LOGIN_URL + "?email=" + email + "&password=" + password);
                response = new StringBuilder();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return null;
                }

                BufferedReader input = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));

                String line;
                while ((line = input.readLine()) != null) {
                    response.append(line + "\n");
                }

                input.close();

                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            String responseString = response.toString();
            return responseString.toString();
        }

        // after executing task finished.
        @Override
        protected void onPostExecute(String response) {
            // if response not null it means user exist and i save it to the shared preference
            // in order to launch straight to home screen in the next time we open the application
            if (response != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                Gson gson = new Gson();
                user = gson.fromJson(response, User.class);
                String userJSON = gson.toJson(user);

                editor.putString("user", userJSON);
                editor.apply();

                new RefreshTokenTask().execute();

                Intent intent = new Intent(LoginActivity.this, CalendarActivity.class);
                startActivity(intent);
                LoginActivity.this.finish();

            } else { // if response is null - user doesnt exist
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, getResources().getString(R.string.login_error)
                        , Toast.LENGTH_LONG).show();
            }
        }

    }

    private class RefreshTokenTask extends AsyncTask<String, Void, String> {
        // get the email and password - before executing task
        String token;

        @Override
        protected void onPreExecute() {
            user.setToken(token);
            token = FirebaseInstanceId.getInstance().getToken();
            Log.e("TOKEN", token);
        }

        // executing
        // make http request to the server, send email and password for verification of the user
        @Override
        protected String doInBackground(String... strings) {
            StringBuilder response;
            try {
                URL url = new URL(ApplicationConstants.REFRESH_TOKEN_URL+"?userid="+user.getId()+"&token="+token);
                response = new StringBuilder();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return null;
                }

                BufferedReader input = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));

                String line;
                while ((line = input.readLine()) != null) {
                    response.append(line + "\n");
                }

                input.close();

                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            String responseString = response.toString();
            return responseString.toString();
        }

        // after executing task finished.
        @Override
        protected void onPostExecute(String response) {
            // if response not null it means user exist and i save it to the shared preference
            // in order to launch straight to home screen in the next time we open the application
            if (response != null) {


            } else { // if response is null - user doesnt exist

            }
        }

    }

}
