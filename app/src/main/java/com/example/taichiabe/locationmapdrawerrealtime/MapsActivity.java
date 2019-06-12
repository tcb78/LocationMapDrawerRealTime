package com.example.taichiabe.locationmapdrawerrealtime;
/*===========================================================*
 * LocationMapDrawerの位置情報描画とGPSLocationAcquisitionerのスレッドによる現在地情報取得をハイブリッド
 */
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private final int REQUEST_PERMISSION = 1000;
    private LocationManager locationManager;
    private static final int MIN_TIME = 1000;
    private static final int MIN_DISTANCE = 50;
    final Handler handler = new Handler();

    Thread locAcq;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //パーミッションの確認
        checkPermission();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
    }

    // 位置情報許可の確認
    public void checkPermission() {
        // 拒否していた場合
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){

            requestLocationPermission();
        }
    }

    // 許可を求める
    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION);

        } else {
            Toast toast = Toast.makeText(this,
                    "許可されないとアプリが実行できません", Toast.LENGTH_SHORT);
            toast.show();

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,},
                    REQUEST_PERMISSION);

        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // 豊洲橋南交差点
        LatLng toyosubashiminamiCrossing = new LatLng(35.660859, 139.793826);
        setIcon(toyosubashiminamiCrossing);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(toyosubashiminamiCrossing, 18));

        String message = "Latitude:35.660859\nLongitude:139.793826";
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.show();

        locAcq = new Thread(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        startGPS();
                    }
                });
            }
        });
        locAcq.start();
    }

    protected void startGPS() {

        Log.d("LocationActivity", "gpsEnabled");
        final boolean gpsEnabled
                = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsEnabled) {
            // GPSを設定するように促す
            enableLocationSettings();
        }

        if (locationManager != null) {
            Log.d("LocationActivity", "locationManager.requestLocationUpdates");

            try {
                // minTime = 1000msec, minDistance = 50m
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED){

                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                /**
                 * requestLocationUpdatesのJavadoc
                 * @param provider プロバイダ名を指定
                 * @param minTime 位置情報の更新間隔をミリ秒で指定
                 * @param minDistance 位置情報の更新距離をメートルで指定
                 * @param listener LocationListenerを指定
                 */
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        0, 0, this);
            } catch (Exception e) {
                e.printStackTrace();

                Toast toast = Toast.makeText(this,
                        "例外が発生、位置情報のPermissionを許可していますか？",
                        Toast.LENGTH_SHORT);
                toast.show();

                //MainActivityに戻す
                finish();
            }
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (locationManager != null) {
            Log.d("LocationActivity", "locationManager.removeUpdates");
            // update を止める
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED){

                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        LatLng myLocation = new LatLng(lat, lng);
        setIcon(myLocation);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 18));
        String message = "Latitude:"+lat+"\nLongitude:"+lng;
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.show();
        try {
            locationManager.removeUpdates(this);
        } catch(SecurityException e) {

        }

    }

    private void setIcon(LatLng latlng){

        // マップに貼り付ける BitmapDescriptor生成
        BitmapDescriptor descriptor =
                BitmapDescriptorFactory.fromResource(R.drawable.seniorcar);

        // 貼り付設定
        GroundOverlayOptions overlayOptions = new GroundOverlayOptions();
        overlayOptions.image(descriptor);

        //　public GroundOverlayOptions anchor (float u, float v)
        // (0,0):top-left, (0,1):bottom-left, (1,0):top-right, (1,1):bottom-right
        overlayOptions.anchor(0.5f, 0.5f);

        // 張り付け画像の大きさ メートル単位
        // public GroundOverlayOptions	position(LatLng location, float width, float height)
        overlayOptions.position(latlng, 10f, 10f);

        // マップに貼り付け・アルファを設定
        GroundOverlay overlay = mMap.addGroundOverlay(overlayOptions);

        // 透明度
        overlay.setTransparency(0.0F);

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        switch (status) {
            case LocationProvider.AVAILABLE:

                break;
            case LocationProvider.OUT_OF_SERVICE:

                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:

                break;
        }
    }

    private void enableLocationSettings() {
        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(settingsIntent);
    }

    private void stopGPS(){
        if (locationManager != null) {
            Log.d("LocationActivity", "onStop()");

            // update を止める
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        stopGPS();
    }
}
