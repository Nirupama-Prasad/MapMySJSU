package com.nirupama.prasad.mapmysjsu;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.RelativeLayout;


public class MapActivity extends AppCompatActivity {

    public static final String LocationProviderString = LocationManager.GPS_PROVIDER;


    public static final double FIXED_PIXEL_DISTANCE = 2.1679;
    public static final int CURRENT_LOCATION_SCALE_CORRECTER = 700;
    //Resource handles
    public static AutoCompleteTextView map_search_bar;
    public static MarkerView marker;
    public static CircleMarkerView circlemarker;
    public static int intXAxisPlotOffset = -20, intYAxisPlotOffset = 600;
    public static ImageView mapImageView;
    public static Matrix mapMatrix = new Matrix();


    //Geolocation statics
    public final static double OneEightyDeg = 180.0d;
    public static double ImageSizeW = 1407, ImageSizeH = 1486.0;


    //Current location
    public static Location locCurrentLocation;
    public static Location locCurrentHardCodedLocation;
    public static String strCurrentUserLatitude = "";
    public static String strCurrentUserLongitude = "";
    private static final String[] LOCATION_PERMS = {
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    public static final int REQUEST_CODE = 1337;
    public static final int LOCATION_REQUEST_CODE = REQUEST_CODE;
    public static final int LOCATION_MIN_TIME = 30000;
    public static final int LOCATION_MIN_DISTANCE = 10;
    public static String strCurrentUserLocation = "";

    public static String strMapTopLeft = "37.335802,-121.885910";
    public static String strMapTopRight = "37.338877,-121.879668";
    public static String strMapBottomLeft = "37.331626,-121.882812";
    public static String strMapBottomRight = "37.334603,-121.876557";
    public static Location locMapTopLeft, locMapTopRight, locMapBottomLeft, locMapBottomRight;

    private static LocationManager mLocationManager;

    public static final int TOTAL_BUILDING_COUNT = 6;

    private static final int[] BUILDING_RESOURCE_NAMES = new int[]{
            R.drawable.sjsu_engineering_web,
            R.drawable.king,
            R.drawable.yoshihiro,
            R.drawable.student_union,
            R.drawable.bbc,
            R.drawable.south_garage
    };

    public static final String[] GEOCOORDINATES = new String[]{
            "37.337359,-121.881909",
            "37.335716,-121.885213",
            "37.333492,-121.883756",
            "37.336361,-121.881282",
            "37.336530,-121.878717",
            "37.333385,-121.880264"
    };


    public static final String[] ADDRESSES = new String[]{
            "Charles W. Davidson College of Engineering, 1 Washington Square, San Jose, CA 95112",
            "Dr. Martin Luther King, Jr. Library, 150 East San Fernando Street, San Jose, CA 95112",
            "Yoshihiro Uchida Hall, San Jose, CA 95112",
            "Student Union Building, San Jose, CA 95112",
            "Boccardo Business Complex, San Jose, CA 95112",
            "San Jose State University South Garage, 330 South 7th Street, San Jose, CA 95112"
    };

    public static final String[] LOCATIONS = new String[]{
            "Engineering Building",
            "King Library",
            "Yoshihiro Uchida Hall",
            "Student Union",
            "BBC",
            "South Parking Garage"
    };

    public static final float[][] coordinates = new float[][]{
            //           TLX  TRY  TRX  BRY
            new float[]{749, 529, 960, 720}, //ENGR BUILDING
            new float[]{193, 493, 312, 690}, //KING LIBRARY
            new float[]{107, 974, 319, 1146}, //YOSHIHIRO HALL
            new float[]{745, 758, 1046, 881}, //STUDENT UNION
            new float[]{1160, 880, 1333, 990}, //BBC
            new float[]{458, 1332, 708, 1504} //SOUTH PARKING
    };

    public Building[] map_buildings = new Building[TOTAL_BUILDING_COUNT];

    public static String strHardCodedCurrentLocation = "37.333492,-121.883756";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        //Intializing
        marker = new MarkerView(this);

        //Get latitudes and longitudes of map ready
        locMapTopLeft = GetLocationFromStrings(strMapTopLeft);
        locMapTopRight = GetLocationFromStrings(strMapTopRight);
        locMapBottomLeft = GetLocationFromStrings(strMapBottomLeft);
        locMapBottomRight = GetLocationFromStrings(strMapBottomRight);
        locCurrentHardCodedLocation = ConvertStringToLatLng(strHardCodedCurrentLocation);

        //Moving to here because Android complains about constructor
        for (int i = 0; i < TOTAL_BUILDING_COUNT; i++) {
            map_buildings[i] = new Building(LOCATIONS[i], coordinates[i]);
            map_buildings[i].setBuilding_address(ADDRESSES[i]);
            map_buildings[i].setBuilding_coordinates(GEOCOORDINATES[i]);
            map_buildings[i].setBuilding_image_resource_name(BUILDING_RESOURCE_NAMES[i]);
        }

        //Get the image view
        mapImageView = (ImageView) findViewById(R.id.mapImageView);

        //Get the search bar
        map_search_bar = (AutoCompleteTextView) findViewById(R.id.map_search_bar);

        //Set up map toolbar
        Toolbar map_toolbar = (Toolbar) findViewById(R.id.map_toolbar);
        setSupportActionBar(map_toolbar);

        //Set up status bar
        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(this.getResources().getColor(R.color.colorToolBar));

        //Set up autocomplete
        ArrayAdapter<String> searchArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, LOCATIONS);
        map_search_bar.setAdapter(searchArrayAdapter);
        map_search_bar.setThreshold(0);

