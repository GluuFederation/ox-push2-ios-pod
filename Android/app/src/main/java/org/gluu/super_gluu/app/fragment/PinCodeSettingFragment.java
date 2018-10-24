package org.gluu.super_gluu.app.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.gluu.super_gluu.app.listener.EntryActivityListener;

import org.gluu.super_gluu.app.settings.Settings;

import SuperGluu.app.R;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by nazaryavornytskyy on 3/24/16.
 */
public class PinCodeSettingFragment extends Fragment {

    @BindView(R.id.yes_button_pin)
    Button yesButton;
    @BindView(R.id.no_button_pin)
    Button noButton;
    @BindView(R.id.pinCodeTitle)
    TextView textSettingsTitle;
    @BindView(R.id.pinSubCodeTitle)
    TextView textSettingsSubTitle;

    EntryActivityListener entryActivityListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_pin_code_setting, container, false);

        ButterKnife.bind(this, view);

        if (!isPin()){
            entryActivityListener.onShowPinFragment(true);
        }

        yesButton.setOnClickListener(v -> {
            entryActivityListener.onShowPinFragment(false);
        });
        noButton.setOnClickListener(v -> {
            entryActivityListener.onNavigateToMainNavDrawerActivity();
            Settings.setPinCodeEnabled(getContext(), false);
            Settings.clearPinCode(getContext());
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof EntryActivityListener) {
            entryActivityListener = (EntryActivityListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement EntryActivityListener");
        }
    }

    public boolean isPin() {
        return Settings.getPinCode(getContext()) == null;
    }
}
