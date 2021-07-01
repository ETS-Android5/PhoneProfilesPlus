package sk.henrichg.phoneprofilesplus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class CheckGitHubReleasesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);

//        PPApplication.logE("[BACKGROUND_ACTIVITY] CheckGitHubReleasesActivity.onCreate", "xxx");
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        // set theme and language for dialog alert ;-)
        GlobalGUIRoutines.setTheme(this, true, false/*, false*/, false, false);
        //GlobalGUIRoutines.setLanguage(this);

        showDialog(this, false, R.id.menu_check_in_github);
    }

    @Override
    public void finish()
    {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @SuppressLint({"SetTextI18n", "InflateParams"})
    static void showDialog(final Activity activity, final boolean fromEditor, final int store) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setTitle(R.string.menu_check_github_releases);
        String message;
        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(PPApplication.PACKAGE_NAME, 0);
            message = activity.getString(R.string.check_github_releases_actual_version) + " " + pInfo.versionName + " (" + PPApplication.getVersionCode(pInfo) + ")";//\n";
        } catch (Exception e) {
            message = "";
        }
        //message = message + activity.getString(R.string.about_application_package_type_github);

        if (store == R.id.menu_check_in_github) {
            message = message + "\n\n";
            message = message + activity.getString(R.string.check_github_releases_install_info_1) + "\n";
            message = message + activity.getString(R.string.check_github_releases_install_info_2) + " ";
            message = message + activity.getString(R.string.event_preferences_PPPExtenderInstallInfo_summary_3);
        }

        View layout;
        LayoutInflater inflater = activity.getLayoutInflater();
        if (store == R.id.menu_check_in_fdroid)
            layout = inflater.inflate(R.layout.dialog_for_fdroid, null);
        else
        if (store == R.id.menu_check_in_galaxy_store)
            layout = inflater.inflate(R.layout.dialog_for_galaxy_store, null);
        else
            layout = inflater.inflate(R.layout.dialog_install_extender, null);
        dialogBuilder.setView(layout);

        TextView text;
        if (store == R.id.menu_check_in_fdroid)
            text = layout.findViewById(R.id.dialog_for_fdroid_info_text);
        else
        if (store == R.id.menu_check_in_galaxy_store)
            text = layout.findViewById(R.id.dialog_for_galaxy_store_info_text);
        else
            text = layout.findViewById(R.id.install_extender_dialog_info_text);
        text.setText(message);

        if (store == R.id.menu_check_in_fdroid) {

            text = layout.findViewById(R.id.dialog_for_fdroid_fdroid_application);
            CharSequence str1 = activity.getString(R.string.check_releases_fdroid_application);
            CharSequence str2 = str1 + " " + PPApplication.FDROID_APPLICATION_URL;
            Spannable sbt = new SpannableString(str2);
            sbt.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), 0, str1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void updateDrawState(TextPaint ds) {
                    ds.setColor(ds.linkColor);    // you can use custom color
                    ds.setUnderlineText(false);    // this remove the underline
                }

                @Override
                public void onClick(@NonNull View textView) {
                    String url = PPApplication.FDROID_APPLICATION_URL;
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    try {
                        activity.startActivity(Intent.createChooser(i, activity.getString(R.string.web_browser_chooser)));
                    } catch (Exception e) {
                        PPApplication.recordException(e);
                    }
                }
            };
            sbt.setSpan(clickableSpan, str1.length()+1, str2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //sbt.setSpan(new UnderlineSpan(), str1.length()+1, str2.length(), 0);
            text.setText(sbt);
            text.setMovementMethod(LinkMovementMethod.getInstance());

            text = layout.findViewById(R.id.dialog_for_fdroid_repository_with_ppp);
            str1 = activity.getString(R.string.check_releases_fdroid_repository_with_ppp);
            str2 = str1 + " " + PPApplication.FDROID_REPOSITORY_URL;
            sbt = new SpannableString(str2);
            sbt.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), 0, str1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            clickableSpan = new ClickableSpan() {
                @Override
                public void updateDrawState(TextPaint ds) {
                    ds.setColor(ds.linkColor);    // you can use custom color
                    ds.setUnderlineText(false);    // this remove the underline
                }

                @Override
                public void onClick(@NonNull View textView) {
                    String url = PPApplication.FDROID_REPOSITORY_URL;
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    try {
                        activity.startActivity(Intent.createChooser(i, activity.getString(R.string.web_browser_chooser)));
                    } catch (Exception e) {
                        PPApplication.recordException(e);
                    }
                }
            };
            sbt.setSpan(clickableSpan, str1.length()+1, str2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //sbt.setSpan(new UnderlineSpan(), str1.length()+1, str2.length(), 0);
            text.setText(sbt);
            text.setMovementMethod(LinkMovementMethod.getInstance());

        }
        else
        if (store == R.id.menu_check_in_github) {
            Button button = layout.findViewById(R.id.install_extender_dialog_showAssets);
            button.setText(activity.getString(R.string.install_extender_where_is_assets_button) + " \"Assets\"?");
            button.setOnClickListener(v -> {
                Intent intent = new Intent(activity, GitHubAssetsScreenshotActivity.class);
                intent.putExtra(GitHubAssetsScreenshotActivity.EXTRA_IMAGE, R.drawable.phoneprofilesplus_assets_screenshot);
                activity.startActivity(intent);
            });
        }

        //dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);
        dialogBuilder.setCancelable(true);
        if (store == R.id.menu_check_in_fdroid) {
            dialogBuilder.setPositiveButton(R.string.check_releases_go_to_fdroid, (dialog, which) -> {
                String url = PPApplication.FDROID_PPP_RELEASES_URL;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                try {
                    activity.startActivity(Intent.createChooser(i, activity.getString(R.string.web_browser_chooser)));
                } catch (Exception e) {
                    PPApplication.recordException(e);
                }
                if (!fromEditor)
                    activity.finish();
            });
        }
        else
        if (store == R.id.menu_check_in_galaxy_store) {
            dialogBuilder.setPositiveButton(R.string.check_releases_open_galaxy_store, (dialog, which) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("samsungapps://ProductDetail/sk.henrichg.phoneprofilesplus"));
                try {
                    activity.startActivity(intent);
                } catch (Exception e) {
                    PPApplication.recordException(e);
                }
                if (!fromEditor)
                    activity.finish();
            });
        }
        else {
            dialogBuilder.setPositiveButton(R.string.check_github_releases_go_to_github, (dialog, which) -> {
                String url = PPApplication.GITHUB_PPP_RELEASES_URL;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                try {
                    activity.startActivity(Intent.createChooser(i, activity.getString(R.string.web_browser_chooser)));
                } catch (Exception e) {
                    PPApplication.recordException(e);
                }
                if (!fromEditor)
                    activity.finish();
            });
        }
        dialogBuilder.setNegativeButton(android.R.string.cancel, null);
        dialogBuilder.setOnCancelListener(dialog -> {
            if (!fromEditor)
                activity.finish();
        });
        if (!fromEditor)
            dialogBuilder.setOnDismissListener(dialog -> activity.finish());
        AlertDialog dialog = dialogBuilder.create();

//        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
//            @Override
//            public void onShow(DialogInterface dialog) {
//                Button positive = ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE);
//                if (positive != null) positive.setAllCaps(false);
//                Button negative = ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
//                if (negative != null) negative.setAllCaps(false);
//            }
//        });

        if (!activity.isFinishing())
            dialog.show();
    }

}
