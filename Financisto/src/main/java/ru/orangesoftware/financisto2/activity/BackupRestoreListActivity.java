package ru.orangesoftware.financisto2.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.support.v4.app.NavUtils;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.Status;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.ViewById;

import java.io.File;
import java.util.List;

import ru.orangesoftware.financisto2.R;
import ru.orangesoftware.financisto2.backup.Backup;
import ru.orangesoftware.financisto2.bus.GreenRobotBus;
import ru.orangesoftware.financisto2.export.BackupExportTask;
import ru.orangesoftware.financisto2.export.BackupImportTask;
import ru.orangesoftware.financisto2.export.Export;
import ru.orangesoftware.financisto2.export.ImportExportAsyncTaskListener;
import ru.orangesoftware.financisto2.export.drive.DoBackup;
import ru.orangesoftware.financisto2.export.drive.DoListFiles;
import ru.orangesoftware.financisto2.export.drive.DoRestore;
import ru.orangesoftware.financisto2.export.drive.DriveBackupError;
import ru.orangesoftware.financisto2.export.drive.DriveBackupFailure;
import ru.orangesoftware.financisto2.export.drive.DriveBackupSuccess;
import ru.orangesoftware.financisto2.export.drive.DriveConnectionFailed;
import ru.orangesoftware.financisto2.export.drive.DriveFileInfo;
import ru.orangesoftware.financisto2.export.drive.DriveFileList;
import ru.orangesoftware.financisto2.export.drive.DriveRestoreSuccess;
import ru.orangesoftware.financisto2.export.sync.ApiServiceFactory;
import ru.orangesoftware.financisto2.export.sync.ApiSyncService;
import ru.orangesoftware.financisto2.export.sync.ApiSyncTask;
import ru.orangesoftware.financisto2.utils.EntityEnum;
import ru.orangesoftware.financisto2.utils.EnumUtils;

@EActivity(R.layout.activity_entity_list)
public class BackupRestoreListActivity extends ListActivity {

    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 1;

    @Bean
    public GreenRobotBus bus;

    final Entity[] entities = Entity.values();

    ProgressDialog progressDialog;

    @ViewById(android.R.id.list)
    protected ListView listView;

    @AfterViews
    protected void afterViews() {
        getActionBar().setDisplayHomeAsUpEnabled(true);
        ListAdapter adapter = EnumUtils.createEntityEnumAdapter(this, entities);
        listView.setAdapter(adapter);
    }

    @ItemClick(android.R.id.list)
    protected void onItemClick(int position) {
        Entity entity = entities[position];
        entity.startActivity(this);
    }

