/*
 *  oxPush2 is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 *  Copyright (c) 2014, Gluu
 */

package org.gluu.super_gluu.app.fragment;

import android.app.Activity;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.gluu.super_gluu.app.fragment.KeyFragmentListFragment;
import org.gluu.super_gluu.app.model.KeyContent.KeyItem;
import org.gluu.super_gluu.u2f.v2.model.TokenEntry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import SuperGluu.app.R;

/**
 * {@link RecyclerView.Adapter} that can display a {@link KeyItem} and makes a call to the
 * specified {@link KeyFragmentListFragment.OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class KeyRecyclerViewAdapter extends ArrayAdapter {

    final SimpleDateFormat isoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
    final SimpleDateFormat userDateTimeFormat = new SimpleDateFormat("MMM d, yyyy HH:mm:ss");

    private final Activity mActivity;
    private final List mValues;
//    private final OnListFragmentInteractionListener mListener;

    public KeyRecyclerViewAdapter(Activity activity, List items) {
        super(activity, R.layout.fragment_key, items);
        mActivity = activity;
        mValues = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            LayoutInflater inflater = mActivity.getLayoutInflater();
            view = inflater.inflate(R.layout.fragment_key, null);
        }
        TokenEntry token = (TokenEntry) mValues.get(position);
        if (token != null) {
            TextView contentView = (TextView) view.findViewById(R.id.content_text_view);

            if (contentView != null) {
                String deviceName = android.os.Build.MODEL;
                String prefixKeyHandle = view.getContext().getString(R.string.keyHandleCell);
                String keyHandleTitle = prefixKeyHandle + " ("+ position + ") " + deviceName;
                contentView.setText(keyHandleTitle);
            }
            TextView createdDate = (TextView) view.findViewById(R.id.created_dateKey);

            if (createdDate != null && token.getCreatedDate() != null) {
                Date date = null;
                try {
                    date = isoDateTimeFormat.parse(token.getCreatedDate());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if (date != null) {
                    String pairingDate = userDateTimeFormat.format(date);
                    createdDate.setText(pairingDate);
                }
            } else {
                createdDate.setText(R.string.no_date);
            }
            Typeface faceLight = Typeface.createFromAsset(mActivity.getAssets(), "ProximaNova-Regular.otf");
            contentView.setTypeface(faceLight);
            createdDate.setTypeface(faceLight);
        }

        return view;
    }

}
