package org.gluu.super_gluu.app.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import org.gluu.super_gluu.app.settings.Settings;
import org.gluu.super_gluu.util.PinEntryEditText;

import SuperGluu.app.R;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by nazaryavornytskyy on 3/24/16.
 */
public class PinCodeFragment extends Fragment {

    @BindView(R.id.nav_drawer_toolbar)
    Toolbar toolbar;
    @BindView(R.id.attemptsLabel)
    TextView attemptsTextView;
    @BindView(R.id.enter_passcode_text_view)
    TextView enterPasscodeTextView;

    @BindView(R.id.pin_code_edit_text)
    PinEntryEditText pinCodeEditText;

    private String initialPinCode = null;

    Context context;

    private EntryType entryType;
    private EntryLevel entryLevel = EntryLevel.ONE;

    /*
    / User can enter this screen in different instances. They can be setting a brand new code,
    / entering a code to get into the app/approve an auth request, or be changing their currently
    / set code
     */
    public enum EntryType {
        SETTING_NEW, ENTERING_NORMAL, CHANGING_CURRENT
    }

    /*
    / User can be on different levels when entering their code.
    / First level could be the user entering a new code initially.
    / Or entering their current code when attempting to change their code.
    /
    / Second level can be the user confirming the previously entered code when setting up their code.
    / Or when a user is entering a new code when changing their already set code.
    */
    public enum EntryLevel {
        ONE, TWO
    }

    public PinCodeViewListener pinCodeViewListener;

