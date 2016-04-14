package com.tonikamitv.busTracker;

import android.app.ProgressDialog;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.tonikamitv.loginregister.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class UserAreaActivity extends AppCompatActivity
        implements GooglePlayServicesClient.ConnectionCallbacks,
        com.google.android.gms.location.LocationListener,
        GooglePlayServicesClient.OnConnectionFailedListener {

    String myJSON;
    String JSON_STRING;
    private static final String TAG_BID = "busId";
    private static final String TAG_LAT = "busLatitude";
    private static final String TAG_LONG = "busLongitude";
    private static final String TAG_SPEED = "busSpeed";
    private static final String TAG_CONTACT = "busContact";
    private static final String TAG_NUMBER  = "busNumber";
    private static final String TAG_RESULTS = "result";
    private static final String TAG_DISTANCE = "distance";
    JSONArray events = null;
    ArrayList<HashMap<String, String>> eventList;
    ListView list;
    ListAdapter myAdapter;
    ProgressDialog pDialog;

    private GoogleMap myMap;            // map reference
    private Location prevLoc = null;
    private LocationClient myLocationClient;

    private Handler handler;
    private Runnable runnableCode;
    private boolean onceUpdated;
    private ImageView arrow;
    private SlidingUpPanelLayout.PanelState prevState;

    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(10000)         // 10 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    private static final String TAG = "UserAreaActivity";
    private SlidingUpPanelLayout mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sliding_main);

        getMapReference();

        //setSupportActionBar((Toolbar) findViewById(R.id.main_toolbar));

        list = (ListView) findViewById(R.id.list);
        eventList = new ArrayList<HashMap<String,String>>();
        //getData();

        onceUpdated = false;
        // Create the Handler object (on the main thread by default)
        handler = new Handler();
        // Define the code block to be executed
        runnableCode = new Runnable() {
            @Override
            public void run() {
                // Do something here on the main thread
                Log.d("Handlers", "Called on main thread");
                // Repeat this the same runnable code block again another 2 seconds
                if(onceUpdated){
                    getData();
                }

                //Toast.makeText(UserAreaActivity.this,"2 seconds over...", Toast.LENGTH_SHORT).show();
                handler.postDelayed(runnableCode, 2000);
            }
        };
