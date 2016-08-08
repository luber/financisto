package ru.orangesoftware.financisto.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.BudgetActivity;
import ru.orangesoftware.financisto.activity.BudgetBlotterActivity;
import ru.orangesoftware.financisto.activity.DateFilterActivity;
import ru.orangesoftware.financisto.activity.IntegrityCheckTask;
import ru.orangesoftware.financisto.activity.RefreshSupportedActivity;
import ru.orangesoftware.financisto.adapter.BudgetListAdapter;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.datetime.PeriodType;
import ru.orangesoftware.financisto.db.BudgetsTotalCalculator;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.filter.DateTimeCriteria;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.loader.BudgetsLoader;
import ru.orangesoftware.financisto.model.Budget;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import ru.orangesoftware.financisto.utils.PinProtection;
import ru.orangesoftware.financisto.utils.RecurUtils;


/**
 * Created by luberello on 03.09.15.
 */
public class BudgetListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<ArrayList<Budget>>,
        RefreshSupportedActivity,
        OnAddButtonListener{

    private static final int NEW_BUDGET_REQUEST = 4001;
    private static final int EDIT_BUDGET_REQUEST = 4002;
    private static final int VIEW_BUDGET_REQUEST = 4003;
    private static final int FILTER_BUDGET_REQUEST = 4004;

    private static final int BUDGET_LIST_LOADER = 300;

    protected static final int MENU_VIEW = Menu.FIRST+1;
    protected static final int MENU_EDIT = Menu.FIRST+2;
    protected static final int MENU_DELETE = Menu.FIRST+3;

    private WhereFilter filter = WhereFilter.empty();
    protected long selectedId = -1;

    private Activity mActivity;

    private BudgetListAdapter mAdapter;

    protected boolean enablePin = true;

    protected DatabaseAdapter db;
    protected MyEntityManager em;

    private BudgetTotalsCalculationTask totalCalculationTask;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;

//        try {
//            mListener = (OnFragmentInteractionListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DatabaseAdapter(mActivity);
        db.open();

        em = db.em();
    }

    @Override
    public void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (enablePin) PinProtection.lock(mActivity);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (enablePin) PinProtection.unlock(mActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_budget_list, container, false);

//        TextView totalText = (TextView)rootView.findViewById(R.id.total);
//        totalText.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showTotals();
//            }
//        });


        if (filter.isEmpty()) {
            filter = WhereFilter.fromSharedPreferences(mActivity.getPreferences(0));
        }
        if (filter.isEmpty()) {
            filter.put(new DateTimeCriteria(PeriodType.THIS_MONTH));
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);
        registerForContextMenu(getListView());

        mAdapter = new BudgetListAdapter(getActivity(), new ArrayList<Budget>());
        setListAdapter(mAdapter);

        getLoaderManager().initLoader(BUDGET_LIST_LOADER, null, this);
    }

    @Override
    public Loader<ArrayList<Budget>> onCreateLoader(int id, Bundle args) {
        return new BudgetsLoader(getActivity(), filter);
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<Budget>> loader, ArrayList<Budget> data) {
        mAdapter.setBudgets(data);
        calculateTotals(data);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<Budget>> loader) {
        mAdapter.setBudgets(new ArrayList<Budget>());
    }

    @Override
    public void reloadData() {
        getLoaderManager().restartLoader(BUDGET_LIST_LOADER, null, this);
    }

    @Override
    public void integrityCheck() {
        new IntegrityCheckTask(mActivity).execute();
    }

    @Override
    public void addItem() {
        Intent intent = new Intent(getActivity(), BudgetActivity.class);
        startActivityForResult(intent, NEW_BUDGET_REQUEST);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        selectedId = ((AdapterView.AdapterContextMenuInfo)menuInfo).id;

        menu.setHeaderTitle(getString(R.string.budget));

        List<MenuItemInfo> menus = new LinkedList<>();
        menus.add(new MenuItemInfo(MENU_VIEW, R.string.view));
        menus.add(new MenuItemInfo(MENU_EDIT, R.string.edit));
        menus.add(new MenuItemInfo(MENU_DELETE, R.string.delete));

        int i = 0;
        for (MenuItemInfo m : menus) {
            if (m.enabled) {
                menu.add(0, m.menuId, i++, m.titleId);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);

        switch (item.getItemId()) {
            case MENU_VIEW: {
                showBudget(selectedId);
                return true;
            }
            case MENU_EDIT: {
                editBudget(selectedId);
                return true;
            }
            case MENU_DELETE: {
                deleteBudget(selectedId);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.budget_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.opt_menu_filter).setIcon(filter.isEmpty() ? R.drawable.ic_menu_filter_off : R.drawable.ic_menu_filter_on);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.opt_menu_filter:
                Intent intent = new Intent(getActivity(), DateFilterActivity.class);
                filter.toIntent(intent);
                startActivityForResult(intent, FILTER_BUDGET_REQUEST);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILTER_BUDGET_REQUEST) {
            if (resultCode == Activity.RESULT_FIRST_USER) {
                filter.clear();
            } else if (resultCode == Activity.RESULT_OK) {
                String periodType = data.getStringExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_TYPE);
                PeriodType p = PeriodType.valueOf(periodType);
                if (PeriodType.CUSTOM == p) {
                    long periodFrom = data.getLongExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_FROM, 0);
                    long periodTo = data.getLongExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_TO, 0);
                    filter.put(new DateTimeCriteria(periodFrom, periodTo));
                } else {
                    filter.put(new DateTimeCriteria(p));
                }
            }
            saveFilter();
        }
        reloadData();
