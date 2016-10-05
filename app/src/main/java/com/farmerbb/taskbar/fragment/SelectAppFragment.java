package com.farmerbb.taskbar.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.farmerbb.taskbar.activity.SelectAppActivity;

public class SelectAppFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    public SelectAppFragment() {}

    public static SelectAppFragment newInstance(int sectionNumber) {
        SelectAppFragment fragment = new SelectAppFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, sectionNumber);

        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SelectAppActivity activity = (SelectAppActivity) getActivity();
        ListView appList = new ListView(activity);
        int type = getArguments().getInt(ARG_SECTION_NUMBER);

        appList.setAdapter(activity.getAppListAdapter(type));

        return appList;
    }
}
