package sk.henrichg.phoneprofilesplus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.TooltipCompat;
import androidx.preference.PreferenceDialogFragmentCompat;

public class ProfileIconPreferenceFragmentX extends PreferenceDialogFragmentCompat {

    private Context prefContext;
    private ProfileIconPreferenceX preference;

    private Button colorChooserButton;

    private ProfileIconPreferenceAdapterX adapter;

    @SuppressLint("InflateParams")
    @Override
    protected View onCreateDialogView(@NonNull Context context)
    {
        prefContext = context;
        preference = (ProfileIconPreferenceX) getPreference();
        preference.fragment = this;

        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.dialog_profileicon_preference, null, false);
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);

        GridView gridView = view.findViewById(R.id.profileicon_pref_dlg_gridview);
        adapter = new ProfileIconPreferenceAdapterX(preference, prefContext/*,
                            preference.imageIdentifier,
                            preference.isImageResourceID,
                            preference.useCustomColor,
                            preference.customColor*/);
        gridView.setAdapter(adapter);
        gridView.setSelection(ProfileIconPreferenceAdapterX.getImageResourcePosition(preference.imageIdentifier/*, prefContext*/));

        gridView.setOnItemClickListener((parent, v, position, id) -> {
            preference.setImageIdentifierAndType(ProfileIconPreferenceAdapterX.getImageResourceName(position),true);
            adapter.imageIdentifierAndTypeChanged(/*preference.imageIdentifier, preference.isImageResourceID*/);
            preference.updateIcon(true);
            colorChooserButton.setEnabled(preference.isImageResourceID);
        });

        preference.dialogIcon = view.findViewById(R.id.profileicon_pref_dlg_icon);

        colorChooserButton = view.findViewById(R.id.profileicon_pref_dlg_change_color);
        colorChooserButton.setOnClickListener(v -> showCustomColorChooser());
        colorChooserButton.setEnabled(preference.isImageResourceID);

        /*final ImageView helpIcon = layout.findViewById(R.id.profileicon_pref_dlg_helpIcon);
        helpIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogHelpPopupWindow.showPopup(helpIcon, prefContext, R.string.profileicon_pref_dialog_info_about_status_bar_icon);
            }
        });*/

        final Button customIconButton = view.findViewById(R.id.profileicon_pref_dlg_custom_icon);
        customIconButton.setOnClickListener(v -> {
            if (Permissions.grantCustomProfileIconPermissions(prefContext)) {
                preference.startGallery();
                //mDialog.dismiss();
            }
        });

        final AppCompatImageButton helpButton = view.findViewById(R.id.profileicon_pref_dlg_custom_icon_helpIcon);
        TooltipCompat.setTooltipText(helpButton, getString(R.string.profile_icon_preference_custumIconFromIconPack_help));
        helpButton.setOnClickListener(v -> {
            String helpString = getString(R.string.profile_icon_preference_custumIconFromIconPack_help_info_1) + "<br><br>" +
                    getString(R.string.profile_icon_preference_custumIconFromIconPack_help_info_2) +
                    " \"ThemeX: Extract Launcher Theme\". " +
                    getString(R.string.profile_icon_preference_custumIconFromIconPack_help_info_3) + "<br><br>" +
                    getString(R.string.profile_icon_preference_custumIconFromIconPack_help_info_4);
                    //"<a href=https://play.google.com/store/apps/details?id=com.redphx.themex>" +
                    //getString(R.string.profile_icon_preference_custumIconFromIconPack_help_info_4) + " &#8658;</a>";
            //iconPacksIfo.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT));
            //iconPacksIfo.setClickable(true);
            //iconPacksIfo.setMovementMethod(LinkMovementMethod.getInstance());
            DialogHelpPopupWindowX.showPopup(helpButton, R.string.profile_icon_preference_custumIconFromIconPack_help, (Activity)prefContext, /*getDialog(),*/ helpString, true);
        });

        preference.getValuePIDP();
        preference.updateIcon(true);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        //PPApplication.logE("ProfileIconPreferenceFragmentX.resetSummary", "xxx");

        if (positiveResult) {
            preference.persistIcon();
        }
        else {
            preference.resetSummary();
        }

        preference.fragment = null;
    }

    private void showCustomColorChooser() {
        if (getActivity() != null) {
            ProfileIconColorChooserDialogX colorDialog = new ProfileIconColorChooserDialogX((Activity) prefContext, preference);
            if (!getActivity().isFinishing())
                colorDialog.show();
        }

        /*
        ColorChooserDialog colorDialog  = new ColorChooserDialog.Builder(prefContext, R.string.colorChooser_pref_dialog_title)
                .titleSub(R.string.colorChooser_pref_dialog_title)  // title of dialog when viewing shades of a color
                .accentMode(false)  // when true, will display accent palette instead of primary palette
                .doneButton(android.R.string.ok)  // changes label of the done button
                .cancelButton(android.R.string.cancel)  // changes label of the cancel button
                .backButton(R.string.empty_string)  // changes label of the back button
                .preselect(customColor)  // optionally preselects a color
                .dynamicButtonColor(false)  // defaults to true, false will disable changing action buttons' color to currently selected color
                .build();
        colorDialog.show(); // an AppCompatActivity which implements ColorCallback
        */
    }

    void setCustomColor() {
        adapter.setCustomColor();
    }

}
