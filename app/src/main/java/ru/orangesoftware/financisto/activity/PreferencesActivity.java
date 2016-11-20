/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p/>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.dialog.FolderBrowser;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.rates.ExchangeRateProviderFactory;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;

import static ru.orangesoftware.financisto.R.string.app_name;

public class PreferencesActivity extends PreferenceActivity implements EasyPermissions.PermissionCallbacks {

    private static final String[] ACCOUNT_TYPE = new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE};

    private static final int SELECT_DATABASE_FOLDER = 100;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String[] SCOPES = { DriveScopes.DRIVE_METADATA_READONLY };

    Preference pOpenExchangeRatesAppId;
    GoogleAccountCredential mCredential;

    protected DatabaseAdapter db;
    protected MyEntityManager em;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        db = new DatabaseAdapter(this);
        db.open();
        em = db.em();

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        setSavedGoogleAccount();

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Preference pLocale = preferenceScreen.findPreference("ui_language");
        pLocale.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String locale = (String) newValue;
                MyPreferences.switchLocale(PreferencesActivity.this, locale);
                return true;
            }
        });
        Preference pNewTransactionShortcut = preferenceScreen.findPreference("shortcut_new_transaction");
        pNewTransactionShortcut.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                addShortcut(".activity.TransactionActivity", R.string.transaction, R.drawable.icon_transaction);
                return true;
            }

        });
        Preference pNewTransferShortcut = preferenceScreen.findPreference("shortcut_new_transfer");
        pNewTransferShortcut.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                addShortcut(".activity.TransferActivity", R.string.transfer, R.drawable.icon_transfer);
                return true;
            }
        });
        Preference pDatabaseBackupFolder = preferenceScreen.findPreference("database_backup_folder");
        pDatabaseBackupFolder.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                selectDatabaseBackupFolder();
                return true;
            }
        });
//        Preference pAuthDropbox = preferenceScreen.findPreference("dropbox_authorize");
//        pAuthDropbox.setOnPreferenceClickListener(new OnPreferenceClickListener(){
//            @Override
//            public boolean onPreferenceClick(Preference arg0) {
//                authDropbox();
//                return true;
//            }
//        });
//        Preference pDeauthDropbox = preferenceScreen.findPreference("dropbox_unlink");
//        pDeauthDropbox.setOnPreferenceClickListener(new OnPreferenceClickListener() {
//            @Override
//            public boolean onPreferenceClick(Preference arg0) {
//                deAuthDropbox();
//                return true;
//            }
//        });
        Preference pExchangeProvider = preferenceScreen.findPreference("exchange_rate_provider");
        pOpenExchangeRatesAppId = preferenceScreen.findPreference("openexchangerates_app_id");
        pExchangeProvider.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                pOpenExchangeRatesAppId.setEnabled(isOpenExchangeRatesProvider((String) newValue));
                return true;
            }

            private boolean isOpenExchangeRatesProvider(String provider) {
                return ExchangeRateProviderFactory.openexchangerates.name().equals(provider);
            }
        });

        Preference pDriveAccount = preferenceScreen.findPreference("google_drive_backup_account");
        pDriveAccount.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                chooseAccount();
                return true;
            }
        });

        Preference pDriveBackupFolder = preferenceScreen.findPreference("backup_folder");
        pDriveBackupFolder.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                chooseDriveFolder();
                return true;
            }
        });

        ArrayList<String> accountNames = new ArrayList<String>();
        ArrayList<String> accountValues = new ArrayList<String>();

        for (ru.orangesoftware.financisto.model.Account account : db.em().getAllAccountsList()) {
            accountNames.add(account.title);
            accountValues.add(String.valueOf(account.id));
        }

        String[] accountEntryNames = accountNames.toArray(new String[accountNames.size()]);
        String[] accountEntryValues = accountValues.toArray(new String[accountValues.size()]);

        ListPreference pPrivatCardAccount = (ListPreference) preferenceScreen.findPreference("main_privatbank_card_account");
        pPrivatCardAccount.setEntries(accountEntryNames);
        pPrivatCardAccount.setEntryValues(accountEntryValues);
