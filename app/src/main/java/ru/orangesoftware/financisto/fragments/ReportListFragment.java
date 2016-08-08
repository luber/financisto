package ru.orangesoftware.financisto.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.Report2DChartActivity;
import ru.orangesoftware.financisto.activity.ReportActivity;
import ru.orangesoftware.financisto.adapter.ReportListAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.graph.Report2DChart;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.report.Report;
import ru.orangesoftware.financisto.report.ReportType;
import ru.orangesoftware.financisto.utils.PinProtection;

/**
 * Created by luberello on 02.09.15.
 */
public class ReportListFragment extends ListFragment {
    public static final String EXTRA_REPORT_TYPE = "reportType";

    public final ReportType[] reports = new ReportType[]{
            ReportType.BY_PERIOD,
            ReportType.BY_CATEGORY,
            ReportType.BY_PAYEE,
            ReportType.BY_LOCATION,
            ReportType.BY_PROJECT,
            ReportType.BY_ACCOUNT_BY_PERIOD,
            ReportType.BY_CATEGORY_BY_PERIOD,
            ReportType.BY_PAYEE_BY_PERIOD,
            ReportType.BY_LOCATION_BY_PERIOD,
            ReportType.BY_PROJECT_BY_PERIOD
    };
    private Activity mActivity;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

//        setHasOptionsMenu(true);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_report_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setListAdapter(new ReportListAdapter(mActivity, reports));
    }

    @Override
    public void onPause() {
        super.onPause();
        PinProtection.lock(mActivity);
    }

    @Override
    public void onResume() {
        super.onResume();
        PinProtection.unlock(mActivity);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (reports[position].isConventionalBarReport()) {
            // Conventional Bars reports
            Intent intent = new Intent(mActivity, ReportActivity.class);
            intent.putExtra(EXTRA_REPORT_TYPE, reports[position].name());
            startActivity(intent);
        } else {
            // 2D Chart reports
            Intent intent = new Intent(mActivity, Report2DChartActivity.class);
            intent.putExtra(Report2DChart.REPORT_TYPE, position);
            startActivity(intent);
        }
    }

    public static Report createReport(Context context, MyEntityManager em, Bundle extras) {
        String reportTypeName = extras.getString(EXTRA_REPORT_TYPE);
        ReportType reportType = ReportType.valueOf(reportTypeName);
        Currency c = em.getHomeCurrency();
        return reportType.createReport(context, c);
    }
}
