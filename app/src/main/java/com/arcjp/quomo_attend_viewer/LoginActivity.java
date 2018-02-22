package com.arcjp.quomo_attend_viewer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.onesignal.OneSignal;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import jp.co.tikyus.quomo.sdk;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {
    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("quomosdk");
    }
    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI reference s.
    private AutoCompleteTextView mEmp_idView;
    private View mProgressView;
    private View mLoginFormView;
    private Timer timer;
    private QuomoTimerTask timerTask = null;
    private sdk _quomosdk;
    private Boolean flgTimer = false;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FirebaseApp.initializeApp(this);
        FirebaseMessaging.getInstance();
        super.onCreate(savedInstanceState);
        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mEmp_idView = (AutoCompleteTextView) findViewById(R.id.emp_id);
        populateAutoComplete();

        Button RegisterButton = (Button) findViewById(R.id.register_button);
        RegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegister();
            }
        });

        Button ViewQuomoButton = (Button) findViewById(R.id.viewer_button);
        ViewQuomoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ViewQuomo();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmp_idView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptRegister() {
        if (mAuthTask != null) {
            return;
        }
        // ログインIDのビューオブジェクトを取得
        EditText editTextLoginAccount = (EditText)findViewById(R.id.emp_id);

        // 「pref_data」という設定データファイルを読み込み
        SharedPreferences pref = getSharedPreferences("pref",MODE_PRIVATE);
        SharedPreferences.Editor e = pref.edit();

        // 「str」に入力したログインIDを格納
        String str = editTextLoginAccount.getText().toString();
        e.putString("login_id",str);

        // 保存
        e.commit();

        // Reset errors.
        mEmp_idView.setError(null);

        // Store values at the time of the login attempt.
        String emp = mEmp_idView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid emp
        if (TextUtils.isEmpty(emp)) {
            mEmp_idView.setError(getString(R.string.error_field_required));
            focusView = mEmp_idView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(emp);
            mAuthTask.execute((Void) null);

            AlertDialog.Builder alertDialog=new AlertDialog.Builder(LoginActivity.this);
            //タイトルを設定する
            alertDialog.setTitle("登録完了");
            //メッセージ内容を設定する
            alertDialog.setMessage("社員番号:"+ str);
            //確認ボタン処理を設定する
            alertDialog.setPositiveButton("OK",new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int whichButton) {
                    setResult(RESULT_OK);
                }
            });
            alertDialog.create();
            AlertDialog dialog = alertDialog.show();

            // メッセージテキストを変更する。
            TextView textView = (TextView) dialog.findViewById(android.R.id.message);
            textView.setTextSize(30.0f);

        }
    }
    private void ViewQuomo() {
        setContentView(R.layout.activity_main);
        _quomosdk=new sdk();
        _quomosdk.QuomoInitialize();
        boolean licenseResult;
        licenseResult = _quomosdk.QuomoSetProductKey("e545491bd88ff5a5152f39327b283c2f04019245240a245c81c323cf033a80fbc3e4aec48af40561dbca2e7c9fb91568fdba6ed47a31251da09cce145b31fcb9");
        Log.d("MAIN","licenseResult:"+licenseResult);
        // 「SharedPreferences」からログインIDを取得
        SharedPreferences pref = getSharedPreferences("pref",MODE_PRIVATE);
        String str = pref.getString("login_id","0");
        // long型に変換
        long i = Long.parseLong(str);

        //firebaseに社員番号、端末トークン、出欠フラグ(false)を登録
        //一旦削除
        //FirebaseDatabase database = FirebaseDatabase.getInstance();
        //String Token = FirebaseInstanceId.getInstance().getToken();
        //DatabaseReference refemp = database.getReference("EmployeeData");
        //refemp.child(str).child("Attendflg").setValue(false);
        //refemp.child(str).child("Token").setValue(Token);

        _quomosdk.QuomoAddCode(i);
        //_quomosdk.QuomoAddCode(10001L);
        //_quomosdk.QuomoAddCode(21212123L);

        //ビューワー用タイマー設定
        timer = new Timer();
        timerTask = new QuomoTimerTask();
        timer.schedule(timerTask, 0, 1);
    }

    class QuomoTimerTask extends TimerTask {
        @Override
        public void run() {
            if(flgTimer) {
                return;
            }
            flgTimer = true;
            handler.post(new Runnable() {
                public void run() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Bitmap bitmap = _quomosdk.makeBitmap();
                            ImageView imageView = (ImageView) findViewById(R.id.imageView);
                            imageView.setImageBitmap(bitmap);
                            _quomosdk.deleteBitmap();
                            flgTimer = false;
                        }
                    });


                }
            });
        }
    }

    //private boolean isempValid(String emp) {
    //TODO: Replace this with your own logic
    //    return emp.contains("@");
    //}

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only emp
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmp_idView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;

        UserLoginTask(String email) {
            mEmail = email;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }

            for (String credential : DUMMY_CREDENTIALS) {
                String[] pieces = credential.split(":");
                if (pieces[0].equals(mEmail)) {
                    // Account exists, return true if the password matches.
                    return pieces[1].equals(mEmail);
                }
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

