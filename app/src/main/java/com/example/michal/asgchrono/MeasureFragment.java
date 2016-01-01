package com.example.michal.asgchrono;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class MeasureFragment extends Fragment
{
    public interface MeasureFragmentInteractionListener {
        void onResetButtonClicked();
        void onSaveButtonClicked();
    }

    private MeasureFragmentInteractionListener mListener;

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
        setUpCallbacks(rootView);
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MeasureFragmentInteractionListener) {
            mListener = (MeasureFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement MeasureFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void setUpCallbacks(View view) {
        final Button debugButton = (Button) view.findViewById(R.id.button_debug);
        debugButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });

        final Button clearButton = (Button) view.findViewById(R.id.button_clear);
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mListener.onResetButtonClicked();
            }
        });

        final Button saveButton = (Button) view.findViewById(R.id.button_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mListener.onSaveButtonClicked();
            }
        });
    }
}