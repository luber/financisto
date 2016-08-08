package ru.orangesoftware.financisto.export.flowzr;


import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import ru.orangesoftware.financisto.export.flowzr.FlowzrSyncEngine;
import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
* IntentService responsible for handling GCM messages.
*/

	public class GCMIntentService extends IntentService {

		static String TAG="flowzr";
		
		public static final int NOTIFICATION_ID = 1;
	    NotificationCompat.Builder builder;

	    public GCMIntentService() {
	        super("GCMIntentService");
	    }

	    @Override
	    protected void onHandleIntent(Intent intent) {
	        Bundle extras = intent.getExtras();
	        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
	        String messageType = gcm.getMessageType(intent);
	        if (!extras.isEmpty()) {
	        	if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {	    		
	        		Log.i(TAG,"starting sync from GCM");
	    			FlowzrSyncEngine.builAndRun(getApplicationContext());
	            }
	        }
	    }
	}
	
	
