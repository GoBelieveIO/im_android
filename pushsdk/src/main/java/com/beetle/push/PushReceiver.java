/*******************************************************************************
 * Copyright 2014 ngds.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/

package com.beetle.push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;
import com.beetle.push.singleton.PushInterfaceProvider;

import java.util.ArrayList;

public class PushReceiver
    extends
    BroadcastReceiver implements PushServiceConstants {
    final static String TAG = "SmartPushReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (null == action) {
            Log.d(TAG, "action is null");
            return;
        }
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            PushReceiver.onNetworkChange();
            try {
                PushInterfaceProvider.getPushServiceInstance(context)
                    .onReceiverNetworkChange(context);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            startSmartPushService(context);
        } else if (action.equals(Intent.ACTION_USER_PRESENT)) {
            Log.d(TAG, "user present");
            startSmartPushService(context);
        } else if (action.equals(PushService.HEART_BEAT_ACTION)) {
            Log.d(TAG, "push alarm");
            Intent startIntent = new Intent(context, PushService.class);
            startIntent.putExtra(IntentKey.KEY_ACTION, PushAction.HEART_BEAT);
            context.startService(startIntent);
        } else {
            startSmartPushService(context);
        }
    }

    private void startSmartPushService(Context context) {
        Intent startIntent = new Intent(context, PushService.class);
        context.startService(startIntent);
    }

    public static interface NetworkStateObserver {
        public void onNetworkChange();
    }


    private static ArrayList<NetworkStateObserver> stateObservers =
        new ArrayList<NetworkStateObserver>();

    public static void addObserver(NetworkStateObserver ob) {
        stateObservers.add(ob);
    }

    public static void removeObserver(NetworkStateObserver ob) {
        stateObservers.remove(ob);
    }

    public static void onNetworkChange() {
        Log.d(TAG, "network changed");
        for (int i = 0; i < stateObservers.size(); i++) {
            NetworkStateObserver ob = stateObservers.get(i);
            ob.onNetworkChange();
        }
    }
}
