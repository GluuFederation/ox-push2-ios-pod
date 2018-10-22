package org.gluu.super_gluu.app.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;

import org.gluu.super_gluu.app.NotificationType;
import org.gluu.super_gluu.app.activities.MainNavDrawerActivity;
import org.gluu.super_gluu.app.base.ToolbarFragment;
import org.gluu.super_gluu.app.customview.CustomAlert;
import org.gluu.super_gluu.u2f.v2.model.TokenEntry;
import org.gluu.super_gluu.util.Utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import SuperGluu.app.R;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by nazaryavornytskyy on 3/1/16.
 */
public class KeyHandleInfoFragment extends ToolbarFragment {

    final SimpleDateFormat isoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
    final SimpleDateFormat userDateTimeFormat = new SimpleDateFormat("MMM d, yyyy HH:mm:ss");

    private static final String ARG_PARAM1 = "tokenEntry";
    private TokenEntry tokenEntry;
    private OnDeleteKeyHandleListener mDeleteListener;

    private Activity mActivity;

    @BindView(R.id.nav_drawer_toolbar)
    Toolbar toolbar;

    private BroadcastReceiver mDeleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
//            Boolean isAdFree = intent.getBooleanExtra("isAdFree", false);
            showAlertView();
        }
    };

    public static KeyHandleInfoFragment newInstance(String tokenEntity) {
        KeyHandleInfoFragment fragment = new KeyHandleInfoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, tokenEntity);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            String tokenString = getArguments().getString(ARG_PARAM1);
            tokenEntry = new Gson().fromJson(tokenString, TokenEntry.class);
        }
        //Setup message receiver\
        if (mActivity == null){
            mActivity = getActivity();
        }
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mDeleteReceiver,
                new IntentFilter("on-delete-key-event"));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_keyhandle_info, container, false);

        ButterKnife.bind(this, rootView);
        setHasOptionsMenu(true);
        setDefaultToolbar(toolbar, getString(R.string.key_details), true);

        updateKeyHandleDetails(rootView);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDeleteKeyHandleListener) {
            mDeleteListener = (OnDeleteKeyHandleListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnDeleteKeyHandleListener");
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mDeleteReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_key_detail, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.delete_action:
                showAlertView();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateKeyHandleDetails(View view) {
        TextView keyHandle_user_name = view.findViewById(R.id.keyHandle_user_name_label_value);
        keyHandle_user_name.setText(tokenEntry.getUserName());
        TextView keyHandle_created = view.findViewById(R.id.keyHandle_created_value);
        setupPairingDateByFormat(keyHandle_created);
        TextView keyHandle_issuer = view.findViewById(R.id.keyHandle_issuer_value);
        try {
            URI uri = new URI(tokenEntry.getIssuer());
            String path = uri.getHost();
            keyHandle_issuer.setText(path);
        } catch (URISyntaxException e) {
            keyHandle_issuer.setText(tokenEntry.getIssuer());
            e.printStackTrace();
        }

        String keyStr = Utils.encodeHexString(tokenEntry.getKeyHandle());
        String keyHandleString = keyStr.substring(0, 6) + "..." + keyStr.substring(keyStr.length()-6);
        TextView keyHandle_id = view.findViewById(R.id.keyHandle_id);
        keyHandle_id.setText(keyHandleString);
    }

    void showAlertView(){
        MainNavDrawerActivity.GluuAlertCallback listener = new MainNavDrawerActivity.GluuAlertCallback(){
            @Override
            public void onPositiveButton() {
                mDeleteListener.onDeleteKeyHandle(tokenEntry);
                android.support.v4.app.FragmentManager fm = getFragmentManager();
                fm.popBackStack();
            }

            @Override
            public void onNegativeButton() {
                //Skip here
            }
        };
        CustomAlert customAlert = new CustomAlert(mActivity);
        customAlert.setHeader(getString(R.string.approve_delete));
        customAlert.setMessage(getString(R.string.delete_key_sub_title));
        customAlert.setPositiveText(getString(R.string.yes));
        customAlert.setNegativeText(getString(R.string.no));
        customAlert.setType(NotificationType.DELETE_KEY);
        customAlert.setListener(listener);
        customAlert.show();
    }

    void setupPairingDateByFormat(TextView textView){

        if (textView != null && tokenEntry.getCreatedDate() != null) {
            Date date = null;
            try {
                date = isoDateTimeFormat.parse(tokenEntry.getCreatedDate());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (date != null) {
                String pairingDate = userDateTimeFormat.format(date);
                textView.setText(pairingDate);
            }
        } else {
            textView.setText(R.string.no_date);
        }
    }

    public interface OnDeleteKeyHandleListener {
        void onDeleteKeyHandle(TokenEntry key);
    }
}
