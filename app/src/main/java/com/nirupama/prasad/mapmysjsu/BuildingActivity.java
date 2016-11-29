package com.nirupama.prasad.mapmysjsu;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

//google api key:
//AIzaSyCGNVOTjmPWnsQ2J7rWOJ4SN8AfMF1z_Yg
//Sample query:
//https://maps.googleapis.com/maps/api/distancematrix/json?units=imperial&origins=37.334940,%20-121.882756&destinations=37.337359,20-121.881909&key=AIzaSyCGNVOTjmPWnsQ2J7rWOJ4SN8AfMF1z_Yg

public class BuildingActivity extends AppCompatActivity {

    //All statics for Google apis
    public static final String STR_GOOG_BASE_URL = "https://maps.googleapis.com/maps/api/distancematrix/json?units=imperial&origins=";
    public static final String STR_GOOG_DEST_URL = "&destinations=";
    public static final String STR_GOOG_API_KEY = "AIzaSyCGNVOTjmPWnsQ2J7rWOJ4SN8AfMF1z_Yg";
    public static final String STR_GOOG_MODE = "&mode=walking";
    public static String STR_BUILDING_COORDS_STREETVIEW = "46.414382,10.013988"; //Default to amphiteater
    public static int requestCodeStreetView = 1333;

    TextView building_info_textview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_building);

        //Set up building toolbar
        Toolbar map_toolbar = (Toolbar) findViewById(R.id.map_toolbar);
        setSupportActionBar(map_toolbar);
        /*final Drawable upArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_material);
        upArrow.setColorFilter(getResources().getColor(R.color.colorWhite), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
*/
        //Set up status bar
        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(this.getResources().getColor(R.color.colorToolBar));

        //Retrieve building data
        String[] building_details = getIntent().getStringArrayExtra("BUILDING_DETAILS");
        String last_known_user_coordinates = getIntent().getStringExtra("LAST_KNOWN_COORDINATES");
        String building_map_coordinates = getIntent().getStringExtra("BLDG_MAP_COORDINATES");

        //Set up basic info about the building
        building_info_textview = (TextView) findViewById(R.id.bldg_textView);
        String str_building_info_joined_string = "";

        str_building_info_joined_string = building_details[0].toUpperCase() + "\n\n";
        str_building_info_joined_string += "ADDRESS: \n" + building_details[1] + "\n\n";
        //DEBUG:
        STR_BUILDING_COORDS_STREETVIEW = building_details[2];

        building_info_textview.setText(str_building_info_joined_string);


        //Start AsyncTask here
        String url = STR_GOOG_BASE_URL + last_known_user_coordinates + STR_GOOG_DEST_URL + building_map_coordinates + STR_GOOG_MODE + STR_GOOG_API_KEY;
        new FetchTimeEstimateTask().execute(url);

    }

    @Override
    protected void onResume() {
        super.onResume();
        int building_image_resource_id = getIntent().getIntExtra("BUILDING_IMAGE_NAME", R.drawable.bbc);
        //Set up building image
        ImageView buildingImageView = (ImageView) findViewById(R.id.bldg_imageView);
        buildingImageView.setImageResource(building_image_resource_id);
    }

    //Trigger street view, with the current building coordinates
    public void btnStreetViewLauncher(View view) {
        String strStreetView = "google.streetview:cbll=" + STR_BUILDING_COORDS_STREETVIEW;
        Uri gmmIntentUri = Uri.parse(strStreetView);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivityForResult(mapIntent, requestCodeStreetView);
    }

    //If we don't do this, then hitting back from streetview takes you to the map
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == requestCodeStreetView){
            if(requestCode == RESULT_OK){
                //User is back from street view
            }
        }
    }

    //Write out AsyncTask to retrieve location distance and time
    private class FetchTimeEstimateTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... args){

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            String url_weather = args[0];
            Log.d("BuildingActivity", "FetchTimeEstimateTask: doInBackground : "  + url_weather);
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String timeEstimateJsonStr = null;

            try {
                URL url = new URL(url_weather);

                // Create the request to google distance matrix, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                //Check for null stream
                if (buffer.length() == 0) {
                    return null;
                }
                timeEstimateJsonStr = buffer.toString();
                Log.d("BuildingActivity", timeEstimateJsonStr);
            }
            catch (IOException e) {
                Log.e("BuildingActivity", "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            }
            finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("BuildingActivity", "Error closing stream", e);
                    }
                }
            }

            return timeEstimateJsonStr;//for success
        }//End of doInBackground

        protected void onPostExecute(String resultJsonString) {
            Log.d("BuildingActivity", "Done executing in the background");
            String strTimeToTarget = "";
            String strDistanceToTarget = "";

            String text = building_info_textview.getText().toString();
            //Parse json object here
            try {

                JSONObject jsonObject = new JSONObject(resultJsonString);
                JSONArray jsonArray = jsonObject.getJSONArray("rows");
                JSONObject jsonRows =  jsonArray.getJSONObject(0);
                JSONArray jsonElementArray = jsonRows.getJSONArray("elements");
                //Get distance
                JSONObject jsonElementObject = jsonElementArray.getJSONObject(0);
                JSONObject jsonDistance = jsonElementObject.getJSONObject("distance");
                strDistanceToTarget = jsonDistance.getString("text");
                //Get time
                JSONObject jsonDuration = jsonElementObject.getJSONObject("duration");
                strTimeToTarget = jsonDuration.getString("text");

            }
            catch (JSONException e) {
                e.printStackTrace();
            }

            building_info_textview.setText(text + "TIME FROM CURRENT LOCATION: \n" +  strTimeToTarget + "\n\n" + "DISTANCE TO LOCATION: \n" + strDistanceToTarget);
        }

    }//End of async task


}
