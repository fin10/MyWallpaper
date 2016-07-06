package com.fin10.android.mywallpaper.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.fin10.android.mywallpaper.Log;
import com.fin10.android.mywallpaper.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

public final class LoginActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String INTENT_ACTION_LOGIN = "login";
    private static final String INTENT_ACTION_LOGOUT = "logout";

    private static final int REQUEST_CODE_CONNECT = 1;
    private static final int REQUEST_CODE_ERROR = 2;

    private GoogleApiClient mGoogleApiClient;

    static void login(@NonNull Fragment fragment, int requestCode) {
        Intent intent = new Intent(fragment.getActivity(), LoginActivity.class);
        intent.setAction(INTENT_ACTION_LOGIN);
        fragment.startActivityForResult(intent, requestCode);
    }

    static void logout(@NonNull Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setAction(INTENT_ACTION_LOGOUT);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d("[onCreate]");
        super.onCreate(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addScope(Drive.SCOPE_APPFOLDER)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("[onActivityResult] requestCode:%d, resultCode:%d", requestCode, resultCode);
        switch (requestCode) {
            case REQUEST_CODE_CONNECT: {
                if (resultCode == Activity.RESULT_OK) {
                    mGoogleApiClient.connect();
                    return;
                }
                break;
            }
        }

        finish();
    }

    @Override
    public void onBackPressed() {
        //for ignore
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("[onConnected] %s", Utils.toString(bundle));
        String action = getIntent().getAction();
        if (INTENT_ACTION_LOGIN.equals(action)) {
            setResult(RESULT_OK);
        } else if (INTENT_ACTION_LOGOUT.equals(action)) {
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.unregisterConnectionFailedListener(this);
            mGoogleApiClient.clearDefaultAccountAndReconnect();
        }

        finish();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d("[onConnectionSuspended] cause:%d", cause);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("[onConnectionFailed] %s", connectionResult.toString());
        String action = getIntent().getAction();
        if (INTENT_ACTION_LOGIN.equals(action)) {
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(this, REQUEST_CODE_CONNECT);
                    return;
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            } else {
                GoogleApiAvailability.getInstance().showErrorDialogFragment(this, connectionResult.getErrorCode(), REQUEST_CODE_ERROR,
                        new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                finish();
                            }
                        });
                return;
            }
        }

        finish();
    }
}