    public static PinCodeFragment newInstance(EntryType entryType) {
        PinCodeFragment pinCodeFragment = new PinCodeFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constant.ENTRY_TYPE, entryType);
        pinCodeFragment.setArguments(bundle);
        return pinCodeFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_pin_code, container, false);
        ButterKnife.bind(this, view);

        context = getContext();

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        entryType = (EntryType) getArguments().getSerializable(Constant.ENTRY_TYPE);

        if(entryType == null) {
            throw new RuntimeException(context.toString()
                    + " must provide entry type to PinCodeFragment");
        }

        if (entryType == EntryType.SETTING_NEW || entryType == EntryType.CHANGING_CURRENT) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if(actionBar != null) {
                actionBar.setTitle(getString(R.string.set_passcode));
            }
        } else {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if(actionBar != null) {
                actionBar.setTitle(getString(R.string.enter_passcode));
            }
        }

        setHasOptionsMenu(true);

        updatePinCodeView();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_pin_code_entry, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.cancel_action:
                exitScreen();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void updatePinCodeView(){
        pinCodeEditText.postDelayed(() -> {
            pinCodeEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if(imm != null) {
                imm.showSoftInput(pinCodeEditText, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 400);


        final String pinCode = Settings.getPinCode(context);

        switch (entryType) {
            case SETTING_NEW:
                enterPasscodeTextView.setText(R.string.enter_your_passcode);
                break;
            case ENTERING_NORMAL:
                enterPasscodeTextView.setText(R.string.enter_your_passcode);
                break;
            case CHANGING_CURRENT:
                enterPasscodeTextView.setText(R.string.enter_your_old_passcode);
                break;
        }

        attemptsTextView.setVisibility(View.GONE);


        pinCodeEditText.setOnPinEnteredListener(str -> {
            handlePinCodeAttempt(str.toString(), pinCode);
        });
    }

    private void handlePinCodeAttempt(String enteredPinCode, String currentPinCode) {

        switch (entryType) {
            case SETTING_NEW:
                userSettingUpPinCode(enteredPinCode);
                break;
            case ENTERING_NORMAL:
                userEnteringNormalPincode(enteredPinCode, currentPinCode);
                break;
            case CHANGING_CURRENT:
                userChangingCurrentPinCode(enteredPinCode, currentPinCode);
                break;
        }

    }

    private void userSettingUpPinCode(String enteredPinCode) {
        if(entryLevel == EntryLevel.ONE) {
            //Entering the pin code for the first time
            initialPinCode = enteredPinCode;
            pinCodeEditText.setText("");
            attemptsTextView.setVisibility(View.INVISIBLE);
            enterPasscodeTextView.setText(R.string.re_enter_your_passcode);
            entryLevel = EntryLevel.TWO;
        } else if(entryLevel == EntryLevel.TWO) {
            //Re-entering the pin code to make sure it matches first code entered
            if (enteredPinCode.equalsIgnoreCase(initialPinCode)) {
                correctPinCode(enteredPinCode, getString(R.string.correct_pin_code));
            } else {
                initialPinCode = null;
                enterPasscodeTextView.setText(R.string.enter_your_passcode);
                entryLevel = EntryLevel.ONE;
                attemptsTextView.setVisibility(View.VISIBLE);
                attemptsTextView.setText(R.string.pin_codes_dont_match);
                pinCodeEditText.setText("");
            }
        }
    }

    private void userEnteringNormalPincode(String enteredPinCode, String currentPinCode) {
        if (enteredPinCode.equalsIgnoreCase(currentPinCode)) {
            //User entered correct pin code
            attemptsTextView.setVisibility(View.VISIBLE);
            attemptsTextView.setText(R.string.correct_pin_code);

            if (pinCodeViewListener != null) {
                pinCodeViewListener.onCorrectPinCode(true);
            }
            Settings.resetCurrentPinAttempts(context);
        } else {
            //User entered wrong pin code
            wrongPinCode();
        }
    }

    private void userChangingCurrentPinCode(String enteredPinCode, String currentPinCode) {
        if(entryLevel == EntryLevel.ONE) {
            //User is entering their old passcode before they can set their new passcode
            if (enteredPinCode.equalsIgnoreCase(currentPinCode)) {
                //User entered correct pin code
                attemptsTextView.setVisibility(View.GONE);
                enterPasscodeTextView.setText(R.string.enter_new_passcode);

                if (entryLevel == EntryLevel.ONE) {
                    pinCodeEditText.setText("");

                    entryLevel = EntryLevel.TWO;
                }
                Settings.resetCurrentPinAttempts(context);
            } else {
                //User entered incorrect pin code
                wrongPinCode();
            }
        } else if(entryLevel == EntryLevel.TWO) {
            //User already entered old passcode and are setting their new passcode
            correctPinCode(enteredPinCode, getString(R.string.code_changed));
        }
    }


    @SuppressLint("DefaultLocale")
    private String getAttemptsLeftText(int attempts) {
        if(attempts > 1) {
            return String.format(getString(R.string.multiple_attempts_left), attempts);
        } else {
            return String.format(getString(R.string.single_attempt_left), attempts);
        }
    }

    private void setNewPin(String passcode){
        Settings.savePinCode(context, passcode);
        Settings.setPinCodeEnabled(context, true);
        pinCodeViewListener.onCorrectPinCode(true);
        Settings.resetCurrentPinAttempts(context);

        //Hide keyboard
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if(inputMethodManager != null) {
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
        }

        getActivity().onBackPressed();
    }

    private void correctPinCode(String passcode, String correctText) {
        setNewPin(passcode);
        attemptsTextView.setVisibility(View.VISIBLE);
        attemptsTextView.setText(correctText);
    }

    private void wrongPinCode(){
        pinCodeEditText.setText("");
        attemptsTextView.setVisibility(View.VISIBLE);

        increaseAttempts();

        int attempts = Settings.getCurrentPinCodeAttempts(context);
        String attemptsText = getAttemptsLeftText(attempts);
        attemptsTextView.setText(attemptsText);

        pinCodeEditText.setText("");
        if (attempts <= 0) {
            Settings.resetCurrentPinAttempts(context);
            if (entryType == EntryType.CHANGING_CURRENT) {
                InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                if(inputMethodManager != null) {
                    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
                }

                getActivity().onBackPressed();

            } else {
                Settings.setAppLocked(context, true);
                Settings.setAppLockedTime(context);
                pinCodeViewListener.onCorrectPinCode(false);
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof PinCodeViewListener) {
            pinCodeViewListener = (PinCodeViewListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement PinCodeViewListener");
        }
    }

    public void increaseAttempts(){
        int attempts = Settings.getCurrentPinCodeAttempts(context);
        attempts--;
        Settings.setCurrentPinCodeAttempts(context, attempts);
    }

    private void exitScreen() {
        Settings.saveIsReset(context);
        pinCodeViewListener.onCancel(entryType);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public interface PinCodeViewListener{
        void onCorrectPinCode(boolean isPinCodeCorrect);
        void onCancel(EntryType entryType);
    }

    public class Constant {
        public static final String ENTRY_TYPE = "entry_type";
    }
}
