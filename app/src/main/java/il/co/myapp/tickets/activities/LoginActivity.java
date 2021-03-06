package il.co.myapp.tickets.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInApi;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;


import org.json.JSONObject;

import il.co.myapp.tickets.R;
import il.co.myapp.tickets.activities.fragments.GoogleLoginFragment;
import il.co.myapp.tickets.controller.AppController;
import il.co.myapp.tickets.data.AsyncLoginResponse;
import il.co.myapp.tickets.model.User;

public class LoginActivity extends FragmentActivity
{
    private static final String TAG = LoginActivity.class.getSimpleName();

    private User user = new User();
    private String nextScreen = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        findViewById(R.id.nextButtonLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginSuccess(user);
            }
        });

        nextScreen = getIntent().getStringExtra("nextScreen");

        findViewById(R.id.nextButtonLogin).setVisibility(View.GONE);
        findViewById(R.id.hiLoginTextPage).setVisibility(View.GONE);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Check if signed in in Google
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        // Check if signed in in Facebook
        final AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isFacebookLoggedIn = accessToken != null && !accessToken.isExpired();

        if (isFacebookLoggedIn) {
            Log.v(TAG,"FACEBOOK TOKEN IS " + accessToken.getToken());
            getFacebookDetails(accessToken);
            return;
        }

        if (account == null) {

        } else {


            user.setName(account.getDisplayName());
            user.setEmail(account.getEmail());
            user.setAccessToken(account.getIdToken());
            user.setLoginType(user.GOOGLE);

            setHelloText();

            findViewById(R.id.facebook_login_fragment).setVisibility(View.GONE);
            findViewById(R.id.nextButtonLogin).setVisibility(View.VISIBLE);
        }

    }



    public void getFacebookDetails(final AccessToken accessToken) {



        GraphRequest request = GraphRequest.newMeRequest(
                accessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject jsonObject,
                                            GraphResponse response) {

                        // Getting FB User Data
                        Bundle facebookData = getFacebookData(jsonObject);
                        user.setName(facebookData.getString("name"));
                        user.setEmail(facebookData.getString("email"));
                        user.setAccessToken(accessToken.getToken());
                        user.setLoginType(user.FACEBOOK);
                        setHelloText();

                        findViewById(R.id.google_login_fragment).setVisibility(View.GONE);
                        findViewById(R.id.nextButtonLogin).setVisibility(View.VISIBLE);

//                        loginSuccess(user);


                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,email");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private Bundle getFacebookData(JSONObject object) {
        Bundle bundle = new Bundle();

        try {
            String id = object.getString("id");


            bundle.putString("idFacebook", id);
            if (object.has("name"))
                bundle.putString("name", object.getString("name"));
            if (object.has("email"))
                bundle.putString("email", object.getString("email"));




        } catch (Exception e) {
            Log.d(TAG, "BUNDLE Exception : "+e.toString());
        }

        return bundle;
    }

    private void setHelloText () {
        findViewById(R.id.hiLoginTextPage).setVisibility(View.VISIBLE);
        TextView textView = (TextView) findViewById(R.id.hiLoginTextPage);
        String message = (String) textView.getText();
        textView.setText(String.format(message, user.getName()));
    }


    public void loginSuccess (final User user) {


        user.preformRegister(this, new AsyncLoginResponse() {
            @Override
            public void LoginResponseReceived(String response, Integer status) {
                if (response == "Success") {
//                    progressBar.setVisibility(View.GONE);
                    // set AppController's user to be the successful sign in - User object.
                    AppController.getInstance().setUser(user);
                    if ( null == nextScreen ) {
                        // Proceed to the Ticket Menu Options Activity with the successful signed-in user
                        startActivity(new Intent(LoginActivity.this, NewTicketOptionsActivity.class));
                    }else {
                        try {
                            /*
                            A call to Class.forName("X") causes the class named X to be dynamically loaded (at runtime).
                            Class.forName("X") returns the Class object associated with the "X" class.
                             The returned Class object is not an instance of the "x" class itself.
                             */
                            Class<?> c = Class.forName(nextScreen);
                            startActivity(new Intent(LoginActivity.this, c));
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                } else {
//                    progressBar.setVisibility(View.GONE);
                    if (status == 410) { // Means access token is invalid
                        Log.d(TAG,"Need to ask for new access token");

                    }else {
                        Toast.makeText(getApplicationContext(),
                                response, Toast.LENGTH_LONG).show();
                    }

                }
            }
        });



    }
}
