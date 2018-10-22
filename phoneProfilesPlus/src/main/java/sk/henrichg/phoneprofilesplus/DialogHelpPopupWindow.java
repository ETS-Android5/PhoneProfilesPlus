package sk.henrichg.phoneprofilesplus;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.labo.kaji.relativepopupwindow.RelativePopupWindow;

class DialogHelpPopupWindow extends GuiInfoPopupWindow {

    private DialogHelpPopupWindow(Context context, String helpString) {
        super(R.layout.dialog_help_popup_window, context);

        // Disable default animation
        setAnimationStyle(0);

        TextView textView = popupView.findViewById(R.id.dialog_help_popup_window_text);
        textView.setText(helpString);
    }

    static void showPopup(ImageView helpIcon, Activity activity, String helpString) {
        DialogHelpPopupWindow popup = new DialogHelpPopupWindow(activity, helpString);

        View contentView = popup.getContentView();
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = contentView.getMeasuredWidth();
        int popupHeight = contentView.getMeasuredHeight();
        PPApplication.logE("DialogHelpPopupWindow.showPopup","popupWidth="+popupWidth);
        PPApplication.logE("DialogHelpPopupWindow.showPopup","popupHeight="+popupHeight);

        ViewGroup activityView = activity.findViewById(android.R.id.content);
        int activityHeight = activityView.getHeight();
        //int activityWidth = activityView.getWidth();
        PPApplication.logE("DialogHelpPopupWindow.showPopup","activityHeight="+activityHeight);

        //int[] activityLocation = new int[2];
        //_eventStatusView.getLocationOnScreen(location);
        //activityView.getLocationInWindow(activityLocation);

        int[] locationHelpIcon = new int[2];
        helpIcon.getLocationOnScreen(locationHelpIcon); // must be used this in dialogs.
        //helpIcon.getLocationInWindow(locationHelpIcon);
        PPApplication.logE("DialogHelpPopupWindow.showPopup","locationHelpIcon[0]="+locationHelpIcon[0]);
        PPApplication.logE("DialogHelpPopupWindow.showPopup","locationHelpIcon[1]="+locationHelpIcon[1]);

        int x = 0;
        int y = 0;

        if (locationHelpIcon[0] + helpIcon.getWidth() - popupWidth < 0)
            x = -(locationHelpIcon[0] + helpIcon.getWidth() - popupWidth);

        if ((locationHelpIcon[1] + popupHeight) > activityHeight)
            y = -(locationHelpIcon[1] - (activityHeight - popupHeight));

        PPApplication.logE("DialogHelpPopupWindow.showPopup","x="+x);
        PPApplication.logE("DialogHelpPopupWindow.showPopup","y="+y);

        popup.setClippingEnabled(false); // disabled for draw outside activity
        popup.showOnAnchor(helpIcon, RelativePopupWindow.VerticalPosition.ALIGN_TOP,
                RelativePopupWindow.HorizontalPosition.ALIGN_RIGHT, x, y, false);
    }

    static void showPopup(ImageView helpIcon, Activity activity, int helpTextResource) {
        String helpString = activity.getString(helpTextResource);
        showPopup(helpIcon, activity, helpString);
    }
}
