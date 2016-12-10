package ru.orangesoftware.financisto.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.List;

import greendroid.widget.QuickActionGrid;
import greendroid.widget.QuickActionWidget;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.AbstractTransactionActivity;
import ru.orangesoftware.financisto.activity.AccountWidget;
import ru.orangesoftware.financisto.activity.BlotterFilterActivity;
import ru.orangesoftware.financisto.activity.BlotterOperations;
import ru.orangesoftware.financisto.activity.BlotterTotalsDetailsActivity;
import ru.orangesoftware.financisto.activity.IntegrityCheckTask;
import ru.orangesoftware.financisto.activity.MonthlyViewActivity;
import ru.orangesoftware.financisto.activity.MyQuickAction;
import ru.orangesoftware.financisto.activity.RefreshSupportedActivity;
import ru.orangesoftware.financisto.activity.SelectTemplateActivity;
import ru.orangesoftware.financisto.activity.TransactionActivity;
import ru.orangesoftware.financisto.activity.TransferActivity;
import ru.orangesoftware.financisto.adapter.BlotterListAdapter;
import ru.orangesoftware.financisto.adapter.TransactionsListAdapter;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.contentProvider.contracts.AccountsContract;
import ru.orangesoftware.financisto.contentProvider.contracts.TransactionContract;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.dialog.TransactionInfoDialog;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.utils.ExecutableEntityEnum;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;
import ru.orangesoftware.financisto.view.NodeInflater;

