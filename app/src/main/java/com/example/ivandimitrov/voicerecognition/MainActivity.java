package com.example.ivandimitrov.voicerecognition;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener, MyDragListener.OnDropListener {
    private TextView mTexSpeechResult;
    private Button   mButtonStartVoiceCommands;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private CustomListAdapter mAdapter;
    private static final String TAG                     = "drive-quickstart";
    private static final int    REQUEST_CODE_CREATOR    = 2;
    private static final int    REQUEST_CODE_RESOLUTION = 3;
    ArrayList<java.io.File> mList = new ArrayList<>();
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTexSpeechResult = (TextView) findViewById(R.id.txtSpeechInput);
        mButtonStartVoiceCommands = (Button) findViewById(R.id.btnSpeak);
        mButtonStartVoiceCommands.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });
        mList = getListFiles(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        mAdapter = new CustomListAdapter(this, mList);
        final ListView listView = (ListView) this.findViewById(R.id.list);
        listView.setAdapter(mAdapter);
        findViewById(R.id.topright).setOnDragListener(new MyDragListener(this, this));
    }

    //============================
    // DRAG AND DROP
    //============================
    @Override
    public void onItemDropped(String filePath) {
        saveFileToDrive(new File(filePath));
    }

    //============================
    // VOICE RECOGNITION
    //============================
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    mTexSpeechResult.setText(result.get(0));
                    try {
                        int fileIndex = Integer.parseInt(parseVoiceInputMessage(result.get(0)));
                        saveFileToDrive(mList.get(fileIndex));
                    } catch (NumberFormatException e) {
                        Log.d("VOICE_INPUT", "INCORRECT INPUT");
                    }
                }
                break;
            }

        }
    }

    private String parseVoiceInputMessage(String message) {
        message = message.toLowerCase();
        if (message.contains("upload file")) {
            message = message.substring(message.lastIndexOf("upload file") + 12);
            if (message.equals("one")) {
                message = "1";
            }
            Log.d("MESSAGE ", message);
            return message;
        }
        return null;
    }

    //============================
    // ACTIVITY METHODS
    //============================
    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    //============================
    // GOOGLE DRIVE
    //============================
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "API client connected.");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }


    private void saveFileToDrive(File file) {
        Log.i(TAG, "Creating new contents.");
        final Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath());
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveContentsResult>() {

                    @Override
                    public void onResult(DriveContentsResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.i(TAG, "Failed to create new contents.");
                            return;
                        }
                        Log.i(TAG, "New contents created.");
                        OutputStream outputStream = result.getDriveContents().getOutputStream();
                        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
                        image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);
                        try {
                            outputStream.write(bitmapStream.toByteArray());
                        } catch (IOException e1) {
                            Log.i(TAG, "Unable to write file contents.");
                        }
                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                .setMimeType("image/jpeg").setTitle("Android Photo.png").build();
                        IntentSender intentSender = Drive.DriveApi
                                .newCreateFileActivityBuilder()
                                .setInitialMetadata(metadataChangeSet)
                                .setInitialDriveContents(result.getDriveContents())
                                .build(mGoogleApiClient);
                        try {
                            startIntentSenderForResult(
                                    intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
                        } catch (SendIntentException e) {
                            Log.i(TAG, "Failed to launch file chooser.");
                        }
                    }
                });
    }

    private ArrayList<java.io.File> getListFiles(java.io.File parentDir) {
        ArrayList<java.io.File> inFiles = new ArrayList<>();
        java.io.File[] files = parentDir.listFiles();

        for (java.io.File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getListFiles(file));
            } else {
                if (file.getName().endsWith(".png")) {
                    inFiles.add(file);
                }
            }
        }
        return inFiles;
    }
}