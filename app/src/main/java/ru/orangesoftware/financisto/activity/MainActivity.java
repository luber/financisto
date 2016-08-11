/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.backup.Backup;
import ru.orangesoftware.financisto.blotter.TotalCalculationTask;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.dialog.WebViewDialog;
import ru.orangesoftware.financisto.export.BackupExportTask;
import ru.orangesoftware.financisto.export.BackupImportTask;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.export.ImportExportAsyncTaskListener;
import ru.orangesoftware.financisto.export.csv.CsvExportOptions;
import ru.orangesoftware.financisto.export.csv.CsvExportTask;
import ru.orangesoftware.financisto.export.csv.CsvImportOptions;
import ru.orangesoftware.financisto.export.csv.CsvImportTask;
import ru.orangesoftware.financisto.export.docs.DriveBackupTask;
import ru.orangesoftware.financisto.export.docs.DriveListFilesTask;
import ru.orangesoftware.financisto.export.docs.DriveRestoreTask;
import ru.orangesoftware.financisto.export.qif.QifExportOptions;
import ru.orangesoftware.financisto.export.qif.QifExportTask;
import ru.orangesoftware.financisto.export.qif.QifImportOptions;
import ru.orangesoftware.financisto.export.qif.QifImportTask;
import ru.orangesoftware.financisto.fragments.AccountListFragment;
import ru.orangesoftware.financisto.fragments.BlotterFragment;
import ru.orangesoftware.financisto.fragments.BudgetListFragment;
import ru.orangesoftware.financisto.fragments.OnAddButtonListener;
import ru.orangesoftware.financisto.fragments.ReportListFragment;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.sync.OnlineSyncTask;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.EntityEnum;
import ru.orangesoftware.financisto.utils.EnumUtils;
import ru.orangesoftware.financisto.utils.ExecutableEntityEnum;
import ru.orangesoftware.financisto.utils.IntegrityFix;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;

import static ru.orangesoftware.financisto.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;
import static ru.orangesoftware.financisto.service.FlowzrAutoSyncScheduler.scheduleNextAutoSync;
import static ru.orangesoftware.financisto.utils.EnumUtils.showPickOneDialog;

public class MainActivity extends AppCompatActivity implements
        AccountListFragment.OnFragmentInteractionListener,
        BlotterFragment.OnFragmentInteractionListener,
        NavigationView.OnNavigationItemSelectedListener {

    private static final String NAV_ITEM_ID = "navItemId";
    protected DatabaseAdapter db;
    protected MyEntityManager em;

    private static final int ACTIVITY_CSV_EXPORT = 10002;
    private static final int ACTIVITY_QIF_EXPORT = 10003;
    private static final int ACTIVITY_CSV_IMPORT = 10004;
    private static final int ACTIVITY_QIF_IMPORT = 10005;
    private static final int CHANGE_PREFERENCES_RESULT = 10006;
//    private static final int ACTIVITY_FLOWZR_SYNC = 10007;

//    public static String STATE_TAB_POSITION = "POSITION";

//    private ArrayList<TabInfo> tabs = new ArrayList<>();

    Toolbar toolbar;
    //    TabLayout tabLayout;
//    ViewPager viewPager;
    FloatingActionButton fab;

    DrawerLayout mDrawerLayout;
    ActionBarDrawerToggle mDrawerToggle;
    NavigationView mNavigationView;
//    CharSequence mDrawerTitle;
//    CharSequence mTitle;
//    FrameLayout mMainFrameContent;

    TextView totalText;

    //    AccountTotalsCalculationTask totalCalculationTask;
    AccountTotalsCalculationTask totalCalculationTask;

    int navMenuItemId;
    long selectedAccountId = -1;

//    AccountListFragment accountListFragment;
//    BlotterFragment blotterFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // enable transitions
//        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        db = new DatabaseAdapter(this);
        db.open();

        em = db.em();

        setupFab();
        setupToolbar();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                toolbar,
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                calculateTotals();
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        //Initializing NavigationView
        mNavigationView = (NavigationView) findViewById(R.id.navigation);
        mNavigationView.setNavigationItemSelectedListener(this);

//        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout){
//            @Override
//            public void onPageSelected(int position) {
//                super.onPageSelected(position);
//
//                setupFabSettingCurrentTab();
//                refreshCurrentTab();
//            }
//        });

        initialLoad();

        // load saved navigation state if present
        if (null == savedInstanceState) {
            MyPreferences.StartupScreen screen = MyPreferences.getStartupScreen(this);
            navMenuItemId = getMenuIdentifier(screen.tag);
        } else {
            navMenuItemId = savedInstanceState.getInt(NAV_ITEM_ID);
        }

        if (navMenuItemId > 0)
            mNavigationView.getMenu().performIdentifierAction(navMenuItemId, 0);


//        viewPager.setCurrentItem(startTabPosition);
//        updateToolbar();

        View header = mNavigationView.getHeaderView(0);
        totalText = (TextView) header.findViewById(R.id.total);
        if (totalText != null) {
            totalText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showTotals();
                }
            });
        }

