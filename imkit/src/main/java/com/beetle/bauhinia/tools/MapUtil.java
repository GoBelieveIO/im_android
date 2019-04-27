/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.tools;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by houxh on 2017/11/23.
 */

public class MapUtil {
    public static boolean isAvailable(Context context, String packageName){
        final PackageManager packageManager = context.getPackageManager();
        //获取所有已安装程序的包信息
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
        List<String> packageNames = new ArrayList<String>();
        if(packageInfos != null){
            for(int i = 0; i < packageInfos.size(); i++){
                String packName = packageInfos.get(i).packageName;
                packageNames.add(packName);
            }
        }
        //判断packageNames中是否有目标程序的包名，有TRUE，没有FALSE
        return packageNames.contains(packageName);
    }

    public static void openBaidu(Context context, String poiname, double longitude, double latitude) {

        try {
            poiname = (poiname != null) ? poiname : "";
            Intent intent = Intent.parseUri("intent://map/direction?" + "destination=latlng:" + latitude + "," + longitude + "|name:" + poiname, 0);
            context.startActivity(intent);
        } catch (URISyntaxException e) {
            Log.e("intent", e.getMessage());
        }

    }

    public static boolean isBaiduAvailable(Context context) {
        return isAvailable(context,"com.baidu.BaiduMap");
    }
    //高德地图
    public static void openAMap(Context context, String poiname, double longitude, double latitude) {

        try{
            if (TextUtils.isEmpty(poiname)) {
                Intent intent = Intent.parseUri("androidamap://navi?sourceApplication=瓜聊" + "&lat=" + latitude + "&lon=" + longitude + "&dev=0", 0);
                context.startActivity(intent);
            } else {
                Intent intent = Intent.parseUri("androidamap://navi?sourceApplication=瓜聊" + "&lat=" + latitude + "&lon=" + longitude + "&dev=0"  + "&poiname=" + poiname, 0);
                context.startActivity(intent);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static boolean isAMapAvailable(Context context) {
        return isAvailable(context, "com.autonavi.minimap");
    }

    public static void openMap(Context context, String poiname, double longitude, double latitude) {
        if (isAMapAvailable(context)) {
            openAMap(context, poiname, longitude, latitude);
        } else if (isBaiduAvailable(context)) {
            openBaidu(context, poiname, longitude, latitude);
        }
    }
}
