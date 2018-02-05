package cristian_sedano.uberclone;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Welcome extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{

    private GoogleMap mMap;

    // play services
    private static final int PERMISSION_REQUEST_CODE = 7000;
    private static final int PLAY_SERVICE_RES_REQUEST = 7001;

    private LocationRequest mLocatonRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    private static int UPDATE_INTERVAL = 5000;
    private static int FATEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    DatabaseReference drivers;
    GeoFire geoFire;

    Marker mCurrent;

    MaterialAnimatedSwitch location_switch;
    SupportMapFragment mapFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Init View

        location_switch = (MaterialAnimatedSwitch) findViewById(R.id.location_switch);
        location_switch.setOnCheckedChangeListener(new MaterialAnimatedSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean isOnLine) {

                if (isOnLine){
                    startLocationUpdates();
                    displayLocation();
                    Snackbar.make(mapFragment.getView(), "You are online", Snackbar.LENGTH_SHORT).show();
                }else {
                    stopLocationUpdates();
                    mCurrent.remove();
                    Snackbar.make(mapFragment.getView(), "You are offline", Snackbar.LENGTH_SHORT).show();
                }

            }
        });

        // Geo Fire
        drivers = FirebaseDatabase.getInstance().getReference("Drivers");
        geoFire = new GeoFire(drivers);

        setUpLocation();
    }

    // Press CTRL + o
    // Because we request runTime permission, we need Override OnRequestPermissionResult method


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if (checkPlayService())
                    {
                        buildGoogleApiClient();
                        createLocationRequest();
                        if (location_switch.isChecked())
                            displayLocation();

                        }

                }
        }
    }

    private void setUpLocation() {

        if (android.support.v4.app.ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                android.support.v4.app.ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Request runtime permissions
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        }else {
            if (checkPlayService()){
                buildGoogleApiClient();
                createLocationRequest();
                if (location_switch.isChecked()){
                    displayLocation();
                }
            }
        }
    }

    private void createLocationRequest() {
        mLocatonRequest = new LocationRequest();
        mLocatonRequest.setInterval(UPDATE_INTERVAL);
        mLocatonRequest.setFastestInterval(FATEST_INTERVAL);
        mLocatonRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocatonRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private boolean checkPlayService() {

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS)
        {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICE_RES_REQUEST).show();
            else {
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void stopLocationUpdates() {
        if (android.support.v4.app.ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                android.support.v4.app.ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            return;
        }

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,  (com.google.android.gms.location.LocationListener)this);

    }

    private void displayLocation() {
        if (android.support.v4.app.ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                android.support.v4.app.ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            if (location_switch.isChecked()) {
                final double latitude = mLastLocation.getLatitude();
                final double longitude = mLastLocation.getLongitude();

                // update to Firebase
                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        // Add Marker
                        if (mCurrent != null)
                            mCurrent.remove(); // remove already marker

                        mCurrent = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.car))
                                .position(new LatLng(latitude, longitude))
                                .title("You"));

                        // Move camera to this position
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15.0f));




                        // Draw animation rotate marker
                        rotateMarker(mCurrent, -360, mMap);
                    }
                });
            }
        }
    }

    private void rotateMarker(final Marker mCurrent, final float i, GoogleMap mMap) {

        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final float startRotation = mCurrent.getRotation();
        final long duration = 1500;

        final Interpolator interpolator = new LinearInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float)elapsed/duration);
                float rot = t*i+(1-t)*startRotation;
                mCurrent.setRotation(-rot > 100?rot/2:rot);
                if (t<1.0){
                    handler.postDelayed(this, 16);
                }


            }
        });
    }

    private void startLocationUpdates() {

        if (android.support.v4.app.ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                android.support.v4.app.ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {

            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocatonRequest,  this);

}

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        displayLocation();

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        displayLocation();
        startLocationUpdates();

    }

    @Override
    public void onConnectionSuspended(int i) {

        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