//        setupFabSettingCurrentTab();
    }

    private int getMenuIdentifier(String tag) {

        if (MyPreferences.StartupScreen.ACCOUNTS.tag.equals(tag)) {
            return R.id.accounts;
        } else if (MyPreferences.StartupScreen.BLOTTER.tag.equals(tag)) {
            return R.id.blotter;
        } else if (MyPreferences.StartupScreen.BUDGETS.tag.equals(tag)) {
            return R.id.budgets;
        } else if (MyPreferences.StartupScreen.REPORTS.tag.equals(tag)) {
            return R.id.reports;
        }

        return 0;
    }

    private void setupFab() {
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_content_frame);
                if (f instanceof OnAddButtonListener) {
                    ((OnAddButtonListener) f).addItem();
                }
            }
        });
    }

    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Show menu icon
        final ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeButtonEnabled(true);
        ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void showTotals() {
        Intent intent = new Intent(this, AccountListTotalsDetailsActivity.class);
        startActivityForResult(intent, -1);
    }

    private void calculateTotals() {
        if (totalCalculationTask != null) {
            totalCalculationTask.stop();
            totalCalculationTask.cancel(true);
        }
        totalCalculationTask = new AccountTotalsCalculationTask(this, totalText);
        totalCalculationTask.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PinProtection.unlock(this);
        if (PinProtection.isUnlocked()) {
            WebViewDialog.checkVersionAndShowWhatsNewIfNeeded(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PinProtection.lock(this);
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
        PinProtection.immediateLock(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(NAV_ITEM_ID, navMenuItemId);
//        outState.putInt(STATE_TAB_POSITION, tabLayout.getSelectedTabPosition());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
//        viewPager.setCurrentItem(savedInstanceState.getInt(STATE_TAB_POSITION));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTIVITY_CSV_EXPORT) {
            if (resultCode == RESULT_OK) {
                CsvExportOptions options = CsvExportOptions.fromIntent(data);
                doCsvExport(options);
            }
        } else if (requestCode == ACTIVITY_QIF_EXPORT) {
            if (resultCode == RESULT_OK) {
                QifExportOptions options = QifExportOptions.fromIntent(data);
                doQifExport(options);
            }
        } else if (requestCode == ACTIVITY_CSV_IMPORT) {
            if (resultCode == RESULT_OK) {
                CsvImportOptions options = CsvImportOptions.fromIntent(data);
                doCsvImport(options);
            }
        } else if (requestCode == ACTIVITY_QIF_IMPORT) {
            if (resultCode == RESULT_OK) {
                QifImportOptions options = QifImportOptions.fromIntent(data);
                doQifImport(options);
            }
        } else if (requestCode == CHANGE_PREFERENCES_RESULT) {
            scheduleNextAutoBackup(this);
            scheduleNextAutoSync(this);
        }
    }

    private void initialLoad() {
        long t3, t2, t1, t0 = System.currentTimeMillis();
        DatabaseAdapter db = new DatabaseAdapter(this);
        db.open();
        try {
            SQLiteDatabase x = db.db();
            x.beginTransaction();
            t1 = System.currentTimeMillis();
            try {
                updateFieldInTable(x, DatabaseHelper.CATEGORY_TABLE, 0, "title", getString(R.string.no_category));
                updateFieldInTable(x, DatabaseHelper.CATEGORY_TABLE, -1, "title", getString(R.string.split));
                updateFieldInTable(x, DatabaseHelper.PROJECT_TABLE, 0, "title", getString(R.string.no_project));
                updateFieldInTable(x, DatabaseHelper.LOCATIONS_TABLE, 0, "name", getString(R.string.current_location));
                x.setTransactionSuccessful();
            } finally {
                x.endTransaction();
            }
            t2 = System.currentTimeMillis();
            if (MyPreferences.shouldUpdateHomeCurrency(this)) {
                db.setDefaultHomeCurrency();
            }
            CurrencyCache.initialize(db.em());
            t3 = System.currentTimeMillis();
            if (MyPreferences.shouldRebuildRunningBalance(this)) {
                db.rebuildRunningBalances();
            }
            if (MyPreferences.shouldUpdateAccountsLastTransactionDate(this)) {
                db.updateAccountsLastTransactionDate();
            }
        } finally {
            db.close();
        }
        long t4 = System.currentTimeMillis();
        Log.d("Financisto", "Load time = " + (t4 - t0) + "ms = " + (t2 - t1) + "ms+" + (t3 - t2) + "ms+" + (t4 - t3) + "ms");
    }

    private void updateFieldInTable(SQLiteDatabase db, String table, long id, String field, String value) {
        db.execSQL("update " + table + " set " + field + "=? where _id=?", new Object[]{value, id});
    }

    @Override
    public void onShowAccountTransactions(long accountId) {
        Account account = em.getAccount(accountId);
        if (account != null) {

            selectedAccountId = accountId;

            mNavigationView.getMenu().performIdentifierAction(R.id.blotter, 0);
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        //Checking if the item is in checked state or not, if not make it in checked state
        if (menuItem.isCheckable()) {
            menuItem.setChecked(true);
        }

        //Closing drawer on item click
        mDrawerLayout.closeDrawers();

        if (menuItem.isChecked()) {
            ActionBar actionBar = getSupportActionBar();
            actionBar.setTitle(menuItem.getTitle());
            actionBar.setIcon(menuItem.getIcon());
        }

        navMenuItemId = menuItem.getItemId();
        //Check to see which item was being clicked and perform appropriate action
        switch (navMenuItemId) {

            //Replacing the main content with ContentFragment Which is our Inbox View;
            case R.id.accounts:
                selectedAccountId = -1;
                android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.main_content_frame, AccountListFragment.newInstance());
//                fragmentTransaction.addToBackStack(null); //TODO: start screen should not be added to the backstack
                fragmentTransaction.commit();

                fab.show();
                break;
            case R.id.blotter:
                android.support.v4.app.FragmentTransaction blotterFragmentTransaction = getSupportFragmentManager().beginTransaction();
                blotterFragmentTransaction.replace(R.id.main_content_frame, BlotterFragment.newInstance(true, selectedAccountId));
                blotterFragmentTransaction.addToBackStack(null);
                blotterFragmentTransaction.commit();

                fab.show();
                break;
            case R.id.scheduled_transactions:
                startActivity(new Intent(this, ScheduledListActivity.class));
                break;
            case R.id.budgets:
                android.support.v4.app.FragmentTransaction budgetFragmentTransaction = getSupportFragmentManager().beginTransaction();
                budgetFragmentTransaction.replace(R.id.main_content_frame, new BudgetListFragment());
                budgetFragmentTransaction.addToBackStack(null);
                budgetFragmentTransaction.commit();

                fab.show();
                break;
            case R.id.reports:
                android.support.v4.app.FragmentTransaction reportsFragmentTransaction = getSupportFragmentManager().beginTransaction();
                reportsFragmentTransaction.replace(R.id.main_content_frame, new ReportListFragment());
                reportsFragmentTransaction.addToBackStack(null);
                reportsFragmentTransaction.commit();

                fab.hide();
                break;
            case R.id.planner:
                startActivity(new Intent(this, PlannerActivity.class));
                break;
            case R.id.entities:
                final MenuEntities[] entities = MenuEntities.values();
                ListAdapter adapter = EnumUtils.createEntityEnumAdapter(this, entities);
                final AlertDialog d = new AlertDialog.Builder(this)
                        .setAdapter(adapter, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                MenuEntities e = entities[which];
                                startActivity(new Intent(MainActivity.this, e.getActivityClass()));
                            }
                        })
                        .create();
                d.setTitle(R.string.entities);
                d.show();
                break;
            case R.id.menu_sync_online:
                doOnlineSync();
                break;
//            case R.id.menu_sync_flowzr:
//                doFlowzrSync();
//                break;
            case R.id.menu_mass_operations:
                startActivity(new Intent(this, MassOpActivity.class));
                break;
            case R.id.menu_restore_database:
                doImport();
                break;
            case R.id.menu_backup_database:
                doBackup();
                break;
            case R.id.menu_backup_database_to:
                doBackupTo();
                break;
            case R.id.menu_backup_restore_database_online:
                showPickOneDialog(this, R.string.backup_restore_database_online, BackupRestoreEntities.values(), this);
                break;
            case R.id.menu_import_export:
                showPickOneDialog(this, R.string.import_export, ImportExportEntities.values(), this);
                break;
            case R.id.menu_settings:
                startActivityForResult(new Intent(this, PreferencesActivity.class), CHANGE_PREFERENCES_RESULT);
                break;
            case R.id.menu_integrity_fix:
                doIntegrityFix();
                break;
            case R.id.menu_donate:
                openBrowser("market://search?q=pname:ru.orangesoftware.financisto.support");
                break;
            case R.id.menu_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            default:
                Toast.makeText(getApplicationContext(), "Somethings Wrong", Toast.LENGTH_SHORT).show();
                fab.hide();
                break;
        }

        return true;
    }

    private void doOnlineSync() {
        ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.sync_online_inprogress), true);
        new OnlineSyncTask(this, handler, d).execute((String[]) null);
    }

