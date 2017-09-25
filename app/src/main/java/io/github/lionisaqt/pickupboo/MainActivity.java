package io.github.lionisaqt.pickupboo;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_ALL = 7;
    private static final String TAG = "Boo";
    public static final String MYPREFS = "myprefs";
    public static final String CONTACT = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ask for permissions
        if (checkAndRequestPermissions()) {
            unlockElements(true);
        }
        else unlockElements(false);
    }

    // Checks if permissions are granted, and if they aren't, add them to a list so the phone can
    // ask users for the necessary permissions
    private boolean checkAndRequestPermissions() {
        int pLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int pSMS = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
        int pContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);

        // Holds all necessary permissions the app needs to ask for
        List<String> listPermissions = new ArrayList<>();

        // Adds permissions to list of they do not have permission
        if (pLocation != PackageManager.PERMISSION_GRANTED) listPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (pSMS != PackageManager.PERMISSION_GRANTED) listPermissions.add(Manifest.permission.SEND_SMS);
        if (pContacts != PackageManager.PERMISSION_GRANTED) listPermissions.add(Manifest.permission.READ_CONTACTS);

        if (!listPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissions.toArray(new String[listPermissions.size()]), PERMISSION_ALL);
            return false;
        }

        // All permissions granted
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ALL: {
                Map<String, Integer> perms = new HashMap<>();
                // Initialize the map with both permissions
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.SEND_SMS, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_CONTACTS, PackageManager.PERMISSION_GRANTED);

                // Fill with actual results from user
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);

                    // Check for both permissions
                    if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        unlockElements(true);
                    }

                    // One or both permissions denied
                    else {
                        Log.i(TAG, "some permissions are not granted, asking again");
                        // Permission is denied (this is the first time, when "never ask again" is not checked) so ask again explaining the usage of permission
                        // shouldShowRequestPermissionRationale will return true
                        // Show the dialog or snackbar saying its necessary and try again otherwise proceed with setup
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
                            showDialogOK("All requested permissions (location and send SMS services) are required for this app",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (which) {
                                                case DialogInterface.BUTTON_POSITIVE:
                                                    checkAndRequestPermissions();
                                                    break;
                                                case DialogInterface.BUTTON_NEGATIVE:
                                                    // App asks for permission(s) again
                                                    break;
                                            }
                                        }
                                    });
                        }
                        // Permission(s) denied (and "never ask again" is checked)
                        // shouldShowRequestPermissionRationale will return false
                        else {
                            Toast.makeText(this, "Go to settings and enable permissions", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        }
    }

    // Helper function that (un)locks on-screen elements when necessary
    public void unlockElements(boolean unlock) {
        EditText editPhone = (EditText)findViewById(R.id.numTextbox);
        EditText editMessage = (EditText)findViewById(R.id.messageTextbox);
        ImageButton contactsButton = (ImageButton)findViewById(R.id.contactsButton);
        Button sendButton = (Button)findViewById(R.id.button);

        if (unlock) {
            Log.i(TAG, "all permissions granted");
            editPhone.setHint(getApplicationContext().getText(R.string.phoneNumHint));
            editMessage.setHint(getApplicationContext().getText(R.string.messageHint));
            editPhone.setEnabled(true);
            editMessage.setEnabled(true);
            sendButton.setText(R.string.requestButton);

            // Turns on contact button's listener
            contactsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                    startActivityForResult(intent, 1);
                }
            });
            contactsButton.setEnabled(true);

        } else {
            Log.e(TAG, "not all permissions granted");
            editPhone.setHint(R.string.noPermissions);
            editMessage.setHint(R.string.noPermissions);
            editPhone.setEnabled(false);
            editMessage.setEnabled(false);
            sendButton.setText(R.string.goToSettings);
            contactsButton.setEnabled(false);
        }
    }

    // Grabs contact's phone number
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            Uri uri = data.getData();

            if (uri != null) {
                Cursor c = null;
                try {
                    c = getContentResolver().query(uri, new String[]{
                                    ContactsContract.CommonDataKinds.Phone.NUMBER},
                            null, null, null, null);
                    if (c != null && c.moveToFirst()) {
                        String number = c.getString(0);
                        enterNumber(number);
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        }
    }

    // Inputs contact's phone number into the EditText
    public void enterNumber(String number) {
        ((EditText)findViewById(R.id.numTextbox)).setText(number.replaceAll("\\D+", "")); // Removes all non-numeric characters

        // Saves regex'd number for future use
        SharedPreferences.Editor editor = getSharedPreferences(MYPREFS, 0).edit();
        editor.putString(CONTACT, ((EditText)findViewById(R.id.numTextbox)).getText().toString());
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Makes sure all permissions are still granted when reopening app
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            unlockElements(true);

            // Get last used phone number, if it is there
            SharedPreferences settings = getSharedPreferences(MYPREFS, 0);
            String myContact = settings.getString(CONTACT, "");
            EditText editPhone = (EditText)findViewById(R.id.numTextbox);
            editPhone.setText(myContact);
        } else {
            // Fail-safe in case permissions are revoked before returning to app
            // onCreate should be called in such scenario, but not completely sure yet
            unlockElements(false);
        }
    }

    // User presses the button, grabs latitude and longitude before calling sendSMS, or goes into
    // app settings to enable permission(s)
    public void sendPressed(View v) {
        // Explicitly needs to check if location information granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        sendSMS(latitude, longitude);
                    } else {
                        Log.e(TAG, "location not on!");
                        Toast.makeText(getApplicationContext(), "Location not on!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            // Open settings so user (hopefully) grants permission(s)
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }
    }

    // Sends message with user's location to recipient
    public void sendSMS(double latitude, double longitude) {
        final SmsManager smsManager = SmsManager.getDefault();
        final String phoneNum = ((EditText)findViewById(R.id.numTextbox)).getText().toString();
        final String mapLink = "https://maps.google.com/?q=" + latitude + "," + longitude;

        ShortURL.makeShortUrl(mapLink, new ShortURL.ShortUrlListener() {
            @Override
            public void OnFinish(String url) {
                // If URL is successfully shortened, send message
                if (url != null && 0 < url.length()) {
                    if (phoneNum.length() > 0) {
                        try {
                            String message = ((EditText)findViewById(R.id.messageTextbox)).getText().toString();
                            // Grab hint if no custom message entered
                            if (message.length() == 0) {message = "Please pick me up!";}
                            message += "\nHere's my last location: " + url;

                            Log.i(TAG, "sending pick up request to " + phoneNum + ":\n\"" + message + "\"");
                            smsManager.sendTextMessage(phoneNum, null, message, null, null);
                            Toast.makeText(getApplicationContext(), "Sent!", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            // TODO: maybe make snackbar instead
                            Log.e(TAG, "sms sending failed");
                            Toast.makeText(getApplicationContext(), "SMS sending failed!", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    } else {
                        Log.e(TAG, "no phone number!");
                        Toast.makeText(getApplicationContext(), "Please enter a phone number", Toast.LENGTH_SHORT).show();
                    }
                } else { // URL unable to be shortened, send full link instead
                    Log.e(TAG, "failed to shorten url");
                    if (phoneNum.length() > 0) {
                        try {
                            String message = ((EditText)findViewById(R.id.messageTextbox)).getText().toString();
                            // Grab hint if no custom message entered
                            if (message.length() == 0) {message = ((EditText)findViewById(R.id.messageTextbox)).getHint().toString();}
                            message += "\nHere's my last location: " + mapLink;

                            Log.i(TAG, "sending pick up request to " + phoneNum + ":\n\"" + message + "\"");
                            smsManager.sendTextMessage(phoneNum, null, message, null, null);
                            Toast.makeText(getApplicationContext(), "Failed to shorten URL, sending long link instead", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            // TODO: maybe make snackbar instead
                            Log.e(TAG, "sms sending failed");
                            Toast.makeText(getApplicationContext(), "SMS sending failed!", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    } else {
                        Log.e(TAG, "no phone number!");
                        Toast.makeText(getApplicationContext(), "Please enter a phone number", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Save number for future use
        SharedPreferences.Editor editor = getSharedPreferences(MYPREFS, 0).edit();
        editor.putString(CONTACT, phoneNum);
        editor.apply();
    }

    // Opens a snackbar
    private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }
}
