package com.loc.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeSet;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.loc.bean.Contact;
import com.loc.receiver.AlarmReceiver;
import com.loc.sqldb.ContactDatabaseHelper;
import com.loc.tab.MainActivity;
import com.loc.util.AppLiterals;
import com.loc.util.AppUtil;
import com.loc.util.RESTUtil;

public class C2DM9Activity extends Activity implements AppLiterals {
	
	Button btnRegisterUser;
	Button tempx;
	
	EditText editxt1;
	EditText fName;
	EditText lName;
	EditText1 Xxx;
	
	EditText address;
	
	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
	
//	private String endPoint_User;// = "http://1-dot-mylocationappx1.appspot.com/userlist";
//	private String endPoint_Location;
	private static final String TAG = "C2DM9Activity";
	
    private ProgressDialog pDialog;
    private Properties prop;
    
    private Contact self;
    private Context context;
	private Contact lastUpdatedContact;
	private String  lastUpdatedContactTS;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        
        getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, 
          		new MyCOntentObserver(null));
        context = getBaseContext();
        
        btnRegisterUser = (Button)findViewById(R.id.btnSave);
        fName = (EditText)findViewById(R.id.FirstName);
        lName=(EditText)findViewById(R.id.LastName);
        mobileNo=(EditText)findViewById(R.id.Mobile);
        address=(EditText)findViewById(R.id.Addresss);
        
        
       
			if(AppUtil.getPrefString(C2DM9Activity.this,
					"isBaseLoadComplete").equalsIgnoreCase("Y")){
			openNextActivity();
			}
			else{
				showProgressbar();
				AppUtil.getAppUsersFromServer(this,context, User_EndPoint, getPhoneContacts());
			}
        
        btnRegisterUser.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if(AppUtil.isConnected(C2DM9Activity.this)){
				String fname = fName.getText().toString();	
				String lname = lName.getText().toString();
				String mobile = mobileNo.getText().toString();
				String address1 = address.getText().toString();
				
				nameValuePairs.add(new BasicNameValuePair("firstName",fname));
				nameValuePairs.add(new BasicNameValuePair("lastName",lname));
				nameValuePairs.add(new BasicNameValuePair("mobileNo",mobile));
				nameValuePairs.add(new BasicNameValuePair("address",address1));
				
				self =new Contact();
				self.setName(fname+" "+lname);
				self.setAddress(address1);
				self.setPhone1(mobile);
				new RegisterUser().execute();
			}
			else {
				// TODO Show a dialog that no internet connection to proceed
					}
				
			}
		});
        
    }
    private void openNextActivity() {
    	 SharedPreferences sharedpreferences = getSharedPreferences("APP_PREFERENCES", Context.MODE_PRIVATE);
 		if (sharedpreferences.getLong(getString(R.string.userId), -1) != -1 ) 
 		{
    	Intent mainIntent = new Intent(context, MainActivity.class);
		C2DM9Activity.this.startActivity(mainIntent);
		C2DM9Activity.this.finish();	
 		}
	}
	private void showProgressbar() {
    	pDialog = new ProgressDialog(C2DM9Activity.this);
        pDialog.setMessage("Please wait...");
        pDialog.setCancelable(false);
        pDialog.show();				
	}
    private void dismissProgressbar() {
		if (pDialog!=null &&  pDialog.isShowing())
            pDialog.dismiss();
	}
        private class RegisterUser extends AsyncTask<Void, Void, Void> {
        	 
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showProgressbar();
     
            }
     
            
			@Override
            protected Void doInBackground(Void... arg0) {
            	long id = 0 ;
     
            	String response = RESTUtil.insertRecord(User_EndPoint, nameValuePairs);
				try {
					
					Log.d(TAG, response);
					JSONObject reader = new JSONObject(response);
					JSONObject key  = reader.getJSONObject("key");
					
					//String kind = key.getString("kind"); -Info field: used to extract the GCM server registration id on success.
					id = key.getLong("id");
					
				} catch (JSONException e) {
					
					e.printStackTrace();
				}
				Editor editor = AppUtil.getPrefEditor(context);
				editor.putLong(getString(R.string.userId), id);
				editor.commit();     
				
                return null;
            }
     
            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);

                dismissProgressbar();
                
                inserttoDB(self);     
                
            	GCMRegister();
            	
            	setRecurringAlarm(getApplicationContext());
            	
            	openNextActivity();
				
            }
        }
        
      //GCM server registration request
        private void GCMRegister() {
			Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
			registrationIntent.putExtra("app", PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(), 0));
