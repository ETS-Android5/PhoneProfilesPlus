package sk.henrichg.phoneprofilesplus;


import android.annotation.SuppressLint;
import android.os.Build;
import android.support.v7.widget.SwitchCompat;
import android.widget.CompoundButton;
import android.widget.TextView;

class EventStatusPopupWindow extends GuiInfoPopupWindow {

    @SuppressLint("SetTextI18n")
    EventStatusPopupWindow(EditorEventListFragment fragment, Event event) {
        super(R.layout.event_status_popup_window, fragment.getActivity().getBaseContext());

        // Disable default animation
        setAnimationStyle(0);

        if (event != null) {
            final EditorEventListFragment _fragment = fragment;
            final Event _event = event;

            TextView eventName = popupView.findViewById(R.id.event_status_popup_window_text0);
            eventName.setText(fragment.getString(R.string.event_string_0)+": "+event._name);

            SwitchCompat checkBox = popupView.findViewById(R.id.event_status_popup_window_checkbox);
            checkBox.setChecked(event.getStatus() != Event.ESTATUS_STOP);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                    //noinspection ConstantConditions
                    if (_fragment != null) {
                        _fragment.runStopEvent(_event);
                    }
                }
            });
        }
    }

}