//        super.onActivityResult(requestCode, resultCode, data);
    }

    private void calculateTotals(List<Budget> budgets) {
        if (totalCalculationTask != null) {
            totalCalculationTask.stop();
            totalCalculationTask.cancel(true);
        }
//        TextView totalText = (TextView)findViewById(R.id.total);
        totalCalculationTask = new BudgetTotalsCalculationTask(getActivity(), budgets, mAdapter, new Handler());
        totalCalculationTask.execute((Void[]) null);
    }

    private void showBudget(long budgetId){
        Budget b = em.load(Budget.class, budgetId);
        Intent intent = new Intent(getActivity(), BudgetBlotterActivity.class);
        Criteria.eq(BlotterFilter.BUDGET_ID, String.valueOf(budgetId))
                .toIntent(b.title, intent);
        startActivityForResult(intent, VIEW_BUDGET_REQUEST);
    }

    private void editBudget(long budgetId) {
        Budget b = em.load(Budget.class, budgetId);
        RecurUtils.Recur recur = b.getRecur();
        if (recur.interval != RecurUtils.RecurInterval.NO_RECUR) {
            Toast t = Toast.makeText(getActivity(), R.string.edit_recurring_budget, Toast.LENGTH_LONG);
            t.show();
        }
        Intent intent = new Intent(getActivity(), BudgetActivity.class);
        intent.putExtra(BudgetActivity.BUDGET_ID_EXTRA, b.parentBudgetId > 0 ? b.parentBudgetId : budgetId);
        startActivityForResult(intent, EDIT_BUDGET_REQUEST);
    }

    private void deleteBudget(final long budgetId) {
        final Budget b = em.load(Budget.class, budgetId);
        if (b.parentBudgetId > 0) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.delete_budget_recurring_select)
                    .setPositiveButton(R.string.delete_budget_one_entry, new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            em.deleteBudgetOneEntry(budgetId);
                            reloadData();
                        }
                    })
                    .setNeutralButton(R.string.delete_budget_all_entries, new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            em.deleteBudget(b.parentBudgetId);
                            reloadData();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            RecurUtils.Recur recur = RecurUtils.createFromExtraString(b.recur);
            new AlertDialog.Builder(getActivity())
                    .setMessage(recur.interval == RecurUtils.RecurInterval.NO_RECUR ? R.string.delete_budget_confirm : R.string.delete_budget_recurring_confirm)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            em.deleteBudget(budgetId);
                            reloadData();
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        }
    }

    private void saveFilter() {
        SharedPreferences preferences = mActivity.getPreferences(0);
        filter.toSharedPreferences(preferences);
        mActivity.invalidateOptionsMenu();
    }

    public class BudgetTotalsCalculationTask extends AsyncTask<Void, Total, Total> {

        private final Context context;
        private final List<Budget> budgets;
        private final BudgetListAdapter adapter;
        private final Handler handler;
        private volatile boolean isRunning = true;

        public BudgetTotalsCalculationTask(Context context, List<Budget> budgets, BudgetListAdapter adapter, Handler handler) {

            this.context = context;
            this.budgets = budgets;
            this.adapter = adapter;
            this.handler = handler;
        }

        @Override
        protected Total doInBackground(Void... params) {
            try {
                BudgetsTotalCalculator c = new BudgetsTotalCalculator(db, budgets);
                c.updateBudgets(handler);
                return c.calculateTotalInHomeCurrency();
            } catch (Exception ex) {
                Log.e("BudgetTotals", "Unexpected error", ex);
                return Total.ZERO;
            }

        }

        @Override
        protected void onPostExecute(Total result) {
            if (isRunning) {
//                Utils u = new Utils(BudgetListActivity.this);
//                u.setTotal(totalText, result);
                adapter.notifyDataSetChanged();
            }
        }

        public void stop() {
            isRunning = false;
        }

    }
}
