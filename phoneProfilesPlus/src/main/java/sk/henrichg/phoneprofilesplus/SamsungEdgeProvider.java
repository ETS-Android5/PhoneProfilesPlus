package sk.henrichg.phoneprofilesplus;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.TypedValue;
import android.widget.RemoteViews;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;

public class SamsungEdgeProvider extends SlookCocktailProvider {

    //private DataWrapper dataWrapper;

    static final String ACTION_REFRESH_EDGEPANEL = PPApplication.PACKAGE_NAME + ".REFRESH_EDGEPANEL";

    private static RemoteViews buildLayout(Context context/*, SlookCocktailManager cocktailBarManager, int appWidgetId*/)
    {
        Intent svcIntent=new Intent(context, SamsungEdgeService.class);
        svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));

        RemoteViews widget;

        boolean applicationSamsungEdgeHeader;
        boolean applicationSamsungEdgeBackgroundType;
        String applicationSamsungEdgeBackgroundColor;
        String applicationSamsungEdgeLightnessB;
        String applicationSamsungEdgeBackground;
        String applicationSamsungEdgeIconLightness;
        String applicationSamsungEdgeIconColor;
        boolean applicationSamsungEdgeCustomIconLightness;
        String applicationSamsungEdgeLightnessT;
        String applicationSamsungEdgeVerticalPosition;
        boolean applicationSamsungEdgeChangeColorsByNightMode;
        synchronized (PPApplication.applicationPreferencesMutex) {
            applicationSamsungEdgeHeader = ApplicationPreferences.applicationSamsungEdgeHeader;
            applicationSamsungEdgeBackgroundType = ApplicationPreferences.applicationSamsungEdgeBackgroundType;
            applicationSamsungEdgeBackgroundColor = ApplicationPreferences.applicationSamsungEdgeBackgroundColor;
            applicationSamsungEdgeLightnessB = ApplicationPreferences.applicationSamsungEdgeLightnessB;
            applicationSamsungEdgeBackground = ApplicationPreferences.applicationSamsungEdgeBackground;
            applicationSamsungEdgeIconLightness = ApplicationPreferences.applicationSamsungEdgeIconLightness;
            applicationSamsungEdgeIconColor = ApplicationPreferences.applicationSamsungEdgeIconColor;
            applicationSamsungEdgeCustomIconLightness = ApplicationPreferences.applicationSamsungEdgeCustomIconLightness;
            applicationSamsungEdgeLightnessT = ApplicationPreferences.applicationSamsungEdgeLightnessT;
            applicationSamsungEdgeVerticalPosition = ApplicationPreferences.applicationSamsungEdgeVerticalPosition;
            applicationSamsungEdgeChangeColorsByNightMode = ApplicationPreferences.applicationSamsungEdgeChangeColorsByNightMode;

            if (Build.VERSION.SDK_INT >= 30) {

                if (applicationSamsungEdgeChangeColorsByNightMode) {
                    int nightModeFlags =
                            context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    switch (nightModeFlags) {
                        case Configuration.UI_MODE_NIGHT_YES:
                            //applicationSamsungEdgeBackground = "75"; // opaque of backgroud = 75%
                            applicationSamsungEdgeBackgroundType = false; // background type = not color
                            //applicationSamsungEdgeBackgroundColor = String.valueOf(0x2f2f2f); // color of background
                            applicationSamsungEdgeLightnessB = "12";  // lighting  of backgroud = 12%
                            applicationSamsungEdgeLightnessT = "100"; // lightness of text = white
                            //applicationSamsungEdgeIconColor = "0"; // icon type = colorful
                            applicationSamsungEdgeIconLightness = "75";
                            break;
                        case Configuration.UI_MODE_NIGHT_NO:
                        case Configuration.UI_MODE_NIGHT_UNDEFINED:
                            //applicationSamsungEdgeBackground = "75"; // opaque of backgroud = 75%
                            applicationSamsungEdgeBackgroundType = false; // background type = not color
                            //applicationSamsungEdgeBackgroundColor = String.valueOf(0xf0f0f0); // color of background
                            applicationSamsungEdgeLightnessB = "87"; // lighting  of backgroud = 87%
                            applicationSamsungEdgeLightnessT = "0"; // lightness of text = black
                            //applicationSamsungEdgeIconColor = "0"; // icon type = colorful
                            applicationSamsungEdgeIconLightness = "62";
                            break;
                    }
                }
            }
        }

        if (applicationSamsungEdgeHeader)
        {
            switch (applicationSamsungEdgeVerticalPosition) {
                case "1":
                    widget=new RemoteViews(PPApplication.PACKAGE_NAME, R.layout.samsung_edge_center);
                    break;
                case "2":
                    widget=new RemoteViews(PPApplication.PACKAGE_NAME, R.layout.samsung_edge_bottom);
                    break;
                default:
                    widget=new RemoteViews(PPApplication.PACKAGE_NAME, R.layout.samsung_edge_top);
                    break;
            }
        }
        else
        {
            switch (applicationSamsungEdgeVerticalPosition) {
                case "1":
                    widget=new RemoteViews(PPApplication.PACKAGE_NAME, R.layout.samsung_edge_center_no_header);
                    break;
                case "2":
                    widget=new RemoteViews(PPApplication.PACKAGE_NAME, R.layout.samsung_edge_bottom_no_header);
                    break;
                default:
                    widget=new RemoteViews(PPApplication.PACKAGE_NAME, R.layout.samsung_edge_top_no_header);
                    break;
            }
        }

        // set background
        int red = 0x00;
        int green;
        int blue;
        if (applicationSamsungEdgeBackgroundType) {
            int bgColor = Integer.parseInt(applicationSamsungEdgeBackgroundColor);
            red = Color.red(bgColor);
            green = Color.green(bgColor);
            blue = Color.blue(bgColor);
        }
        else {
            //if (applicationWidgetListLightnessB.equals("0")) red = 0x00;
            if (applicationSamsungEdgeLightnessB.equals("12")) red = 0x20;
            if (applicationSamsungEdgeLightnessB.equals("25")) red = 0x40;
            if (applicationSamsungEdgeLightnessB.equals("37")) red = 0x60;
            if (applicationSamsungEdgeLightnessB.equals("50")) red = 0x80;
            if (applicationSamsungEdgeLightnessB.equals("62")) red = 0xA0;
            if (applicationSamsungEdgeLightnessB.equals("75")) red = 0xC0;
            if (applicationSamsungEdgeLightnessB.equals("87")) red = 0xE0;
            if (applicationSamsungEdgeLightnessB.equals("100")) red = 0xFF;
            green = red;
            blue = red;
        }
        int alpha = 0x80;
        if (applicationSamsungEdgeBackground.equals("0")) alpha = 0x00;
        if (applicationSamsungEdgeBackground.equals("12")) alpha = 0x20;
        if (applicationSamsungEdgeBackground.equals("25")) alpha = 0x40;
        if (applicationSamsungEdgeBackground.equals("37")) alpha = 0x60;
        //if (applicationSamsungEdgeBackground.equals("50")) alpha = 0x80;
        if (applicationSamsungEdgeBackground.equals("62")) alpha = 0xA0;
        if (applicationSamsungEdgeBackground.equals("75")) alpha = 0xC0;
        if (applicationSamsungEdgeBackground.equals("87")) alpha = 0xE0;
        if (applicationSamsungEdgeBackground.equals("100")) alpha = 0xFF;
        widget.setInt(R.id.widget_profile_list_root, "setBackgroundColor", Color.argb(alpha, red, green, blue));


        // header
        if (applicationSamsungEdgeHeader)
        {
            int monochromeValue = 0xFF;
            if (applicationSamsungEdgeIconLightness.equals("0")) monochromeValue = 0x00;
            if (applicationSamsungEdgeIconLightness.equals("12")) monochromeValue = 0x20;
            if (applicationSamsungEdgeIconLightness.equals("25")) monochromeValue = 0x40;
            if (applicationSamsungEdgeIconLightness.equals("37")) monochromeValue = 0x60;
            if (applicationSamsungEdgeIconLightness.equals("50")) monochromeValue = 0x80;
            if (applicationSamsungEdgeIconLightness.equals("62")) monochromeValue = 0xA0;
            if (applicationSamsungEdgeIconLightness.equals("75")) monochromeValue = 0xC0;
            if (applicationSamsungEdgeIconLightness.equals("87")) monochromeValue = 0xE0;
            //if (applicationWidgetListIconLightness.equals("100")) monochromeValue = 0xFF;

            DataWrapper dataWrapper = new DataWrapper(context.getApplicationContext(), false, 0, false, DataWrapper.IT_FOR_WIDGET, 0, 0f);
            Profile profile = DatabaseHandler.getInstance(dataWrapper.context).getActivatedProfile();

            //boolean fullyStarted = false;
            //if (PhoneProfilesService.getInstance() != null)
            //    fullyStarted = PhoneProfilesService.getInstance().getApplicationFullyStarted();
            //boolean fullyStarted = PPApplication.applicationFullyStarted;
            //boolean applicationPackageReplaced = PPApplication.applicationPackageReplaced;
            //if ((!fullyStarted) /*|| applicationPackageReplaced*/)
            //    profile = null;

            boolean isIconResourceID;
            String iconIdentifier;
            Spannable profileName;
            if (profile != null)
            {
                profile.generateIconBitmap(context.getApplicationContext(),
                        applicationSamsungEdgeIconColor.equals("1"),
                        monochromeValue,
                        applicationSamsungEdgeCustomIconLightness);
                isIconResourceID = profile.getIsIconResourceID();
                iconIdentifier = profile.getIconIdentifier();
                profileName = DataWrapper.getProfileNameWithManualIndicator(profile, true, "", true, true, true, dataWrapper);
            }
            else
            {
                // create empty profile and set icon resource
                profile = new Profile();
                profile._name = context.getString(R.string.profiles_header_profile_name_no_activated);
                profile._icon = Profile.PROFILE_ICON_DEFAULT+"|1|0|0";

                profile.generateIconBitmap(context.getApplicationContext(),
                        applicationSamsungEdgeIconColor.equals("1"),
                        monochromeValue,
                        applicationSamsungEdgeCustomIconLightness);
                /*profile.generatePreferencesIndicator(context,
                        applicationSamsungEdgeIconColor.equals("1"),
                        monochromeValue);*/
                isIconResourceID = profile.getIsIconResourceID();
                iconIdentifier = profile.getIconIdentifier();
                profileName = new SpannableString(profile._name);
            }
            if (isIconResourceID)
            {
                if (profile._iconBitmap != null)
                    widget.setImageViewBitmap(R.id.widget_profile_list_header_profile_icon, profile._iconBitmap);
                else {
                    //int iconResource = context.getResources().getIdentifier(iconIdentifier, "drawable", context.PPApplication.PACKAGE_NAME);
                    int iconResource = Profile.getIconResource(iconIdentifier);
                    widget.setImageViewResource(R.id.widget_profile_list_header_profile_icon, iconResource);
                }
            }
            else
            {
                widget.setImageViewBitmap(R.id.widget_profile_list_header_profile_icon, profile._iconBitmap);
            }

            red = 0xFF;
            if (applicationSamsungEdgeLightnessT.equals("0")) red = 0x00;
            if (applicationSamsungEdgeLightnessT.equals("12")) red = 0x20;
            if (applicationSamsungEdgeLightnessT.equals("25")) red = 0x40;
            if (applicationSamsungEdgeLightnessT.equals("37")) red = 0x60;
            if (applicationSamsungEdgeLightnessT.equals("50")) red = 0x80;
            if (applicationSamsungEdgeLightnessT.equals("62")) red = 0xA0;
            if (applicationSamsungEdgeLightnessT.equals("75")) red = 0xC0;
            if (applicationSamsungEdgeLightnessT.equals("87")) red = 0xE0;
            //if (applicationWidgetListLightnessT.equals("100")) red = 0xFF;
            green = red; blue = red;
            widget.setTextColor(R.id.widget_profile_list_header_profile_name, Color.argb(0xFF, red, green, blue));
            widget.setTextViewTextSize(R.id.widget_profile_list_header_profile_name, TypedValue.COMPLEX_UNIT_DIP, 15);
            widget.setTextViewText(R.id.widget_profile_list_header_profile_name, profileName);
            /*if (applicationSamsungEdgePrefIndicator)
            {
                if (profile._preferencesIndicator != null)
                    widget.setImageViewBitmap(R.id.widget_profile_list_header_profile_pref_indicator, profile._preferencesIndicator);
                else
                    widget.setImageViewResource(R.id.widget_profile_list_header_profile_pref_indicator, R.drawable.ic_empty);
            }*/

            red = 0xFF;
            if (applicationSamsungEdgeLightnessT.equals("0")) red = 0x00;
            if (applicationSamsungEdgeLightnessT.equals("12")) red = 0x20;
            if (applicationSamsungEdgeLightnessT.equals("25")) red = 0x40;
            if (applicationSamsungEdgeLightnessT.equals("37")) red = 0x60;
            if (applicationSamsungEdgeLightnessT.equals("50")) red = 0x80;
            if (applicationSamsungEdgeLightnessT.equals("62")) red = 0xA0;
            if (applicationSamsungEdgeLightnessT.equals("75")) red = 0xC0;
            if (applicationSamsungEdgeLightnessT.equals("87")) red = 0xE0;
            //if (applicationWidgetListLightnessT.equals("100")) red = 0xFF;
            //noinspection ConstantConditions
            green = red;
            //noinspection ConstantConditions
            blue = red;
            widget.setInt(R.id.widget_profile_list_header_separator, "setBackgroundColor", Color.argb(0xFF, red, green, blue));

            /*
            if (Event.getGlobalEventsRunning(context)) {
                if (applicationSamsungEdgeIconColor.equals("1")) {
                    monochromeValue = 0xFF;
                    if (applicationWidgetListIconLightness.equals("0")) monochromeValue = 0x00;
                    if (applicationWidgetListIconLightness.equals("25")) monochromeValue = 0x40;
                    if (applicationWidgetListIconLightness.equals("50")) monochromeValue = 0x80;
                    if (applicationWidgetListIconLightness.equals("75")) monochromeValue = 0xC0;
                    //if (applicationWidgetListIconLightness.equals("100")) monochromeValue = 0xFF;
                    //Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_widget_restart_events);
                    Bitmap bitmap = BitmapManipulator.getBitmapFromResource(R.drawable.ic_widget_restart_events, context);
                    bitmap = BitmapManipulator.monochromeBitmap(bitmap, monochromeValue);
                    widget.setImageViewBitmap(R.id.widget_profile_list_header_restart_events, bitmap);
                }
                else
                    widget.setImageViewResource(R.id.widget_profile_list_header_restart_events, R.drawable.ic_widget_restart_events);
            }
            */

        }
        ////////////////////////////////////////////////

        // clicks
        Intent intent = new Intent(context, EditorActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        widget.setOnClickPendingIntent(R.id.widget_profile_list_header, pendingIntent);

        /*
        if (Event.getGlobalEventsRunning(context)) {
            widget.setViewVisibility(R.id.widget_profile_list_header_restart_events, View.VISIBLE);
            Intent intentRE = new Intent(context, RestartEventsFromGUIActivity.class);
            PendingIntent pIntentRE = PendingIntent.getActivity(context, 2, intentRE, PendingIntent.FLAG_UPDATE_CURRENT);
            widget.setOnClickPendingIntent(R.id.widget_profile_list_header_restart_events, pIntentRE);
        }
        else
            widget.setViewVisibility(R.id.widget_profile_list_header_restart_events, View.GONE);
        */

        /*if (!applicationSamsungEdgeGridLayout)
            widget.setRemoteAdapter(R.id.widget_profile_list, svcIntent);
        else*/
            widget.setRemoteAdapter(R.id.widget_profile_grid, svcIntent);

        // The empty view is displayed when the collection has no items.
        // It should be in the same layout used to instantiate the RemoteViews
        // object above.
        /*if (!applicationSamsungEdgeGridLayout)
            widget.setEmptyView(R.id.widget_profile_list, R.id.widget_profiles_list_empty);
        else*/
            widget.setEmptyView(R.id.widget_profile_grid, R.id.widget_profiles_list_empty);

        Intent clickIntent=new Intent(context, BackgroundActivateProfileActivity.class);
        clickIntent.putExtra(PPApplication.EXTRA_STARTUP_SOURCE, PPApplication.STARTUP_SOURCE_WIDGET);
        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent clickPI=PendingIntent.getActivity(context, 400,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        /*if (!applicationSamsungEdgeGridLayout)
            widget.setPendingIntentTemplate(R.id.widget_profile_list, clickPI);
        else*/
            widget.setPendingIntentTemplate(R.id.widget_profile_grid, clickPI);

        return widget;
    }

    /*
    private void createProfilesDataWrapper(Context context)
    {
        if (dataWrapper == null)
        {
            dataWrapper = new DataWrapper(context.getApplicationContext(), false, 0, false);
        }
    }
    */

    private static void doOnUpdate(Context context, SlookCocktailManager cocktailBarManager, int cocktailId, boolean fromOnUpdate)
    {
        RemoteViews widget = buildLayout(context);
        try {
            cocktailBarManager.updateCocktail(cocktailId, widget);
        } catch (Exception e) {
            PPApplication.recordException(e);
        }
        if (!fromOnUpdate) {
            /*if (!ApplicationPreferences.applicationSamsungEdgeGridLayout(context))
                cocktailManager.notifyCocktailViewDataChanged(cocktailId, R.id.widget_profile_list);
            else*/
            cocktailBarManager.notifyCocktailViewDataChanged(cocktailId, R.id.widget_profile_grid);
        }
    }

    @Override
    public void onUpdate(Context context, final SlookCocktailManager cocktailManager, final int[] cocktailIds) {
        super.onUpdate(context, cocktailManager, cocktailIds);
//        PPApplication.logE("[IN_LISTENER] SamsungEdgeProvider.onUpdate", "xxx");
        if (cocktailIds.length > 0) {
            //final int[] _cocktailIds = cocktailIds;

            final Context appContext = context;
            PPApplication.startHandlerThreadWidget();
            final Handler __handler = new Handler(PPApplication.handlerThreadWidget.getLooper());
            //__handler.post(new PPHandlerThreadRunnable(context, cocktailManager) {
            __handler.post(() -> {
//                    PPApplication.logE("[IN_THREAD_HANDLER] PPApplication.startHandlerThreadWidget", "START run - from=SamsungEdgeProvider.onUpdate");

                //Context appContext= appContextWeakRef.get();
                //SlookCocktailManager cocktailManager = cocktailManagerWeakRef.get();

                //if ((appContext != null) && (cocktailManager != null)) {
                    //createProfilesDataWrapper(_context);

                    for (int cocktailId : cocktailIds) {
                        doOnUpdate(appContext, cocktailManager, cocktailId, true);
                    }

                    //if (dataWrapper != null)
                    //    dataWrapper.invalidateDataWrapper();
                    //dataWrapper = null;
                //}
            });
        }
    }

    @Override
    public void onReceive(Context context, final Intent intent) {
        super.onReceive(context, intent); // calls onUpdate, is required for widget
//        PPApplication.logE("[IN_BROADCAST] SamsungEdgeProvider.onReceive", "xxx");

        final String action = intent.getAction();

        if ((action != null) &&
                (action.equalsIgnoreCase(ACTION_REFRESH_EDGEPANEL))) {
            final SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);
            final int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(context, SamsungEdgeProvider.class));

            if ((cocktailIds != null) && (cocktailIds.length > 0)) {
                final Context appContext = context;
                PPApplication.startHandlerThreadWidget();
                final Handler __handler = new Handler(PPApplication.handlerThreadWidget.getLooper());
                //__handler.post(new PPHandlerThreadRunnable(context, cocktailManager) {
                __handler.post(() -> {
//                        PPApplication.logE("[IN_THREAD_HANDLER] PPApplication.startHandlerThreadWidget", "START run - from=SamsungEdgeProvider.onReceive");

                    //Context appContext= appContextWeakRef.get();
                    //SlookCocktailManager cocktailManager = cocktailManagerWeakRef.get();

                    //if ((appContext != null) && (cocktailManager != null)) {
                        //if (EditorActivity.doImport)
                        //    return;

                        //createProfilesDataWrapper(context);

                        for (int cocktailId : cocktailIds) {
                            doOnUpdate(appContext, cocktailManager, cocktailId, false);
                        }

                        //if (dataWrapper != null)
                        //    dataWrapper.invalidateDataWrapper();
                        //dataWrapper = null;
                    //}
                });
            }
        }
    }

    /*
    private static void updateAfterWidgetOptionsChanged(Context context, int cocktailId) {
        try {
            SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);

            doOnUpdate(context, cocktailManager, cocktailId, false);

        } catch (Exception e) {
            PPApplication.recordException(e);
        }
    }
    */

    /*
    private static void _updateWidgets(Context context) {
        try {
            SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);
            int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(context, SamsungEdgeProvider.class));

            if (cocktailIds != null) {
                for (int cocktailId : cocktailIds) {
                    updateWidget(context, cocktailId);
                }
            }
        } catch (Exception e) {
            PPApplication.recordException(e);
        }
    }
    */

    /*
    @Override
    public void onVisibilityChanged(Context context, int cocktailId, int visibility) {

    }
    */

    static void updateWidgets(final Context context/*, final boolean refresh*/) {
        //createProfilesDataWrapper(context);

        /*DataWrapper dataWrapper = new DataWrapper(context.getApplicationContext(), false, 0, false);
        Profile profile = dataWrapper.getActivatedProfileFromDB(false, false);
        //dataWrapper.getEventTimelineList(true);

        String pName;
        if (profile != null)
            pName = DataWrapper.getProfileNameWithManualIndicatorAsString(profile, true, "", true, false, false, dataWrapper);
        else
            pName = context.getString(R.string.profiles_header_profile_name_no_activated);

        if (!refresh) {
            String pNameWidget = PPApplication.prefWidgetProfileName4;

            if (!pNameWidget.isEmpty()) {
                if (pName.equals(pNameWidget)) {
                    //PPApplication.logE("SamsungEdgeProvider.onUpdate", "activated profile NOT changed");
                    return;
                }
            }
        }

        PPApplication.setWidgetProfileName(context, 4, pName);*/

//        PPApplication.logE("[LOCAL_BROADCAST_CALL] SamsungEdgeProvider.updateWidgets", "xxx");
        Intent intent3 = new Intent(ACTION_REFRESH_EDGEPANEL);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent3);

        //Intent intent = new Intent(context, SamsungEdgeProvider.class);
        //intent.setAction(ACTION_REFRESH_EDGEPANEL);
        //context.sendBroadcast(intent);
        //_updateWidgets(context);

        //if (dataWrapper != null)
        //    dataWrapper.invalidateDataWrapper();
        //dataWrapper = null;
    }

/*    private static abstract class PPHandlerThreadRunnable implements Runnable {

        final WeakReference<Context> appContextWeakRef;
        final WeakReference<SlookCocktailManager> cocktailManagerWeakRef;

        PPHandlerThreadRunnable(Context appContext,
                                       SlookCocktailManager cocktailManager) {
            this.appContextWeakRef = new WeakReference<>(appContext);
            this.cocktailManagerWeakRef = new WeakReference<>(cocktailManager);
        }

    }*/

}