//    private void doFlowzrSync() {
//        Intent intent = new Intent(this, FlowzrSyncActivity.class);
//        startActivityForResult(intent, ACTIVITY_FLOWZR_SYNC);
//    }

    private void doIntegrityFix() {
        new IntegrityFixTask().execute();
    }

    private void openBrowser(String url) {
        try {
            Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception ex) {
            //eventually market is not available
            Toast.makeText(this, R.string.donate_error, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Treat asynchronous requests to popup error messages
     */
    private Handler handler = new Handler() {
        /**
         * Schedule the popup of the given error message
         * @param msg The message to display
         **/
        @Override
        public void handleMessage(Message msg) {
            showErrorPopup(MainActivity.this, msg.what);
        }
    };

    public void showErrorPopup(Context context, int message) {
        new AlertDialog.Builder(context)
                .setMessage(message)
                .setTitle(R.string.error)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(true)
                .create().show();
    }

    private void doBackup() {
        ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.backup_database_inprogress), true);
        new BackupExportTask(this, d, true).execute();
    }

    private void doBackupTo() {
        ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.backup_database_inprogress), true);
        final BackupExportTask t = new BackupExportTask(this, d, false);
        t.setShowResultDialog(false);
        t.setListener(new ImportExportAsyncTaskListener() {
            @Override
            public void onCompleted() {
                String backupFileName = t.backupFileName;
                startBackupToChooser(backupFileName);
            }
        });
        t.execute((String[]) null);
    }

    private void startBackupToChooser(String backupFileName) {
        File file = Export.getBackupFile(this, backupFileName);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        intent.setType("text/plain");
        startActivity(Intent.createChooser(intent, getString(R.string.backup_database_to_title)));
    }

    private void doCsvExport(CsvExportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.csv_export_inprogress), true);
        new CsvExportTask(this, progressDialog, options).execute();
    }

    private void doCsvImport(CsvImportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.csv_import_inprogress), true);
        new CsvImportTask(this, handler, progressDialog, options).execute();
    }

    private void doQifExport(QifExportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.qif_export_inprogress), true);
        new QifExportTask(this, progressDialog, options).execute();
    }

    private void doQifImport(QifImportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.qif_import_inprogress), true);
        new QifImportTask(this, handler, progressDialog, options).execute();
    }

    private void doCsvExport() {
        Intent intent = new Intent(this, CsvExportActivity.class);
        startActivityForResult(intent, ACTIVITY_CSV_EXPORT);
    }

    private void doCsvImport() {
        Intent intent = new Intent(this, CsvImportActivity.class);
        startActivityForResult(intent, ACTIVITY_CSV_IMPORT);
    }

    private void doQifExport() {
        Intent intent = new Intent(this, QifExportActivity.class);
        startActivityForResult(intent, ACTIVITY_QIF_EXPORT);
    }

    private void doQifImport() {
        Intent intent = new Intent(this, QifImportActivity.class);
        startActivityForResult(intent, ACTIVITY_QIF_IMPORT);
    }

    private String selectedBackupFile;
    private com.google.api.services.drive.model.File selectedDriveFile;

    private void doImport() {
        final String[] backupFiles = Backup.listBackups(this);
        new AlertDialog.Builder(this)
                .setTitle(R.string.restore_database)
                .setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (selectedBackupFile != null) {
                            ProgressDialog d = ProgressDialog.show(MainActivity.this, null, getString(R.string.restore_database_inprogress), true);
                            new BackupImportTask(MainActivity.this, d).execute(selectedBackupFile);
                        }
                    }
                })
                .setSingleChoiceItems(backupFiles, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (backupFiles != null && which >= 0 && which < backupFiles.length) {
                            selectedBackupFile = backupFiles[which];
                        }
                    }
                })
                .show();
    }

    private void doBackupOnGoogleDrive() {
        ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.backup_database_gdocs_inprogress), true);
        new DriveBackupTask(this, d).execute();
    }

    private void doRestoreFromGoogleDrive() {
        ProgressDialog d = ProgressDialog.show(MainActivity.this, null, getString(R.string.google_drive_loading_files), true);
        new DriveListFilesTask(this, d).execute();
    }

    public void doImportFromGoogleDrive(final com.google.api.services.drive.model.File[] backupFiles) {
        if (backupFiles != null) {
            String[] backupFilesNames = getBackupFilesTitles(backupFiles);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.restore_database)
                    .setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (selectedDriveFile != null) {
                                ProgressDialog d = ProgressDialog.show(MainActivity.this, null, getString(R.string.restore_database_inprogress_gdocs), true);
                                new DriveRestoreTask(MainActivity.this, d, selectedDriveFile).execute();
                            }
                        }
                    })
                    .setSingleChoiceItems(backupFilesNames, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which >= 0 && which < backupFiles.length) {
                                selectedDriveFile = backupFiles[which];
                            }
                        }
                    })
                    .show();
        }
    }

    private String[] getBackupFilesTitles(com.google.api.services.drive.model.File[] backupFiles) {
        int count = backupFiles.length;
        String[] titles = new String[count];
        for (int i = 0; i < count; i++) {
            titles[i] = backupFiles[i].getTitle();
        }
        return titles;
    }

