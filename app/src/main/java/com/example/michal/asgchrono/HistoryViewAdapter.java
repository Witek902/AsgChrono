package com.example.michal.asgchrono;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class HistoryViewAdapter extends ArrayAdapter<HistoryEntry> {

    public HistoryViewAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public HistoryViewAdapter(Context context, int resource, List<HistoryEntry> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.history_row, null);
        }

        HistoryEntry p = getItem(position);

        if (p != null) {
            TextView tt1 = (TextView) v.findViewById(R.id.textView_name);
            TextView tt2 = (TextView) v.findViewById(R.id.textView_velocity);
            TextView tt3 = (TextView) v.findViewById(R.id.textView_firerate);

            if (tt1 != null)
                tt1.setText(p.name);

            if (tt2 != null)
                tt2.setText(String.format("Velocity: %.1f ft/s", p.velocity));

            if (tt3 != null)
                tt3.setText(String.format("Fire rate: %.1f /min", p.fireRate));
        }

        return v;
    }

}

/*
public class HistoryViewAdapter extends BaseAdapter {
    Context context;

    String[] names;
    double[] velocities;
    double[] fireRates;

    private static LayoutInflater inflater = null;

    public HistoryViewAdapter(Context context, String[] names, double[] velocities, double[] fireRates) {
        this.context = context;
        this.names = names;
        this.velocities = velocities;
        this.fireRates = fireRates;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return names.length;
    }

    @Override
    public Object getItem(int position) {
        return names[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        if (vi == null)
            vi = inflater.inflate(R.layout.history_row, null);

        TextView text;
        text = (TextView) vi.findViewById(R.id.textView_name);
        text.setText(names[position]);
        text = (TextView) vi.findViewById(R.id.textView_velocity);
        text.setText(String.format("Velocity: %.1f ft/s", velocities[position]));
        text = (TextView) vi.findViewById(R.id.textView_firerate);
        text.setText(String.format("Fire rate: %.1f /min", fireRates[position]));

        return vi;
    }
}
*/