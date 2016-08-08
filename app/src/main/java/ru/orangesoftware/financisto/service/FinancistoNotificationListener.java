package ru.orangesoftware.financisto.service;

import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * Created by luberello on 28.12.15.
 */
public class FinancistoNotificationListener extends NotificationListenerService {

    private String TAG = this.getClass().getSimpleName();
    private PrivatbankReceiver mReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        mReceiver = new PrivatbankReceiver();

        IntentFilter filter = new IntentFilter();
//        filter.add
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        Log.i(TAG, "**********  onNotificationPosted");
        Log.i(TAG,"ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText + "\t" + sbn.getPackageName());
//        Intent i = new  Intent("com.kpbird.nlsexample.NOTIFICATION_LISTENER_EXAMPLE");
//        i.putExtra("notification_event","onNotificationPosted :" + sbn.getPackageName() + "\n");
//        sendBroadcast(i);
        Intent intent = new Intent();
        intent.putExtra("messageBody", sbn.getNotification().tickerText);
        sendBroadcast(intent);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG,"********** onNOtificationRemoved");
        Log.i(TAG,"ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText +"\t" + sbn.getPackageName());
//        Intent i = new  Intent("com.kpbird.nlsexample.NOTIFICATION_LISTENER_EXAMPLE");
//        i.putExtra("notification_event", "onNotificationRemoved :" + sbn.getPackageName() + "\n");

        Intent intent = new Intent();
        sendBroadcast(intent);
    }
}