        //Set up touch
        mapImageView.setOnTouchListener(map_touch_listener);

        //Start map at the center
        CenterMapImage();

        //Test out current location code
        GetCurrentLocation(this);
        Log.d("MainActivity", strCurrentUserLocation);

        //DEBUG:
        //TestStaticLocations(locMapTopLeft, locMapTopRight, locMapBottomLeft, locMapBottomRight);
    }

    public void updateCurrentUserLocationOnMap() {

        ExecutePlotCurrentUserOnMap(locMapTopLeft, locMapTopRight, locMapBottomLeft, locMapBottomRight, locCurrentLocation);

    }


    //Get text from AutoCompleteTextView
    //Match to what we know -
    //Get its coordinates - PlotPin
    public void btnSearchHandler(View v) {

        AutoCompleteTextView auto_text = (AutoCompleteTextView) findViewById(R.id.map_search_bar);
        String search_text = auto_text.getText().toString();

        if (search_text.isEmpty() == true || search_text == "" || search_text == null) {
            return;
        }

        int building_count = -1;
        search_text = search_text.toLowerCase();
        for (int i = 0; i < LOCATIONS.length; i++) {
            if (search_text.equals(LOCATIONS[i].toLowerCase())) {
                building_count = i;
                break;
            }
        }

        if (building_count == -1) {
            return;
        }
        //Check only if user has entered a valid building
        if (marker != null) {
            RelativeLayout map_layout = (RelativeLayout) findViewById(R.id.activity_map);
            map_layout.removeView(marker);
        }

        float[] building_pixel_coordinates = map_buildings[building_count].getPixel_coordinates();

        float pixelTopRightX = building_pixel_coordinates[2];
        float pixelTopRightY = building_pixel_coordinates[1];
        float pixelBottomRightY = building_pixel_coordinates[3];
        float pixelTopLeftX = building_pixel_coordinates[0];

        float plotPixelX = (pixelTopLeftX + pixelTopRightX) / 2;
        float plotPixelY = (pixelTopRightY + pixelBottomRightY) / 2;

        //Plot it
        PlotPin(this, plotPixelX, plotPixelY);

    }

    @Override
    protected void onResume() {
        super.onResume();

        final AutoCompleteTextView auto_text_view = (AutoCompleteTextView) findViewById(R.id.map_search_bar);
        auto_text_view.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
                //Log.d("MapActivity", "after text changed");
                if (auto_text_view.getText().toString().equals("") || auto_text_view.getText().toString() == null) {
                    if (marker != null) {
                        RelativeLayout map_layout = (RelativeLayout) findViewById(R.id.activity_map);
                        map_layout.removeView(marker);
                    }
                }
            }
        });
    }

    private void ProcessTouchCoordinate(View v, float x, float y) {

        for (int i = 0; i < TOTAL_BUILDING_COUNT; i++) {
            if (map_buildings[i].IsWithinPixelBounds(x, y)) {
                //Toast.makeText(v.getContext(), map_buildings[i].building_name, Toast.LENGTH_SHORT).show();
                Intent bldgIntent = new Intent(this, BuildingActivity.class);
                bldgIntent.putExtra("BUILDING_DETAILS", new String[]{
                        map_buildings[i].building_name,
                        map_buildings[i].building_address,
                        map_buildings[i].building_coordinates
                });

                bldgIntent.putExtra("BUILDING_IMAGE_NAME", map_buildings[i].getBuilding_image_resource_name());
                bldgIntent.putExtra("LAST_KNOWN_COORDINATES", strCurrentUserLocation);
                bldgIntent.putExtra("BLDG_MAP_COORDINATES", map_buildings[i].building_coordinates);
                startActivity(bldgIntent);


            }
            //DEBUG:
            //PlotPin(this, x + intXAxisPlotOffset, y + intYAxisPlotOffset);
        }

    }

    private Location GetLocationFromStrings(String strMap) {
        Location location = ConvertStringToLatLng(strMap);
        return location;
    }

    public Location ConvertStringToLatLng(String strCoord) {
        String[] latlong = strCoord.split(",");
        double latitude = Double.parseDouble(latlong[0]);
        double longitude = Double.parseDouble(latlong[1]);

        //TRY: Put actual location provider here
        Location location = new Location("dummyprovider");
        location.setLatitude(latitude);
        location.setLongitude(longitude);

        return location;
    }


    private void PlotPin(Context context, float x, float y) {
        //Normalize incoming pixels
        x = x + intXAxisPlotOffset;
        y = y + intYAxisPlotOffset;

        RelativeLayout map_layout = (RelativeLayout) findViewById(R.id.activity_map);
        marker = new MarkerView(context);
        marker.set_x_y_coord(x, y);
        map_layout.addView(marker);
    }


    private void TestStaticLocations(Location locMapTopLeft, Location locMapTopRight,
                                     Location locMapBottomLeft, Location locMapBottomRight) {

        //Test out hardcoded map locations here

        //Last test = Clark hall - ACCURATE
        //Location locTestLocation = ConvertStringToLatLng("37.336322,-121.882402");
        //Building - Accuracy - Scaling issue
        //Musi
        // c building - ACCURATE
        //Location locTestLocation = ConvertStringToLatLng("37.335605,-121.880868");

        //Engineering building - ACCURATE
        //Location locTestLocation = ConvertStringToLatLng("37.336714,-121.881522");

        //Morris auditorium - BIT OFF
        //Location locTestLocation = ConvertStringToLatLng("37.335289,-121.883142");

        //YUCHIRO hall - WAY OFF
        //Location locTestLocation = ConvertStringToLatLng("37.333617,-121.883861");

        //West parking garage - BIT OFF
        //Location locTestLocation = ConvertStringToLatLng("37.332679,-121.883153");

        //Student union - ACCURATE
        //Location locTestLocation = ConvertStringToLatLng("37.336526,-121.880921");

        //Cooling plant - ACCURATE
        //Location locTestLocation = ConvertStringToLatLng("37.336193,-121.878475");

        //Campus west - BIT OFF
        //Location locTestLocation = ConvertStringToLatLng("37.334427,-121.877370");

        //Dining commons - BIT OFF
        //Location locTestLocation = ConvertStringToLatLng("37.334188, -121.878529");

        //South parking garage
        Location locTestLocation = ConvertStringToLatLng("37.333233, -121.880814");

        //MLK building
        //Location locTestLocation = ConvertStringToLatLng("37.335724,-121.884934");

        ExecutePlotCurrentUserOnMap(locMapTopLeft, locMapTopRight, locMapBottomLeft, locMapBottomRight, locTestLocation);

    }


    private void ExecutePlotCurrentUserOnMap(Location locMapTopLeft, Location locMapTopRight,
                                             Location locMapBottomLeft, Location locMapBottomRight,
                                             Location locCurrentUserLocation) {

        //This is the angle the earth is tilted by - and the map is tilted by, in accordance with the static image
        double angle = 10.0;
        double r = Math.toRadians(angle);

        double bearingTLtoTR = locMapTopLeft.bearingTo(locMapTopRight);
        double distanceTLtoTR = locMapTopLeft.distanceTo(locMapTopRight);
        double bearingTRtoTL = locMapTopRight.bearingTo(locMapTopLeft);


        double bearingTLtoCL = locMapTopLeft.bearingTo(locCurrentUserLocation);
        double diffAngle = (bearingTLtoCL - bearingTLtoTR);

        double hypot_current_loc = locMapTopLeft.distanceTo(locCurrentUserLocation);
        double current_x_dist = hypot_current_loc * Math.cos(Math.toRadians(diffAngle));
        double current_y_dist = hypot_current_loc * Math.sin(Math.toRadians(diffAngle));


        double current_x_pixels = FIXED_PIXEL_DISTANCE * current_x_dist;
        double current_y_pixels = FIXED_PIXEL_DISTANCE * current_y_dist;

        //Get the old points floated out
        float current_X = (float) current_x_pixels;
        float current_Y = (float) current_y_pixels;

        ////Let's rotate this about the top left
        float center_X = 13;
        float center_Y = 445;

        float new_X = (float) (center_X + ((current_X - center_X) * Math.cos(r) + (current_Y - center_Y) * Math.sin(r)));
        float new_Y = (float) (center_Y + ((current_X - center_X) * -1 * (Math.sin(r)) + (current_Y - center_Y) * Math.cos(r)));

        PlotCircle(this, new_X, new_Y);

    }

    private void PlotCircle(Context context, float x, float y) {
        //Normalize incoming pixels
        x = x + intXAxisPlotOffset;
        y = y + intYAxisPlotOffset + CURRENT_LOCATION_SCALE_CORRECTER;

        RelativeLayout map_layout = (RelativeLayout) findViewById(R.id.activity_map);

        if (circlemarker != null) {
            map_layout.removeView(circlemarker);
        }

        circlemarker = new CircleMarkerView(context);
        circlemarker.set_x_y_coord(x, y);
        map_layout.addView(circlemarker);
    }


    private void CenterMapImage() {
        //Get image dimensions
        Drawable mapDrawable = mapImageView.getDrawable();
        float imageWidth = mapDrawable.getIntrinsicWidth();
        float imageHeight = mapDrawable.getIntrinsicHeight();

        //Get Screen dimensions
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        //Now center the image to scale to the view's center
        RectF mapDrawableRect = new RectF(0, 0, imageWidth, imageHeight);
        RectF viewImageRect = new RectF(0, 0, screenWidth, screenHeight);
        mapMatrix.setRectToRect(mapDrawableRect, viewImageRect, Matrix.ScaleToFit.CENTER);
        mapImageView.setImageMatrix(mapMatrix);


    }

    private class MarkerView extends View {
        private float x_coord = 1000, y_coord = 1000;

        public MarkerView(Context context) {
            super(context);
        }

        public void set_x_y_coord(float x, float y) {
            x_coord = x;
            y_coord = y;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            Bitmap marker = BitmapFactory.decodeResource(getResources(), R.drawable.pin);
            canvas.drawBitmap(marker, x_coord, y_coord, null);
        }
    }

    private class CircleMarkerView extends View {
        private float x_coord = 1000, y_coord = 1000;

        public CircleMarkerView(Context context) {
            super(context);
        }

        public void set_x_y_coord(float x, float y) {
            x_coord = x;
            y_coord = y;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            Bitmap marker = BitmapFactory.decodeResource(getResources(), R.drawable.current_small);
            canvas.drawBitmap(marker, x_coord, y_coord, null);
        }
    }


    //Start user current location
    @TargetApi(Build.VERSION_CODES.M)
    public void GetCurrentLocation(Context context) {
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        Location location;
        boolean isNetworkEnabled = mLocationManager.isProviderEnabled(LocationProviderString);

        if (isNetworkEnabled) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            location = mLocationManager.getLastKnownLocation(LocationProviderString);

            mLocationManager.requestLocationUpdates(LocationProviderString, LOCATION_MIN_TIME, LOCATION_MIN_DISTANCE, mLocationListener);

            if (location != null) {
                strCurrentUserLatitude = Location.convert(location.getLatitude(), Location.FORMAT_DEGREES);
                strCurrentUserLongitude = Location.convert(location.getLongitude(), Location.FORMAT_DEGREES);
                strCurrentUserLocation = strCurrentUserLatitude + "," + strCurrentUserLongitude;
                locCurrentLocation = location;
                Log.d("MainActivity", "Location of user is currently: " + strCurrentUserLocation);
                updateCurrentUserLocationOnMap(); //when app starts up
                return;
            }
        }
        //BUG: fix for all versions of android
        requestPermissions(LOCATION_PERMS, LOCATION_REQUEST_CODE);
    }

    //Let's handle user's location now
    //First off define a listener for location changed events
    public final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {

            strCurrentUserLatitude = Location.convert(location.getLatitude(), Location.FORMAT_DEGREES);
            strCurrentUserLongitude = Location.convert(location.getLongitude(), Location.FORMAT_DEGREES);
            strCurrentUserLocation = strCurrentUserLatitude + "," + strCurrentUserLongitude;


            //String location_string = "geo:37.7749,-122.4194";
            String location_string = "geo:" + strCurrentUserLatitude + "," + strCurrentUserLongitude;
            Log.d("MainActivity", "Location changed: "+ location_string);

            //Update current user location and strings
            strCurrentUserLocation = strCurrentUserLatitude + "," + strCurrentUserLongitude;
            locCurrentLocation = location;

            //Call after current location is known to continuously plot user on map
            updateCurrentUserLocationOnMap(); //When users location changes (once every minute?)

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    //Now we need to handle it after getting appropriate permissions:
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationManager.requestLocationUpdates(LocationProviderString, LOCATION_MIN_TIME, LOCATION_MIN_DISTANCE, mLocationListener);
                //DEBUG:
                //mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, mLocationListener, null);
            }

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private View.OnTouchListener map_touch_listener =
            new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        float screenX = event.getX();
                        float screenY = event.getY();
                        float viewX = screenX - v.getLeft();
                        float viewY = screenY - v.getTop();

                        Log.d("MapActivity", "X: " + viewX + " Y: " + viewY + " ScreenX: " + screenX + " ScreenY:" + screenY);

                        //DEBUG:
                        //plotPin(v.getContext(), viewX, viewY);
                        ProcessTouchCoordinate(v, viewX, viewY);
                        return true;
                    }
                    return false;
                }
            };

    //End of class
}