//			registrationIntent.putExtra("sender","441015042214");--old one
			registrationIntent.putExtra("sender","1057446360872");
			getApplicationContext().startService(registrationIntent);				
		}

		private void inserttoDB(Contact self) {
			ContactDatabaseHelper contactDatabaseHelper = new ContactDatabaseHelper(getApplicationContext());
			 long id =contactDatabaseHelper.addContact(self);
			 //set to preference so as to update user later with GCM details and etc
    		 AppUtil.getPrefEditor(C2DM9Activity.this).putLong("self_rowid", id).commit();				
		}
        
        
       
        
        private List<Contact> getPhoneContacts(){
        	ContentResolver cr = getContentResolver();
        	Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
        			null, null, null, "upper("+ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + ") ASC");
        	List<Contact> contactList = new ArrayList<Contact>();
       /* 	Uri lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.
        			CONTENT_LOOKUP_URI,ContactsContract.Contacts.LOOKUP_KEY );
        	cr.registerContentObserver(
        			lookupUri, true,
        			new MyCOntentObserver()); */

        	Map<String, Contact> hm = new HashMap <String, Contact>();
        	if (cur != null && cur.getCount() > 0) {
        		while (cur.moveToNext()) {
        			
        			//lastUpdated is available after API 18 in case its not available, use _ID 
        			String lastUpdatedTS= cur.getString(cur.getColumnIndex(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP));
        			String _ID = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));

        			if (("1").equals(cur.getString(cur.getColumnIndex(
        					ContactsContract.Contacts.HAS_PHONE_NUMBER)))) {

        				Contact contact = new Contact();
            	
                String id = cur.getString(cur.getColumnIndex(
                            ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(
                            ContactsContract.Contacts.DISPLAY_NAME));
               
               
                contact.setName(name);
                Cursor pCur = cr.query(
                                       ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                                + " = ?", new String[] { id }, null);
                
                HashSet<String> phoneNumSet = new HashSet<String>();
                while (pCur != null && pCur.moveToNext()) {
                	String phoneNo = pCur.getString(pCur.getColumnIndex(
                			ContactsContract.CommonDataKinds.Phone.NUMBER));
                	StringBuffer sb = new StringBuffer(phoneNo.replaceAll("[^+\\d]", ""));
                	int length = sb.length();
                	if(length > 10)
                		phoneNumSet.add(sb.substring(length - 10));
                	else 
                		phoneNumSet.add(phoneNo);
                }
                pCur.close();
             
                if(phoneNumSet.size() > 0){
                String phoneNumbers = android.text.TextUtils.join(",", phoneNumSet);
                contact.setPhone1(phoneNumbers);
                //String[] phoneNumbers = phoneNumSet.toArray(new String[phoneNumSet.size()]);
                //contact.setPhone1(Arrays.toString(phoneNumbers));
                contactList.add(contact);
                hm.put(lastUpdatedTS,contact);
                Log.d(TAG,name+"::"+android.text.TextUtils.join(",", phoneNumSet));
                }
                
            }

        }
        		TreeSet myTreeSet = new TreeSet();
                myTreeSet.addAll(hm.keySet());
                System.out.println("last"+myTreeSet.last() +"Numebr"+hm.get(myTreeSet.last()));
                lastUpdatedContact = hm.get(myTreeSet.last());
                lastUpdatedContactTS = (String) myTreeSet.last() ;
                
        Log.d(TAG,"Total Contacts::"+cur.getCount());
        cur.close();
    }
    
    return contactList;
}
        
        
    /*    public String getAppUsersLocation (String endpoint) {
        	try{
        		
        		return RESTUtil.getAppUsersLocation(endpoint, NVPUserLocation);
        	}

        	catch(Exception e){

        		return "Exception";
        	}
        }*/
        
        
        public class MyCOntentObserver extends ContentObserver{
            public MyCOntentObserver(Handler handler) {
                super(handler);
            }
            @Override
            public void onChange(boolean selfChange) {
            	C2DM9Activity.this.getPhoneContacts();
            	super.onChange(selfChange);
            }
            @Override
            public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange);
                Log.e("","~~~~~~"+selfChange+"uri"+uri.getUserInfo());
                C2DM9Activity.this.getPhoneContacts();
                if(!AppUtil.getPrefString(context, "lastUpdatedContactTS").equalsIgnoreCase(lastUpdatedContactTS)){
                	
                	List<Contact> lastUpdatedList = new ArrayList<Contact>();
                	lastUpdatedList.add(lastUpdatedContact);
                	AppUtil.getAppUsersFromServer(C2DM9Activity.this,context, User_EndPoint, getPhoneContacts());
                }
                AppUtil.writePref(context, "lastUpdatedContactTS", lastUpdatedContactTS);
            
            }  

            @Override
            public boolean deliverSelfNotifications() {
                return true;
            }
        }
        
        public void setDefaults(String data) {
        	dismissProgressbar();
        	
        	Editor editor = AppUtil.getPrefEditor(context);
        	editor.putString("proximityQueryVar", proximityQueryVarList);
        	editor.putString("isBaseLoadComplete", "Y");
        	editor.commit();
        	
        	openNextActivity();
		}
        
        private void setRecurringAlarm(Context context) {
        	 
            Calendar updateTime = Calendar.getInstance();
            updateTime.setTimeZone(TimeZone.getTimeZone("GMT"));
//            updateTime.set(Calendar.HOUR_OF_DAY, 11);
//            updateTime.set(Calendar.MINUTE, 45);
            Intent locationUpdater = new Intent(context, AlarmReceiver.class);
            PendingIntent recurringLocationUpdate = PendingIntent.getBroadcast(context,
                    0, locationUpdater, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager alarms = (AlarmManager)context.getSystemService(
                    Context.ALARM_SERVICE);
            alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                    updateTime.getTimeInMillis(),
                    AlarmManager.INTERVAL_FIFTEEN_MINUTES, recurringLocationUpdate);
        }

    
}
