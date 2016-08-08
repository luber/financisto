package ru.orangesoftware.financisto.contentProvider.contracts;

import android.content.ContentResolver;
import android.net.Uri;

/**
 * Created by luberello on 25.08.15.
 */
public final class TransactionContract {

    public static final Uri CONTENT_URI = Uri.withAppendedPath(BaseContract.CONTENT_URI, "transactions");

    /**
     * The mime type of a directory of items.
     */
    public static final String CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.ru.orangesoftware.financisto_transaction";
    /**
     * The mime type of a single item.
     */
    public static final String CONTENT_ITEM_TYPE =
            ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.ru.orangesoftware.financisto_transaction";
}
