package sk.henrichg.phoneprofilesplus;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;

public class ContactGroupsCache {

    private ArrayList<ContactGroup> contactGroupList;
    private boolean cached;
    private boolean cancelled;

    public ContactGroupsCache()
    {
        contactGroupList = new ArrayList<ContactGroup>();
        cached = false;
    }

    public void getContactGroupList(Context context)
    {
        if (cached) return;

        cancelled = false;

        contactGroupList.clear();

        if (Permissions.checkContacts(context)) {
            String[] projection = new String[]{
                    ContactsContract.Groups._ID,
                    ContactsContract.Groups.TITLE,
                    ContactsContract.Groups.SUMMARY_COUNT};
            String selection = ContactsContract.Groups.DELETED + "!='1'";// + " AND "+
            //ContactsContract.Groups.GROUP_VISIBLE+"!='0' ";
            String order = ContactsContract.Groups.TITLE + " ASC";

            Cursor mCursor = context.getContentResolver().query(ContactsContract.Groups.CONTENT_SUMMARY_URI, projection, selection, null, order);

            if (mCursor != null) {
                while (mCursor.moveToNext()) {
                    long contactGroupId = mCursor.getLong(mCursor.getColumnIndex(ContactsContract.Groups._ID));
                    String name = mCursor.getString(mCursor.getColumnIndex(ContactsContract.Groups.TITLE));
                    int count = mCursor.getInt(mCursor.getColumnIndex(ContactsContract.Groups.SUMMARY_COUNT));

                    if (count > 0) {
                        ContactGroup aContactGroup = new ContactGroup();
                        aContactGroup.groupId = contactGroupId;
                        aContactGroup.name = name;
                        aContactGroup.count = count;

                        contactGroupList.add(aContactGroup);
                    }

                    if (cancelled)
                        break;

                }
                mCursor.close();
            }

            if (cancelled)
                return;

            cached = true;
        }
    }

    public int getLength()
    {
        if (cached)
            return contactGroupList.size();
        else
            return 0;
    }

    public List<ContactGroup> getList()
    {
        if (cached)
            return contactGroupList;
        else
            return null;
    }

    public ContactGroup getContactGroup(int position)
    {
        if (cached)
            return contactGroupList.get(position);
        else
            return null;
    }

    public long getContactGroupId(int position)
    {
        if (cached)
            return contactGroupList.get(position).groupId;
        else
            return 0;
    }

    public String getContactGroupDisplayName(int position)
    {
        if (cached)
            return contactGroupList.get(position).name;
        else
            return "";
    }

    public void clearCache(boolean nullList)
    {
        contactGroupList.clear();
        if (nullList)
            contactGroupList = null;
        cached = false;
    }

    public boolean isCached()
    {
        return cached;
    }

    public void cancelCaching()
    {
        cancelled = true;
    }

}
