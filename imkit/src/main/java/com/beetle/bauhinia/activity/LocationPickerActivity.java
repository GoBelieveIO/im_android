package com.beetle.bauhinia.activity;

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
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
//import com.amap.api.location.LocationManagerProxy;
//import com.amap.api.location.LocationProviderProxy;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.beetle.imkit.R;

import java.lang.ref.WeakReference;

/**
 * AMapV1地图demo总汇
 */
public class LocationPickerActivity extends BaseActivity implements GeocodeSearch.OnGeocodeSearchListener, AMapLocationListener, LocationSource {
    private static final int MSG_LOCATION_TIMEOUT = 1000;
    private MapView mapView;

    private AMap aMap;
    private View pin;
    private TextView label;
    private GeocodeSearch mGeocodeSearch;

    private OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;

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

        setUpMap();
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
        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (result != null && result.getRegeocodeAddress() != null
                    && result.getRegeocodeAddress().getFormatAddress() != null) {
                String addressName = result.getRegeocodeAddress().getFormatAddress();
                setLocation(latitude, longitude, addressName);

            } else {
                setLocation(latitude, longitude, "");
            }
        }

    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {
    }

    /**
     * 设置一些amap的属性
     */
    private void setUpMap() {
        aMap.setLocationSource(this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        setupLocationStyle();
    }

    private void setupLocationStyle(){

    }

    /**
     * 激活定位
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();
        }
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }


    /*======== 定位回调 begin ======== */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            setLocation(aMapLocation.getLatitude(), aMapLocation.getLongitude(), aMapLocation.getAddress());

            if (mListener != null && aMapLocation.getErrorCode() == 0) {
                mListener.onLocationChanged(aMapLocation);// 显示系统小蓝点
                aMap.moveCamera(CameraUpdateFactory.zoomTo(18));
            }

        } else {
            setLocation(0, 0, null);
        }
        stopLocation();
    }

    private void stopLocation() {

        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
            mlocationClient = null;
        }

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
