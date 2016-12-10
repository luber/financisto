package ru.orangesoftware.financisto.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import greendroid.widget.QuickActionGrid;
import greendroid.widget.QuickActionWidget;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.AbstractTransactionActivity;
import ru.orangesoftware.financisto.activity.AccountActivity;
import ru.orangesoftware.financisto.activity.IntegrityCheckTask;
import ru.orangesoftware.financisto.activity.MyQuickAction;
import ru.orangesoftware.financisto.activity.PurgeAccountActivity;
import ru.orangesoftware.financisto.activity.RefreshSupportedActivity;
import ru.orangesoftware.financisto.activity.TransactionActivity;
import ru.orangesoftware.financisto.activity.TransferActivity;
import ru.orangesoftware.financisto.adapter.AccountListFragmentAdapter;
import ru.orangesoftware.financisto.contentProvider.contracts.AccountsContract;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.dialog.AccountInfoDialog;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import ru.orangesoftware.financisto.utils.PinProtection;
import ru.orangesoftware.financisto.view.NodeInflater;

import static ru.orangesoftware.financisto.utils.AndroidUtils.isGreenDroidSupported;
import static ru.orangesoftware.financisto.utils.MyPreferences.isQuickMenuEnabledForAccount;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AccountListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AccountListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AccountListFragment extends Fragment
        implements android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor>,
        AccountListFragmentAdapter.AccountViewHolder.IAccountListItemClicksListener,
        RefreshSupportedActivity, OnAddButtonListener {

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onShowAccountTransactions(long accountId);
    }

    private static final int NEW_ACCOUNT_REQUEST = 1;

    public static final int EDIT_ACCOUNT_REQUEST = 2;
    public static final int VIEW_ACCOUNT_REQUEST = 3;
    public static final int PURGE_ACCOUNT_REQUEST = 4;

    protected static final int MENU_VIEW = Menu.FIRST+1;
    protected static final int MENU_EDIT = Menu.FIRST+2;
    protected static final int MENU_DELETE = Menu.FIRST+3;
    protected static final int MENU_ADD = Menu.FIRST+4;

    protected long selectedId = -1;

    protected DatabaseAdapter db;
    protected MyEntityManager em;
    protected Cursor cursor;
//    protected ListAdapter adapter;

    protected boolean enablePin = true;
    protected Activity mActivity;

    protected RecyclerView mRecyclerView;

    private static final int MENU_UPDATE_BALANCE = MENU_ADD+1;
    private static final int MENU_CLOSE_OPEN_ACCOUNT = MENU_ADD+2;
    private static final int MENU_PURGE_ACCOUNT = MENU_ADD+3;

    private QuickActionWidget accountActionGrid;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_PARAM1 = "param1";
//    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
//    private String mParam1;
//    private String mParam2;

    private AccountListFragmentAdapter mAdapter;

    private OnFragmentInteractionListener mListener;

    private TextView mAccountListStartText;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AccountListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AccountListFragment newInstance() {
        AccountListFragment fragment = new AccountListFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
        return fragment;
    }

    public AccountListFragment() {
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

//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }

        prepareAccountActionGrid();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_account_list, container, false);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.accountsList);
        mAccountListStartText = (TextView) rootView.findViewById(R.id.accountsListStartText);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        integrityCheck();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);

        mAdapter = new AccountListFragmentAdapter(getActivity(), null, this, this);