//        pPrivatCardAccount.setDefaultValue("");

        ListPreference pCashAccount = (ListPreference) preferenceScreen.findPreference("cache_account");
        pCashAccount.setEntries(accountEntryNames);
        pCashAccount.setEntryValues(accountEntryValues);
//        pCashAccount.setDefaultValue("");

        ListPreference pSavingsAccount = (ListPreference) preferenceScreen.findPreference("savings_account");
        pSavingsAccount.setEntries(accountEntryNames);
        pSavingsAccount.setEntryValues(accountEntryValues);
//        pCashAccount.setDefaultValue("");


        ArrayList<String> categoryNames = new ArrayList<String>();
        ArrayList<String> categoryValues = new ArrayList<String>();

        for (Category category : db.em().getAllCategoriesList(false)) {
            categoryNames.add(category.title);
            categoryValues.add(String.valueOf(category.id));
        }

        String[] categoryEntryNames = categoryNames.toArray(new String[categoryNames.size()]);
        String[] categoryEntryValues = categoryValues.toArray(new String[categoryValues.size()]);

        ListPreference pAtmCommisionCategory = (ListPreference) preferenceScreen.findPreference("atm_commision_category");
        pAtmCommisionCategory.setEntries(categoryEntryNames);
        pAtmCommisionCategory.setEntryValues(categoryEntryValues);
//        pCashAccount.setDefaultValue("");

        ListPreference pBankCommisionCategory = (ListPreference) preferenceScreen.findPreference("bank_commision_category");
        pBankCommisionCategory.setEntries(categoryEntryNames);
        pBankCommisionCategory.setEntryValues(categoryEntryValues);
//        pCashAccount.setDefaultValue("");

//        linkToDropbox();
        setCurrentDatabaseBackupFolder();
        enableOpenExchangeApp();
        updateSelectedAccountPreference();
    }

    private void chooseDriveFolder() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            Toast.makeText(this, "No network connection available.", Toast.LENGTH_LONG);
        } else {
            new MakeRequestTask(mCredential).execute();
        }
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        try {
            if (EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)) {
                setSavedGoogleAccount();

                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);

            } else {
                // Request the GET_ACCOUNTS permission via a user dialog
                EasyPermissions.requestPermissions(
                        this,
                        "This app needs to access your Google account (via Contacts).",
                        REQUEST_PERMISSION_GET_ACCOUNTS,
                        Manifest.permission.GET_ACCOUNTS);
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.google_drive_account_select_error, Toast.LENGTH_LONG).show();
        }
    }

    private void setSavedGoogleAccount() {
        String accountName = MyPreferences.getGoogleDriveAccount(this);
        if (accountName != null) {
            mCredential.setSelectedAccountName(accountName);
        }
    }

    @AfterPermissionGranted(SELECT_DATABASE_FOLDER)
    private void selectDatabaseBackupFolder() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
