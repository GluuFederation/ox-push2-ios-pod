package org.gluu.super_gluu.app.fragment;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import SuperGluu.app.R;

/**
 * Created by nazaryavornytskyy on 10/13/17.
 */

public class PurchaseFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.settings_purchase, container, false);
        WebView purchaseWebView = (WebView) view.findViewById(R.id.purchase_webView);
        purchaseWebView.loadDataWithBaseURL(null, readPurchaseTxt(), "text/html", "UTF-8", null);

        Button upgrateButton = (Button) view.findViewById(R.id.upgrate_ad_free_button);
        upgrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("on-ad-free-flow");
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
            }
        });
        Button restoreButton = (Button) view.findViewById(R.id.restore_purchase_button);
        restoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("on-restore-purchase-flow");
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
            }
        });
        View actionBarView = (View) view.findViewById(R.id.actionBarSettings);
        LinearLayout action_left_button = (LinearLayout) actionBarView.findViewById(R.id.action_left_button);
        actionBarView.findViewById(R.id.action_right_button).setVisibility(View.GONE);
        action_left_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        //Setup fonts
        Typeface face = Typeface.createFromAsset(getActivity().getAssets(), "ProximaNova-Regular.otf");
        upgrateButton.setTypeface(face);
        restoreButton.setTypeface(face);

        return view;
    }

    private String readPurchaseTxt(){

        InputStream inputStream = getResources().openRawResource(R.raw.purchase_text);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int i;
        try {
            i = inputStream.read();
            while (i != -1)
            {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return byteArrayOutputStream.toString();
    }

}
