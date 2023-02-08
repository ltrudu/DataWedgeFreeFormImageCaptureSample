package com.zebra.datawedgefreeformcapturesample;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "FreeformCapture";
    TextView txtStatus = null;
    LinearLayout linearLayout = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtStatus = findViewById(R.id.txtStatus);
        linearLayout = findViewById(R.id.ll_result);
        registerReceivers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        queryProfileList();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterReceivers();
    }

    private void queryProfileList(){
        Intent i = new Intent();
        i.setAction(IntentKeys.DATAWEDGE_API_ACTION);
        i.setPackage(IntentKeys.DATAWEDGE_PACKAGE);
        i.putExtra(IntentKeys.EXTRA_GET_PROFILES_LIST, "");
        sendBroadcast(i);
    }

    private void deleteProfile() {
        Intent intent = new Intent();
        intent.setAction(IntentKeys.DATAWEDGE_API_ACTION);
        intent.setPackage(IntentKeys.DATAWEDGE_PACKAGE);
        intent.putExtra(IntentKeys.EXTRA_DELETE_PROFILE,IntentKeys.PROFILE_NAME);
        this.sendBroadcast(intent);
    }

    private void registerReceivers(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IntentKeys.RESULT_ACTION);
        intentFilter.addAction(IntentKeys.NOTIFICATION_ACTION);
        intentFilter.addAction(IntentKeys.INTENT_OUTPUT_ACTION);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(broadcastReceiver, intentFilter);
        registerUnregisterForNotifications(true, "WORKFLOW_STATUS");
        registerUnregisterForNotifications(true, "SCANNER_STATUS");
    }

    private void unRegisterReceivers(){
        registerUnregisterForNotifications(false, "WORKFLOW_STATUS");
        registerUnregisterForNotifications(false, "SCANNER_STATUS");
        unregisterReceiver(broadcastReceiver);
    }

    void registerUnregisterForNotifications(boolean register, String type) {
        Bundle b = new Bundle();
        b.putString("com.symbol.datawedge.api.APPLICATION_NAME", getPackageName());
        b.putString("com.symbol.datawedge.api.NOTIFICATION_TYPE", type);
        Intent i = new Intent();
        i.putExtra("APPLICATION_PACKAGE", getPackageName());
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.setPackage("com.symbol.datawedge");
        if (register)
            i.putExtra("com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION", b);
        else
            i.putExtra("com.symbol.datawedge.api.UNREGISTER_FOR_NOTIFICATION", b);
        this.sendBroadcast(i);
    }

    public void onCreateProfile(View view){
        createProfile();
    }

    public void onClickClearScannedData(View view)
    {
        linearLayout.removeAllViews();
    }

    public void onClickScan(View view) {
        Intent i = new Intent();
        i.setPackage(IntentKeys.DATAWEDGE_PACKAGE);
        i.setAction(IntentKeys.DATAWEDGE_API_ACTION);
        i.putExtra(IntentKeys.EXTRA_SOFT_SCAN_TRIGGER, "TOGGLE_SCANNING");
        this.sendBroadcast(i);
    }

    private void createProfile(){
        Bundle bMain = new Bundle();

        Bundle bConfigWorkflow = new Bundle();
        Bundle barcodeParams = new Bundle();
        ArrayList<Bundle> bundlePluginConfig = new ArrayList<>();
        barcodeParams.putString("scanner_selection", "auto"); //Make scanner selection as auto

        /*###Configuration for Workflow Input [start]###*/
        bConfigWorkflow.putString("PLUGIN_NAME", "WORKFLOW");//Plugin name as Barcode
        bConfigWorkflow.putString("RESET_CONFIG", "true"); //Reset existing configurations of barcode input plugin
        bConfigWorkflow.putString("workflow_input_enabled", "true");
        bConfigWorkflow.putString("selected_workflow_name", "free_form_capture");
        bConfigWorkflow.putString("workflow_input_source", "1"); //input source 1- imager, 2- camera

        Bundle paramList = new Bundle();
        paramList.putString("workflow_name","free_form_capture");
        paramList.putString("workflow_input_source","1");

        Bundle paramSetContainerDecoderModule = new Bundle();
        paramSetContainerDecoderModule.putString("module","BarcodeTrackerModule");
        Bundle moduleContainerDecoderModule = new Bundle();
        moduleContainerDecoderModule.putString("session_timeout", "16000");
        moduleContainerDecoderModule.putString("illumination", "off");
        moduleContainerDecoderModule.putString("decoding_led_feedback", "off");
        moduleContainerDecoderModule.putString("decode_and_highlight_barcodes", "1"); //1-off, 2-highlight, 3- decode and highlight
        paramSetContainerDecoderModule.putBundle("module_params",moduleContainerDecoderModule);

        Bundle paramSetFeedbackModule = new Bundle();
        paramSetFeedbackModule.putString("module","FeedbackModule");
        Bundle moduleParamsFeedback = new Bundle();
        moduleParamsFeedback.putString("decode_haptic_feedback", "false");
        moduleParamsFeedback.putString("decode_audio_feedback_uri", "Electra");
        moduleParamsFeedback.putString("volume_slider_type", "0");// 0- Ringer, 1- Music and Media, 2-Alarms, 3- Notification
        moduleParamsFeedback.putString("decoding_led_feedback", "true");
        paramSetFeedbackModule.putBundle("module_params",moduleParamsFeedback);

        ArrayList<Bundle> paramSetList = new ArrayList<>();
        paramSetList.add(paramSetContainerDecoderModule);
        paramSetList.add(paramSetFeedbackModule);

        paramList.putParcelableArrayList("workflow_params", paramSetList);

        ArrayList<Bundle> workFlowList = new ArrayList<>();
        workFlowList.add(paramList);

        bConfigWorkflow.putParcelableArrayList("PARAM_LIST", workFlowList);
        bundlePluginConfig.add(bConfigWorkflow);
        /*###Configuration for Workflow Input [Finish]###*/


        /*###Configuration for Intent Output[Start]###*/
        Bundle intentConfig = new Bundle();
        Bundle intentParams = new Bundle();
        intentConfig.putString("PLUGIN_NAME", "INTENT"); //Plugin name as intent
        intentConfig.putString("RESET_CONFIG", "true"); //Reset existing configurations of intent output plugin
        intentParams.putString("intent_output_enabled", "true"); //Enable intent output plugin
        intentParams.putString("intent_action", IntentKeys.INTENT_OUTPUT_ACTION); //Set the intent action
        intentParams.putString("intent_category", IntentKeys.INTENT_CATEGORY); //Set a category for intent
        intentParams.putInt("intent_delivery", IntentKeys.INTENT_DELIVERY); // Set intent delivery mechanism
        intentParams.putString("intent_use_content_provider", "true"); //Enable content provider
        intentConfig.putBundle("PARAM_LIST", intentParams);
        bundlePluginConfig.add(intentConfig);
        /*### Configurations for Intent Output[Finish] ###*/

        //Putting the INTENT and BARCODE plugin settings to the PLUGIN_CONFIG extra
        bMain.putParcelableArrayList("PLUGIN_CONFIG", bundlePluginConfig);

        /*### Associate this application to the profile [Start] ###*/
        Bundle appConfig = new Bundle();
        appConfig.putString("PACKAGE_NAME",getPackageName());//Get Package name of the application
        appConfig.putStringArray("ACTIVITY_LIST", new String[]{"*"});//Add all activities of this application
        bMain.putParcelableArray("APP_LIST", new Bundle[]{
                appConfig
        });
        /*### Associate this application to the profile [Finish] ###*/

        bMain.putString("PROFILE_NAME", IntentKeys.PROFILE_NAME); //Initialize the profile name
        bMain.putString("PROFILE_ENABLED","true");//Enable the profile
        bMain.putString("CONFIG_MODE", "CREATE_IF_NOT_EXIST");
        bMain.putString("RESET_CONFIG", "true");//Enable reset configuration if already exist

        Intent intent = new Intent();
        intent.setAction(IntentKeys.DATAWEDGE_API_ACTION);
        intent.setPackage(IntentKeys.DATAWEDGE_PACKAGE);
        intent.putExtra(IntentKeys.DATAWEDGE_CONFIG, bMain);
        intent.putExtra("SEND_RESULT", "COMPLETE_RESULT");
        intent.putExtra(IntentKeys.COMMAND_IDENTIFIER_EXTRA,
                IntentKeys.COMMAND_IDENTIFIER_CREATE_PROFILE);
        this.sendBroadcast(intent);

    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                Bundle extras = intent.getExtras();

                if (intent.hasExtra(IntentKeys.EXTRA_RESULT_GET_PROFILES_LIST)){
                    String[] arrayProfileList = extras.getStringArray(IntentKeys.EXTRA_RESULT_GET_PROFILES_LIST);
                    List<String> profileList = Arrays.asList(arrayProfileList);
                    //check whether the profile is exist or not
                    if(profileList.contains(IntentKeys.PROFILE_NAME)){
                        //if the profile is already exist
                        setStatus("Profile already exists, not creating the profile");
                    }else{
                        //if the profile doest exist
                        setStatus("Profile does not exists. Creating the profile..");
                        createProfile();
                    }
                }
                else if(extras.containsKey(IntentKeys.COMMAND_IDENTIFIER_EXTRA)){
                    /*## Processing the result of CREATE_PROFILE[Start] ###*/
                    if(extras.getString(IntentKeys.COMMAND_IDENTIFIER_EXTRA)
                            .equalsIgnoreCase(IntentKeys.COMMAND_IDENTIFIER_CREATE_PROFILE)){
                        ArrayList<Bundle> bundleList = intent.getParcelableArrayListExtra("RESULT_LIST");
                        if (bundleList != null && bundleList.size()>0){
                            boolean allSuccess = true;
                            StringBuilder resultInfo = new StringBuilder();
                            //Iterate through the result list for each module
                            for(Bundle bundle : bundleList){
                                if (bundle.getString("RESULT")
                                        .equalsIgnoreCase(IntentKeys.INTENT_RESULT_CODE_FAILURE)){
                                    //if the profile creation failure for that module, provide more information on that
                                    allSuccess = false;
                                    resultInfo.append("Module Name : ")
                                            .append(bundle.getString("MODULE"))
                                            .append("\n"); //Name of the module

                                    resultInfo.append("Result code: ").
                                            append(bundle.getString("RESULT_CODE")).
                                            append("\n");//Information of the moule

                                    if(bundle.containsKey("SUB_RESULT_CODE")) {
                                        resultInfo.append("\tSub Result code: ")
                                                .append(bundle.getString("SUB_RESULT_CODE"))
                                                .append("\n");
                                    }
                                    break; // Breaking the loop as there is a failure
                                }else {
                                    //if the profile creation success for the module.
                                    resultInfo.append("Module: " )
                                            .append(bundle.getString("MODULE"))
                                            .append("\n");

                                    resultInfo.append("Result: ")
                                            .append(bundle.getString("RESULT"))
                                            .append("\n");
                                }
                            }
                            if (allSuccess) {
                                setStatus("Profile created successfully");
                            } else {
                                setStatus("Profile creation failed!\n\n" + resultInfo);
                                deleteProfile();
                            }
                        }
                    }
                    /*### Processing the result of CREATE_PROFILE [Finish] ###*/
                }
                else if(action.equals(IntentKeys.INTENT_OUTPUT_ACTION)){
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String dataToDecode = intent.getStringExtra(IntentKeys.DATA);
                            if (dataToDecode != null){
                                Log.d(TAG, "Data to decode: "+ dataToDecode);
                                try {
                                    processingDecodeData(dataToDecode); //Processing the decode data
                                } catch (JSONException e) {
                                    setStatus("JSONException: " + e.getMessage());
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    setStatus("IO Exception: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    thread.start();
                }
                else if(action.equals("com.symbol.datawedge.api.NOTIFICATION_ACTION"))
                {
                    if (intent.hasExtra("com.symbol.datawedge.api.NOTIFICATION")) {
                        Bundle b = intent.getBundleExtra("com.symbol.datawedge.api.NOTIFICATION");
                        String NOTIFICATION_TYPE = b.getString("NOTIFICATION_TYPE");
                        if (NOTIFICATION_TYPE != null) {
                            switch (NOTIFICATION_TYPE) {
                                case "WORKFLOW_STATUS":
                                case "SCANNER_STATUS":

                                    String status = b.getString("STATUS");
                                    setStatus("Status: " + status);
                                    break;
                            }
                        }
                    }
                }
            }catch (Exception ex){
                Log.e(TAG, "onReceive: ", ex);
            }
        }
    };


    private synchronized void processingDecodeData(String dataToDecode) throws JSONException, IOException {
        JSONArray dataArray = new JSONArray(dataToDecode);
        for (int i = 0; i < dataArray.length(); i++)
        {
            JSONObject workflowObject = dataArray.getJSONObject(i);
            if (workflowObject.has("string_data"))
            {
                String label = workflowObject.getString("label");
                if (label.equalsIgnoreCase(""))
                {
                    //  Each decoded barcode is stored in workflowObject.getString("string_data")
                    //  Symbology returned in workflowObject.getString("barcodetype")
                }
            }
            else
            {
                //  string_data is absent, therefore this is an image
                if (workflowObject.has("uri")) {
                    String imageURIAsString = workflowObject.get("uri").toString();
                    byte[] imageAsBytes = null;
                    imageAsBytes = processUri(imageURIAsString); //  Extract data from content provider
                    int width = workflowObject.getInt("width");
                    int height = workflowObject.getInt("height");
                    int stride = workflowObject.getInt("stride");
                    int orientation = workflowObject.getInt("orientation");
                    String imageFormat = workflowObject.getString("imageformat");
                    //  See the sample or programmer's guide for getBitmap(...)
                    Bitmap bitmap =
                            ImageProcessing.getInstance().getBitmap(imageAsBytes, imageFormat, orientation, stride, width, height);
                    //  Show bitmap on UI
                    if(bitmap != null)
                    {
                        final ImageView img = new ImageView(getApplicationContext());
                        img.setImageBitmap(bitmap);
                        setUIForResult(null, img);
                        setStatus("Image decoding successful.");
                    }
                }
            }
        }
    }

    @SuppressLint("Range")
    private byte[] processUri(String uri)
    {
        Cursor cursor = getContentResolver().query(Uri.parse(uri),null,null,null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if(cursor != null)
        {
            cursor.moveToFirst();
            try {
                baos.write(cursor.getBlob(cursor.getColumnIndex("raw_data")));
            } catch (IOException e) {
                Log.w(TAG, "Output Stream Write error " + e.getMessage());
            }
            String nextURI = cursor.getString(cursor.getColumnIndex("next_data_uri"));
            while (nextURI != null && !nextURI.isEmpty())
            {
                Cursor cursorNextData = getContentResolver().query(Uri.parse(nextURI),
                        null,null,null);
                if(cursorNextData != null)
                {
                    cursorNextData.moveToFirst();
                    try {
                        baos.write(cursorNextData.getBlob(cursorNextData.
                                getColumnIndex("raw_data")));
                    } catch (IOException e) {
                        Log.w(TAG, "Output Stream Write error " + e.getMessage());
                    }
                    nextURI = cursorNextData.getString(cursorNextData.
                            getColumnIndex("next_data_uri"));

                    cursorNextData.close();
                }
            }
            cursor.close();
        }
        return baos.toByteArray();
    }


    private String getImageFormat(int type){
        String imageFormat = "";
        switch (type){
            case 1:
                imageFormat = "JPEG";
                break;
            case 3:
                imageFormat = "BMP";
                break;
            case 4:
                imageFormat = "TIFF";
                break;
            case 5:
                imageFormat = "YUV";
                break;
        }
        return imageFormat;
    }

    public void setStatus(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus.setText("Status: " + status);
            }
        });
    }

    private void setUIForResult(TextView textView, ImageView imageView){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (textView!=null){
                    linearLayout.addView(textView);
                }
                if(imageView != null){
                    linearLayout.addView(imageView);
                }
            }
        });
    }

}