//            setSavedGoogleAccount();

            // Start a dialog from which the user can choose a Folder
            Intent intent = new Intent(this, FolderBrowser.class);
            intent.putExtra(FolderBrowser.PATH, getDatabaseBackupFolder());
            startActivityForResult(intent, SELECT_DATABASE_FOLDER);
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your external storage.",
                    SELECT_DATABASE_FOLDER,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void enableOpenExchangeApp() {
        pOpenExchangeRatesAppId.setEnabled(MyPreferences.isOpenExchangeRatesProviderSelected(this));
    }

    private String getDatabaseBackupFolder() {
        return Export.getBackupFolder(this).getAbsolutePath();
    }

    private void setCurrentDatabaseBackupFolder() {
        Preference pDatabaseBackupFolder = getPreferenceScreen().findPreference("database_backup_folder");
        String summary = getString(R.string.database_backup_folder_summary, getDatabaseBackupFolder());
        pDatabaseBackupFolder.setSummary(summary);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    Toast.makeText(this,
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.",
                            Toast.LENGTH_LONG);
                } else {
//                    getResultsFromApi();
                }
                break;
            case SELECT_DATABASE_FOLDER:
                if (resultCode == RESULT_OK) {
                    String databaseBackupFolder = data.getStringExtra(FolderBrowser.PATH);
                    MyPreferences.setDatabaseBackupFolder(this, databaseBackupFolder);
                    setCurrentDatabaseBackupFolder();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (data != null && resultCode == RESULT_OK) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    Log.d("Preferences", "Selected account: " + accountName);
                    if (accountName != null && accountName.length() > 0) {
                        mCredential.setSelectedAccountName(accountName);
                        MyPreferences.setGoogleDriveAccount(this, accountName);
                        updateSelectedAccountPreference();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
//                    getResultsFromApi();
                }
                break;
        }
    }

    private void updateSelectedAccountPreference() {
        Preference pDriveAccount = getPreferenceScreen().findPreference("google_drive_backup_account");
        Account account = mCredential.getSelectedAccount();
        if (account != null) {
            pDriveAccount.setSummary(account.name);
        }
    }

    private void addShortcut(String activity, int nameId, int iconId) {
        Intent intent = createShortcutIntent(activity, getString(nameId), Intent.ShortcutIconResource.fromContext(this, iconId),
                "com.android.launcher.action.INSTALL_SHORTCUT");
        sendBroadcast(intent);
    }

    private Intent createShortcutIntent(String activity, String shortcutName, ShortcutIconResource shortcutIcon, String action) {
        Intent shortcutIntent = new Intent();
        shortcutIntent.setComponent(new ComponentName(this.getPackageName(), activity));
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortcutIcon);
        intent.setAction(action);
        return intent;
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                PreferencesActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

//    Dropbox dropbox = new Dropbox(this);

//    private void authDropbox() {
//        dropbox.startAuth();
//    }
//
//    private void deAuthDropbox() {
//        dropbox.deAuth();
//        linkToDropbox();
//    }

    @Override
    protected void onPause() {
        super.onPause();
        PinProtection.lock(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PinProtection.unlock(this);
//        dropbox.completeAuth();
//        linkToDropbox();
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }

    /**
     * An asynchronous task that handles the Drive API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.drive.Drive mService = null;
        private Exception mLastError = null;

        public MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(getString(R.string.app_name))
                    .build();
        }

        /**
         * Background task to call Drive API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of up to 10 file names and IDs.
         * @return List of Strings describing files, or an empty list if no files
         *         found.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            // Get a list of up to 10 files.
            List<String> fileInfo = new ArrayList<>();
//            FileList result = mService.files().list()
//                    .setPageSize(10)
//                    .setFields("nextPageToken, files(id, name)")
//                    .execute();
            FileList result = mService.files().list()
                    .setQ("mimeType='application/vnd.google-apps.folder'")
                    .execute();
            List<File> files = result.getFiles();
            if (files != null) {
                for (File file : files) {
                    fileInfo.add(String.format("%s (%s)\n",
                            file.getName(), file.getId()));
                }
            }
            return fileInfo;
        }


        @Override
        protected void onPreExecute() {
//            mOutputText.setText("");
//            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
//            mProgress.hide();
            if (output == null || output.size() == 0) {
//                mOutputText.setText("No results returned.");
            } else {
                output.add(0, "Data retrieved using the Drive API:");
//                mOutputText.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
//            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            PreferencesActivity.REQUEST_AUTHORIZATION);
                } else {
//                    mOutputText.setText("The following error occurred:\n"
//                            + mLastError.getMessage());
                }
            } else {
//                mOutputText.setText("Request cancelled.");
            }
        }
    }
}
