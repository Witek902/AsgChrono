package com.example.michal.asgchrono;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MeasureFragment extends Fragment
{
    public MeasureFragment() {}


    public static MeasureFragment newInstance()
    {
        MeasureFragment fragment = new MeasureFragment();
        fragment.setArguments(null);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        return rootView;
    }
}