    @OptionsItem(android.R.id.home)
    public void onHome() {
        NavUtils.navigateUpFromSameTask(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bus.register(this);
    }

    @Override
    protected void onPause() {
        bus.unregister(this);
        super.onPause();
    }

    private void doBackupDatabase() {
        ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.backup_database_inprogress), true);
        new BackupExportTask(this, d, true).execute();
    }

    private String selectedBackupFile;

    private void doRestoreDatabase() {
        final String[] backupFiles = Backup.listBackups(this);
        final Context context = BackupRestoreListActivity.this;
        new AlertDialog.Builder(this)
                .setTitle(R.string.restore_database)
                .setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (selectedBackupFile != null) {
                            ProgressDialog d = ProgressDialog.show(context, null, getString(R.string.restore_database_inprogress), true);
                            new BackupImportTask(context, d).execute(selectedBackupFile);
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

    private void doSendBackupTo() {
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

    private void doBackupOnGoogleDrive() {
        progressDialog = ProgressDialog.show(this, null, getString(R.string.backup_database_gdocs_inprogress), true);
        bus.post(new DoBackup());
    }

    private DriveFileInfo selectedDriveFile;

    private void doRestoreFromGoogleDrive() {
        progressDialog = ProgressDialog.show(this, null, getString(R.string.google_drive_loading_files), true);
        bus.post(new DoListFiles());
    }

    public void onEventMainThread(DriveFileList event) {
        dissmissProgressDialog();
        final List<DriveFileInfo> files = event.files;
        final BackupRestoreListActivity context = this;
        ArrayAdapter<DriveFileInfo> adapter = new ArrayAdapter<DriveFileInfo>(context, android.R.layout.simple_list_item_single_choice, files);
        new AlertDialog.Builder(context)
                .setTitle(R.string.restore_database)
                .setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (selectedDriveFile != null) {
                            progressDialog = ProgressDialog.show(context, null, getString(R.string.google_drive_restore_in_progress), true);
                            bus.post(new DoRestore(selectedDriveFile));
                        }
                    }
                })
                .setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which >= 0 && which < files.size()) {
                            selectedDriveFile = files.get(which);
                        }
                    }
                })
                .show();
    }

    public void onEventMainThread(DriveBackupFailure event) {
        dissmissProgressDialog();
        Status status = event.status;
        if (status.hasResolution()) {
            try {
                status.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
                onEventMainThread(new DriveBackupError(e));
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(status.getStatusCode(), this, 0).show();
        }
    }

    public void onEventMainThread(DriveBackupSuccess event) {
        dissmissProgressDialog();
        Toast.makeText(this, getString(R.string.google_drive_backup_success, event.fileName), Toast.LENGTH_LONG).show();
    }

    public void onEventMainThread(DriveRestoreSuccess event) {
        dissmissProgressDialog();
        Toast.makeText(this, R.string.restore_database_success, Toast.LENGTH_LONG).show();
    }

    public void onEventMainThread(DriveBackupError event) {
        dissmissProgressDialog();
        Toast.makeText(this, getString(R.string.google_drive_connection_failed, event.e), Toast.LENGTH_LONG).show();
    }

    public void onEventMainThread(DriveConnectionFailed event) {
        dissmissProgressDialog();
        ConnectionResult connectionResult = event.connectionResult;
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
                onEventMainThread(new DriveBackupError(e));
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }
    }

    @OnActivityResult(RESOLVE_CONNECTION_REQUEST_CODE)
    public void onConnectionRequest(int resultCode) {
        if (resultCode == RESULT_OK) {
            Toast.makeText(this, R.string.google_drive_connection_resolved, Toast.LENGTH_LONG).show();
        }
    }

    private void dissmissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private enum Entity implements EntityEnum {
        SYNC(R.string.sync, R.drawable.ic_menu_refresh){
            @Override
            public void startActivity(BackupRestoreListActivity context) {
//                FlowzrSyncActivity_.intent(context).start();
                context.doSync();
            }
        },
        BACKUP(R.string.backup_database, R.drawable.backup_file){
            @Override
            public void startActivity(BackupRestoreListActivity context) {
                context.doBackupDatabase();
            }
        },
        RESTORE(R.string.restore_database, R.drawable.backup_file){
            @Override
            public void startActivity(BackupRestoreListActivity context) {
                context.doRestoreDatabase();
            }
        },
        FLOWZR_SYNC(R.string.flowzr_sync, R.drawable.backup_flowzr){
            @Override
            public void startActivity(BackupRestoreListActivity context) {
                FlowzrSyncActivity_.intent(context).start();
            }
        },
        GOOGLE_DRIVE_BACKUP(R.string.backup_database_online_google_drive, R.drawable.backup_google_drive){
            @Override
            public void startActivity(BackupRestoreListActivity context) {
                context.doBackupOnGoogleDrive();
            }
        },
        GOOGLE_DRIVE_RESTORE(R.string.restore_database_online_google_drive, R.drawable.backup_google_drive){
            @Override
            public void startActivity(BackupRestoreListActivity context) {
                context.doRestoreFromGoogleDrive();
            }
        },
        DROPBOX_BACKUP(R.string.backup_database_online_dropbox, R.drawable.backup_dropbox){
            @Override
            public void startActivity(BackupRestoreListActivity context) {
                ExchangeRatesListActivity_.intent(context).start();
            }
        },
        DROPBOX_RESTORE(R.string.restore_database_online_dropbox, R.drawable.backup_dropbox){
            @Override
            public void startActivity(BackupRestoreListActivity context) {
                ExchangeRatesListActivity_.intent(context).start();
            }
        },
        SEND_BACKUP_TO(R.string.backup_database_to, R.drawable.backup_share_2){
            @Override
            public void startActivity(BackupRestoreListActivity context) {
                context.doSendBackupTo();
            }
        },
        CSV_EXPORT(R.string.csv_export, R.drawable.backup_csv){
            @Override
            public void startActivity(BackupRestoreListActivity context) {
                ExchangeRatesListActivity_.intent(context).start();
            }
        },
        CSV_IMPORT(R.string.csv_import, R.drawable.backup_csv){
            @Override
            public void startActivity(BackupRestoreListActivity context) {
                ExchangeRatesListActivity_.intent(context).start();
            }
        },
        QIF_EXPORT(R.string.qif_export, R.drawable.backup_qif){
            @Override
            public void startActivity(BackupRestoreListActivity context) {
                ExchangeRatesListActivity_.intent(context).start();
            }
        },
        QIF_IMPORT(R.string.qif_import, R.drawable.backup_qif){
            @Override
            public void startActivity(BackupRestoreListActivity context) {
                ExchangeRatesListActivity_.intent(context).start();
            }
        };

        private final int titleId;
        private final int iconId;

        private Entity(int titleId, int iconId) {
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

        public abstract void startActivity(BackupRestoreListActivity context);

    }

    private void doSync() {
        new ApiSyncTask(this).execute();
    }

}
