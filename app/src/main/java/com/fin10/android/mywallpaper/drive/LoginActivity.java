package com.fin10.android.mywallpaper.drive;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.fin10.android.mywallpaper.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public final class LoginActivity extends Activity {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginActivity.class);

    private static final String ACTION_SIGN_IN = "sign-in";
    private static final String ACTION_SIGN_OUT = "sign-out";

    private static final int REQUEST_CODE_SIGN_IN = 1;

    public static void login(@NonNull Fragment fragment, int requestCode) {
        Intent intent = new Intent(fragment.getActivity(), LoginActivity.class);
        intent.setAction(ACTION_SIGN_IN);
        fragment.startActivityForResult(intent, requestCode);
    }

    public static void logout(@NonNull Fragment fragment, int requestCode) {
        Intent intent = new Intent(fragment.getActivity(), LoginActivity.class);
        intent.setAction(ACTION_SIGN_OUT);
        fragment.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder()
                .requestScopes(new Scope(Scopes.DRIVE_FILE))
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(this, gso);

        String action = getIntent().getAction();
        if (ACTION_SIGN_IN.equals(action)) {
            startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
        } else if (ACTION_SIGN_OUT.equals(action)) {
            client.signOut()
                    .addOnSuccessListener(v -> {
                        LOGGER.info("Sign out succeed.");
                        setResult(RESULT_OK);
                    })
                    .addOnFailureListener(e -> {
                        LOGGER.error(e.getLocalizedMessage(), e);
                        setResult(RESULT_CANCELED);
                    })
                    .addOnCompleteListener(v -> finish());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LOGGER.debug("requestCode:{}, resultCode:{}", requestCode, resultCode);
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN: {
                LOGGER.info("Sign in succeed.");
                setResult(RESULT_OK);
                break;
            }
        }

        finish();
    }

    @Override
    public void onBackPressed() {
        //Should be ignored
    }
}
