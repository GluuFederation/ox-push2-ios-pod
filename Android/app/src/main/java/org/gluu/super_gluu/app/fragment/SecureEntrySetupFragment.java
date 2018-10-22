package org.gluu.super_gluu.app.fragment;

/**
 * Created by SamIAm on 3/21/18.
 */
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.gluu.super_gluu.app.base.ToolbarFragment;
import org.gluu.super_gluu.app.fingerprint.Fingerprint;
import org.gluu.super_gluu.app.listener.EntrySelectedListener;

import SuperGluu.app.R;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by SamIAm on 3/21/18.
 */

public class SecureEntrySetupFragment extends ToolbarFragment {

    @BindView(R.id.nav_drawer_toolbar)
    Toolbar toolbar;

    @BindView(R.id.fingerprint_relative_layout)
    RelativeLayout fingerprintRelativeLayout;

    @BindView(R.id.pin_code_relative_layout)
    RelativeLayout pinCodeRelativeLayout;

    private EntrySelectedListener secureEntryListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof EntrySelectedListener) {
            secureEntryListener = (EntrySelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement EntrySelectedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_secure_entry_setup, container, false);

        ButterKnife.bind(this, rootView);

        setDefaultToolbar(toolbar, getString(R.string.add_secure_entry), false);

        setHasOptionsMenu(true);

        fingerprintRelativeLayout.setOnClickListener(view -> secureEntryListener.setupFingerprintAuthentication());

        pinCodeRelativeLayout.setOnClickListener(view -> secureEntryListener.onPinCodeSelected());

        Fingerprint fingerprint = new Fingerprint(getContext(), false);

        if(!fingerprint.checkIfFingerprintEnabled()) {
            fingerprintRelativeLayout.setVisibility(View.GONE);
        }

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_secure_entry, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.skip_action:
                secureEntryListener.onSkipSelected();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

