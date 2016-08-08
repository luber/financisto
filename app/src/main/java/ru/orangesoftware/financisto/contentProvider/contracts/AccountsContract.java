package ru.orangesoftware.financisto.contentProvider.contracts;

import android.content.ContentResolver;
import android.net.Uri;

import ru.orangesoftware.financisto.db.DatabaseHelper;

/**
 * Created by luberello on 24.08.15.
 */
public final class AccountsContract implements BaseContract.CommonColumns {
    public static final String CREATION_DATE = "creation_date";
    public static final String LAST_TRANSACTION_DATE = "last_transaction_date";
    public static final String CURRENCY_ID = "currency_id";
    public static final String TYPE = "type";
    public static final String CARD_ISSUER = "card_issuer";
    public static final String ISSUER = "issuer";
    public static final String NUMBER = "number";
    public static final String TOTAL_AMOUNT = "total_amount";
    public static final String TOTAL_LIMIT = "total_limit";
    public static final String SORT_ORDER = "sort_order";
    public static final String IS_ACTIVE = "is_active";
    public static final String IS_INCLUDE_INTO_TOTALS = "is_include_into_totals";
    public static final String LAST_ACCOUNT_ID = "last_account_id";
    public static final String LAST_CATEGORY_ID = "last_category_id";
    public static final String CLOSING_DAY = "closing_day";
    public static final String PAYMENT_DAY = "payment_day";
    public static final String NOTE = "note";

    public static final Uri CONTENT_URI = Uri.withAppendedPath(BaseContract.CONTENT_URI, "accounts");

    public static Uri getAccountTransactionsUri(long accountId){
        return Uri.withAppendedPath(CONTENT_URI, String.valueOf(accountId) + "/transactions");
    }

    /**
     * The mime type of a directory of items.
     */
    public static final String CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.ru.orangesoftware.financisto_account";
    /**
     * The mime type of a single item.
     */
    public static final String CONTENT_ITEM_TYPE =
            ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.ru.orangesoftware.financisto_account";

    /**
     * A projection of all columns
     * in the items table.
     */
    public static final String[] PROJECTION_ALL =
            {_ID, TITLE, UPDATED_ON, REMOTE_KEY,
                    CREATION_DATE, LAST_TRANSACTION_DATE, CURRENCY_ID, TYPE, CARD_ISSUER,
                    ISSUER, NUMBER, TOTAL_AMOUNT, TOTAL_LIMIT, IS_ACTIVE, IS_INCLUDE_INTO_TOTALS,
                    LAST_ACCOUNT_ID, LAST_CATEGORY_ID, CLOSING_DAY, PAYMENT_DAY, NOTE, SORT_ORDER};
    /**
     * The default sort order for
     * queries containing NAME fields.
     */
    public static final String SORT_ORDER_DEFAULT =
            IS_ACTIVE + " DESC," + SORT_ORDER + " ASC," + TITLE + " ASC";

    public static final String JOIN_CURRENCY_STATEMENT =
            " JOIN " + DatabaseHelper.CURRENCY_TABLE + " ON (" +
                    addPrefix(CURRENCY_ID) +
            " = " + DatabaseHelper.CURRENCY_TABLE + "." + _ID;

    public static String addPrefix(String column) {
        return DatabaseHelper.ACCOUNT_TABLE + "." + column;
    }

    public static String[] getQualifiedColumns() {
        String[] qualifiedColumns = new String[PROJECTION_ALL.length];
        for (int i = 0; i < PROJECTION_ALL.length; i++) {
            qualifiedColumns[i] = addPrefix(PROJECTION_ALL[i]);
        }

        return qualifiedColumns;
    }
}