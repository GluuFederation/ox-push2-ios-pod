package org.gluu.super_gluu.app.customview;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.gluu.super_gluu.app.NotificationType;
import org.gluu.super_gluu.app.activities.MainNavDrawerActivity;

import SuperGluu.app.R;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by nazaryavornytskyy on 4/15/16.
 */
public class CustomAlert extends Dialog {

    private String header, message, positiveText, negativeText;
    private MainNavDrawerActivity.GluuAlertCallback listener;
    private Boolean isTextView = false;
    private String enteredText;

    private NotificationType type = NotificationType.DEFAULT;

    @BindView(R.id.alert_message_textView)
    TextView headerTextView;
    @BindView(R.id.alert_message_subText)
    TextView messageTextView;
    @BindView(R.id.alert_textField)
    EditText alertEditText;
    @BindView(R.id.yes_button)
    Button positiveButton;
    @BindView(R.id.no_button)
    Button negativeButton;
    @BindView(R.id.actionbar_icon)
    ImageView alertImageView;
    @BindView(R.id.alert_buttons_view)
    LinearLayout alertButtonsContainer;

    public CustomAlert(Activity activity) {
        super(activity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_alert);

        ButterKnife.bind(this);

        if (header != null && !header.isEmpty()){
            headerTextView.setText(header);
        }

        if (message != null && !message.isEmpty()){
            messageTextView.setText(message);
        }

        if (positiveText != null && !positiveText.isEmpty()){
            positiveButton.setText(positiveText);
        } else {
            positiveButton.setVisibility(View.GONE);
        }

        if (negativeText != null && !negativeText.isEmpty()){
            negativeButton.setText(negativeText);
        } else {
            negativeButton.setVisibility(View.GONE);
        }
        if (positiveButton.getVisibility() == View.GONE && negativeButton.getVisibility() == View.GONE){
            positiveButton.setVisibility(View.VISIBLE);
            positiveButton.setText(R.string.ok);
        }
        if (isTextView){
            alertEditText.setVisibility(View.VISIBLE);
            alertEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)});
            alertEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    enteredText = String.valueOf(s);
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
        } else {
            alertEditText.setVisibility(View.GONE);
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) alertButtonsContainer.getLayoutParams();
            params.topMargin = 0;
            alertButtonsContainer.requestLayout();
        }


        positiveButton.setOnClickListener(view -> {
            if (listener != null){
                listener.onPositiveButton();
            }
            dismiss();
        });

        negativeButton.setOnClickListener(view -> {
            if (listener != null){
                listener.onNegativeButton();
            }
            dismiss();
        });


        switch (type) {
            case DELETE_KEY:
            case DELETE_LOG:
            case EDIT_PIN:
                alertImageView.setImageResource(R.drawable.delete_log_icon);
                break;
            case RENAME_KEY:
                alertImageView.setImageResource(R.drawable.edit_key_icon);
                break;
            default:
                alertImageView.setImageResource(R.drawable.default_alert_icon);
        }

        Window window = getWindow();
        if(window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setPositiveText(String positiveText) {
        this.positiveText = positiveText;
    }

    public void setNegativeText(String negativeText) {
        this.negativeText = negativeText;
    }

    public void setListener(MainNavDrawerActivity.GluuAlertCallback listener) {
        this.listener = listener;
    }

    public void setIsTextView(Boolean isTextView) {
        this.isTextView = isTextView;
    }

    public String getEnteredText() {
        return enteredText;
    }

}