//    private void doBackupOnDropbox() {
//        ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.backup_database_dropbox_inprogress), true);
//        new DropboxBackupTask(this, d).execute();
//    }
//
//    private void doRestoreFromDropbox() {
//        ProgressDialog d = ProgressDialog.show(MainActivity.this, null, getString(R.string.dropbox_loading_files), true);
//        new DropboxListFilesTask(this, d).execute();
//    }
//
//    private String selectedDropboxFile;
//
//    public void doImportFromDropbox(final String[] backupFiles) {
//        if (backupFiles != null) {
//            new AlertDialog.Builder(MainActivity.this)
//                    .setTitle(R.string.restore_database)
//                    .setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            if (selectedDropboxFile != null) {
//                                ProgressDialog d = ProgressDialog.show(MainActivity.this, null, getString(R.string.restore_database_inprogress_dropbox), true);
//                                new DropboxRestoreTask(MainActivity.this, d, selectedDropboxFile).execute();
//                            }
//                        }
//                    })
//                    .setSingleChoiceItems(backupFiles, -1, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            if (which >= 0 && which < backupFiles.length) {
//                                selectedDropboxFile = backupFiles[which];
//                            }
//                        }
//                    })
//                    .show();
//        }
//    }

    public void refreshCurrentTab() {
        //TODO: remove
    }

    //    private void setupFabSettingCurrentTab() {
