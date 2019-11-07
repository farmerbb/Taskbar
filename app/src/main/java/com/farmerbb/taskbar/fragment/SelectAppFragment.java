/* Copyright 2016 Braden Farmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
