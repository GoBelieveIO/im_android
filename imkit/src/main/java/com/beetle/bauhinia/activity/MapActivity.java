package com.beetle.bauhinia.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;

import com.beetle.bauhinia.tools.MapUtil;
import com.beetle.imkit.R;
/**
 * AMapV1地图demo总汇
 */
public class MapActivity extends BaseActivity implements GeocodeSearch.OnGeocodeSearchListener, AMapLocationListener {
    private MapView mapView;

    private AMap aMap;
    private GeocodeSearch mGeocodeSearch;


    TextView addressView;
    TextView townshipView;
    double longitude;
    double latitude;
    String poiname = "";

    public static Intent newIntent(Context context, float longitude, float latitude) {
        Intent intent = new Intent();
        intent.setClass(context, MapActivity.class);
        intent.putExtra("longitude", longitude);
        intent.putExtra("latitude", latitude);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_location);


        Intent intent = getIntent();
        longitude = intent.getFloatExtra("longitude", 0);
        latitude = intent.getFloatExtra("latitude", 0);


        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 必须要写
        aMap = mapView.getMap();

        mGeocodeSearch = new GeocodeSearch(this);
        mGeocodeSearch.setOnGeocodeSearchListener(this);


        addressView = (TextView)findViewById(R.id.address);
        townshipView = (TextView)findViewById(R.id.township);

        setLocation(latitude, longitude, "");

        // 根据经纬度定位
        queryLocation();
    }

    public void onNavigation(View sender) {
        MapUtil.openMap(this, poiname, longitude, latitude);
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

    private void queryLocation() {
        // 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
        RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(latitude, longitude), 200, GeocodeSearch.AMAP);
        mGeocodeSearch.getFromLocationAsyn(query);// 设置同步逆地理编码请求
    }

    private void setLocation(double latitude, double longitude, String address) {
        LatLng latLng = new LatLng(latitude, longitude);
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

        Marker marker = aMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(address)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .draggable(true));
        marker.showInfoWindow();
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int rCode) {

        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (result != null && result.getRegeocodeAddress() != null
                    && result.getRegeocodeAddress().getFormatAddress() != null) {
                setLocation(latitude, longitude, result.getRegeocodeAddress().getFormatAddress());
                RegeocodeAddress addr = result.getRegeocodeAddress();
                poiname = addr.getFormatAddress();
                addressView.setText(addr.getFormatAddress());
                townshipView.setText(addr.getTownship());
            }
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {
    }



    /*======== 定位回调 begin ======== */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {

    }

}