// Start the initial runnable task by posting through the handler
        //handler.postDelayed(runnableCode,2000);
        handler.post(runnableCode);

        arrow = (ImageView) findViewById(R.id.id_arrow);
        prevState = SlidingUpPanelLayout.PanelState.COLLAPSED;
        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                Log.i(TAG, "onPanelSlide, offset " + slideOffset);
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                Log.i(TAG, "onPanelStateChanged " + newState);

                if(newState.equals(SlidingUpPanelLayout.PanelState.DRAGGING)
                        &&prevState.equals(SlidingUpPanelLayout.PanelState.EXPANDED)){
                    arrow.setImageResource(R.drawable.up_mdpi);
                    prevState = SlidingUpPanelLayout.PanelState.COLLAPSED;
                    System.out.println("Dragging Down");
                }
                else if(newState.equals(SlidingUpPanelLayout.PanelState.DRAGGING)
                        &&prevState.equals(SlidingUpPanelLayout.PanelState.COLLAPSED)){
                    arrow.setImageResource(R.drawable.down_mdpi);
                    prevState = SlidingUpPanelLayout.PanelState.EXPANDED;
                    System.out.println("Dragging Up");
                }
            }
        });
        mLayout.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });
        TextView t = (TextView) findViewById(R.id.name);
        t.setText("Bus List");

    }

    /**
     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
     // Inflate the menu; this adds items to the action bar if it is present.
     getMenuInflater().inflate(R.menu.demo, menu);
     MenuItem item = menu.findItem(R.id.action_toggle);
     if (mLayout != null) {
     if (mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN) {
     item.setTitle(R.string.action_show);
     } else {
     item.setTitle(R.string.action_hide);
     }
     }
     return true;
     }

     @Override
     public boolean onPrepareOptionsMenu(Menu menu) {
     return super.onPrepareOptionsMenu(menu);
     }

     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
     switch (item.getItemId()){
     case R.id.action_toggle: {
     if (mLayout != null) {
     if (mLayout.getPanelState() != SlidingUpPanelLayout.PanelState.HIDDEN) {
     mLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
     item.setTitle(R.string.action_show);
     } else {
     mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
     item.setTitle(R.string.action_hide);
     }
     }
     return true;
     }
     case R.id.action_anchor: {
     if (mLayout != null) {
     if (mLayout.getAnchorPoint() == 1.0f) {
     mLayout.setAnchorPoint(0.7f);
     mLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
     item.setTitle(R.string.action_anchor_disable);
     } else {
     mLayout.setAnchorPoint(1.0f);
     mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
     item.setTitle(R.string.action_anchor_enable);
     }
     }
     return true;
     }
     }
     return super.onOptionsItemSelected(item);
     }

     */

    @Override
    public void onBackPressed() {
        if (mLayout != null &&
                (mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED || mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)) {
            mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            handler.removeCallbacks(runnableCode);
            super.onBackPressed();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        onceUpdated = true;
        if(prevLoc==null){
            prevLoc = location;
        }

        double distance = location.distanceTo(prevLoc);
        //Toast.makeText(UserAreaActivity.this,"location changed..."+distance, Toast.LENGTH_SHORT).show();
        if(distance>1){
            prevLoc = location;
        }

    }

    @Override
    public void onConnected(Bundle bundle) {
        myLocationClient.requestLocationUpdates(
                REQUEST,
                this); // LocationListener

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    /**
     *     Activity's lifecycle event.
     *     onResume will be called when the Activity receives focus
     *     and is visible
     */
    @Override
    protected  void onResume(){
        handler.post(runnableCode);
        super.onResume();
        getMapReference();
        wakeUpLocationClient();
        myLocationClient.connect();

    }

    /**
     *      Activity's lifecycle event.
     *      onPause will be called when activity is going into the background,
     */
    @Override
    public void onPause(){
        handler.removeCallbacks(runnableCode);
        super.onPause();
        if(myLocationClient != null){
            myLocationClient.disconnect();
        }
    }

    /**
     *
     * @param lat - latitude of the location to move the camera to
     * @param lng - longitude of the location to move the camera to
     *            Prepares a CameraUpdate object to be used with  callbacks
     */
    private void gotoMyLocation(double lat, double lng) {
        changeCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(new LatLng(lat, lng))
                        .zoom(15.5f)
                        .bearing(0)
                        .tilt(25)
                        .build()
        ), new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                // Your code here to do something after the Map is rendered
            }

            @Override
            public void onCancel() {
                // Your code here to do something after the Map rendering is cancelled
            }
        });
    }

    /**
     *      When we receive focus, we need to get back our LocationClient
     *      Creates a new LocationClient object if there is none
     */
    private void wakeUpLocationClient() {
        if(myLocationClient == null){
            myLocationClient = new LocationClient(getApplicationContext(),
                    this,       // Connection Callbacks
                    this);      // OnConnectionFailedListener
        }
    }

    /**
     *      Get a map object reference if none exits and enable blue arrow icon on map
     */
    private void getMapReference() {
        if(myMap == null){
            myMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
        }
        if(myMap != null){
            myMap.setMyLocationEnabled(true);
        }

    }

    private void changeCamera(CameraUpdate update, GoogleMap.CancelableCallback callback) {
        myMap.moveCamera(update);
    }

    public void getData(){

        GetDataJSON g = new GetDataJSON();
        g.execute();
    }

    private class GetDataJSON extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute()
        {
            //pDialog = new ProgressDialog(UserAreaActivity.this);
            //pDialog.setMessage("Loading...");
            //pDialog.show();
        }
        @Override
        protected String doInBackground(String... params) {
            try {

                URL url = new URL("http://battikgp.net23.net/Buses.php");
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setUseCaches(true);

                //System.out.println("Inside url3");
                InputStream inputStream = httpURLConnection.getInputStream();
                //System.out.println("Inside url4");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                //System.out.println("Inside url4b");
                StringBuilder stringBuilder = new StringBuilder();
                while ((JSON_STRING = bufferedReader.readLine())!=null)
                {
                    //System.out.println("Inside url5");
                    stringBuilder.append(JSON_STRING+"\n");
                }
                // System.out.println("Inside url6");
                bufferedReader.close();
                inputStream.close();
                httpURLConnection.disconnect();
                return stringBuilder.toString().trim();

            } catch (MalformedURLException e) {
                //System.out.println("Inside url7");
                e.printStackTrace();
            } catch (IOException e) {
                //System.out.println("Inside url8");
                e.printStackTrace();
            }
            //System.out.println("Inside url9");
            return null;
        }

        @Override
        protected void onPostExecute(String result){
            if(result != null){
                myJSON=result;
                showList();
                //pDialog.dismiss();

            }else{
                //pDialog.dismiss();
                Toast.makeText(UserAreaActivity.this, "Network Error", Toast.LENGTH_SHORT).show();

            }
        }
    }

    protected double roundTwoDecimals(double d) {
        DecimalFormat twoDecimals = new DecimalFormat("#.##");
        return Double.valueOf(twoDecimals.format(d));
    }

    protected void showList(){
        try {
            JSONObject jsonObj = new JSONObject(myJSON);
            events = jsonObj.getJSONArray(TAG_RESULTS);
            //System.out.println("Inside try");
            eventList.clear();
            myMap.clear();
            for(int i=0;i<events.length();i++){
                JSONObject c = events.getJSONObject(i);
                String bid = c.getString(TAG_BID);
                String latitude = c.getString(TAG_LAT);
                String longitude = c.getString(TAG_LONG);
                String contact = c.getString(TAG_CONTACT);
                String speed = c.getString(TAG_SPEED);
                String bnumber = c.getString(TAG_NUMBER);

                double b_lat = Double.parseDouble(latitude);
                double b_long = Double.parseDouble(longitude);
                Location busLoc = new Location("");//provider name is unecessary
                busLoc.setLatitude(b_lat);//your coords of course
                busLoc.setLongitude(b_long);

                //Location myLocation = (Location) getIntent().getSerializableExtra("myLoc");
                double my_lat = prevLoc.getLatitude(); //22.321923;//getIntent().getExtras().getDouble("myLat");
                double my_long = prevLoc.getLongitude(); //87.3064147;//getIntent().getExtras().getDouble("myLong");

                Location currentLoc = new Location("");//provider name is unecessary
                currentLoc.setLatitude(my_lat);//your coords of course
                currentLoc.setLongitude(my_long);
                String dist = "Turn on GPS...";
                if(currentLoc!=null){
                    double distance = currentLoc.distanceTo(busLoc)/1000;
                    distance = roundTwoDecimals(distance);
                    dist = String.valueOf(distance)+" km";
                }

                /**
                 Geocoder geocoder;
                 List<Address> addresses;
                 geocoder = new Geocoder(this, Locale.getDefault());
                 addresses = geocoder.getFromLocation(busLoc.getLatitude(), busLoc.getLongitude(), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5

                 String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                 String city = addresses.get(0).getLocality();
                 */
                Marker marker = myMap.addMarker(new MarkerOptions()
                        .position(new LatLng(b_lat, b_long))
                        .snippet("Running")
                        .title(bnumber));

                HashMap<String,String> events = new HashMap<String,String>();

                events.put(TAG_BID,bid);
                events.put(TAG_LAT,latitude);
                events.put(TAG_LONG,longitude);
                events.put(TAG_SPEED,speed);
                events.put(TAG_CONTACT,contact);
                events.put(TAG_NUMBER, bnumber);
                events.put(TAG_DISTANCE,dist );
                eventList.add(events);
            }
            Collections.sort(eventList, new MapComparator(TAG_DISTANCE));

            //System.out.println("Around getdata3");
            //System.out.println("Inside try1");
            ListAdapter myAdapter = new SimpleAdapter(
                    getApplicationContext(), eventList, R.layout.bus_list,
                    new String[]{TAG_NUMBER,TAG_DISTANCE},
                    new int[]{R.id.id_bus_number,R.id.id_bus_dist}
            );
            //System.out.println("Inside show list");
            list.setAdapter(myAdapter);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    //System.out.println("Inside list click1");
                    Toast.makeText(UserAreaActivity.this, "Item at position :"+position+" clicked", Toast.LENGTH_SHORT).show();

                }

            });



        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    class MapComparator implements Comparator<Map<String, String>>
    {
        private final String key;

        public MapComparator(String key)
        {
            this.key = key;
        }

        public int compare(Map<String, String> first,
                           Map<String, String> second)
        {
            // TODO: Null checking, both for maps and values
            String firstValue = first.get(key);
            String secondValue = second.get(key);
            return firstValue.compareTo(secondValue);
        }
    }
}
