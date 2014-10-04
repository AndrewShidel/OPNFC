package com.shidel.opnfc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import net.sf.andpdf.pdfviewer.PdfViewerActivity;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

public class PdfHandler {
	private static Activity activity;
	public PdfHandler(Activity a){
		activity = a;
	}
	public void showPDF(String networkURL){
		new DownloadTask(activity).execute(networkURL);
	}
	public static class DownloadTask extends AsyncTask<String, String, String>{
		Context c;
		Toast loading;
		public DownloadTask(Context context){
			c = context;
		}
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			loading = Toast.makeText(activity, "Loading...", Toast.LENGTH_SHORT);
			loading.show();
		}

		@Override
		protected String doInBackground(String... uri) {
			try{
				URL aURL = new URL(uri[0]);
				URLConnection conn = aURL.openConnection();
				conn.connect();
				InputStream is = conn.getInputStream();

				int start = uri[0].lastIndexOf("/");
				String filename  = uri[0].substring(start+1);

				File outFile = new File(c.getExternalFilesDir(null), filename);
				OutputStream out = new FileOutputStream(outFile);

				byte[] buffer = new byte[2048];
				int bytesRead = 0;
				while ((bytesRead = is.read(buffer, 0, buffer.length)) >= 0) {
					out.write(buffer, 0, bytesRead);
				}

				out.close();
				is.close();
				return outFile.getPath();
			}catch(IOException e){
				e.printStackTrace();
			}
			return null;
		}
		@Override
		protected void onPostExecute(String path) {
			super.onPostExecute(path);
			
			loading.cancel();
			if (path!=null)
				pdfActivity(c, path);
		}
	}

	@SuppressLint("SetJavaScriptEnabled") 
	private static void pdfActivity(Context context, String id){
		final Intent intent = new Intent(context, ShowPdfActivity.class);
		intent.putExtra(PdfViewerActivity.EXTRA_PDFFILENAME, id);
		context.startActivity(intent);
	}
}
