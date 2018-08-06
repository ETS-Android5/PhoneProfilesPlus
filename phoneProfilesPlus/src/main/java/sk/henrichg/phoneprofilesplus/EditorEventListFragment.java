package sk.henrichg.phoneprofilesplus;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class EditorEventListFragment extends Fragment
                                        implements OnStartDragItemListener {

    public DataWrapper activityDataWrapper;

    RecyclerView listView;
    private TextView activeProfileName;
    private ImageView activeProfileIcon;
    private Toolbar bottomToolbar;
    TextView textViewNoData;
    private LinearLayout progressBar;

    private EditorEventListAdapter eventListAdapter;
    private ItemTouchHelper itemTouchHelper;

    private WeakReference<LoadEventListAsyncTask> asyncTaskContext;

    public static final int EDIT_MODE_UNDEFINED = 0;
    public static final int EDIT_MODE_INSERT = 1;
    public static final int EDIT_MODE_DUPLICATE = 2;
    public static final int EDIT_MODE_EDIT = 3;
    public static final int EDIT_MODE_DELETE = 4;

    public static final String FILTER_TYPE_ARGUMENT = "filter_type";
    public static final String ORDER_TYPE_ARGUMENT = "order_type";
    public static final String START_TARGET_HELPS_ARGUMENT = "start_target_helps";

    public static final int FILTER_TYPE_ALL = 0;
    public static final int FILTER_TYPE_RUNNING = 1;
    public static final int FILTER_TYPE_PAUSED = 2;
    public static final int FILTER_TYPE_STOPPED = 3;
    public static final int FILTER_TYPE_START_ORDER = 4;

    public static final int ORDER_TYPE_START_ORDER = 0;
    public static final int ORDER_TYPE_EVENT_NAME = 1;
    public static final int ORDER_TYPE_PROFILE_NAME = 2;
    public static final int ORDER_TYPE_PRIORITY = 3;

    public boolean targetHelpsSequenceStarted;
    public static final String PREF_START_TARGET_HELPS = "editor_event_list_fragment_start_target_helps";

    private int filterType = FILTER_TYPE_ALL;
    private int orderType = ORDER_TYPE_EVENT_NAME;

    /**
     * The fragment's current callback objects
     */
    private OnStartEventPreferences onStartEventPreferencesCallback = sDummyOnStartEventPreferencesCallback;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified.
     */
    // invoked when start profile preference fragment/activity needed
    interface OnStartEventPreferences {
        void onStartEventPreferences(Event event, int editMode, int predefinedEventIndex/*, boolean startTargetHelps*/);
    }

    /**
     * A dummy implementation of the Callbacks interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static final OnStartEventPreferences sDummyOnStartEventPreferencesCallback = new OnStartEventPreferences() {
        public void onStartEventPreferences(Event event, int editMode, int predefinedEventIndex/*, boolean startTargetHelps*/) {
        }
    };

    public EditorEventListFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof OnStartEventPreferences)) {
            throw new IllegalStateException(
                    "Activity must implement fragment's callbacks.");
        }
        onStartEventPreferencesCallback = (OnStartEventPreferences) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        onStartEventPreferencesCallback = sDummyOnStartEventPreferencesCallback;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // this is really important in order to save the state across screen
        // configuration changes for example
        setRetainInstance(true);

        filterType = getArguments() != null ? 
                getArguments().getInt(FILTER_TYPE_ARGUMENT, EditorEventListFragment.FILTER_TYPE_ALL) :
                    EditorEventListFragment.FILTER_TYPE_ALL;
        orderType = getArguments() != null ? 
                getArguments().getInt(ORDER_TYPE_ARGUMENT, EditorEventListFragment.ORDER_TYPE_EVENT_NAME) :
                    EditorEventListFragment.ORDER_TYPE_EVENT_NAME;

        //Log.d("EditorEventListFragment.onCreate","filterType="+filterType);
        //Log.d("EditorEventListFragment.onCreate","orderType="+orderType);

        activityDataWrapper = new DataWrapper(getActivity().getApplicationContext(), false, 0);

        getActivity().getIntent();

        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView;

        //rootView = inflater.inflate(R.layout.editor_event_list, container, false);
        if (ApplicationPreferences.applicationEditorPrefIndicator(activityDataWrapper.context) && ApplicationPreferences.applicationEditorHeader(activityDataWrapper.context))
            rootView = inflater.inflate(R.layout.editor_event_list, container, false);
        else
        if (ApplicationPreferences.applicationEditorHeader(activityDataWrapper.context))
            rootView = inflater.inflate(R.layout.editor_event_list_no_indicator, container, false);
        else
            rootView = inflater.inflate(R.layout.editor_event_list_no_header, container, false);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        doOnViewCreated(view/*, savedInstanceState*/);

        boolean startTargetHelps = getArguments() != null && getArguments().getBoolean(START_TARGET_HELPS_ARGUMENT, false);
        if (startTargetHelps)
            showTargetHelps();
    }

    private void doOnViewCreated(View view/*, Bundle savedInstanceState*/)
    {
        //super.onActivityCreated(savedInstanceState);

        activeProfileName = view.findViewById(R.id.activated_profile_name);
        activeProfileIcon = view.findViewById(R.id.activated_profile_icon);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        listView = view.findViewById(R.id.editor_events_list);
        listView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        listView.setLayoutManager(layoutManager);
        listView.setHasFixedSize(true);
        textViewNoData = view.findViewById(R.id.editor_events_list_empty);
        progressBar = view.findViewById(R.id.editor_events_list_linla_progress);

        /*
        View footerView =  ((LayoutInflater)getActivity().getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.editor_list_footer, null, false);
        listView.addFooterView(footerView, null, false);
        */

        final Activity activity = getActivity();
        final EditorEventListFragment fragment = this;

        bottomToolbar = getActivity().findViewById(R.id.editor_list_bottom_bar);
        Menu menu = bottomToolbar.getMenu();
        if (menu != null) menu.clear();
        bottomToolbar.inflateMenu(R.menu.editor_events_bottom_bar);
        bottomToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_add_event:
                        if (eventListAdapter != null) {
                            ((EditorProfilesActivity) getActivity()).addEventDialog = new AddEventDialog(activity, fragment);
                            ((EditorProfilesActivity) getActivity()).addEventDialog.show();
                        }
                        return true;
                    case R.id.menu_delete_all_events:
                        deleteAllEvents();
                        return true;
                    case R.id.menu_default_profile:
                        Intent intent = new Intent(getActivity(), PhoneProfilesPreferencesActivity.class);
                        intent.putExtra(PhoneProfilesPreferencesActivity.EXTRA_SCROLL_TO, "profileActivationCategory");
                        startActivity(intent);
                        return true;
                }
                return false;
            }
        });

        LinearLayout orderLayout = getActivity().findViewById(R.id.editor_list_bottom_bar_order_root);
        if (filterType == EditorEventListFragment.FILTER_TYPE_START_ORDER)
            orderLayout.setVisibility(View.GONE);
        else
            orderLayout.setVisibility(View.VISIBLE);

        synchronized (activityDataWrapper.eventList) {
            if (!activityDataWrapper.eventListFilled) {
                LoadEventListAsyncTask asyncTask = new LoadEventListAsyncTask(this, filterType, orderType);
                this.asyncTaskContext = new WeakReference<>(asyncTask);
                asyncTask.execute();
            } else {
                listView.setAdapter(eventListAdapter);
                synchronized (activityDataWrapper.profileList) {
                    Profile profile = activityDataWrapper.getActivatedProfileFromDB(true,
                            ApplicationPreferences.applicationEditorPrefIndicator(activityDataWrapper.context));
                    updateHeader(profile);
                }
            }
        }
    }

    private static class LoadEventListAsyncTask extends AsyncTask<Void, Void, Void> {

        private final WeakReference<EditorEventListFragment> fragmentWeakRef;
        private final DataWrapper _dataWrapper;
        private final int _filterType;
        private final int _orderType;

        private LoadEventListAsyncTask (EditorEventListFragment fragment, int filterType, int orderType) {
            fragmentWeakRef = new WeakReference<>(fragment);
            _filterType = filterType;
            _orderType = orderType;
            _dataWrapper = new DataWrapper(fragment.getActivity().getApplicationContext(), false, 0);
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

            EditorEventListFragment fragment = this.fragmentWeakRef.get();

            if ((fragment != null) && (fragment.isAdded())) {
                fragment.textViewNoData.setVisibility(View.GONE);
                fragment.progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            _dataWrapper.fillProfileList(true, ApplicationPreferences.applicationEditorPrefIndicator(_dataWrapper.context));
            _dataWrapper.fillEventList();
            //Log.d("EditorEventListFragment.LoadEventListAsyncTask","filterType="+filterType);
            if (_filterType == FILTER_TYPE_START_ORDER)
                EditorEventListFragment.sortList(_dataWrapper.eventList, ORDER_TYPE_START_ORDER, _dataWrapper);
            else
                EditorEventListFragment.sortList(_dataWrapper.eventList, _orderType, _dataWrapper);

            return null;
        }

        @Override
        protected void onPostExecute(Void response) {
            super.onPostExecute(response);
            
            EditorEventListFragment fragment = fragmentWeakRef.get();
            
            if ((fragment != null) && (fragment.isAdded())) {
                fragment.progressBar.setVisibility(View.GONE);

                // get local profileList
                _dataWrapper.fillProfileList(true, ApplicationPreferences.applicationEditorPrefIndicator(_dataWrapper.context));
                // set local profile list into activity dataWrapper
                fragment.activityDataWrapper.copyProfileList(_dataWrapper);

                // get local eventList
                _dataWrapper.fillEventList();
                // set local event list into activity dataWrapper
                fragment.activityDataWrapper.copyEventList(_dataWrapper);

                fragment.eventListAdapter = new EditorEventListAdapter(fragment, fragment.activityDataWrapper, _filterType, fragment);

                // added touch helper for drag and drop items
                ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(fragment.eventListAdapter, false, false);
                fragment.itemTouchHelper = new ItemTouchHelper(callback);
                fragment.itemTouchHelper.attachToRecyclerView(fragment.listView);

                fragment.listView.setAdapter(fragment.eventListAdapter);

                Profile profile = fragment.activityDataWrapper.getActivatedProfileFromDB(true,
                        ApplicationPreferences.applicationEditorPrefIndicator(_dataWrapper.context));
                fragment.updateHeader(profile);

            }
        }
    }

    boolean isAsyncTaskPendingOrRunning() {
        try {
            return this.asyncTaskContext != null &&
                    this.asyncTaskContext.get() != null &&
                    !this.asyncTaskContext.get().getStatus().equals(AsyncTask.Status.FINISHED);
        } catch (Exception e) {
            return false;
        }
    }

    void stopRunningAsyncTask() {
        this.asyncTaskContext.get().cancel(true);
    }

    @Override
    public void onDestroy()
    {
        if (isAsyncTaskPendingOrRunning()) {
            stopRunningAsyncTask();
        }

        if (listView != null)
            listView.setAdapter(null);
        if (eventListAdapter != null)
            eventListAdapter.release();

        if (activityDataWrapper != null)
            activityDataWrapper.invalidateDataWrapper();
        activityDataWrapper = null;

        super.onDestroy();

    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
    }

    public void startEventPreferencesActivity(Event event, int predefinedEventIndex)
    {
        int editMode;

        if (event != null)
        {
            // edit event
            int eventPos = eventListAdapter.getItemPosition(event);
            /*int last = listView.getLastVisiblePosition();
            int first = listView.getFirstVisiblePosition();
            if ((eventPos <= first) || (eventPos >= last)) {
                listView.setSelection(eventPos);
            }*/
            listView.getLayoutManager().scrollToPosition(eventPos);

            boolean startTargetHelps = getArguments() != null && getArguments().getBoolean(START_TARGET_HELPS_ARGUMENT, false);
            if (startTargetHelps)
                showAdapterTargetHelps();

            editMode = EDIT_MODE_EDIT;
        }
        else
        {
            // add new event
            editMode = EDIT_MODE_INSERT;

        }

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) one must start profile preferences
        onStartEventPreferencesCallback.onStartEventPreferences(event, editMode, predefinedEventIndex);
    }

    void runStopEvent(final Event event)
    {
        if (Event.getGlobalEventsRunning(activityDataWrapper.context)) {
            // events are not globally stopped

            List<EventTimeline> eventTimelineList = activityDataWrapper.getEventTimelineList();
            if (event.getStatusFromDB(activityDataWrapper.context) == Event.ESTATUS_STOP) {
                // pause event
                // not needed to use handlerThread, profile is not activated (activateReturnProfile=false)
                event.pauseEvent(activityDataWrapper, eventTimelineList, false, false,
                        false, /*false,*/ null, false);
            } else {
                // stop event
                // not needed to use handlerThread, profile is not activated (activateReturnProfile=false)
                event.stopEvent(activityDataWrapper, eventTimelineList, false, false,
                        true/*, false*/); // activate return profile
            }

            // redraw event list
            updateListView(event, false, false, true);

            // restart events
            PPApplication.logE("$$$ restartEvents","from EditorEventListFragment.runStopEvent");
            activityDataWrapper.restartEvents(false, true/*, false*/, true, true);

            Intent serviceIntent = new Intent(activityDataWrapper.context, PhoneProfilesService.class);
            serviceIntent.putExtra(PhoneProfilesService.EXTRA_REREGISTER_RECEIVERS_AND_JOBS, true);
            serviceIntent.putExtra(PhoneProfilesService.EXTRA_ONLY_START, false);
            PPApplication.startPPService(activityDataWrapper.context, serviceIntent);
        }
        else
        {
            if (event.getStatusFromDB(activityDataWrapper.context) == Event.ESTATUS_STOP) {
                // pause event
                event.setStatus(Event.ESTATUS_PAUSE);
            } else {
                // stop event
                event.setStatus(Event.ESTATUS_STOP);
            }

            // update event in DB
            DatabaseHandler.getInstance(activityDataWrapper.context).updateEvent(event);
            // redraw event list
            updateListView(event, false, false, true);
        }
    }

    private void duplicateEvent(Event origEvent)
    {
        /*
        Event newEvent = new Event(
                   origEvent._name+"_d",
                   origEvent._type,
                   origEvent._fkProfile,
                   origEvent._status
                    );
        newEvent.copyEventPreferences(origEvent);

        // add event into db and set id and order
        databaseHandler.addEvent(newEvent);
        // add event into listview
        eventListAdapter.addItem(newEvent, false);

        updateListView(newEvent, false);

        startEventPreferencesActivity(newEvent);
        */

        int editMode;

        // duplicate event
        editMode = EDIT_MODE_DUPLICATE;

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) one must start profile preferences
        onStartEventPreferencesCallback.onStartEventPreferences(origEvent, editMode, 0);

    }

    private void deleteEvent(final Event event)
    {
        if (activityDataWrapper.getEventById(event._id) == null)
            // event not exists
            return;

        activityDataWrapper.addActivityLog(DatabaseHandler.ALTYPE_EVENTDELETED, event._name, null, null, 0);

        listView.getRecycledViewPool().clear();

        eventListAdapter.deleteItemNoNotify(event);
        DatabaseHandler.getInstance(activityDataWrapper.context).deleteEvent(event);

        // restart events
        PPApplication.logE("$$$ restartEvents", "from EditorEventListFragment.deleteEvent");
        activityDataWrapper.restartEvents(false, true/*, false*/, true, true);

        eventListAdapter.notifyDataSetChanged();

        Intent serviceIntent = new Intent(getActivity().getApplicationContext(), PhoneProfilesService.class);
        serviceIntent.putExtra(PhoneProfilesService.EXTRA_REREGISTER_RECEIVERS_AND_JOBS, true);
        serviceIntent.putExtra(PhoneProfilesService.EXTRA_ONLY_START, false);
        PPApplication.startPPService(getActivity(), serviceIntent);

        onStartEventPreferencesCallback.onStartEventPreferences(null, EDIT_MODE_DELETE, 0);
    }

    public void showEditMenu(View view)
    {
        //Context context = ((AppCompatActivity)getActivity()).getSupportActionBar().getThemedContext();
        Context context = view.getContext();
        PopupMenu popup;
        //if (android.os.Build.VERSION.SDK_INT >= 19)
            popup = new PopupMenu(context, view, Gravity.END);
        //else
        //    popup = new PopupMenu(context, view);
        Menu menu = popup.getMenu();
        getActivity().getMenuInflater().inflate(R.menu.event_list_item_edit, menu);

        final Event event = (Event)view.getTag();

        MenuItem menuItem = menu.findItem(R.id.event_list_item_menu_run_stop);
        //if (PPApplication.getGlobalEventsRunning(dataWrapper.context))
        //{
            //menuItem.setVisible(true);

            if (event.getStatusFromDB(activityDataWrapper.context) == Event.ESTATUS_STOP)
            {
                menuItem.setTitle(R.string.event_list_item_menu_run);
            }
            else
            {
                menuItem.setTitle(R.string.event_list_item_menu_stop);
            }
        //}
        //else
        //	menuItem.setVisible(false);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            public boolean onMenuItemClick(android.view.MenuItem item) {
                switch (item.getItemId()) {
                case R.id.event_list_item_menu_run_stop:
                    runStopEvent(event);
                    return true;
                case R.id.event_list_item_menu_duplicate:
                    duplicateEvent(event);
                    return true;
                case R.id.event_list_item_menu_delete:
                    deleteEventWithAlert(event);
                    return true;
                default:
                    return false;
                }
            }
            });


        popup.show();
    }

    private void deleteEventWithAlert(Event event)
    {
        final Event _event = event;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(getResources().getString(R.string.event_string_0) + ": " + event._name);
        dialogBuilder.setMessage(getResources().getString(R.string.delete_event_alert_message));
        //dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);
        dialogBuilder.setPositiveButton(R.string.alert_button_yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                deleteEvent(_event);
            }
        });
        dialogBuilder.setNegativeButton(R.string.alert_button_no, null);
        AlertDialog dialog = dialogBuilder.create();
        /*dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positive = ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                if (positive != null) positive.setAllCaps(false);
                Button negative = ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                if (negative != null) negative.setAllCaps(false);
            }
        });*/
        dialog.show();
    }

    private void deleteAllEvents()
    {
        if (eventListAdapter != null) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            dialogBuilder.setTitle(getResources().getString(R.string.alert_title_delete_all_events));
            dialogBuilder.setMessage(getResources().getString(R.string.alert_message_delete_all_events));
            //dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);
            dialogBuilder.setPositiveButton(R.string.alert_button_yes, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    activityDataWrapper.addActivityLog(DatabaseHandler.ALTYPE_ALLEVENTSDELETED, null, null, null, 0);

                    listView.getRecycledViewPool().clear();

                    activityDataWrapper.stopAllEventsFromMainThread(true, false);

                    eventListAdapter.clear();
                    // this is in eventListAdapter.clear()
                    //eventListAdapter.notifyDataSetChanged();

                    Intent serviceIntent = new Intent(getActivity().getApplicationContext(), PhoneProfilesService.class);
                    serviceIntent.putExtra(PhoneProfilesService.EXTRA_UNREGISTER_RECEIVERS_AND_JOBS, true);
                    serviceIntent.putExtra(PhoneProfilesService.EXTRA_ONLY_START, false);
                    PPApplication.startPPService(getActivity(), serviceIntent);

                    onStartEventPreferencesCallback.onStartEventPreferences(null, EDIT_MODE_DELETE, 0);
                }
            });
            dialogBuilder.setNegativeButton(R.string.alert_button_no, null);
            AlertDialog dialog = dialogBuilder.create();
            /*dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    Button positive = ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                    if (positive != null) positive.setAllCaps(false);
                    Button negative = ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                    if (negative != null) negative.setAllCaps(false);
                }
            });*/
            dialog.show();
        }
    }

    public void updateHeader(Profile profile)
    {
        if (!ApplicationPreferences.applicationEditorHeader(activityDataWrapper.context))
            return;

        //Log.e("***** EditorEventListFragment.updateHeader", "xxx");

        if ((activeProfileName == null) || (activeProfileIcon == null))
            return;

        //Log.e("***** EditorEventListFragment.updateHeader", "profile="+profile);

        if (profile == null)
        {
            activeProfileName.setText(getResources().getString(R.string.profiles_header_profile_name_no_activated));
            activeProfileIcon.setImageResource(R.drawable.ic_profile_default);
        }
        else
        {
            activeProfileName.setText(DataWrapper.getProfileNameWithManualIndicator(profile, true, true, false, activityDataWrapper, true));
            if (profile.getIsIconResourceID())
            {
                if (profile._iconBitmap != null)
                    activeProfileIcon.setImageBitmap(profile._iconBitmap);
                else {
                    //int res = getResources().getIdentifier(profile.getIconIdentifier(), "drawable", getActivity().getPackageName());
                    int res = Profile.getIconResource(profile.getIconIdentifier());
                    activeProfileIcon.setImageResource(res); // icon resource
                }
            }
            else
            {
                activeProfileIcon.setImageBitmap(profile._iconBitmap);
            }
        }

        if (ApplicationPreferences.applicationEditorPrefIndicator(activityDataWrapper.context))
        {
            ImageView profilePrefIndicatorImageView = getActivity().findViewById(R.id.activated_profile_pref_indicator);
            if (profilePrefIndicatorImageView != null)
            {
                //profilePrefIndicatorImageView.setImageBitmap(ProfilePreferencesIndicator.paint(profile, getActivity().getBaseContext()));
                if (profile == null)
                    profilePrefIndicatorImageView.setImageResource(R.drawable.ic_empty);
                else {
                    if (profile._preferencesIndicator != null)
                        profilePrefIndicatorImageView.setImageBitmap(profile._preferencesIndicator);
                    else
                        profilePrefIndicatorImageView.setImageResource(R.drawable.ic_empty);
                }
            }
        }
    }

    public void updateListView(Event event, boolean newEvent, boolean refreshIcons, boolean setPosition)
    {
        /*if (listView != null)
            listView.cancelDrag();*/

        if (eventListAdapter != null)
            listView.getRecycledViewPool().clear();

        if (eventListAdapter != null) {
            if ((newEvent) && (event != null))
                // add event into listview
                eventListAdapter.addItem(event);
        }

        synchronized (activityDataWrapper.eventList) {
            if (activityDataWrapper.eventList != null) {
                // sort list
                sortList(activityDataWrapper.eventList, orderType, activityDataWrapper);
            }
        }

        if (eventListAdapter != null) {
            int eventPos = ListView.INVALID_POSITION;

            if (event != null)
                eventPos = eventListAdapter.getItemPosition(event);
            //else
            //    eventPos = listView.getCheckedItemPosition();

            eventListAdapter.notifyDataSetChanged(refreshIcons);

            if (setPosition || newEvent) {
                if (eventPos != ListView.INVALID_POSITION) {
                    // set event visible in list
                    //int last = listView.getLastVisiblePosition();
                    //int first = listView.getFirstVisiblePosition();
                    //if ((eventPos <= first) || (eventPos >= last)) {
                    //    listView.setSelection(eventPos);
                    //}
                    listView.getLayoutManager().scrollToPosition(eventPos);
                }
            }

            boolean startTargetHelps = getArguments() != null && getArguments().getBoolean(START_TARGET_HELPS_ARGUMENT, false);
            if (startTargetHelps)
                showAdapterTargetHelps();

        }
    }

    /*
    public int getFilterType()
    {
        return filterType;
    }
    */

    public void changeListOrder(int orderType)
    {
        if (isAsyncTaskPendingOrRunning()) {
            this.asyncTaskContext.get().cancel(true);
        }

        this.orderType = orderType;
        if (eventListAdapter != null) {
            listView.getRecycledViewPool().clear();
            synchronized (activityDataWrapper.eventList) {
                sortList(activityDataWrapper.eventList, orderType, activityDataWrapper);
            }
            eventListAdapter.notifyDataSetChanged();
        }
    }

    private static void sortList(List<Event> eventList, int orderType, DataWrapper _dataWrapper)
    {
        final DataWrapper dataWrapper = _dataWrapper;

        class EventNameComparator implements Comparator<Event> {
            public int compare(Event lhs, Event rhs) {
                if (GlobalGUIRoutines.collator != null)
                    return GlobalGUIRoutines.collator.compare(lhs._name, rhs._name);
                else
                    return 0;
            }
        }

        class StartOrderComparator implements Comparator<Event> {
            public int compare(Event lhs, Event rhs) {
                return lhs._startOrder - rhs._startOrder;
            }
        }

        class ProfileNameComparator implements Comparator<Event> {
            public int compare(Event lhs, Event rhs) {
                if (GlobalGUIRoutines.collator != null) {
                    Profile profileLhs = dataWrapper.getProfileById(lhs._fkProfileStart, false, false,false);
                    Profile profileRhs = dataWrapper.getProfileById(rhs._fkProfileStart, false, false, false);
                    String nameLhs = "";
                    if (profileLhs != null) nameLhs = profileLhs._name;
                    String nameRhs = "";
                    if (profileRhs != null) nameRhs = profileRhs._name;
                    return GlobalGUIRoutines.collator.compare(nameLhs, nameRhs);
                }
                else
                    return 0;
            }
        }

        class PriorityComparator implements Comparator<Event> {
            public int compare(Event lhs, Event rhs) {
                //int res =  lhs._priority - rhs._priority;
                return rhs._priority - lhs._priority;
            }
        }

        switch (orderType)
        {
            case ORDER_TYPE_EVENT_NAME:
                Collections.sort(eventList, new EventNameComparator());
                break;
            case ORDER_TYPE_START_ORDER:
                Collections.sort(eventList, new StartOrderComparator());
                break;
            case ORDER_TYPE_PROFILE_NAME:
                Collections.sort(eventList, new ProfileNameComparator());
                break;
            case ORDER_TYPE_PRIORITY:
                if (ApplicationPreferences.applicationEventUsePriority(_dataWrapper.context))
                    Collections.sort(eventList, new PriorityComparator());
                else
                    Collections.sort(eventList, new StartOrderComparator());
                break;
        }
    }

    public void refreshGUI(boolean refreshIcons, boolean setPosition)
    {
        synchronized (activityDataWrapper.eventList) {
            if ((activityDataWrapper == null) || (!activityDataWrapper.eventListFilled))
                return;

            //noinspection ForLoopReplaceableByForEach
            for (Iterator<Event> it = activityDataWrapper.eventList.iterator(); it.hasNext(); ) {
                Event event = it.next();
                int status = DatabaseHandler.getInstance(activityDataWrapper.context).getEventStatus(event);
                event.setStatus(status);
                event._isInDelayStart = DatabaseHandler.getInstance(activityDataWrapper.context).getEventInDelayStart(event);
                event._isInDelayEnd = DatabaseHandler.getInstance(activityDataWrapper.context).getEventInDelayEnd(event);
                DatabaseHandler.getInstance(activityDataWrapper.context).setEventCalendarTimes(event);
                DatabaseHandler.getInstance(activityDataWrapper.context).getSMSStartTime(event);
                //DatabaseHandler.getInstance(activityDataWrapper.context).getNotificationStartTime(event);
                DatabaseHandler.getInstance(activityDataWrapper.context).getNFCStartTime(event);
                DatabaseHandler.getInstance(activityDataWrapper.context).getCallStartTime(event);
            }
        }
        Profile profileFromDB = DatabaseHandler.getInstance(activityDataWrapper.context).getActivatedProfile();
        if (profileFromDB != null) {
            PPApplication.logE("EditorEventListFragment.refreshGUI", "profile activated");
            Profile profileFromDataWrapper = activityDataWrapper.getProfileById(profileFromDB._id, true,
                    ApplicationPreferences.applicationEditorPrefIndicator(activityDataWrapper.context), false);
            if (profileFromDataWrapper != null)
                profileFromDataWrapper._checked = true;
            updateHeader(profileFromDataWrapper);
        } else {
            PPApplication.logE("EditorEventListFragment.refreshGUI", "profile not activated");
            updateHeader(null);
        }
        updateListView(null, false, refreshIcons, setPosition);
    }

    public void removeAdapter() {
        if (listView != null)
            listView.setAdapter(null);
    }

    void showTargetHelps() {
        /*if (Build.VERSION.SDK_INT <= 19)
            // TapTarget.forToolbarMenuItem FC :-(
            // Toolbar.findViewById() returns null
            return;*/

        if (getActivity() == null)
            return;

        if (((EditorProfilesActivity)getActivity()).targetHelpsSequenceStarted)
            return;

        ApplicationPreferences.getSharedPreferences(getActivity());

        boolean showTargetHelps = ApplicationPreferences.preferences.getBoolean(PREF_START_TARGET_HELPS, true);
        boolean showTargetHelpsDefaultProfile = ApplicationPreferences.preferences.getBoolean(EditorProfilesActivity.PREF_START_TARGET_HELPS_DEFAULT_PROFILE, true);
        if (showTargetHelps || showTargetHelpsDefaultProfile ||
                ApplicationPreferences.preferences.getBoolean(EditorEventListAdapter.PREF_START_TARGET_HELPS, true) ||
                ApplicationPreferences.preferences.getBoolean(EditorEventListAdapter.PREF_START_TARGET_HELPS_ORDER, true)) {

            //Log.d("EditorEventListFragment.showTargetHelps", "PREF_START_TARGET_HELPS_ORDER=true");

            if (showTargetHelps || showTargetHelpsDefaultProfile) {

                //Log.d("EditorEventListFragment.showTargetHelps", "PREF_START_TARGET_HELPS=true");

                SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
                editor.putBoolean(PREF_START_TARGET_HELPS, false);
                editor.putBoolean(EditorProfilesActivity.PREF_START_TARGET_HELPS_DEFAULT_PROFILE, false);
                editor.apply();

                int circleColor = R.color.tabTargetHelpCircleColor;
                if (ApplicationPreferences.applicationTheme(getActivity()).equals("dark"))
                    circleColor = R.color.tabTargetHelpCircleColor_dark;
                int textColor = R.color.tabTargetHelpTextColor;
                if (ApplicationPreferences.applicationTheme(getActivity()).equals("white"))
                    textColor = R.color.tabTargetHelpTextColor_white;
                boolean tintTarget = !ApplicationPreferences.applicationTheme(getActivity()).equals("white");

                final TapTargetSequence sequence = new TapTargetSequence(getActivity());
                List<TapTarget> targets = new ArrayList<>();
                int id = 1;
                if (showTargetHelps) {
                    try {
                        targets.add(
                                TapTarget.forToolbarMenuItem(bottomToolbar, R.id.menu_add_event, getString(R.string.editor_activity_targetHelps_newEventButton_title), getString(R.string.editor_activity_targetHelps_newEventButton_description))
                                        .targetCircleColor(circleColor)
                                        .textColor(textColor)
                                        .tintTarget(tintTarget)
                                        .drawShadow(true)
                                        .id(id)
                        );
                        ++id;
                    } catch (Exception ignored) {
                    } // not in action bar?
                    try {
                        targets.add(
                                TapTarget.forToolbarMenuItem(bottomToolbar, R.id.menu_delete_all_events, getString(R.string.editor_activity_targetHelps_deleteAllEventsButton_title), getString(R.string.editor_activity_targetHelps_deleteAllEventsButton_description))
                                        .targetCircleColor(circleColor)
                                        .textColor(textColor)
                                        .tintTarget(tintTarget)
                                        .drawShadow(true)
                                        .id(id)
                        );
                        ++id;
                    } catch (Exception ignored) {
                    } // not in action bar?
                }
                if (showTargetHelpsDefaultProfile) {
                    try {
                        targets.add(
                                TapTarget.forToolbarMenuItem(bottomToolbar, R.id.menu_default_profile, getString(R.string.editor_activity_targetHelps_backgroundProfileButton_title), getString(R.string.editor_activity_targetHelps_backgroundProfileButton_description))
                                        .targetCircleColor(circleColor)
                                        .textColor(textColor)
                                        .tintTarget(tintTarget)
                                        .drawShadow(true)
                                        .id(id)
                        );
                        ++id;
                    } catch (Exception ignored) {
                    } // not in action bar?
                }

                sequence.targets(targets)
                        .listener(new TapTargetSequence.Listener() {
                            // This listener will tell us when interesting(tm) events happen in regards
                            // to the sequence
                            @Override
                            public void onSequenceFinish() {
                                targetHelpsSequenceStarted = false;
                                showAdapterTargetHelps();
                            }

                            @Override
                            public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                                //Log.d("TapTargetView", "Clicked on " + lastTarget.id());
                            }

                            @Override
                            public void onSequenceCanceled(TapTarget lastTarget) {
                                targetHelpsSequenceStarted = false;
                                SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
                                editor.putBoolean(EditorEventListAdapter.PREF_START_TARGET_HELPS, false);
                                if (filterType == FILTER_TYPE_START_ORDER)
                                    editor.putBoolean(EditorEventListAdapter.PREF_START_TARGET_HELPS_ORDER, false);
                                editor.apply();
                            }
                        });
                sequence.continueOnCancel(true)
                        .considerOuterCircleCanceled(true);
                targetHelpsSequenceStarted = true;
                sequence.start();
            }
            else {
                //Log.d("EditorEventListFragment.showTargetHelps", "PREF_START_TARGET_HELPS=false");
                final Handler handler = new Handler(getActivity().getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showAdapterTargetHelps();
                    }
                }, 500);
            }
        }
    }

    private void showAdapterTargetHelps() {
        /*if (Build.VERSION.SDK_INT <= 19)
            // TapTarget.forToolbarMenuItem FC :-(
            // Toolbar.findViewById() returns null
            return;*/

        if (getActivity() == null)
            return;

        View itemView;
        if (listView.getChildCount() > 1)
            itemView = listView.getChildAt(1);
        else
            itemView = listView.getChildAt(0);
        if ((eventListAdapter != null) && (itemView != null))
            eventListAdapter.showTargetHelps(getActivity(), this, itemView);
        else {
            targetHelpsSequenceStarted = false;
            ApplicationPreferences.getSharedPreferences(getActivity());
            SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
            editor.putBoolean(EditorEventListAdapter.PREF_START_TARGET_HELPS, false);
            if (filterType == FILTER_TYPE_START_ORDER)
                editor.putBoolean(EditorEventListAdapter.PREF_START_TARGET_HELPS_ORDER, false);
            editor.apply();
        }
    }

}
