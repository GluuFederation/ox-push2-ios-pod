package org.gluu.super_gluu.app.fragment;

import android.app.Activity;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;

import org.gluu.super_gluu.app.LogState;
import org.gluu.super_gluu.app.model.LogInfo;
import org.gluu.super_gluu.app.settings.Settings;
import org.gluu.super_gluu.model.OxPush2Request;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import SuperGluu.app.R;

/**
 * Created by nazaryavornytskyy on 4/5/16.
 */
public class LogsFragmentListAdapter extends BaseAdapter {

    private List<LogInfo> list;
    private List<LogInfo> selectedLogList = new ArrayList<>();
    private LayoutInflater mInflater;
    private LogsFragment.LogInfoListener mListener;
    private Activity activity;

    public Boolean isEditingMode = false;
    public Boolean isSelectAllMode = false;

    public LogsFragmentListAdapter(Activity activity, List<LogInfo> logs, LogsFragment.LogInfoListener listener){
        this.list = logs;
        this.activity = activity;
        mInflater = LayoutInflater.from(activity);
        mListener = listener;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;
        if (view == null) {
            view = mInflater.inflate(R.layout.fragment_log, parent, false);
            holder = new ViewHolder();
            View swipeView = view.findViewById(R.id.delete_layout);
            swipeView.setTag(position);
            holder.deleteButton = swipeView.findViewById(R.id.swipe_delete_button);
            holder.showButton = swipeView.findViewById(R.id.swipe_show_button);
            holder.deleteButton.setTag(position);
            holder.showButton.setTag(position);
            holder.swipeLayout =  view.findViewById(R.id.swipe_layout);
            holder.index = position;
            final View finalView = view;
            holder.swipeLayout.addSwipeListener(new SwipeLayout.SwipeListener() {
                @Override
                public void onStartOpen(SwipeLayout layout) {

                }

                @Override
                public void onOpen(SwipeLayout layout) {
                    ViewHolder tag = (ViewHolder) finalView.getTag();
                    int position = tag.index;
                    selectedLogList.add(list.get(position));
                }

                @Override
                public void onStartClose(SwipeLayout layout) {

                }

                @Override
                public void onClose(SwipeLayout layout) {
                    ViewHolder tag = (ViewHolder) finalView.getTag();
                    int position = tag.index;
                    LogInfo checkedLogInfo = list.get(position);
                    Iterator<LogInfo> iter = selectedLogList.iterator();
                    while (iter.hasNext()) {
                        LogInfo logInf = iter.next();
                        if (checkedLogInfo.getCreatedDate().equalsIgnoreCase(logInf.getCreatedDate())){
                            iter.remove();
                        }
                    }
                }

                @Override
                public void onUpdate(SwipeLayout layout, int leftOffset, int topOffset) {

                }

                @Override
                public void onHandRelease(SwipeLayout layout, float xvel, float yvel) {

                }
            });
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        LogInfo log = list.get(position);
        TextView contentView = view.findViewById(R.id.content_text_view);
        if (log.getLogState() == LogState.ENROL_FAILED || log.getLogState() == LogState.LOGIN_FAILED
                || log.getLogState() == LogState.UNKNOWN_ERROR || log.getLogState() == LogState.LOGIN_DECLINED || log.getLogState() == LogState.ENROL_DECLINED){
            ImageView logo = view.findViewById(R.id.log_image_view);
            logo.setImageResource(R.drawable.log_row_item_red_icon);
        } else {
            ImageView logo = view.findViewById(R.id.log_image_view);
            logo.setImageResource(R.drawable.log_row_item_green_icon);
        }
        RelativeLayout log_main_view = view.findViewById(R.id.log_main_view);
        log_main_view.setTag(position);
        log_main_view.setOnClickListener(v -> {
            int position1 = (int) v.getTag();

            OxPush2Request request = oxPush2Adapter(list.get(position1));
            RequestDetailFragment requestDetailFragment =
                    RequestDetailFragment.newInstance(true, log, request, null);

            if (mListener != null) {
                Settings.setIsBackButtonVisibleForLog(activity.getApplicationContext(), true);
                mListener.onKeyHandleInfo(requestDetailFragment);
            }
        });
        String title = log.getIssuer();
            String timeAgo = (String) DateUtils.getRelativeTimeSpanString(Long.valueOf(log.getCreatedDate()), System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);
            TextView createdTime = view.findViewById(R.id.created_date_text_view);
            createdTime.setText(timeAgo);
        switch (log.getLogState()){
            case LOGIN_SUCCESS:
                title = "Logged in " + log.getIssuer();
                break;
            case ENROL_SUCCESS:
                title = "Enroll to " + log.getIssuer();
                break;
            case ENROL_DECLINED:
                title = "Declined enroll to " + log.getIssuer();
                break;
            case LOGIN_DECLINED:
                title = "Declined login to " + log.getIssuer();
                break;

            default:
                break;
        }
        contentView.setText(title);

        final String item = log.getUserName();
        if (item != null) {

            holder.deleteButton.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onDeleteLogEvent();
                }
            });
            final View finalView = view;
            holder.showButton.setOnClickListener(v -> {
                ViewHolder tag = (ViewHolder) finalView.getTag();
                int position12 = (int) tag.deleteButton.getTag();

                OxPush2Request request = oxPush2Adapter(list.get(position12));
                RequestDetailFragment requestDetailFragment =
                        RequestDetailFragment.newInstance(true, log, request, null);
                requestDetailFragment.setPush2Request(request);
                if (mListener != null) {
                    Settings.setIsBackButtonVisibleForLog(activity.getApplicationContext(), true);
                    mListener.onKeyHandleInfo(requestDetailFragment);
                }
            });
        }

        //Setup fonts
        Typeface face = Typeface.createFromAsset(activity.getAssets(), "ProximaNova-Regular.otf");
        Typeface faceTitle = Typeface.createFromAsset(activity.getAssets(), "ProximaNova-Semibold.otf");
        contentView.setTypeface(faceTitle);
        createdTime.setTypeface(face);

        //Show hide checkboxes depends on editing mode
        final CheckBox check = (CheckBox) view.findViewById(R.id.logCheckBox);
        check.setTag(position);
        check.setChecked(selectedLogList.size() > 0);
        LogInfo checkedLogInfo = list.get((Integer) check.getTag());
        for (LogInfo logInf : new ArrayList<>(selectedLogList)){
            if (isSelectAllMode){
                check.setChecked(isSelectAllMode);
            } else {
                check.setChecked(checkedLogInfo.getCreatedDate().equalsIgnoreCase(logInf.getCreatedDate()));
            }
        }
        check.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked){
                selectedLogList.add(list.get((Integer) check.getTag()));
            } else {
                LogInfo checkedLogInfo1 = list.get((Integer) check.getTag());
                Iterator<LogInfo> iter = selectedLogList.iterator();
                while (iter.hasNext()) {
                    LogInfo logInf = iter.next();
                    if (checkedLogInfo1.getCreatedDate().equalsIgnoreCase(logInf.getCreatedDate())){
                        iter.remove();
                    }
                }
            }
        });
        if (isEditingMode){
            check.setVisibility(View.VISIBLE);
        } else {
            check.setVisibility(View.GONE);
        }

        return view;
    }

    private OxPush2Request oxPush2Adapter(LogInfo logInfo) {
        OxPush2Request request = new OxPush2Request();
        request.setUserName(logInfo.getUserName());
        request.setIssuer(logInfo.getIssuer());
        request.setLocationCity(logInfo.getLocationAddress());
        request.setLocationIP(logInfo.getLocationIP());
        request.setCreated(logInfo.getCreatedDate());
        request.setMethod(logInfo.getMethod());
        return request;
    }

    public void updateResults(List<LogInfo> results) {
        list = results;
        //Triggers the list update
        notifyDataSetChanged();
    }

    public List<LogInfo> getSelectedLogList(){
        return selectedLogList;
    }

    public void selectAllLogs(){
        selectedLogList.clear();
        selectedLogList.addAll(list);
    }

    public void deSelectAllLogs(){
        selectedLogList.clear();
    }

    private class ViewHolder {
        int index;
        TextView textView;
        RelativeLayout deleteButton;
        RelativeLayout showButton;
        SwipeLayout swipeLayout;
    }
}
