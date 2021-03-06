package sk.henrichg.phoneprofilesplus;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class ProfileIconPreferenceAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater inflater = null;
    String imageIdentifier;
    boolean isImageResourceID;
    private boolean useCustomColor;
    private int customColor;

    static final String[] ThumbsIds = {
        "ic_profile_default",

        "ic_profile_home", "ic_profile_home_2",

        "ic_profile_outdoors_1", "ic_profile_outdoors_2", "ic_profile_outdoors_3", "ic_profile_outdoors_4",
        "ic_profile_outdoors_5",

        "ic_profile_meeting", "ic_profile_meeting_2", "ic_profile_meeting_3", "ic_profile_mute", "ic_profile_mute_2",
        "ic_profile_volume_1", "ic_profile_volume_2", "ic_profile_volume_3",

        "ic_profile_work_1", "ic_profile_work_2", "ic_profile_work_3", "ic_profile_work_4", "ic_profile_work_5",
        "ic_profile_work_6", "ic_profile_work_7", "ic_profile_work_8", "ic_profile_work_9", "ic_profile_work_10",
        "ic_profile_work_11", "ic_profile_work_12",

        "ic_profile_sleep", "ic_profile_sleep_2", "ic_profile_night",
        "ic_profile_call_1",

        "ic_profile_car_1", "ic_profile_car_2", "ic_profile_car_3", "ic_profile_car_4", "ic_profile_car_5",
        "ic_profile_car_6", "ic_profile_car_7", "ic_profile_car_8", "ic_profile_car_9",
        "ic_profile_airplane_1", "ic_profile_airplane_2", "ic_profile_airplane_3",

        "ic_profile_battery_1", "ic_profile_battery_2", "ic_profile_battery_3",

        "ic_profile_culture_1", "ic_profile_culture_2", "ic_profile_culture_3", "ic_profile_culture_4"
    };

    static final int[] ThumbsColors = {
            0xff1c9cd7,

            0xff99cc00, 0xff99cc00,

            0xffffbc33, 0xffffbc33, 0xffffbc33, 0xffffbc33,
            0xffffbc33,

            0xffcc0000, 0xffcc0000, 0xffcc0000, 0xffcc0000, 0xffcc0000,
            0xffcc0000, 0xffcc0000, 0xffcc0000,

            0xffa801ff, 0xffa801ff, 0xffa801ff, 0xffa801ff, 0xffa801ff,
            0xffa801ff, 0xffa801ff, 0xffa801ff, 0xffa801ff, 0xffa801ff,
            0xffa801ff, 0xffa801ff,

            0xff0099cc, 0xff0099cc, 0xff0099cc,
            0xff0099cc,

            0xff28a174, 0xff28a174, 0xff28a174, 0xff28a174, 0xff28a174,
            0xff28a174, 0xff28a174, 0xff28a174, 0xff28a174,
            0xff28a174, 0xff28a174, 0xff28a174,

            0xffdb3514, 0xffdb9714, 0xff2aa561,

            0xff38d043, 0xff38d043, 0xff38d043, 0xff38d043
    };

    public ProfileIconPreferenceAdapter(Context c, String imageIdentifier, boolean isImageResourceID, boolean useCustomColor, int customColor)
    {
        context = c;

        inflater = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        this.imageIdentifier = imageIdentifier;
        this.isImageResourceID = isImageResourceID;
        this.useCustomColor = useCustomColor;
        this.customColor = customColor;
    }

    public int getCount() {
        return ThumbsIds.length;
    }

    public Object getItem(int position) {
        return ThumbsIds[position];
    }

    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
          ImageView icon;
          int position;
        }

    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        View vi = convertView;
        if (convertView == null)
        {
            vi = inflater.inflate(R.layout.profileicon_preference_gridview_item, parent, false);
            holder = new ViewHolder();
            holder.icon = (ImageView)vi.findViewById(R.id.profileicon_preference_gridview_item_icon);
            vi.setTag(holder);
        }
        else
        {
            holder = (ViewHolder)vi.getTag();
        }

        if (ThumbsIds[position].equals(imageIdentifier) && isImageResourceID) {
            if (GlobalData.applicationTheme.equals("dark"))
                holder.icon.setBackgroundResource(R.drawable.abc_list_pressed_holo_dark);
            else
                holder.icon.setBackgroundResource(R.drawable.abc_list_pressed_holo_light);
        }
        else
            holder.icon.setBackgroundResource(0);

        int res = context.getResources().getIdentifier(ThumbsIds[position], "drawable", context.getPackageName());
        if (ThumbsIds[position].equals(imageIdentifier) && isImageResourceID && useCustomColor) {
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), res);
            bitmap = BitmapManipulator.recolorBitmap(bitmap, customColor, context);
            holder.icon.setImageBitmap(bitmap);
        }
        else
            holder.icon.setImageResource(res);

        return vi;
    }

    public void imageIdentifierAndTypeChanged(String imageIdentifier, boolean isImageResourceID) {
        if (!this.imageIdentifier.equals(imageIdentifier)) {
            this.useCustomColor = false;
            this.customColor = 0;
        }
        this.imageIdentifier = imageIdentifier;
        this.isImageResourceID = isImageResourceID;
        notifyDataSetChanged();
    }

    public static int getImageResourcePosition(String imageIdentifier) {
        for (int pos = 0; pos < ThumbsIds.length; pos++) {
            if (ThumbsIds[pos].equals(imageIdentifier))
                return pos;
        }
        return 0;
    }

    public void setCustomColor(boolean newUseCustomColor, int newCustomColor) {
        useCustomColor = newUseCustomColor;
        customColor = newCustomColor;
        notifyDataSetChanged();
    }

    public static int getIconColor(String imageIdentifier) {
        return ThumbsColors[getImageResourcePosition(imageIdentifier)];
    }

}
