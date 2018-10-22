package org.gluu.super_gluu.app.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;

import org.gluu.super_gluu.app.NotificationType;
import org.gluu.super_gluu.app.base.ToolbarFragment;
import org.gluu.super_gluu.app.customview.CustomAlert;
import org.gluu.super_gluu.store.AndroidKeyDataStore;
import org.gluu.super_gluu.u2f.v2.model.TokenEntry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import SuperGluu.app.R;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by nazaryavornytskyy on 3/1/16.
 */
public class KeyFragmentListFragment extends ToolbarFragment {

    private KeyFragmentListAdapter listAdapter;
    private KeyHandleInfo mListener;
    private KeyHandleChangeName cListener;
    private AndroidKeyDataStore dataStore;
    private List<TokenEntry> listToken;

    @BindView(R.id.nav_drawer_toolbar)
    Toolbar toolbar;

    @BindView(R.id.keys_list_view)
    ListView keysListView;

    @BindView(R.id.empty_keys_text_view)
    TextView emptyKeysTextView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View rootView = inflater.inflate(R.layout.fragment_key_list, container, false);

        ButterKnife.bind(this, rootView);

        setDefaultToolbar(toolbar, getString(R.string.keys), true);
        setHasOptionsMenu(true);

        listToken = getListToken(rootView);

        setEmptyTextVisibility(listToken.size() > 0);

        mListener = new KeyHandleInfo() {
            @Override
            public void onKeyHandleInfo(KeyHandleInfoFragment infoFragment) {
                getActivity().invalidateOptionsMenu();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.main_frame_layout, infoFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }

            @Override
            public void onUpdateList(Boolean isEmptyList) {
                setEmptyTextVisibility(!isEmptyList);
            }
        };

        cListener = keyID -> {
            CustomAlert gluuAlert = new CustomAlert(getActivity());
            gluuAlert.setMessage(getString(R.string.new_key_name_title));
            gluuAlert.setHeader(getString(R.string.enter_new_key_name));
            gluuAlert.setPositiveText(getString(R.string.yes));
            gluuAlert.setNegativeText(getString(R.string.no));
            gluuAlert.setType(NotificationType.RENAME_KEY);
            gluuAlert.show();
        };

        ViewGroup header = (ViewGroup) inflater.inflate(R.layout.key_list_header, keysListView,false);
        keysListView.addHeaderView(header);

        listAdapter = new KeyFragmentListAdapter(getActivity(), listToken, mListener);
        keysListView.setAdapter(listAdapter);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkTokenList();
    }

    void checkTokenList(){
//        if (listToken.size() == 0) {
//            renameText.setVisibility(View.GONE);
//        }
    }

    List<TokenEntry> getListToken(View view){
        Context context = view.getContext();
        dataStore = new AndroidKeyDataStore(context);
        List<String> tokensString = dataStore.getTokenEntries();
        List<TokenEntry> tokens = new ArrayList<TokenEntry>();
        for (String tokenString : tokensString){
            TokenEntry token = new Gson().fromJson(tokenString, TokenEntry.class);
            tokens.add(token);
        }
        //Sort keys by created date
        List<TokenEntry> tokensFromDB = new ArrayList<TokenEntry>(tokens);
        Collections.sort(tokensFromDB, (key1, key2) -> {
            SimpleDateFormat isoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
            try {
                Date date1 = isoDateTimeFormat.parse(key1.getCreatedDate());
                Date date2 = isoDateTimeFormat.parse(key2.getCreatedDate());
                return date1.compareTo(date2);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        });
        Collections.reverse(tokensFromDB);

        return tokensFromDB;
    }

    public void setEmptyTextVisibility(boolean listVisible) {
        if(listVisible) {
            emptyKeysTextView.setVisibility(View.GONE);
        } else {
            emptyKeysTextView.setVisibility(View.VISIBLE);
        }
    }

    public interface KeyHandleInfo {
        void onKeyHandleInfo(KeyHandleInfoFragment infoFragment);
        void onUpdateList(Boolean isEmtryList);
    }

    public interface KeyHandleChangeName {
        void onKeyNameChanged(String keyID);
    }

}
