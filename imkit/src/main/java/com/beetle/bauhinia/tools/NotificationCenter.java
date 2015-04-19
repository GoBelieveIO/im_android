package com.beetle.bauhinia.tools;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by houxh on 14-12-5.
 */

public class NotificationCenter {
    public static interface NotificationCenterObserver {
        public void onNotification(Notification notification);
    }

    private static NotificationCenter instance = new NotificationCenter();
    public static NotificationCenter defaultCenter() {
        return instance;
    }

    private class Observer {
        public NotificationCenterObserver ob;
        public HashSet<String> names = new HashSet<String>();
    }


    private ArrayList<Observer> observers = new ArrayList<Observer>();

    public void postNotification(Notification notification) {
        for (Observer obs : observers) {
            if (obs.names.contains(notification.name)) {
                obs.ob.onNotification(notification);
            }
        }
    }

    public void addObserver(NotificationCenterObserver ob, String name) {
        for (Observer obs : observers) {
            if (obs.ob == ob) {
                obs.names.add(name);
                return;
            }
        }


        Observer obs = new Observer();
        obs.ob = ob;
        obs.names.add(name);
        observers.add(obs);
    }

    public void removeObserver(NotificationCenterObserver ob, String name) {
        Observer finded = null;
        for (Observer obs : observers) {
            if (obs.ob == ob) {
                finded = obs;
                break;
            }
        }

        if (finded == null) {
            return;
        }

        finded.names.remove(name);
        if (finded.names.size() == 0) {
            observers.remove(finded);
        }
    }

    public void removeObserver(NotificationCenterObserver ob) {
        Observer finded = null;
        for (Observer obs : observers) {
            if (obs.ob == ob) {
                finded = obs;
                break;
            }
        }

        if (finded == null) {
            return;
        }

        observers.remove(finded);
    }

}
