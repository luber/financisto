///*
// * Copyright (c) 2012 Emmanuel Florent.
// * All rights reserved. This program and the accompanying materials
// * are made available under the terms of the GNU Public License v2.0
// * which accompanies this distribution, and is available at
// * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
// */
//
//package ru.orangesoftware.financisto.export.flowzr;
//
//import android.app.ProgressDialog;
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.os.AsyncTask;
//import android.preference.PreferenceManager;
//import android.util.Log;
//
//import ru.orangesoftware.financisto.R;
//import ru.orangesoftware.financisto.activity.FlowzrSyncActivity;
//import ru.orangesoftware.financisto.db.DatabaseAdapter;
//
//public class FlowzrSyncTask extends AsyncTask<String, String, Object> {
//	protected final Context context;
//    private final FlowzrSyncOptions options;
////    private final DefaultHttpClient http_client;
//    private final FlowzrSyncActivity flowzrSyncActivity;
//    FlowzrSyncEngine flowzrSync;
//    public static ProgressDialog mProgress;
//	public static final String TAG = "flowzr";
//
//
//    public FlowzrSyncTask(FlowzrSyncActivity flowzrSyncActivity, FlowzrSyncEngine _flowzrSyncEngine, FlowzrSyncOptions options
////			, DefaultHttpClient pHttp_client
//	) {
//        this.options = options;
////        this.http_client=pHttp_client;
//        this.context=flowzrSyncActivity;
//        this.flowzrSyncActivity=flowzrSyncActivity;
//        this.flowzrSync=_flowzrSyncEngine;
//        mProgress = new ProgressDialog(this.flowzrSyncActivity);
//        mProgress.setIcon(R.drawable.icon);
//        mProgress.setTitle(flowzrSyncActivity.getString(R.string.flowzr_sync));
//        mProgress.setMessage(flowzrSyncActivity.getString(R.string.flowzr_sync_inprogress));
//        mProgress.setCancelable(true);
//        mProgress.setCanceledOnTouchOutside(false);
//        mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        try {
//        mProgress.show();
//        } catch(Exception e) {
//        	Log.e(TAG,"avoid a leaked window");
//        }
//
//    }
//
//
//
//    protected Object work(Context context, DatabaseAdapter db, String... params) {
//
//    	try {
//    		flowzrSyncActivity.notifyUser(flowzrSyncActivity.getString(R.string.flowzr_sync_auth_inprogress), 30);
//    		//FlowzrBilling flowzrBilling = new FlowzrBilling(flowzrSyncActivity, flowzrSyncActivity.getApplicationContext(), http_client, options.useCredential);
//    		//if (flowzrBilling.checkSubscription()) {
//    		//        	Boolean sync=false;
//    		//            if (flowzrBilling!=null) {
//    		//            	sync=flowzrBilling.checkSubscription();
//    		//            } else {
//    		//            	sync=false;
//    		//            	return new Exception(context.getString(R.string.flowzr_account_setup));
//    		//            }
//    		if (this.checkSubscriptionFromWeb()) {
//    			flowzrSync.doSync();
//    			return null;
//    		} else {
//    			flowzrSyncActivity.notifyUser(flowzrSyncActivity.getString(R.string.flowzr_subscription_required), 100);
//    			flowzrSyncActivity.setRunning();
//    			return new Exception(context.getString(R.string.flowzr_subscription_required));
//    		}
//    	} catch (Exception e) {
//    		return e;
//    	}
//    }
//
//    public boolean checkSubscriptionFromWeb() {
//    	final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
//    	String registrationId = prefs.getString(FlowzrSyncOptions.PROPERTY_REG_ID, "");
//    	if (registrationId=="") {
//    		Log.i(TAG, "Registration not found.");
//    	}
//
//    	String url=FlowzrSyncOptions.FLOWZR_API_URL + "?action=checkSubscription&regid=" + registrationId;
//
//    	try {
//    		flowzrSyncActivity.notifyUser(flowzrSyncActivity.getString(R.string.flowzr_sync_auth_inprogress), 40);
////    		HttpGet httpGet = new HttpGet(url);
////    		HttpResponse httpResponse = http_client.execute(httpGet);
//    		flowzrSyncActivity.notifyUser(flowzrSyncActivity.getString(R.string.flowzr_sync_inprogress), 50);
////    		int code = httpResponse.getStatusLine().getStatusCode();
////    		if (code==402) {
//    			return false;
////    		}
////    		httpResponse.getEntity().consumeContent();
//    	} catch (Exception e) {
//    		e.printStackTrace();
//    	}
//    	return true;
//    }
//
//    @Override
//	protected Object doInBackground(String... params) {
//
//    	DatabaseAdapter db = new DatabaseAdapter(context);
//		db.open();
//		try {
//			return work(context, db, params);
//		} finally {
//			db.close();
//		}
//
//	}
//
//    @Override
//    protected void onProgressUpdate(String... values) {
//        super.onProgressUpdate(values);
//        mProgress.setProgress(Integer.parseInt(values[0]));
//    }
//
//
//
//	@Override
//	protected void onPostExecute(Object result) {
//			flowzrSync.finishDelete();
//			flowzrSyncActivity.setReady();
//			flowzrSyncActivity.nm.cancel(FlowzrSyncActivity.NOTIFICATION_ID);
// 			mProgress.hide();
//	}
//}
//