//        mAdapter = new SimpleCursorAdapter(getActivity(), null);
        mRecyclerView.setAdapter(mAdapter);

        mAccountListStartText.setText(R.string.loading_accounts);
        mAccountListStartText.setVisibility(View.VISIBLE);

        getLoaderManager().initLoader(0, null, this);
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

        reloadData();
    }

    @Override
    public void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    public void reloadData() {
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(getActivity(), AccountsContract.CONTENT_URI,
                null, null, null, null);

        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader, so we don't care about the ID.
//        String select = "";
//        if (MyPreferences.isHideClosedAccounts(mActivity)){
//            select = "(" + AccountsContract.IS_ACTIVE + "=1)";
//        }
//
//        MyPreferences.AccountSortOrder sortOrder = MyPreferences.getAccountSortOrder(mActivity);
//        EntityDefinition ed = EntityManager.getEntityDefinitionOrThrow(Account.class);
//        String colName = ed.getFieldInfo(sortOrder.property).columnName;
//
//        String sort = AccountsContract.IS_ACTIVE + " DESC, " + colName;
//        if (sortOrder.asc) {
//            sort = sort + " ASC, ";
//        } else {
//            sort = sort + " DESC, ";
//        }
//        sort = sort + AccountsContract.TITLE + " ASC";
//        return new CursorLoader(getActivity(), AccountsContract.CONTENT_URI,
//                AccountsContract.PROJECTION_ALL, select, null, sort);
    }

    @Override
    public void onLoadFinished(Loader loader, Cursor cursor) {
        if (cursor.getCount() == 0){
            mAccountListStartText.setText(R.string.no_accounts);
            mAccountListStartText.setVisibility(View.VISIBLE);
        } else {
            mAccountListStartText.setVisibility(View.GONE);
        }

        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    @Override
    public void OnAccountClick(long accountId, View v) {
        if (isQuickMenuEnabledForAccount(mActivity)) {
            selectedId = accountId;
            accountActionGrid.show(v);
        } else {
            showAccountTransactions(accountId);
        }
    }

    @Override
    public boolean OnAccountLongClick(long accountId, View v) {
        //TODO: context menu
        selectedId = accountId;
        return false;
    }

    protected void prepareAccountActionGrid() {
        if (isGreenDroidSupported()) {
            accountActionGrid = new QuickActionGrid(mActivity);
            accountActionGrid.addQuickAction(new MyQuickAction(mActivity, R.drawable.gd_action_bar_info, R.string.info));
            accountActionGrid.addQuickAction(new MyQuickAction(mActivity, R.drawable.gd_action_bar_list, R.string.blotter));
            accountActionGrid.addQuickAction(new MyQuickAction(mActivity, R.drawable.gd_action_bar_edit, R.string.edit));
            accountActionGrid.addQuickAction(new MyQuickAction(mActivity, R.drawable.ic_input_add, R.string.transaction));
            accountActionGrid.addQuickAction(new MyQuickAction(mActivity, R.drawable.ic_input_transfer, R.string.transfer));
            accountActionGrid.addQuickAction(new MyQuickAction(mActivity, R.drawable.ic_action_bar_mark, R.string.balance));
            accountActionGrid.setOnQuickActionClickListener(accountActionListener);
        }
    }

    private QuickActionWidget.OnQuickActionClickListener accountActionListener = new QuickActionWidget.OnQuickActionClickListener() {
        public void onQuickActionClicked(QuickActionWidget widget, int position) {
            switch (position) {
                case 0:
                    showAccountInfo(selectedId);
                    break;
                case 1:
                    showAccountTransactions(selectedId);
                    break;
                case 2:
                    editAccount(selectedId);
                    break;
                case 3:
                    addTransaction(selectedId, TransactionActivity.class);
                    break;
                case 4:
                    addTransaction(selectedId, TransferActivity.class);
                    break;
                case 5:
                    updateAccountBalance(selectedId);
                    break;
            }
        }

    };

    private void addTransaction(long accountId, Class<? extends AbstractTransactionActivity> clazz) {
        Intent intent = new Intent(mActivity, clazz);
        intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, accountId);
        startActivityForResult(intent, VIEW_ACCOUNT_REQUEST);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.setHeaderTitle(getString(R.string.account));

        List<MenuItemInfo> menus = new LinkedList<>();
        menus.add(new MenuItemInfo(MENU_VIEW, R.string.view));
        menus.add(new MenuItemInfo(MENU_EDIT, R.string.edit));
        menus.add(new MenuItemInfo(MENU_DELETE, R.string.delete));

        Account a = em.getAccount(selectedId);
        if (a != null && a.isActive) {
            menus.add(new MenuItemInfo(MENU_UPDATE_BALANCE, R.string.update_balance));
            menus.add(new MenuItemInfo(MENU_PURGE_ACCOUNT, R.string.delete_old_transactions));
            menus.add(new MenuItemInfo(MENU_CLOSE_OPEN_ACCOUNT, R.string.close_account));
        } else {
            menus.add(new MenuItemInfo(MENU_CLOSE_OPEN_ACCOUNT, R.string.reopen_account));
        }

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
                showAccountTransactions(selectedId);
                return true;
            }
            case MENU_EDIT: {
                editAccount(selectedId);
                return true;
            }
            case MENU_DELETE: {
                deleteItem(selectedId);
                return true;
            }
            case MENU_UPDATE_BALANCE: {
                updateAccountBalance(selectedId);
                return true;
            }
            case MENU_PURGE_ACCOUNT: {
                Intent intent = new Intent(mActivity, PurgeAccountActivity.class);
                intent.putExtra(PurgeAccountActivity.ACCOUNT_ID, selectedId);
                startActivityForResult(intent, PURGE_ACCOUNT_REQUEST);
                return true;
            }
            case MENU_CLOSE_OPEN_ACCOUNT: {
                Account a = em.getAccount(selectedId);
                a.isActive = !a.isActive;
                em.saveAccount(a);
                reloadData();
                return true;
            }
        }
        return false;
    }

    private boolean updateAccountBalance(long id) {
        Account a = em.getAccount(id);
        if (a != null) {
            Intent intent = new Intent(mActivity, TransactionActivity.class);
            intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, a.id);
            intent.putExtra(TransactionActivity.CURRENT_BALANCE_EXTRA, a.totalAmount);
            startActivityForResult(intent, 0);
            return true;
        }
        return false;
    }

    public void addItem() {
//        Intent intent = new Intent(AccountListActivity.this, AccountActivity.class);
        Intent intent = new Intent(mActivity, AccountActivity.class);
        startActivityForResult(intent, NEW_ACCOUNT_REQUEST);
    }

    protected void deleteItem(final long id) {
        new AlertDialog.Builder(mActivity)
                .setMessage(R.string.delete_account_confirm)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        db.deleteAccount(id);
                        reloadData();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void editAccount(long id) {
//        Intent intent = new Intent(AccountListActivity.this, AccountActivity.class);
        Intent intent = new Intent(mActivity, AccountActivity.class);
        intent.putExtra(AccountActivity.ACCOUNT_ID_EXTRA, id);
        startActivityForResult(intent, EDIT_ACCOUNT_REQUEST);
    }

    private void showAccountInfo(long id) {
        LayoutInflater layoutInflater = (LayoutInflater)mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        NodeInflater inflater = new NodeInflater(layoutInflater);
        AccountInfoDialog accountInfoDialog = new AccountInfoDialog(mActivity, id, db, inflater);
        accountInfoDialog.show();
    }

    private void showAccountTransactions(long id) {
        if (mListener != null) {
            mListener.onShowAccountTransactions(id);
        }
    }

    @Override
    public void integrityCheck() {
        new IntegrityCheckTask(mActivity).execute();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK || requestCode == VIEW_ACCOUNT_REQUEST || requestCode == PURGE_ACCOUNT_REQUEST) {
            reloadData();
        }
    }

}