//        String currentTabName = tabs.get(viewPager.getCurrentItem()).getTabName();
//
//        switch (currentTabName) {
//            case ACCOUNTS:
//                fab.show();
//                break;
//            case BLOTTER:
//                fab.show();
//                SectionPagerAdapter adapter = (SectionPagerAdapter) viewPager.getAdapter();
//                BlotterFragment f = (BlotterFragment)adapter.getFragment(viewPager.getCurrentItem());
//
//                if (f != null) {
//                    long accountId = f.blotterFilter.getAccountId();
//                    if (accountId != -1) {
//                        Account a = em.getAccount(accountId);
//                        if (a == null || !a.isActive)
//                            fab.hide();
//                    }
//                }
//                break;
//            default:
//                fab.hide();
//                break;
//        }
//    }

    private enum MenuEntities implements EntityEnum {

        CURRENCIES(R.string.currencies, R.drawable.menu_entities_currencies, CurrencyListActivity.class),
        EXCHANGE_RATES(R.string.exchange_rates, R.drawable.menu_entities_exchange_rates, ExchangeRatesListActivity.class),
        CATEGORIES(R.string.categories, R.drawable.menu_entities_categories, CategoryListActivity2.class),
        PAYEES(R.string.payees, R.drawable.menu_entities_payees, PayeeListActivity.class),
        PROJECTS(R.string.projects, R.drawable.menu_entities_projects, ProjectListActivity.class),
        LOCATIONS(R.string.locations, R.drawable.menu_entities_locations, LocationsListActivity.class);

        private final int titleId;
        private final int iconId;
        private final Class<?> actitivyClass;

        MenuEntities(int titleId, int iconId, Class<?> activityClass) {
            this.titleId = titleId;
            this.iconId = iconId;
            this.actitivyClass = activityClass;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }

        @Override
        public int getIconId() {
            return iconId;
        }

        public Class<?> getActivityClass() {
            return actitivyClass;
        }

    }

    private enum ImportExportEntities implements ExecutableEntityEnum<MainActivity> {

        CSV_EXPORT(R.string.csv_export, R.drawable.ic_menu_back) {
            @Override
            public void execute(MainActivity mainActivity) {
                mainActivity.doCsvExport();
            }
        },
        CSV_IMPORT(R.string.csv_import, R.drawable.ic_menu_forward) {
            @Override
            public void execute(MainActivity mainActivity) {
                mainActivity.doCsvImport();
            }
        },
        QIF_EXPORT(R.string.qif_export, R.drawable.ic_menu_back) {
            @Override
            public void execute(MainActivity mainActivity) {
                mainActivity.doQifExport();
            }
        },
        QIF_IMPORT(R.string.qif_import, R.drawable.ic_menu_forward) {
            @Override
            public void execute(MainActivity mainActivity) {
                mainActivity.doQifImport();
            }
        };

        private final int titleId;
        private final int iconId;

        private ImportExportEntities(int titleId, int iconId) {
            this.titleId = titleId;
            this.iconId = iconId;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }

        @Override
        public int getIconId() {
            return iconId;
        }

    }

    private enum BackupRestoreEntities implements ExecutableEntityEnum<MainActivity> {

        GOOGLE_DRIVE_BACKUP(R.string.backup_database_online_google_drive, R.drawable.ic_menu_back) {
            @Override
            public void execute(MainActivity mainActivity) {
                mainActivity.doBackupOnGoogleDrive();
            }
        },
        GOOGLE_DRIVE_RESTORE(R.string.restore_database_online_google_drive, R.drawable.ic_menu_forward) {
            @Override
            public void execute(MainActivity mainActivity) {
                mainActivity.doRestoreFromGoogleDrive();
            }
        };
//        ,
//        DROPBOX_BACKUP(R.string.backup_database_online_dropbox, R.drawable.ic_menu_back) {
//            @Override
//            public void execute(MainActivity mainActivity) {
//                mainActivity.doBackupOnDropbox();
//            }
//        },
//        DROPBOX_RESTORE(R.string.restore_database_online_dropbox, R.drawable.ic_menu_forward) {
//            @Override
//            public void execute(MainActivity mainActivity) {
//                mainActivity.doRestoreFromDropbox();
//            }
//        };

        private final int titleId;
        private final int iconId;

        private BackupRestoreEntities(int titleId, int iconId) {
            this.titleId = titleId;
            this.iconId = iconId;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }

        @Override
        public int getIconId() {
            return iconId;
        }

    }

    private class IntegrityFixTask extends AsyncTask<Void, Void, Void> {

        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this, null, getString(R.string.integrity_fix_in_progress), true);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Void o) {
//            refreshCurrentTab();
            progressDialog.dismiss();
        }

        @Override
        protected Void doInBackground(Void... objects) {
            DatabaseAdapter db = new DatabaseAdapter(MainActivity.this);
            new IntegrityFix(db).fix();
            return null;
        }
    }

    public class AccountTotalsCalculationTask extends TotalCalculationTask {

        public AccountTotalsCalculationTask(Context context, TextView totalText) {
            super(context, totalText);
        }

        @Override
        public Total getTotalInHomeCurrency() {
            return db.getAccountsTotalInHomeCurrency();
        }

        @Override
        public Total[] getTotals() {
            return new Total[0];
        }

    }
}
