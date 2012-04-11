package com.handbagdevices.handbag;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.SmsManager;
import android.util.Log;

// TODO: Figure out a way to make permission for this optional?

public class SmsFeature extends FeatureConfig {

    final static int SMS_ARRAY_OFFSET_ACTION = 2;
    final static int SMS_ARRAY_OFFSET_CONTACT = 3;
    final static int SMS_ARRAY_OFFSET_TEXT = 4;


    private String action;
    private String contact;
    private String text;

    private SmsManager manager;

    private ContentResolver resolver;


    public SmsFeature(Context context, String action, String contact, String text) {
        this.action = action;
        this.contact = contact;
        this.text = text;

        manager = SmsManager.getDefault();
        resolver = context.getContentResolver();
    }


    @Override
    void doAction() {
        if (action.equals("send")) {
            sendSms(contact, text);
        } else {
            Log.d(this.getClass().getSimpleName(), "Unknown SMS action: " + action);
        }
    }


    private String getContactNumber(String contact) {

        // Use as a "number"/address if we don't find it as a matching name.
        String contactAddress = contact;

        // Try to get a matching name in contacts first:
        Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI,
                new String[] { ContactsContract.Contacts._ID }, "DISPLAY_NAME = ?", new String[] { contact }, null);

        if (cursor.moveToFirst()) {
            // TODO: Check if "has number" is true first?
            String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            Cursor phoneNumbers = resolver.query(Phone.CONTENT_URI,
                    new String[] { Phone.NUMBER }, Phone.CONTACT_ID + " = ? and " + Phone.TYPE
                            + " = ? ", new String[] { contactId, Integer.toString(Phone.TYPE_MOBILE) }, null);
            if (phoneNumbers.moveToFirst()) {
                contactAddress = phoneNumbers.getString(phoneNumbers.getColumnIndex(Phone.NUMBER));
            }
            phoneNumbers.close();
        }
        cursor.close();

        return contactAddress;

    }

    private void sendSms(String contact, String text) {
        String destinationAddress = getContactNumber(contact);

        Log.d(this.getClass().getSimpleName(), "Sending to: " + destinationAddress + " (" + contact + ")  Message: " + text);

        // TODO: Display a toast or something when this succeeds?
        manager.sendTextMessage(destinationAddress, null, text, null, null);
    }


    // @Hide
    public static FeatureConfig fromArray(Context context, String[] theArray) {
        return new SmsFeature(context, theArray[SMS_ARRAY_OFFSET_ACTION], theArray[SMS_ARRAY_OFFSET_CONTACT],
                theArray[SMS_ARRAY_OFFSET_TEXT]);
    }

}
