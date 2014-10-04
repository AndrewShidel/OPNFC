package com.shidel.opnfc;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;


public class NfcHandler {
	private static Activity activity;
	private NfcAdapter mNfcAdapter;

	public static final String MIME_TEXT_PLAIN = "text/plain";
	
	
	public enum StatusType {NONE, DISSABLED, ACTIVE};
	public StatusType status;
	
	public NfcHandler(Activity a){
		activity = a;
		
		mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
		if (mNfcAdapter==null)
			status = StatusType.NONE;
		else if(mNfcAdapter.isEnabled())
			status = StatusType.ACTIVE;
		else
			status = StatusType.DISSABLED;
	}
    public void setupForegroundDispatch(final Activity activity) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
 
        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
 
        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};
 
        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }
         
        mNfcAdapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }
    
    /**
     * @param activity The corresponding {@link BaseActivity} requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public void stopForegroundDispatch() {
    	mNfcAdapter.disableForegroundDispatch(activity);
    }
    
	public void handleIntent(Intent intent) {
	    String action = intent.getAction();
	    if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
	         
	        String type = intent.getType();
	        if (MIME_TEXT_PLAIN.equals(type)) {
	 
	            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
	            new NdefReaderTask().execute(tag);
	             
	        } else {
	            Log.d("Debug", "Wrong mime type: " + type);
	        }
	    } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
	         
	        // In case we would still use the Tech Discovered Intent
	        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
	        String[] techList = tag.getTechList();
	        String searchedTech = Ndef.class.getName();
	         
	        for (String tech : techList) {
	            if (searchedTech.equals(tech)) {
	                new NdefReaderTask().execute(tag);
	                break;
	            }
	        }
	    }
	}
	
	private class NdefReaderTask extends AsyncTask<Tag, Void, String> {
		 
	    @Override
	    protected String doInBackground(Tag... params) {
	        Tag tag = params[0];
	         
	        Ndef ndef = Ndef.get(tag);
	        if (ndef == null) {
	            // NDEF is not supported by this Tag. 
	            return null;
	        }
	 
	        NdefMessage ndefMessage = ndef.getCachedNdefMessage();
	 
	        NdefRecord[] records = ndefMessage.getRecords();
	        for (NdefRecord ndefRecord : records) {
	            if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
	                try {
	                    return readText(ndefRecord);
	                } catch (UnsupportedEncodingException e) {
	                    Log.e("Debug", "Unsupported Encoding", e);
	                }
	            }
	        }
	 
	        return null;
	    }
	     
	    private String readText(NdefRecord record) throws UnsupportedEncodingException {
	        byte[] payload = record.getPayload();
	 
	        // Get the Text Encoding
	        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
	 
	        // Get the Language Code
	        int languageCodeLength = payload[0] & 0063;

	        // Get the Text
	        return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
	    }
	     
	    @Override
	    protected void onPostExecute(String result) {
	        if (result != null) {
	        	new PdfHandler(activity).showPDF(((MainActivity)activity).baseUrl+"/"+result+".pdf");
	            ((TextView) activity.findViewById(R.id.mainMessage)).setText("Read content: " + result);
	        }
	    }
	}
}
