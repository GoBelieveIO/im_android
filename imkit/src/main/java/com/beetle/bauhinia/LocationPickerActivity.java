package com.beetle.bauhinia;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.LocationManagerProxy;
import com.amap.api.location.LocationProviderProxy;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.beetle.bauhinia.activity.BaseActivity;
import com.beetle.imkit.R;

import java.lang.ref.WeakReference;

/**
 * AMapV1地图demo总汇
 */
public class LocationPickerActivity extends BaseActivity implements GeocodeSearch.OnGeocodeSearchListener, AMapLocationListener {
    private static final int MSG_LOCATION_TIMEOUT = 1000;
    private MapView mapView;

    private AMap aMap;
    private View pin;
    private TextView label;
    private GeocodeSearch mGeocodeSearch;
    private LocationManagerProxy mLocationManagerProxy;
    private MyHandler mMyHandler;


    double longitude = 0;
    double latitude = 0;
    String address;

    private boolean isCameraChanging = false;

    public static Intent newIntent(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, LocationPickerActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location_picker);

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 必须要写
        aMap = mapView.getMap();

        label = (TextView) findViewById(R.id.label);
        pin = findViewById(R.id.pin);
        aMap.setOnMapTouchListener(new AMap.OnMapTouchListener() {
            @Override
            public void onTouch(MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    if (!isCameraChanging) {
                        isCameraChanging = true;
                        pinUp();
                        setLabel(null);
                    }
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    isCameraChanging = false;
                    pinDown();
                    latitude = aMap.getCameraPosition().target.latitude;
                    longitude = aMap.getCameraPosition().target.longitude;
                    queryLocation();
                }
            }
        });

        mGeocodeSearch = new GeocodeSearch(this);
        mGeocodeSearch.setOnGeocodeSearchListener(this);

        // 定位当前位置
        getLocation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.location_picker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_send) {
            if (longitude == 0.0 && latitude == 0.0) {
                return true;
            }

            Intent intent = new Intent();
            intent.putExtra("longitude", (float) longitude);
            intent.putExtra("latitude", (float) latitude);
            intent.putExtra("address", address);
            setResult(RESULT_OK, intent);

            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    private void setLocation(double latitude, double longitude, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;

        LatLng latLng = new LatLng(latitude, longitude);
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        setLabel(address);

//        Toast.makeText(this, String.format(Locale.getDefault(), "lat:%f,lon:%f,%s", latitude, longitude, address), Toast.LENGTH_SHORT).show();
//        Marker marker = aMap.addMarker(new MarkerOptions()
//                .position(latLng)
//                .title(address)
//                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
//                .draggable(true));
//        marker.showInfoWindow();
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
        if (rCode == 0 && result != null && result.getRegeocodeAddress() != null) {
            String address;
            if (result.getRegeocodeAddress().getPois() != null && result.getRegeocodeAddress().getPois().size() > 0) {
                address = result.getRegeocodeAddress().getPois().get(0).getTitle();
            } else {
                address = result.getRegeocodeAddress().getFormatAddress();
            }
            setLocation(latitude, longitude, address);
        } else {
            // 定位失败;
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {
    }


    public void getLocation() {
        mLocationManagerProxy = LocationManagerProxy.getInstance(this);
        /*
         * mAMapLocManager.setGpsEnable(false);//
		 * 1.0.2版本新增方法，设置true表示混合定位中包含gps定位，false表示纯网络定位，默认是true Location
		 * API定位采用GPS和网络混合定位方式
		 * ，第一个参数是定位provider，第二个参数时间最短是2000毫秒，第三个参数距离间隔单位是米，第四个参数是定位监听者
		 */
        mLocationManagerProxy.requestLocationData(LocationProviderProxy.AMapNetwork, 2000, 10, this);

        sendMsg(MSG_LOCATION_TIMEOUT, 12000);// 设置超过12秒还没有定位到就停止定位
    }

    private void sendMsg(int what, int delayMS) {
        if (mMyHandler == null) {
            mMyHandler = new MyHandler(this);
        }
        mMyHandler.sendEmptyMessageDelayed(what, delayMS);
    }

    /*======== 定位回调 begin ======== */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            setLocation(aMapLocation.getLatitude(), aMapLocation.getLongitude(), aMapLocation.getAddress());
        } else {
            setLocation(0, 0, null);
        }
        stopLocation(false);
    }

    @Override
    public void onLocationChanged(Location location) {

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
    /*======== 定位回调 end ======== */

    private static class MyHandler extends Handler {

        private final WeakReference<LocationPickerActivity> mWeakReference;

        public MyHandler(LocationPickerActivity locationUtil) {
            this.mWeakReference = new WeakReference<LocationPickerActivity>(locationUtil);
        }

        @Override
        public void handleMessage(Message msg) {
            LocationPickerActivity locationUtil = mWeakReference.get();
            if (locationUtil != null) {
                switch (msg.what) {
                    case MSG_LOCATION_TIMEOUT:
                        locationUtil.stopLocation(true);
                        break;
                }
            }
        }
    }

    private void stopLocation(boolean isTimeout) {
        if (isTimeout && mLocationManagerProxy != null) {
            // 超时并仍在定位
            Toast.makeText(this, "超时", Toast.LENGTH_SHORT).show();
        }
        if (mLocationManagerProxy != null) {
            mLocationManagerProxy.removeUpdates(this);
            mLocationManagerProxy.destory();
        }
        mLocationManagerProxy = null;
    }

    private void queryLocation() {
        // 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
        RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(latitude, longitude), 200, GeocodeSearch.AMAP);
        mGeocodeSearch.getFromLocationAsyn(query);// 设置同步逆地理编码请求
    }

    private void pinUp() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(pin, "TranslationY", -getPinY());
        anim.setDuration(500);
        anim.setInterpolator(new AccelerateInterpolator());
        anim.start();
    }

    private void pinDown() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(pin, "TranslationY", 0);
        anim.setDuration(1000);
        anim.setInterpolator(new BounceInterpolator());
        anim.start();
    }

    private int getPinY() {
        return pin.getHeight() - getResources().getDimensionPixelOffset(R.dimen.pin_margin);
    }

    private void setLabel(String address) {
        label.setText(address);
        if (!TextUtils.isEmpty(address)) {
            label.setVisibility(View.VISIBLE);
        } else {
            label.setVisibility(View.GONE);
        }
    }
}
