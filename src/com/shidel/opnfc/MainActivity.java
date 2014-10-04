package com.shidel.opnfc;

import java.io.File;

import com.shidel.opnfc.NfcHandler.StatusType;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	//Text View for the main Message.
	TextView mTextView;

	private NfcHandler nfcHandler;
	
	public String baseUrl = "http://10.0.2.2:3000/";
	private static final String BASE_URL_KEY = "com.shidel.opnfc.urlKey";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//Mark any previous PDFs to be deleted.
		clearCache();
		
		
		//Get the Base URL from SharedPreferences.
		SharedPreferences prefs = getSharedPreferences(
			      "com.shidel.opnfc", Context.MODE_PRIVATE);
		baseUrl = prefs.getString(BASE_URL_KEY, baseUrl);

		mTextView = (TextView)findViewById(R.id.mainMessage);

		nfcHandler = new NfcHandler(this);

		if (nfcHandler.status == StatusType.NONE) { 
			Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_SHORT).show();
			mTextView.setText(R.string.noNFC);
			new PdfHandler(this).showPDF("http://www.eduplace.com/graphicorganizer/pdf/venn.pdf");
			return;
		} else if (nfcHandler.status == StatusType.DISSABLED) {
			mTextView.setText(R.string.NFCDissabled);
		} else {
			mTextView.setText(R.string.explanation);
		}
		nfcHandler.handleIntent(getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent) { 
		nfcHandler.handleIntent(intent);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (nfcHandler.status == StatusType.ACTIVE) {
			nfcHandler.setupForegroundDispatch(this);
		}
	}
	@Override
	protected void onPause() {
		if (nfcHandler.status == StatusType.ACTIVE) {
			nfcHandler.stopForegroundDispatch();
		}
		super.onPause();
	}

	//Marks all file in the application's external storage directory to be deleted.
	public void clearCache(){
		File f = getExternalFilesDir(null);
		File file[] = f.listFiles();
		for (int i=0; i < file.length; i++) {
			Log.d("Files", "FileName:" + file[i].getName());
			file[i].deleteOnExit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("Enter a new base URL");
			alert.setMessage("Base URL:");

			// Set an EditText view to get user input 
			final EditText input = new EditText(this);
			input.setText(baseUrl);
			alert.setView(input);

			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					baseUrl = input.getText().toString();
					
					//Change the value in SharedPreferences.
					getBaseContext().getSharedPreferences(
						      "com.shidel.opnfc", Context.MODE_PRIVATE).edit().putString(BASE_URL_KEY, baseUrl);
				}
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
				}
			});

			alert.show();
		}
		return super.onOptionsItemSelected(item);
	}
}
