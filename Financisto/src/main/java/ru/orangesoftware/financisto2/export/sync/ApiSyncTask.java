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
import android.database.Cursor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import org.androidannotations.annotations.sharedpreferences.Pref;

import java.util.List;

import ru.orangesoftware.financisto2.R;
import ru.orangesoftware.financisto2.db.DatabaseAdapter;
import ru.orangesoftware.financisto2.db.DatabaseAdapter_;
import ru.orangesoftware.financisto2.db.DatabaseHelper;
import ru.orangesoftware.financisto2.model.Currency;
import ru.orangesoftware.financisto2.model.Payee;
//import ru.orangesoftware.financisto2.export.flowzr.FlowzrSyncEngine;
//import ru.orangesoftware.financisto2.export.flowzr.FlowzrSyncOptions;

public class ApiSyncTask extends AsyncTask<String, String, Object> {
    private static final String LAST_API_SYNC_TIMESTAMP = "LAST_API_SYNC_TIMESTAMP";
    private final DatabaseAdapter_ dba;
    long last_sync_ts=-1; //zero is default server ...
    long startTimestamp=-1; //useful only for not pushing what have just been pooled

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
        this.dba = DatabaseAdapter_.getInstance_(context);
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

        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        last_sync_ts = pref.getLong(LAST_API_SYNC_TIMESTAMP, 0);

        startTimestamp = System.currentTimeMillis();

        try {
/*
            if (last_sync_ts == 0){ //initial sync
                //download everything from server

            }else{ //sequential sync
*/

                //pull deleted objects after last_sync_ts
                //TODO

                //push deleted objects after last_sync_ts
//TODO

                //pull updated objects after last_sync_ts
                publishProgress("Downloading currencies...", "5");
                List<Currency> currencyList = syncService.listCurrencies(last_sync_ts);
                for (Currency c : currencyList){
                    db.saveOrUpdate(c);
                }
                publishProgress("Downloading currencies...", "95");

                publishProgress("Uploading currencies...", "5");
                List<Currency> currencies = db.getAllCurrenciesList();
                for (Currency currency : currencies){
                    if (currency.remoteKey == null) {
                        Currency updatedCurrency = syncService.createCurrency(currency);
                        db.saveOrUpdate(updatedCurrency);
                    } else
                        syncService.updateCurrency(currency.id, currency);
                }
                publishProgress("Uploading currencies...", "95");

                //push created objects after last_sync_ts

                //push updated objects after last_sync_ts
//            }

//            last_sync_ts = startTimestamp;

            SharedPreferences.Editor editor = pref.edit();
            editor.putLong(LAST_API_SYNC_TIMESTAMP, System.currentTimeMillis());
            editor.apply();

            return null;
        } catch (Exception e) {
            return e;
        }
    }

    public void execDelete(String tableName,String remoteKey) {
        long id = dba.getLocalKey(tableName, remoteKey);

        if (id > 0) {
            if (tableName.equals(DatabaseHelper.ACCOUNT_TABLE)) {
                dba.deleteAccount(id);
            } else if (tableName.equals(DatabaseHelper.TRANSACTION_TABLE)) {
                dba.deleteTransaction(id);
            } else if (tableName.equals(DatabaseHelper.CURRENCY_TABLE)) {
                dba.deleteCurrency(id);
            } else if (tableName.equals(DatabaseHelper.BUDGET_TABLE)) {
                dba.deleteBudget(id);
            } else if (tableName.equals(DatabaseHelper.LOCATIONS_TABLE)) {
                dba.deleteLocation(id);
            } else if (tableName.equals(DatabaseHelper.PROJECT_TABLE)) {
                dba.deleteProject(id);
            } else if (tableName.equals(DatabaseHelper.PAYEE_TABLE)) {
                dba.delete(Payee.class,id);
            } else  if (tableName.equals(DatabaseHelper.CATEGORY_TABLE)) {
                //dba.deleteCategory(id);
            }
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
        mProgress.setMessage(values[0]);
        mProgress.setProgress(Integer.parseInt(values[1]));
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

