package ru.orangesoftware.financisto.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import java.util.ArrayList;

import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Budget;

/**
 * Created by luberello on 03.09.15.
 */
public class BudgetsLoader extends AsyncTaskLoader<ArrayList<Budget>> {
    protected MyEntityManager em;
    private final WhereFilter filter;

    private ArrayList<Budget> mBudgets;

    /**
     * Stores away the application context associated with context. Since Loaders can be used
     * across multiple activities it's dangerous to store the context directly.
     *
     * @param context used to retrieve the application context.
     */
    public BudgetsLoader(Context context, WhereFilter filter) {
        super(context);

        DatabaseAdapter db = new DatabaseAdapter(context);
        db.open();

        this.em = db.em();

        this.filter = filter;
    }

    @Override
    public ArrayList<Budget> loadInBackground() {
        mBudgets = this.em.getAllBudgets(this.filter);
        return mBudgets;
    }

    @Override
    public void deliverResult(ArrayList<Budget> data) {
        if (isReset()) {
            // An async query came in while the loader is stopped
            return;
        }

        if (isStarted()) {
            super.deliverResult(data);
        }
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();

        if (mBudgets != null) {
            deliverResult(mBudgets);
        }
        if (takeContentChanged() || mBudgets == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        super.onStopLoading();

        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();

        onStopLoading();

        mBudgets = null;
    }
}
