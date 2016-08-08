/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.EntityListAdapter;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.model.Payee;

import java.util.List;

public class PayeeListActivity extends AbstractListActivity implements SearchView.OnQueryTextListener {
    private static final int NEW_ENTITY_REQUEST = 1;
    private static final int EDIT_ENTITY_REQUEST = 2;

    private List<Payee> payees;
    private SearchView mSearchView;

    public PayeeListActivity() {
        super(R.layout.payee_list);
    }

    @Override
    protected void internalOnCreate(Bundle savedInstanceState) {
        super.internalOnCreate(savedInstanceState);

        payees = em.getAllPayeeList();

        mSearchView=(SearchView) findViewById(R.id.payeeSearchView);
        this.getListView().setTextFilterEnabled(true);

        setupSearchView();
    }

    private void setupSearchView()
    {
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setSubmitButtonEnabled(true);
        mSearchView.setQueryHint("Search Here");
    }

    @Override
    protected void addItem() {
        Intent intent = new Intent(PayeeListActivity.this, PayeeActivity.class);
        startActivityForResult(intent, NEW_ENTITY_REQUEST);
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        return new EntityListAdapter<Payee>(this, payees);
    }

    @Override
    protected Cursor createCursor() {
        return null;
    }

    @Override
    public void reloadData() {
        payees = em.getAllPayeeList();
        @SuppressWarnings("unchecked")
        EntityListAdapter<Payee> a = (EntityListAdapter<Payee>)adapter;
        a.setEntities(payees);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            reloadData();
        }
    }

    @Override
    protected void deleteItem(View v, int position, final long id) {
        em.delete(Payee.class, id);
        reloadData();
    }

    @Override
    public void editItem(View v, int position, long id) {
        Intent intent = new Intent(PayeeListActivity.this, PayeeActivity.class);
        intent.putExtra(MyEntityActivity.ENTITY_ID_EXTRA, id);
        startActivityForResult(intent, EDIT_ENTITY_REQUEST);
    }

    @Override
    protected void viewItem(View v, int position, long id) {
        Payee e = em.load(Payee.class, id);
        Intent intent = new Intent(this, BlotterActivity.class);
        Criteria blotterFilter = createBlotterCriteria(e);
        blotterFilter.toIntent(e.title, intent);
        startActivity(intent);
    }

    @Override
    protected String getContextMenuHeaderTitle(int position) {
        return getString(R.string.payee);
    }

    private Criteria createBlotterCriteria(Payee p) {
        return Criteria.eq(BlotterFilter.PAYEE_ID, String.valueOf(p.id));
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        ListView mListView = this.getListView();
        if (TextUtils.isEmpty(newText)) {
            mListView.clearTextFilter();
        } else {
            mListView.setFilterText(newText);
        }
        return true;
    }
}
