package sk.henrichg.phoneprofilesplus;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ShortcutCreatorListFragment extends Fragment {

    private DataWrapper dataWrapper;
    private List<Profile> profileList;
    private ShortcutProfileListAdapter profileListAdapter;
    private ListView listView;

    private WeakReference<LoadProfileListAsyncTask> asyncTaskContext;

    public ShortcutCreatorListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // this is really important in order to save the state across screen
        // configuration changes for example
        setRetainInstance(true);

        dataWrapper = new DataWrapper(getActivity().getApplicationContext(), true, false, 0);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView;

        rootView = inflater.inflate(R.layout.shortcut_creator_list, container, false);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        doOnViewCreated(view, savedInstanceState);
    }

    //@Override
    public void doOnViewCreated(View view, Bundle savedInstanceState)
    {
        listView = (ListView)view.findViewById(R.id.shortcut_profiles_list);

        listView.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                createShortcut(position);

            }

        });

        if (profileList == null)
        {
            LoadProfileListAsyncTask asyncTask = new LoadProfileListAsyncTask(this);
            this.asyncTaskContext = new WeakReference<LoadProfileListAsyncTask >(asyncTask );
            asyncTask.execute();
        }
        else
        {
            listView.setAdapter(profileListAdapter);
        }

    }

    private static class LoadProfileListAsyncTask extends AsyncTask<Void, Void, Void> {

        private WeakReference<ShortcutCreatorListFragment> fragmentWeakRef;
        private DataWrapper dataWrapper;

        private class ProfileComparator implements Comparator<Profile> {
            public int compare(Profile lhs, Profile rhs) {
                int res = GUIData.collator.compare(lhs._name, rhs._name);
                return res;
            }
        }

        private LoadProfileListAsyncTask (ShortcutCreatorListFragment fragment) {
            this.fragmentWeakRef = new WeakReference<ShortcutCreatorListFragment>(fragment);
            this.dataWrapper = new DataWrapper(fragment.getActivity().getApplicationContext(), true, false, 0);
        }

        @Override
        protected Void doInBackground(Void... params) {
            List<Profile> profileList = this.dataWrapper.getProfileList();
            Collections.sort(profileList, new ProfileComparator());
            return null;
        }

        @Override
        protected void onPostExecute(Void response) {
            super.onPostExecute(response);
            
            ShortcutCreatorListFragment fragment = this.fragmentWeakRef.get(); 
            
            if ((fragment != null) && (fragment.isAdded())) {

                // get local profileList
                List<Profile> profileList = this.dataWrapper.getProfileList();

                // add restart events
                Profile profile = this.dataWrapper.getNoinitializedProfile(this.dataWrapper.context.getString(R.string.menu_restart_events),
                                            "ic_action_events_restart_color", 0);
                profileList.add(0, profile);

                // set copy local profile list into activity profilesDataWrapper
                fragment.dataWrapper.setProfileList(profileList, false);
                // set reference of profile list from profilesDataWrapper
                fragment.profileList = fragment.dataWrapper.getProfileList();

                fragment.profileListAdapter = new ShortcutProfileListAdapter(fragment, fragment.profileList);
                fragment.listView.setAdapter(fragment.profileListAdapter);
            }
        }
    }

    private boolean isAsyncTaskPendingOrRunning() {
        return this.asyncTaskContext != null &&
              this.asyncTaskContext.get() != null &&
              !this.asyncTaskContext.get().getStatus().equals(AsyncTask.Status.FINISHED);
    }

    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    public void onDestroy()
    {
        if (!isAsyncTaskPendingOrRunning())
        {
            if (listView != null)
                listView.setAdapter(null);
            if (profileListAdapter != null)
                profileListAdapter.release();

            profileList = null;

            if (dataWrapper != null)
                dataWrapper.invalidateDataWrapper();
            dataWrapper = null;
        }

        super.onDestroy();
    }

    private void createShortcut(int position)
    {
        Profile profile = profileList.get(position);
        boolean isIconResourceID;
        String iconIdentifier;
        Bitmap profileBitmap = null;
        Bitmap shortcutOverlayBitmap;
        Bitmap profileShortcutBitmap;
        String profileName;
        boolean useCustomColor;

        if (profile != null)
        {
            isIconResourceID = profile.getIsIconResourceID();
            iconIdentifier = profile.getIconIdentifier();
            profileName = profile._name;
            useCustomColor = profile.getUseCustomColorForIcon();
        }
        else
        {
            isIconResourceID = true;
            iconIdentifier = GlobalData.PROFILE_ICON_DEFAULT;
            profileName = getResources().getString(R.string.profile_name_default);
            useCustomColor = false;
        }

        Intent shortcutIntent;
        if (position == 0) {
            // restart events
            shortcutIntent = new Intent(getActivity().getApplicationContext(), ActionForExternalApplicationActivity.class);
            shortcutIntent.setAction(ActionForExternalApplicationActivity.ACTION_RESTART_EVENTS);
        }
        else {
            shortcutIntent = new Intent(getActivity().getApplicationContext(), BackgroundActivateProfileActivity.class);
            // BackgroundActivateProfileActivity musi toto testovat, a len spravit aktivaciu profilu
            shortcutIntent.putExtra(GlobalData.EXTRA_STARTUP_SOURCE, GlobalData.STARTUP_SOURCE_SHORTCUT);
            shortcutIntent.putExtra(GlobalData.EXTRA_PROFILE_ID, profile._id);
        }

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, profileName);

        if (isIconResourceID)
        {
            if (profile._iconBitmap != null)
                profileBitmap = profile._iconBitmap;
            else {
                int iconResource = getResources().getIdentifier(iconIdentifier, "drawable", getActivity().getPackageName());
                profileBitmap = BitmapFactory.decodeResource(getResources(), iconResource);
            }
            shortcutOverlayBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_shortcut_overlay);
        }
        else
        {
            Resources resources = getResources();
            int height = (int) resources.getDimension(android.R.dimen.app_icon_size);
            int width = (int) resources.getDimension(android.R.dimen.app_icon_size);
            profileBitmap = BitmapManipulator.resampleBitmap(iconIdentifier, width, height, getActivity().getApplicationContext());
            if (profileBitmap == null) {
                int iconResource = R.drawable.ic_profile_default;
                profileBitmap = BitmapFactory.decodeResource(getResources(), iconResource);
            }
            shortcutOverlayBitmap = BitmapManipulator.resampleResource(resources, R.drawable.ic_shortcut_overlay, width, height);
        }
        
        if (GlobalData.applicationWidgetIconColor.equals("1"))
        {
            int monochromeValue = 0xFF;
            if (GlobalData.applicationWidgetIconLightness.equals("0")) monochromeValue = 0x00;
            if (GlobalData.applicationWidgetIconLightness.equals("25")) monochromeValue = 0x40;
            if (GlobalData.applicationWidgetIconLightness.equals("50")) monochromeValue = 0x80;
            if (GlobalData.applicationWidgetIconLightness.equals("75")) monochromeValue = 0xC0;
            if (GlobalData.applicationWidgetIconLightness.equals("100")) monochromeValue = 0xFF;
            
            if (isIconResourceID || useCustomColor) {
                // icon is from resource or colored by custom color
                profileBitmap = BitmapManipulator.monochromeBitmap(profileBitmap, monochromeValue, getActivity().getBaseContext());
            }
            else
                profileBitmap = BitmapManipulator.grayscaleBitmap(profileBitmap);
        }

        profileShortcutBitmap = combineImages(profileBitmap, shortcutOverlayBitmap);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, profileShortcutBitmap);

        //intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        //getActivity().getApplicationContext().sendBroadcast(intent);

        getActivity().setResult(Activity.RESULT_OK, intent);

        getActivity().finish();
    }

    private Bitmap combineImages(Bitmap bitmap1, Bitmap bitmap2)
    {
        Bitmap combined;

        int width;
        int height;

        width = bitmap2.getWidth();
        height = bitmap2.getHeight();

        combined = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(combined);
        canvas.drawBitmap(bitmap1, 0f, 0f, null);
        if (GlobalData.applicationShortcutEmblem)
            canvas.drawBitmap(bitmap2, 0f, 0f, null);

        return combined;
    }

}
