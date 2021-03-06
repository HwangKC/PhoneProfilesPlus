package sk.henrichg.phoneprofilesplus;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.readystatesoftware.systembartint.SystemBarTintManager;

public class ImportantInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        GlobalData.loadPreferences(getApplicationContext());

        // must by called before super.onCreate() for PreferenceActivity
        GUIData.setTheme(this, false, false); // must by called before super.onCreate()
        GUIData.setLanguage(getBaseContext());

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_important_info);

        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) && (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)) {
            Window w = getWindow(); // in Activity's onCreate() for instance
            //w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            // create our manager instance after the content view is set
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            // enable status bar tint
            tintManager.setStatusBarTintEnabled(true);
            // set a custom tint color for status bar
            if (GlobalData.applicationTheme.equals("material"))
                tintManager.setStatusBarTintColor(Color.parseColor("#ff237e9f"));
            else
                tintManager.setStatusBarTintColor(Color.parseColor("#ff202020"));
        }

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.info_notification_title);

        PackageInfo pinfo = null;
        int versionCode = 0;
        Context context = getApplicationContext();
        try {
            pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionCode = pinfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            //e.printStackTrace();
        }

        boolean news = false;
        boolean newsLatest = (versionCode >= ImportantInfoNotification.VERSION_CODE_FOR_NEWS);
        boolean news1804 = ((versionCode >= 1804) && (versionCode < ImportantInfoNotification.VERSION_CODE_FOR_NEWS));
        boolean news1772 = ((versionCode >= 1772) && (versionCode < ImportantInfoNotification.VERSION_CODE_FOR_NEWS));

        if (newsLatest) {
            // empty this, for switch off news
            TextView infoText1 = (TextView) findViewById(R.id.activity_info_event_start_order1);
            infoText1.setVisibility(View.GONE);
            TextView infoText2 = (TextView) findViewById(R.id.activity_info_event_start_order2);
            infoText2.setVisibility(View.GONE);
            news = true;
        }
        else {
            // empty this, for switch off news
            TextView infoText1 = (TextView) findViewById(R.id.activity_info_event_start_order1_news);
            infoText1.setVisibility(View.GONE);
            TextView infoText2 = (TextView) findViewById(R.id.activity_info_event_start_order2_news);
            infoText2.setVisibility(View.GONE);
        }

        if (news1804) {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                TextView infoText16 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text16);
                infoText16.setVisibility(View.GONE);
                TextView infoText18 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text18);
                infoText18.setVisibility(View.GONE);
                TextView infoText19 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text19);
                infoText19.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                        startActivity(intent);
                    }
                });
                TextView infoText20 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text20);
                infoText20.setVisibility(View.GONE);
                TextView infoText21 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text21);
                infoText21.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                        startActivity(intent);
                    }
                });
                TextView infoText22 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text22);
                infoText22.setVisibility(View.GONE);
                news = true;
            }
        }
        else {
            TextView infoText15 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text15);
            infoText15.setVisibility(View.GONE);
            TextView infoText17 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text17);
            infoText17.setVisibility(View.GONE);
            TextView infoText19 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text19);
            infoText19.setVisibility(View.GONE);
            TextView infoText20 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text20);
            infoText20.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    startActivity(intent);
                }
            });
            TextView infoText21 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text21);
            infoText21.setVisibility(View.GONE);
            TextView infoText22 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text22);
            infoText22.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    startActivity(intent);
                }
            });
            TextView infoText10a = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text10a);
            infoText10a.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    startActivity(intent);
                }
            });
        }

        if (news1772) {
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                TextView infoText14 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text14);
                infoText14.setVisibility(View.GONE);
                TextView infoText13 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text13);
                infoText13.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                        startActivity(intent);
                    }
                });
                news = true;
            }
        }
        else {
            TextView infoText13 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text13);
            infoText13.setVisibility(View.GONE);
            TextView infoText14 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text14);
            infoText14.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                    startActivity(intent);
                }
            });
        }

        if (android.os.Build.VERSION.SDK_INT < 23) {
            TextView infoText15 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text15);
            infoText15.setVisibility(View.GONE);
            TextView infoText16 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text16);
            infoText16.setVisibility(View.GONE);
            TextView infoText17 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text17);
            infoText17.setVisibility(View.GONE);
            TextView infoText18 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text18);
            infoText18.setVisibility(View.GONE);
            TextView infoText19 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text19);
            infoText19.setVisibility(View.GONE);
            TextView infoText20 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text20);
            infoText20.setVisibility(View.GONE);
            TextView infoText21 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text21);
            infoText21.setVisibility(View.GONE);
            TextView infoText22 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text22);
            infoText22.setVisibility(View.GONE);
            TextView infoText10a = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text10a);
            infoText10a.setVisibility(View.GONE);
        }

        if (android.os.Build.VERSION.SDK_INT < 21) {
            TextView infoText11 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text11);
            infoText11.setVisibility(View.GONE);
            TextView infoText13 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text13);
            infoText13.setVisibility(View.GONE);
            TextView infoText14 = (TextView) findViewById(R.id.activity_info_notification_dialog_info_text14);
            infoText14.setVisibility(View.GONE);
        }

        TextView infoText3 = (TextView)findViewById(R.id.activity_info_notification_dialog_info_text3);
        infoText3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), PhoneProfilesPreferencesActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(PhoneProfilesPreferencesActivity.EXTRA_SCROLL_TO, "categorySystem");
                //intent.putExtra(PhoneProfilesPreferencesActivity.EXTRA_SCROLL_TO_TYPE, "screen");
                startActivity(intent);
            }
        });

        TextView infoText40 = (TextView)findViewById(R.id.activity_info_default_profile);
        infoText40.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), PhoneProfilesPreferencesActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(PhoneProfilesPreferencesActivity.EXTRA_SCROLL_TO, "profileActivationCategory");
                //intent.putExtra(PhoneProfilesPreferencesActivity.EXTRA_SCROLL_TO_TYPE, "screen");
                startActivity(intent);
            }
        });

        TextView infoText41 = (TextView)findViewById(R.id.activity_info_activate_profile_from_tasker_params);
        infoText41.setText("Send Intent [ \n" +
                    " Action:sk.henrichg.phoneprofilesplus.ACTION_ACTIVATE_PROFILE\n" +
                    " Extra:profile_name:profile name\n" +
                    " Target:Activity\n" +
                "]");
        TextView infoText42 = (TextView)findViewById(R.id.activity_info_manage_events_from_tasker_params_restart_events);
        infoText42.setText("Send Intent [ \n" +
                " Action:sk.henrichg.phoneprofilesplus.ACTION_RESTART_EVENTS\n" +
                " Target:Activity\n" +
                "]");
        TextView infoText43 = (TextView)findViewById(R.id.activity_info_manage_events_from_tasker_params_enable_run_for_event);
        infoText43.setText("Send Intent [ \n" +
                " Action:sk.henrichg.phoneprofilesplus.ACTION_ENABLE_RUN_FOR_EVENT\n" +
                " Extra:event_name:event name\n" +
                " Target:Activity\n" +
                "]");
        TextView infoText44 = (TextView)findViewById(R.id.activity_info_manage_events_from_tasker_params_pause_event);
        infoText44.setText("Send Intent [ \n" +
                " Action:sk.henrichg.phoneprofilesplus.ACTION_PAUSE_EVENT\n" +
                " Extra:event_name:event name\n" +
                " Target:Activity\n" +
                "]");
        TextView infoText45 = (TextView)findViewById(R.id.activity_info_manage_events_from_tasker_params_stop_event);
        infoText45.setText("Send Intent [ \n" +
                " Action:sk.henrichg.phoneprofilesplus.ACTION_STOP_EVENT\n" +
                " Extra:event_name:event name\n" +
                " Target:Activity\n" +
                "]");

        if (!news) {
            TextView infoTextNews = (TextView) findViewById(R.id.activity_info_notification_dialog_news);
            infoTextNews.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

}