import static ru.orangesoftware.financisto.utils.AndroidUtils.isGreenDroidSupported;
import static ru.orangesoftware.financisto.utils.EnumUtils.showPickOneDialog;
import static ru.orangesoftware.financisto.utils.MyPreferences.isQuickMenuEnabledForTransaction;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BlotterFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BlotterFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BlotterFragment extends ListFragment
        implements android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor>,
        RefreshSupportedActivity, OnAddButtonListener {
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public static final int BLOTTER_LOADER = 1;

    public static final String SAVE_FILTER = "saveFilter";
    public static final String FOR_ACCOUNT_ID = "forAccountId";
//    public static final String EXTRA_FILTER_ACCOUNTS = "filterAccounts";

    private static final int NEW_TRANSACTION_REQUEST = 1001;
    private static final int NEW_TRANSFER_REQUEST = 1003;
    private static final int NEW_TRANSACTION_FROM_TEMPLATE_REQUEST = 1005;
    private static final int MONTHLY_VIEW_REQUEST = 1006;
    private static final int BILL_PREVIEW_REQUEST = 1007;
    protected static final int FILTER_REQUEST = 1008;


    protected static final int MENU_VIEW = Menu.FIRST+1;
    protected static final int MENU_EDIT = Menu.FIRST+2;
    protected static final int MENU_DELETE = Menu.FIRST+3;
    protected static final int MENU_ADD = Menu.FIRST+4;

    private static final int MENU_DUPLICATE = MENU_ADD+1;
    private static final int MENU_SAVE_AS_TEMPLATE = MENU_ADD+2;

    protected long selectedId = -1;
    private long forAccountId = -1;

    protected DatabaseAdapter db;
    protected MyEntityManager em;
    protected Cursor cursor;
//    protected ListAdapter adapter;

    protected boolean enablePin = true;
    protected Activity mActivity;

//    protected TextView totalText;

    private QuickActionWidget transactionActionGrid;
    private QuickActionWidget addButtonActionGrid;

//    private TotalCalculationTask calculationTask;

    public WhereFilter blotterFilter = WhereFilter.empty();

    private boolean isAccountBlotter = false;
    private boolean showAllBlotterButtons = true;

    protected boolean saveFilter;

    private BlotterListAdapter mAdapter;

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param saveFilter Parameter 1.
     * @param forAccountId Filter for Account
     * @return A new instance of fragment BlotterFragment.
     */
    public static BlotterFragment newInstance(Boolean saveFilter, long forAccountId) {
        BlotterFragment fragment = new BlotterFragment();
        Bundle args = new Bundle();
        args.putBoolean(SAVE_FILTER, saveFilter);
        args.putLong(FOR_ACCOUNT_ID, forAccountId);
        fragment.setArguments(args);
        return fragment;
    }

    public BlotterFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;

        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DatabaseAdapter(mActivity);
        db.open();

        em = db.em();

        Bundle args = getArguments();
        if (args != null) {
            saveFilter = args.getBoolean(SAVE_FILTER);
            forAccountId = args.getLong(FOR_ACCOUNT_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_blotter, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        showAllBlotterButtons = !MyPreferences.isCollapseBlotterButtons(mActivity);

//        mRecyclerView = (RecyclerView) view.findViewById(R.id.transactionList);

//        totalText = (TextView)view.findViewById(R.id.total);
//        totalText.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showTotals();
//            }
//        });

        prepareTransactionActionGrid();
        prepareAddButtonActionGrid();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Intent intent = mActivity.getIntent();
        if (intent != null) {
            blotterFilter = WhereFilter.fromIntent(intent);
            saveFilter = intent.getBooleanExtra(SAVE_FILTER, false);
            isAccountBlotter = intent.getBooleanExtra(BlotterFilterActivity.IS_ACCOUNT_FILTER, false);
        }
        if (savedInstanceState != null) {
            blotterFilter = WhereFilter.fromBundle(savedInstanceState);
        }
        if (saveFilter && blotterFilter.isEmpty()) {
            blotterFilter = WhereFilter.fromSharedPreferences(mActivity.getPreferences(0));
        }

        if (forAccountId > 0){
            blotterFilter.remove(BlotterFilter.FROM_ACCOUNT_ID);
            String filterSql = BlotterFilter.FROM_ACCOUNT_ID + "=" + String.valueOf(forAccountId) +
                    " OR " + BlotterFilter.TO_ACCOUNT_ID + "=" + String.valueOf(forAccountId);
            blotterFilter.put(Criteria.raw(filterSql));
        }

        applyFilter(); //make UI controls visible according to filter settings

//        calculateTotals();
        integrityCheck();

//        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
//        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
//        mRecyclerView.setLayoutManager(layoutManager);
//
//        mAdapter = new AccountListFragmentAdapter(getActivity(), null, this, this);
//        mRecyclerView.setAdapter(mAdapter);

        long accountId = blotterFilter.getAccountId();
        if (accountId != -1) {
            mAdapter = new TransactionsListAdapter(mActivity, db, null);
        } else {
            mAdapter = new BlotterListAdapter(mActivity, db, null);
        }
        setListAdapter(mAdapter);
        //TODO!!!!

        getLoaderManager().initLoader(BLOTTER_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
//        Uri contentUri;
        Uri contentUri = TransactionContract.CONTENT_URI;
        WhereFilter filter = blotterFilter;
        long accountId = filter.getAccountId();

        if (accountId != -1) {
//            contentUri = AccountsContract.getAccountTransactionsUri(accountId);
            filter = DatabaseAdapter.enhanceFilterForAccountBlotter(blotterFilter);
        } else {
//            contentUri = TransactionContract.CONTENT_URI;
        }

        String sortOrder = db.getBlotterSortOrder(filter);

        return new CursorLoader(getActivity(), contentUri,
                DatabaseHelper.BlotterColumns.NORMAL_PROJECTION, filter.getSelection(),
                filter.getSelectionArgs(), sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    public void ShowTransactionsForAccount(long accountId){
        blotterFilter.remove(BlotterFilter.FROM_ACCOUNT_ID);
        blotterFilter.put(Criteria.eq(BlotterFilter.FROM_ACCOUNT_ID, String.valueOf(accountId)));

        reloadData();
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
    public void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

//    protected void calculateTotals() {
//        if (calculationTask != null) {
//            calculationTask.stop();
//            calculationTask.cancel(true);
//        }
//        calculationTask = createTotalCalculationTask();
//        calculationTask.execute();
//    }

    //TODO:!!!
//    protected TotalCalculationTask createTotalCalculationTask() {
//        WhereFilter filter = WhereFilter.copyOf(blotterFilter);
//        if (filter.getAccountId() > 0) {
//            return new AccountTotalCalculationTask(mActivity, db, filter, totalText);
//        } else {
//            return new BlotterTotalCalculationTask(mActivity, db, filter, totalText);
//        }
//    }

    @Override
    public void reloadData() {
        getLoaderManager().restartLoader(BLOTTER_LOADER, null, this);
//        calculateTotals();
    }

    @Override
    public void integrityCheck() {
        new IntegrityCheckTask(mActivity).execute();
    }


    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    private void showTotals() {
        Intent intent = new Intent(mActivity, BlotterTotalsDetailsActivity.class);
        blotterFilter.toIntent(intent);
        startActivityForResult(intent, -1);
    }

    protected void prepareTransactionActionGrid() {
        if (isGreenDroidSupported()) {
            transactionActionGrid = new QuickActionGrid(mActivity);
            transactionActionGrid.addQuickAction(new MyQuickAction(mActivity, R.drawable.gd_action_bar_info, R.string.info));
            transactionActionGrid.addQuickAction(new MyQuickAction(mActivity, R.drawable.gd_action_bar_edit, R.string.edit));
            transactionActionGrid.addQuickAction(new MyQuickAction(mActivity, R.drawable.gd_action_bar_trashcan, R.string.delete));
            transactionActionGrid.addQuickAction(new MyQuickAction(mActivity, R.drawable.gd_action_bar_share, R.string.duplicate));
            transactionActionGrid.addQuickAction(new MyQuickAction(mActivity, R.drawable.ic_action_bar_mark, R.string.clear));
            transactionActionGrid.addQuickAction(new MyQuickAction(mActivity, R.drawable.ic_action_bar_double_mark, R.string.reconcile));
            transactionActionGrid.setOnQuickActionClickListener(transactionActionListener);
        }
    }

    private QuickActionWidget.OnQuickActionClickListener transactionActionListener = new QuickActionWidget.OnQuickActionClickListener() {
        public void onQuickActionClicked(QuickActionWidget widget, int position) {
            switch (position) {
                case 0:
                    showTransactionInfo(selectedId);
                    break;
                case 1:
                    editTransaction(selectedId);
                    break;
                case 2:
                    deleteTransaction(selectedId);
                    break;
                case 3:
                    duplicateTransaction(selectedId, 1);
                    break;
                case 4:
                    clearTransaction(selectedId);
                    break;
                case 5:
                    reconcileTransaction(selectedId);
                    break;
            }
        }

    };

    private void prepareAddButtonActionGrid() {
        if (isGreenDroidSupported()) {
            addButtonActionGrid = new QuickActionGrid(mActivity);
            addButtonActionGrid.addQuickAction(new MyQuickAction(mActivity, R.drawable.ic_input_add, R.string.transaction));
            addButtonActionGrid.addQuickAction(new MyQuickAction(mActivity, R.drawable.ic_input_transfer, R.string.transfer));
            addButtonActionGrid.addQuickAction(new MyQuickAction(mActivity, R.drawable.ic_input_templates, R.string.template));
            addButtonActionGrid.setOnQuickActionClickListener(addButtonActionListener);
        }
    }

    private QuickActionWidget.OnQuickActionClickListener addButtonActionListener = new QuickActionWidget.OnQuickActionClickListener() {
        public void onQuickActionClicked(QuickActionWidget widget, int position) {
            switch (position) {
                case 0:
                    addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class);
                    break;
                case 1:
                    addItem(NEW_TRANSFER_REQUEST, TransferActivity.class);
                    break;
                case 2:
                    createFromTemplate();
                    break;
            }
        }

    };

    private void clearTransaction(long selectedId) {
        new BlotterOperations(mActivity, db, selectedId).clearTransaction();
        reloadData();
    }

    private void reconcileTransaction(long selectedId) {
        new BlotterOperations(mActivity, db, selectedId).reconcileTransaction();
        reloadData();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        blotterFilter.toBundle(outState);
    }

    protected void createFromTemplate() {
        Intent intent = new Intent(mActivity, SelectTemplateActivity.class);
        startActivityForResult(intent, NEW_TRANSACTION_FROM_TEMPLATE_REQUEST);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        String header = getString(blotterFilter.isTemplate() ? R.string.template : R.string.transaction);
        menu.setHeaderTitle(header);

        List<MenuItemInfo> menus = createContextMenus(selectedId);
        int i = 0;
        for (MenuItemInfo m : menus) {
            if (m.enabled) {
                menu.add(0, m.menuId, i++, m.titleId);
            }
        }
    }

    protected List<MenuItemInfo> createContextMenus(long id) {
        List<MenuItemInfo> menus = new LinkedList<>();
        menus.add(new MenuItemInfo(MENU_VIEW, R.string.view));
        menus.add(new MenuItemInfo(MENU_EDIT, R.string.edit));
        menus.add(new MenuItemInfo(MENU_DELETE, R.string.delete));

        if (!blotterFilter.isTemplate() && !blotterFilter.isSchedule()) {
            menus.add(new MenuItemInfo(MENU_DUPLICATE, R.string.duplicate));
            menus.add(new MenuItemInfo(MENU_SAVE_AS_TEMPLATE, R.string.save_as_template));
        }
        return menus;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (!super.onContextItemSelected(item)) {
            AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
            switch (item.getItemId()) {
                case MENU_VIEW: {
                    showTransactionInfo(mi.id);
                    return true;
                }
                case MENU_EDIT: {
                    editTransaction(mi.id);
                    return true;
                }
                case MENU_DELETE: {
                    deleteTransaction(mi.id);
                    return true;
                }
                case MENU_DUPLICATE:
                    duplicateTransaction(mi.id, 1);
                    return true;
                case MENU_SAVE_AS_TEMPLATE:
                    new BlotterOperations(mActivity, db, mi.id).duplicateAsTemplate();
                    Toast.makeText(mActivity, R.string.save_as_template_success, Toast.LENGTH_SHORT).show();
                    return true;
            }
        }
        return false;
    }

    private long duplicateTransaction(long id, int multiplier) {
        long newId = new BlotterOperations(mActivity, db, id).duplicateTransaction(multiplier);
        String toastText;
        if (multiplier > 1) {
            toastText = getString(R.string.duplicate_success_with_multiplier, multiplier);
        } else {
            toastText = getString(R.string.duplicate_success);
        }
        Toast.makeText(mActivity, toastText, Toast.LENGTH_LONG).show();
        reloadData();
        AccountWidget.updateWidgets(mActivity);
        return newId;
    }

    public void addItem() {
        if (showAllBlotterButtons) {
            addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class);
        } else {
            if (isGreenDroidSupported()) {
//                addButtonActionGrid.show(bAdd);
                //TODO: expand fab like on google inbox
            } else {
                showPickOneDialog(mActivity, R.string.add_transaction, TransactionQuickMenuEntities.values(), addButtonActionListener);
            }
        }
    }

    protected void addItem(int requestId, Class<? extends AbstractTransactionActivity> clazz) {
//        Intent intent = new Intent(BlotterActivity.this, clazz);
        Intent intent = new Intent(mActivity, clazz);
//        long accountId = blotterFilter.getAccountId();
//        if (accountId != -1) {
        if (forAccountId > 0){
            intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, forAccountId);
        }
        intent.putExtra(TransactionActivity.TEMPLATE_EXTRA, blotterFilter.getIsTemplate());
        startActivityForResult(intent, requestId);
    }

    private void deleteTransaction(long id) {
        new BlotterOperations(mActivity, db, id).deleteTransaction();
    }

    //Used in BlotterOperations. TODO: Change to callback interface
    protected void afterDeletingTransaction(long id) {
        reloadData();
        AccountWidget.updateWidgets(mActivity);
    }

    private void editTransaction(long id) {
        new BlotterOperations(mActivity, db, id).editTransaction();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILTER_REQUEST) {
            if (resultCode == Activity.RESULT_FIRST_USER) {
                blotterFilter.clear();
            } else if (resultCode == Activity.RESULT_OK) {
                blotterFilter = WhereFilter.fromIntent(data);
            }
            if (saveFilter) {
                saveFilter();
            }
            applyFilter();
            reloadData();
        } else if (resultCode == Activity.RESULT_OK && requestCode == NEW_TRANSACTION_FROM_TEMPLATE_REQUEST) {
            createTransactionFromTemplate(data);
        }
        if (resultCode == Activity.RESULT_OK || resultCode == Activity.RESULT_FIRST_USER) {
//            calculateTotals();
        }
    }

    private void createTransactionFromTemplate(Intent data) {
        long templateId = data.getLongExtra(SelectTemplateActivity.TEMPATE_ID, -1);
        int multiplier = data.getIntExtra(SelectTemplateActivity.MULTIPLIER, 1);
        boolean edit = data.getBooleanExtra(SelectTemplateActivity.EDIT_AFTER_CREATION, false);
        if (templateId > 0) {
            long id = duplicateTransaction(templateId, multiplier);
            Transaction t = db.getTransaction(id);
            if (t.fromAmount == 0 || edit) {
                new BlotterOperations(mActivity, db, id).asNewFromTemplate().editTransaction();
            }
        }
    }

    private void saveFilter() {
        SharedPreferences preferences = mActivity.getPreferences(0);
        blotterFilter.toSharedPreferences(preferences);
    }

    protected void applyFilter() {
        String title = blotterFilter.getTitle();
        if (title != null) {
            mActivity.setTitle(getString(R.string.blotter) + " : " + title);
        }

        mActivity.invalidateOptionsMenu();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (isQuickMenuEnabledForTransaction(mActivity)) {
            selectedId = id;
            transactionActionGrid.show(v);
        } else {
            showTransactionInfo(id);
        }
    }

    private void showTransactionInfo(long id) {
        LayoutInflater layoutInflater = (LayoutInflater)mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        NodeInflater inflater = new NodeInflater(layoutInflater);

        TransactionInfoDialog transactionInfoView = new TransactionInfoDialog(mActivity, db, inflater);
        transactionInfoView.show(mActivity, id);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.blotter_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        long accountId = blotterFilter.getAccountId();

        boolean isTransferVisible = showAllBlotterButtons;
        if (accountId != -1) {
            Account a = em.getAccount(accountId);
            if (showAllBlotterButtons) {
                isTransferVisible = a != null && a.isActive;
            }
        }

        menu.findItem(R.id.opt_menu_transfer).setVisible(isTransferVisible);
        menu.findItem(R.id.opt_menu_templates).setVisible(showAllBlotterButtons);
        menu.findItem(R.id.opt_menu_filter).setIcon(blotterFilter.isEmpty() ? R.drawable.ic_menu_filter_off : R.drawable.ic_menu_filter_on);
        menu.findItem(R.id.opt_menu_month).setVisible(accountId != -1);
        menu.findItem(R.id.opt_menu_bill).setVisible(accountId != -1);

        if (accountId != -1) {
            // get account type
            Account account = em.getAccount(accountId);
            AccountType type = AccountType.valueOf(account.type);

            menu.findItem(R.id.opt_menu_bill).setVisible(type.isCreditCard);
        }

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        long accountId = blotterFilter.getAccountId();

        Intent intent = new Intent(mActivity, MonthlyViewActivity.class);
        intent.putExtra(MonthlyViewActivity.ACCOUNT_EXTRA, accountId);

        switch (item.getItemId()) {
            case R.id.opt_menu_transfer:
                addItem(NEW_TRANSFER_REQUEST, TransferActivity.class);
                break;
            case R.id.opt_menu_templates:
                createFromTemplate();
                break;
            case R.id.opt_menu_filter:
                Intent filterIntent = new Intent(mActivity, BlotterFilterActivity.class);
                blotterFilter.toIntent(filterIntent);
                intent.putExtra(BlotterFilterActivity.IS_ACCOUNT_FILTER, isAccountBlotter && blotterFilter.getAccountId() > 0);
                startActivityForResult(filterIntent, FILTER_REQUEST);
                break;
            case R.id.opt_menu_month:
                // call credit card bill activity sending account id
                intent.putExtra(MonthlyViewActivity.BILL_PREVIEW_EXTRA, false);
                startActivityForResult(intent, MONTHLY_VIEW_REQUEST);
                break;
            case R.id.opt_menu_bill:
                if (accountId != -1) {
                    Account account = em.getAccount(accountId);

                    // call credit card bill activity sending account id
                    if (account.paymentDay>0 && account.closingDay>0) {
                        intent.putExtra(MonthlyViewActivity.BILL_PREVIEW_EXTRA, true);
                        startActivityForResult(intent, BILL_PREVIEW_REQUEST);
                    } else {
                        // display message: need payment and closing day
                        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(mActivity);
                        dlgAlert.setMessage(R.string.statement_error);
                        dlgAlert.setTitle(R.string.ccard_statement);
                        dlgAlert.setPositiveButton(R.string.ok, null);
                        dlgAlert.setCancelable(true);
                        dlgAlert.create().show();
                    }
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private enum TransactionQuickMenuEntities implements ExecutableEntityEnum<QuickActionWidget.OnQuickActionClickListener> {

        NEW_TRANSACTION(R.string.transaction, R.drawable.ic_input_add){
            @Override
            public void execute(QuickActionWidget.OnQuickActionClickListener listener) {
                listener.onQuickActionClicked(null, 0);
            }
        },
        NEW_TRANSFER(R.string.transfer, R.drawable.ic_input_transfer) {
            @Override
            public void execute(QuickActionWidget.OnQuickActionClickListener listener) {
                listener.onQuickActionClicked(null, 1);
            }
        },
        NEW_TEMPLATE(R.string.template, R.drawable.ic_input_templates) {
            @Override
            public void execute(QuickActionWidget.OnQuickActionClickListener listener) {
                listener.onQuickActionClicked(null, 2);
            }
        };

        private final int titleId;
        private final int iconId;

        TransactionQuickMenuEntities(int titleId, int iconId) {
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
}
