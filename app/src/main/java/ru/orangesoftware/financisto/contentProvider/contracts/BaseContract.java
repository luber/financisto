package ru.orangesoftware.financisto.contentProvider.contracts;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by luberello on 24.08.15.
 */
public final class BaseContract {
    public static final String AUTHORITY = "ru.orangesoftware.financisto";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    public interface CommonColumns extends BaseColumns {
        String TITLE = "title";
        String UPDATED_ON = "updated_on";
        String REMOTE_KEY = "remote_key";
    }
}
