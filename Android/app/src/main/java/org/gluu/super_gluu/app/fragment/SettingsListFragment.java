package org.gluu.super_gluu.app.fragment;

/**
 * Created by nazaryavornytskyy on 3/23/16.
 *
 * Leaving this class in the codebase so we know how project was implementing following fragments:
 *
 * Checking to add the fingerprint option
 *
 * Purchase Fragment aka AD_FREE_FRAGMENT
 *
 * Empty fragments for some reason
 *
 */
public class SettingsListFragment {


//        //Check if device api version supports fingerprint functionality
//        int version_api = Build.VERSION.SDK_INT;
//        if (version_api > 22){
//            Map<String, Integer> item2 = new HashMap<>();
//            item2.put("Fingerprint", FragmentType.SETTINGS_FRAGMENT_TYPE.FINGERPRINT_FRAGMENT.ordinal());
//            listSettings.add(item2);
//        }

//        Map<String, Integer> item4 = new HashMap<>();
//        item4.put("", FragmentType.SETTINGS_FRAGMENT_TYPE.EMPTY_FRAGMENT.ordinal());
//        listSettings.add(item4);


//        Boolean isAdFree = Settings.getPurchase(context);
//        if (!isAdFree){
//            Map<String, Integer> item7 = new HashMap<>();
//            item7.put("Upgrade to Ad-Free", FragmentType.SETTINGS_FRAGMENT_TYPE.AD_FREE_FRAGMENT.ordinal());
//            listSettings.add(item7);
//        }

//        Map<String, Integer> item8 = new HashMap<>();
//        item8.put("", FragmentType.SETTINGS_FRAGMENT_TYPE.EMPTY_FRAGMENT.ordinal());
//        listSettings.add(item8);


    //todo go over these and see how the project previously created any settings fragments. Nav drawer covers all of these but purchase as of 3/15/18
//        view.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Map.Entry<String, Integer> entry = (Map.Entry<String, Integer>) v.getTag();
//                if (mListener != null && entry.getValue() == FragmentType.SETTINGS_FRAGMENT_TYPE.PIN_CODE_FRAGMENT.ordinal()) {
//                    mListener.onSettingsList(new SettingsPinCode());
//                } else if (mListener != null && entry.getValue() == FragmentType.SETTINGS_FRAGMENT_TYPE.FINGERPRINT_FRAGMENT.ordinal()) {
//                    mListener.onSettingsList(createSettingsFragment(SettingsFragment.Constant.FINGERPRINT_TYPE));
//                } else if (mListener != null && entry.getValue() == FragmentType.SETTINGS_FRAGMENT_TYPE.SSL_FRAGMENT.ordinal()) {
//                    mListener.onSettingsList(createSettingsFragment(SettingsFragment.Constant.SSL_CONNECTION_TYPE));
//                } else if (entry.getValue() == FragmentType.SETTINGS_FRAGMENT_TYPE.USER_GUIDE_FRAGMENT.ordinal()){
//                    Uri uri = Uri.parse(SettingsFragment.Constant.USER_GUIDE_URL);
//                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//                    activity.startActivity(intent);
//                } else if (entry.getValue() == FragmentType.SETTINGS_FRAGMENT_TYPE.PRIVACY_POLICY_FRAGMENT.ordinal()) {
//                    LicenseFragment licenseFragment = new LicenseFragment();
//                    licenseFragment.setForFirstLoading(false);
//                    mListener.onSettingsList(licenseFragment);
//                } else if (entry.getValue() == FragmentType.SETTINGS_FRAGMENT_TYPE.AD_FREE_FRAGMENT.ordinal()){
//                    PurchaseFragment purchaseFragment = new PurchaseFragment();
//                    mListener.onSettingsList(purchaseFragment);
//                }
//            }
//        });
//
//        return view;
//    }

    //    private void initIAPurchaseService(){
//        inAppPurchaseService.initInAppService(context);
//        inAppPurchaseService.setCustomEventListener(new InAppPurchaseService.OnInAppServiceListener() {
//            @Override
//            public void onSubscribed(Boolean isSubscribed) {
//                if (isSubscribed){
//                    list.remove(indexAdFree);
//                    notifyDataSetChanged();
//                }
//            }
//        });
//    }


}