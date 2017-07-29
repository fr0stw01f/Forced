package me.zhenhao.forced.android.util;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import me.zhenhao.forced.android.tracing.BytecodeLogger;

import java.util.ArrayList;


public class UtilAddContact {

    public static void writePhoneContact(String displayName, String number)
    {
        Context context = BytecodeLogger.applicationContext; //Application's context or Activity's context

        ArrayList<ContentProviderOperation> cntProOper = new ArrayList<>();
        int contactIndex = cntProOper.size();    //ContactSize

        // Newly Inserted contact
        // A raw contact will be inserted ContactsContract.RawContacts table in contacts database.
        cntProOper.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)    //Step1
                .withValue(RawContacts.ACCOUNT_TYPE, null)
                .withValue(RawContacts.ACCOUNT_NAME, null).build());

        // Display name will be inserted in ContactsContract.Data table
        cntProOper.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)            //Step2
                .withValueBackReference(Data.RAW_CONTACT_ID,contactIndex)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, displayName)                 // Name of the contact
                .build());
        // Mobile number will be inserted in ContactsContract.Data table
        cntProOper.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)            //Step 3
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID,contactIndex)
                .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, number)                                     // Number to be added
                .withValue(Phone.TYPE, Phone.TYPE_MOBILE).build());                 //Type like HOME, MOBILE etc
        try {
            // We will do batch operation to insert all above data
            // Contains the output of the app of a ContentProviderOperation.
            // It is sure to have exactly one of uri or count set
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, cntProOper);
        } catch (RemoteException | OperationApplicationException exp) {
            //logs;
        }
    }
}
