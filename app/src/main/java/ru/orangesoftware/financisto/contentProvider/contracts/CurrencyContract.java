package ru.orangesoftware.financisto.contentProvider.contracts;

import android.content.ContentResolver;
import android.net.Uri;

import ru.orangesoftware.financisto.db.DatabaseHelper;

/**
 * Created by luberello on 24.08.15.
 */
public final class CurrencyContract implements BaseContract.CommonColumns {

    public static final String NAME = "name";
    public static final String SYMBOL = "name";
    public static final String SYMBOL_FORMAT = "symbol_format";
    public static final String IS_DEFAULT = "is_default";
    public static final String DECIMALS = "decimals";
    public static final String DECIMAL_SEPARATOR = "decimal_separator";
    public static final String GROUP_SEPARATOR = "group_separator";

    public static final Uri CONTENT_URI = Uri.withAppendedPath(BaseContract.CONTENT_URI, "currencies");

    /**
     * The mime type of a directory of items.
     */
    public static final String CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.ru.orangesoftware.financisto_currency";
    /**
     * The mime type of a single item.
     */
    public static final String CONTENT_ITEM_TYPE =
            ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.ru.orangesoftware.financisto_currency";

    /**
     * A projection of all columns
     * in the items table.
     */
    public static final String[] PROJECTION_ALL =
            {_ID, TITLE, UPDATED_ON, REMOTE_KEY,
                    NAME, SYMBOL, SYMBOL_FORMAT, IS_DEFAULT, DECIMALS,
                    DECIMAL_SEPARATOR, GROUP_SEPARATOR};

    public static String addPrefix(String column) {
        return DatabaseHelper.CURRENCY_TABLE + "." + column;
    }

    public static String[] getQualifiedColumns() {
        String[] qualifiedColumns = new String[PROJECTION_ALL.length];
        for (int i = 0; i < PROJECTION_ALL.length; i++) {
            qualifiedColumns[i] = addPrefix(PROJECTION_ALL[i]);
        }

        return qualifiedColumns;
    }

}
