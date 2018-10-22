package org.gluu.super_gluu.app.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.gluu.super_gluu.app.listener.EntrySelectedListener;

import SuperGluu.app.R;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by SamIAm on 3/27/18.
 */

public class SecureEntryFragment extends Fragment {

    @BindView(R.id.button_enter_fingerprint)
    Button enterFingerprintButton;

    @BindView(R.id.button_enter_passcode)
    Button enterPasscodeButton;

    EntrySelectedListener entrySelectedListener;

    public static SecureEntryFragment newInstance(boolean showPinCode, boolean showFingerprint) {
        SecureEntryFragment secureEntryFragment = new SecureEntryFragment();
        Bundle args = new Bundle();
        args.putBoolean(Constant.SHOW_PIN_CODE, showPinCode);
        args.putBoolean(Constant.SHOW_FINGERPRINT, showFingerprint);
        secureEntryFragment.setArguments(args);

        return secureEntryFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof EntrySelectedListener) {
            entrySelectedListener = (EntrySelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement EntrySelectedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_secure_entry, container, false);
        ButterKnife.bind(this, rootView);

        boolean showPinCode = getArguments().getBoolean(Constant.SHOW_PIN_CODE);
        boolean showFingerprint = getArguments().getBoolean(Constant.SHOW_FINGERPRINT);

        if(showPinCode) {
            enterPasscodeButton.setVisibility(View.VISIBLE);
        } else {
            enterPasscodeButton.setVisibility(View.INVISIBLE);
        }

        if(showFingerprint) {
            enterFingerprintButton.setVisibility(View.VISIBLE);
        } else {
            enterFingerprintButton.setVisibility(View.INVISIBLE);
        }

        enterPasscodeButton.setOnClickListener(view -> entrySelectedListener.onPinCodeSelected());

        enterFingerprintButton.setOnClickListener(view -> entrySelectedListener.startFingerprintAuthentication());

        return rootView;
    }

    public static class Constant {
        public static final String SHOW_PIN_CODE = "show_pin_code";
        public static final String SHOW_FINGERPRINT = "show_fingerprint";
    }


}
