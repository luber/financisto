package ru.orangesoftware.financisto.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.datetime.DateUtils;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.CardIssuer;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.orb.EntityManager;

/**
 * Created by luberello on 27.08.15.
 */
public class AccountListFragmentAdapter extends CursorRecyclerViewAdapter<AccountListFragmentAdapter.AccountViewHolder> {
    private final Utils u;
    private final Context mContext;
    private final AccountViewHolder.IAccountListItemClicksListener onListItemClickListener;
    private final View.OnCreateContextMenuListener onCreateContextMenuListener;
    private DateFormat df;
    private boolean isShowAccountLastTransactionDate;

    public AccountListFragmentAdapter(Context context, Cursor c,
                                      AccountViewHolder.IAccountListItemClicksListener onListItemClickListener,
                                      View.OnCreateContextMenuListener onCreateContextMenuListener) {
        super(context, c);

        mContext = context;
        this.onListItemClickListener = onListItemClickListener;
        this.onCreateContextMenuListener = onCreateContextMenuListener;
        this.u = new Utils(context);
        this.df = DateUtils.getShortDateFormat(context);
        this.isShowAccountLastTransactionDate = MyPreferences.isShowAccountLastTransactionDate(context);
    }

    @Override
    public AccountViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.generic_list_item_2, parent, false);

        return new AccountViewHolder(itemView, onListItemClickListener, onCreateContextMenuListener);
    }

    @Override
    public void onBindViewHolder(AccountViewHolder holder, Cursor cursor) {
        Account a = EntityManager.loadFromCursor(cursor, Account.class);

        holder.accountId = a.id;
        holder.vCenterText.setText(a.title);

        AccountType type = AccountType.valueOf(a.type);
        if (type.isCard && a.cardIssuer != null) {
            CardIssuer cardIssuer = CardIssuer.valueOf(a.cardIssuer);
            holder.vIcon.setImageResource(cardIssuer.iconId);
        } else {
            holder.vIcon.setImageResource(type.iconId);
        }
        if (a.isActive) {
            holder.vIcon.getDrawable().mutate().setAlpha(0xFF);
            holder.vActiveIcon.setVisibility(View.INVISIBLE);
        } else {
            holder.vIcon.getDrawable().mutate().setAlpha(0x77);
            holder.vActiveIcon.setVisibility(View.VISIBLE);
        }

        StringBuilder sb = new StringBuilder();
        if (!Utils.isEmpty(a.issuer)) {
            sb.append(a.issuer);
        }
        if (!Utils.isEmpty(a.number)) {
            sb.append(" #").append(a.number);
        }
        if (sb.length() == 0) {
            sb.append(mContext.getString(type.titleId));
        }
        holder.vTopText.setText(sb.toString());

        long date = a.creationDate;
        if (isShowAccountLastTransactionDate && a.lastTransactionDate > 0) {
            date = a.lastTransactionDate;
        }
        holder.vBottomText.setText(df.format(new Date(date)));

        long amount = a.totalAmount;
        if (type == AccountType.CREDIT_CARD && a.limitAmount != 0) {
            long limitAmount = Math.abs(a.limitAmount);
            long balance = limitAmount + amount;
            long balancePercentage = 10000*balance/limitAmount;
            u.setAmountText(holder.vRightCenterText, a.currency, amount, false);
            u.setAmountText(holder.vRightText, a.currency, balance, false);
            holder.vRightCenterText.setVisibility(View.VISIBLE);
            holder.vProgressBar.setMax(10000);
            holder.vProgressBar.setProgress((int)balancePercentage);
            holder.vProgressBar.setVisibility(View.VISIBLE);
        } else {
            u.setAmountText(holder.vRightText, a.currency, amount, false);
            holder.vRightCenterText.setVisibility(View.GONE);
            holder.vProgressBar.setVisibility(View.GONE);
            holder.vProgressText.setVisibility(View.GONE);
        }
    }

    public static class AccountViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {

        private final IAccountListItemClicksListener mClicksListener;
        private final View.OnCreateContextMenuListener mOnCreateContextMenuListener;

        protected ImageView vIcon;
        protected ImageView vActiveIcon;
        protected TextView vTopText;
        protected TextView vCenterText;
        protected TextView vRightCenterText;
        protected TextView vBottomText;
        protected TextView vRightText;

        protected ProgressBar vProgressBar;
        protected TextView vProgressText;

        protected long accountId;

        public AccountViewHolder(View itemView, IAccountListItemClicksListener clicksListener,
                                 View.OnCreateContextMenuListener onCreateContextMenuListener) {
            super(itemView);

            mClicksListener = clicksListener;
            mOnCreateContextMenuListener = onCreateContextMenuListener;

            itemView.setClickable(true);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            itemView.setOnCreateContextMenuListener(mOnCreateContextMenuListener);

            vIcon = (ImageView)itemView.findViewById(R.id.icon);
            vActiveIcon = (ImageView)itemView.findViewById(R.id.active_icon);
            vTopText = (TextView)itemView.findViewById(R.id.top);
            vCenterText = (TextView)itemView.findViewById(R.id.center);
            vRightCenterText = (TextView)itemView.findViewById(R.id.right_center);
            vRightCenterText.setVisibility(View.GONE);
            vBottomText = (TextView)itemView.findViewById(R.id.bottom);
            vRightText = (TextView)itemView.findViewById(R.id.right);
            vProgressBar = (ProgressBar)itemView.findViewById(R.id.progress);
            vProgressBar.setVisibility(View.GONE);
            vProgressText = (TextView)itemView.findViewById(R.id.progress_text);
            vProgressText.setVisibility(View.GONE);
        }

        @Override
        public void onClick(View v) {
            if (mClicksListener != null)
                mClicksListener.OnAccountClick(accountId, v);
        }

        @Override
        public boolean onLongClick(View v) {
            if (mClicksListener != null)
                mClicksListener.OnAccountLongClick(accountId, v);

            return false;
        }

        public interface IAccountListItemClicksListener {
            void OnAccountClick(long accountId, View v);
            boolean OnAccountLongClick(long accountId, View v);
        }
    }
}
