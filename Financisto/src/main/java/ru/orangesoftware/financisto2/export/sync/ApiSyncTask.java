/*
 * Copyright (c) 2012 Emmanuel Florent.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto2.export.sync;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.List;

import ru.orangesoftware.financisto2.R;
import ru.orangesoftware.financisto2.activity.FlowzrSyncActivity;
import ru.orangesoftware.financisto2.db.DatabaseAdapter;
import ru.orangesoftware.financisto2.db.DatabaseAdapter_;
import ru.orangesoftware.financisto2.model.Currency;
//import ru.orangesoftware.financisto2.export.flowzr.FlowzrSyncEngine;
//import ru.orangesoftware.financisto2.export.flowzr.FlowzrSyncOptions;

public class ApiSyncTask extends AsyncTask<String, String, Object> {
	protected final Context context;
    private ApiSyncService syncService;
    //    private final FlowzrSyncOptions options;
//    private final DefaultHttpClient http_client;
//    private final FlowzrSyncActivity flowzrSyncActivity;
//    FlowzrSyncEngine flowzrSync;
    public static ProgressDialog mProgress;
	public static final String TAG = "apiSync";


    public ApiSyncTask(Activity context,
                       ApiSyncService syncService
//            FlowzrSyncActivity flowzrSyncActivity,
//                       FlowzrSyncEngine _flowzrSyncEngine,
//                       FlowzrSyncOptions options,
//                       DefaultHttpClient pHttp_client
    ) {
//        this.options = options;
//        this.http_client=pHttp_client;
        this.context=context;
        this.syncService = syncService;
//        this.flowzrSyncActivity=flowzrSyncActivity;
//        this.flowzrSync=_flowzrSyncEngine;

        mProgress = new ProgressDialog(this.context);
        mProgress.setIcon(R.drawable.icon);
        mProgress.setTitle(context.getString(R.string.sync));
        mProgress.setMessage(context.getString(R.string.flowzr_sync_inprogress));
        mProgress.setCancelable(true);
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        try {
            mProgress.show();
        } catch(Exception e) {
            Log.e(TAG,"avoid a leaked window");
        }
    }

    @Override
	protected Object doInBackground(String... params) {
    	DatabaseAdapter db = DatabaseAdapter_.getInstance_(context);

        try {
            List<Currency> currencies = db.getAllCurrenciesList();
            for (Currency currency : currencies){
                syncService.createCurrency(currency);
            }

            List<Currency> currencyList = syncService.listCurrencies();
//    		flowzrSyncActivity.notifyUser(flowzrSyncActivity.getString(R.string.flowzr_sync_auth_inprogress), 30);
//            if (this.checkSubscriptionFromWeb()) {
//    			flowzrSync.doSync();
//                return null;
//            } else {
//    			flowzrSyncActivity.notifyUser(flowzrSyncActivity.getString(R.string.flowzr_subscription_required), 100);
//    			flowzrSyncActivity.setRunning();
//                return new Exception(context.getString(R.string.flowzr_subscription_required));
//            }

            return null;
        } catch (Exception e) {
            return e;
        }
    }

    public boolean checkSubscriptionFromWeb() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
//    	String registrationId = prefs.getString(FlowzrSyncOptions.PROPERTY_REG_ID, "");
//    	if (registrationId=="") {
//    		Log.i(TAG, "Registration not found.");
//    	}

//    	String url=FlowzrSyncOptions.FLOWZR_API_URL + "?action=checkSubscription&regid=" + registrationId;

        try {
//    		flowzrSyncActivity.notifyUser(flowzrSyncActivity.getString(R.string.flowzr_sync_auth_inprogress), 40);
//    		HttpGet httpGet = new HttpGet(url);
//    		HttpResponse httpResponse = http_client.execute(httpGet);
//    		flowzrSyncActivity.notifyUser(flowzrSyncActivity.getString(R.string.flowzr_sync_inprogress), 50);
//    		int code = httpResponse.getStatusLine().getStatusCode();
//    		if (code==402) {
//    			return false;
//    		}
//    		httpResponse.getEntity().consumeContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        mProgress.setProgress(Integer.parseInt(values[0]));        
    }
    
	@Override
	protected void onPostExecute(Object result) {
//        flowzrSync.finishDelete();
//        flowzrSyncActivity.setReady();
        if (!(result instanceof Exception)) {
//            flowzrSyncActivity.nm.cancel(FlowzrSyncActivity.NOTIFICATION_ID);
            mProgress.hide();
        }
    }
}

