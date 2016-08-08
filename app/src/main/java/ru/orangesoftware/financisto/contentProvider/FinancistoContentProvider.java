package ru.orangesoftware.financisto.contentProvider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.List;

import ru.orangesoftware.financisto.contentProvider.contracts.AccountsContract;
import ru.orangesoftware.financisto.contentProvider.contracts.BaseContract;
import ru.orangesoftware.financisto.contentProvider.contracts.TransactionContract;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.utils.MyPreferences;

import static ru.orangesoftware.financisto.db.DatabaseHelper.V_BLOTTER;
import static ru.orangesoftware.financisto.db.DatabaseHelper.V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS;

public class FinancistoContentProvider extends ContentProvider {
    private static final int ACCOUNTS = 1;
    private static final int ACCOUNT_ID = 2;
    private static final int ACCOUNT_TRANSACTIONS = 3;
    private static final int ACCOUNT_TRANSACTION_ID = 4;
    private static final int TRANSACTIONS = 5;

//    private static final int PEOPLE_CONTACTMETHODS = 7;
//    private static final int PEOPLE_CONTACTMETHODS_ID = 8;
//
//    private static final int DELETED_PEOPLE = 20;
//
//    private static final int PHONES = 9;
//    private static final int PHONES_ID = 10;
//    private static final int PHONES_FILTER = 14;
//
//    private static final int CONTACTMETHODS = 18;
//    private static final int CONTACTMETHODS_ID = 19;
//
//    private static final int CALLS = 11;
//    private static final int CALLS_ID = 12;
//    private static final int CALLS_FILTER = 15;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static
    {
        sURIMatcher.addURI(BaseContract.AUTHORITY, "accounts", ACCOUNTS);
        sURIMatcher.addURI(BaseContract.AUTHORITY, "accounts/#", ACCOUNT_ID);
        sURIMatcher.addURI(BaseContract.AUTHORITY, "accounts/#/transactions", ACCOUNT_TRANSACTIONS);
        sURIMatcher.addURI(BaseContract.AUTHORITY, "transactions", TRANSACTIONS);
        sURIMatcher.addURI(BaseContract.AUTHORITY, "transactions/#", ACCOUNT_TRANSACTION_ID);
//        sURIMatcher.addURI("contacts", "people/#/phones/#", PEOPLE_PHONES_ID);
//        sURIMatcher.addURI("contacts", "people/#/contact_methods", PEOPLE_CONTACTMETHODS);
//        sURIMatcher.addURI("contacts", "people/#/contact_methods/#", PEOPLE_CONTACTMETHODS_ID);
//        sURIMatcher.addURI("contacts", "deleted_people", DELETED_PEOPLE);
//        sURIMatcher.addURI("contacts", "phones", PHONES);
//        sURIMatcher.addURI("contacts", "phones/filter/*", PHONES_FILTER);
//        sURIMatcher.addURI("contacts", "phones/#", PHONES_ID);
//        sURIMatcher.addURI("contacts", "contact_methods", CONTACTMETHODS);
//        sURIMatcher.addURI("contacts", "contact_methods/#", CONTACTMETHODS_ID);
//        sURIMatcher.addURI("call_log", "calls", CALLS);
//        sURIMatcher.addURI("call_log", "calls/filter/*", CALLS_FILTER);
//        sURIMatcher.addURI("call_log", "calls/#", CALLS_ID);
    }


    private DatabaseAdapter dba;
    private MyEntityManager em;
    private DatabaseHelper mDbHelper;

    public FinancistoContentProvider() {}

    @Override
    public boolean onCreate() {
        mDbHelper = DatabaseHelper.getHelper(getContext());
        dba = new DatabaseAdapter(getContext());
        dba.open();

        em = dba.em();

        return true;
    }

    @Override
    public String getType(Uri uri) {
        int match = sURIMatcher.match(uri);
        switch (match)
        {
            case ACCOUNTS:
                return AccountsContract.CONTENT_TYPE;
            case ACCOUNT_ID:
                return AccountsContract.CONTENT_ITEM_TYPE;
            case ACCOUNT_TRANSACTIONS:
            case TRANSACTIONS:
                return TransactionContract.CONTENT_TYPE;
            case ACCOUNT_TRANSACTION_ID:
                return TransactionContract.CONTENT_ITEM_TYPE;
//            case PEOPLE_ADDRESS_ID:
//                return "vnd.android.cursor.item/snail-mail";
            default:
                return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = null;

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

        switch (sURIMatcher.match(uri)) {
            case ACCOUNTS:
                cursor = em.getAllAccounts(MyPreferences.isHideClosedAccounts(getContext()));
                break;
            case ACCOUNT_ID:
                long accountId = ContentUris.parseId(uri);
                cursor = em.getAllAccounts(MyPreferences.isHideClosedAccounts(getContext()), accountId);
                break;
            case ACCOUNT_TRANSACTIONS:
//                List<String> pathSegments = uri.getPathSegments();
//                long transactionsAccountId = Long.parseLong(pathSegments.get(1));
                builder.setTables(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS);
//                cursor = db.query(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, projection,
//                        selection, selectionArgs, null, null,
//                        sortOrder);
                break;
            case ACCOUNT_TRANSACTION_ID:
                builder.setTables(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS);
                long transactionId = ContentUris.parseId(uri);
                builder.appendWhere(BaseContract.CommonColumns._ID + " = " + transactionId);
                break;
            case TRANSACTIONS:
                builder.setTables(V_BLOTTER);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported URI: " + uri);
        }

        if (cursor == null) {
            cursor =
                    builder.query(
                            db,
                            projection,
                            selection,
                            selectionArgs,
                            null,
                            null,
                            sortOrder);
        }

        // if we want to be notified of any changes:
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (sURIMatcher.match(uri) != ACCOUNTS) {
            throw new IllegalArgumentException(
                    "Unsupported URI for insertion: " + uri);
        }

        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int updateCount;

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        switch (sURIMatcher.match(uri)) {
            case ACCOUNTS:
                updateCount = db.update(DatabaseHelper.ACCOUNT_TABLE, values, selection,
                        selectionArgs);
                break;
            case ACCOUNT_ID:
                String idStr = uri.getLastPathSegment();
                String where = AccountsContract._ID + " = " + idStr;
                if (!TextUtils.isEmpty(selection)) {
                    where += " AND " + selection;
                }
                //values, ed.idField.columnName+"=?", new String[]{String.valueOf(id)}
                updateCount = db.update(DatabaseHelper.ACCOUNT_TABLE, values, where,
                        selectionArgs);
                break;
            default:
                // no support for updating photos!
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        // notify all listeners of changes:
        if (updateCount > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return updateCount;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
