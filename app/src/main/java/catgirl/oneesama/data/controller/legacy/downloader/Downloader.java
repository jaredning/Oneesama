package catgirl.oneesama.data.controller.legacy.downloader;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import catgirl.oneesama.application.Application;
import catgirl.oneesama.application.Config;
import catgirl.oneesama.data.controller.FileManager;
import catgirl.oneesama.data.model.chapter.ui.UiPage;

public class Downloader {

	public BookDownloaderDelegate delegate = null;
	
	public boolean requiresToken = false;
	public boolean requiresPassword = false;
	public String login;
	public String password;
	
	public String token;
	
	private boolean active = true;
	
	public boolean checkModify = false;
	public Date modifyDate;
	
	Handler handler = new Handler(Looper.getMainLooper());
	
	public boolean continueOnFailure = true;
	
		public HashSet<Integer> ids = new HashSet<Integer>();
	
	private static final ExecutorService executor = Executors.newFixedThreadPool(4);

	boolean paused = false;

	int bookId;
	
	public Downloader(BookDownloaderDelegate delegate, int bookId)
	{
		init(delegate, bookId);
	}

	public void pause()
	{
		paused = true;
	}
	
	public void init(BookDownloaderDelegate delegate, int bookId)
	{
		this.delegate = delegate;
		this.bookId = bookId;
	}
	
	public static boolean isOnline() {
	    if(Application.getContextOfApplication() == null)
	    {
	    	Log.v("Downloader", "App context not found");
	    	return false;
	    }
	    
		ConnectivityManager cm =
	        (ConnectivityManager) Application.getContextOfApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
		
	    if(cm == null)
	    {
	    	Log.v("Downloader", "Connectivity manager not found");
	    	return false;
	    }
	    
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
		return netInfo != null && netInfo.isConnected();
	}
	
	public synchronized int getId()
	{
		int id = 0;
		while(ids.contains(id))
			id++;
		ids.add(id);
		return id;
	}
	
	public synchronized void freeId(int id)
	{
		ids.remove(id);
	}
	
	Downloader self = this;
	
	public void download(final int pageId, final UiPage page)
	{
		isCancelled = false;
		// The download thread
		executor.execute(new Runnable() {
			public void run()
			{
				int id = 0;
				id = getId();

				if(id < 0)
					return;
					
				int i = 5;
				while(!isOnline())
				{	
					try {
						Thread.sleep(100);
						i--;
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					}
					if(i <= 0)
					{
						delegate.onDownloadFailed(pageId, true, false);
						return;
					}
				}

				RetStream input = null;
				String suffix = "";

				Log.v("Log", "Downloading " + page.getUrl());

				try {
					input = self.openDownloadStream(Config.apiEndpoint + page.getUrl());
				} catch (Exception e) {
					Log.v("Downloader", "Resource not found\n" + page.getUrl());
					e.printStackTrace();
					Log.v("Downloader", Log.getStackTraceString(e));

					if(delegate != null)
					{
						delegate.onDownloadFailed(pageId, true, false);
					}
					freeId(id);
					return;
				}
				   
				try {
					if(input == null)
						throw new DownloaderException("Input RetStream is null, shouldn't happen");
					else
					{
						if(input.input == null)
							throw new DownloaderException("Input InputStream is null, shouldn't happen.");

						boolean success;

						File src = FileManager.getCache("" + bookId + "_temp" + id + ".publ");

						OutputStream output = null;
						output = new FileOutputStream(src);

						byte data[] = new byte[16 * 1024];
						long total = 0;
						int count;
						long lastUpdate = 0;
						while ((count = input.input.read(data)) != -1) {
							total += count;
							if(delegate != null && input.length > 0)
							{
								long now = System.currentTimeMillis();
								if (now - lastUpdate > 100 || total == input.length) {
									delegate.onDownloadProgress(pageId, (float) total / (float) input.length);
									lastUpdate = now;
								}
							}
							output.write(data, 0, count);
						}

						output.flush();
						output.close();
						input.input.close();

						if(isCancelled) {
							src.delete();
							success = false;
						} else {
							DocumentFile chapterDir = FileManager.getChapterDirectory(Application.getContextOfApplication(), bookId);
							if (chapterDir != null && chapterDir.exists()) {
								String[] parts = page.getUrl().split("/");
								String fileName = parts[parts.length - 1];
								DocumentFile file = chapterDir.findFile(fileName);
								if (file != null) {
									// Rename before delete to avoid SAF ghosting/concurrency issues
									file.renameTo("del_" + System.currentTimeMillis() + "_" + fileName);
									file.delete();
								}
								file = chapterDir.createFile("image/*", fileName);
								if (file != null) {
									try (InputStream in = new FileInputStream(src);
										 OutputStream out = Application.getContextOfApplication().getContentResolver().openOutputStream(file.getUri())) {
										byte[] buf = new byte[16 * 1024];
										int len;
										while ((len = in.read(buf)) > 0) {
											out.write(buf, 0, len);
										}
										success = true;
									} catch (Exception e) {
										e.printStackTrace();
										success = false;
									}
									src.delete();
								} else {
									success = false;
								}
							} else {
								File file = FileManager.getPageFile(bookId, page);
								if(file.getParentFile() != null)
									file.getParentFile().mkdirs();
								success = src.renameTo(file);
							}
						}
					    
					    if(!success)
					    {
					    	if(delegate != null)
					    	{
								Log.v("Downloader", "Download failed on move\n" + page.getUrl() );
					    		delegate.onDownloadFailed(pageId, false, false);
					    	}
					    }
					    else
					    {
					    	if(delegate != null)
					    		delegate.onDownloadComplete(pageId, false);

							Log.v("Downloader", "Download succeeded\n" + page.getUrl() );
					    }
					}
				    
				} catch (DownloaderException e) {					
					Log.v("Downloader", "Download failed\n" + page.getUrl() );
					Log.v("Downloader", Log.getStackTraceString(e));
					
			    	if(delegate != null)
			    	{
			    		delegate.onDownloadFailed(pageId, true, false);
			    	}
				} catch (FileNotFoundException e) {
					Log.v("Downloader", Log.getStackTraceString(e));
			    	if(delegate != null)
			    	{
			    		delegate.onDownloadFailed(pageId, true, false);
			    	}
				} catch (IOException e) {
					Log.v("Downloader", Log.getStackTraceString(e));
			    	if(delegate != null)
			    	{
			    		delegate.onDownloadFailed(pageId, true, false);
			    	}
				}
				finally
				{
					freeId(id);
				}
				return;

			}

		});
	}

	public void cancel() {
		isCancelled = true;
	}


	public class RetStream { 
		public InputStream input; 
		public int length;
	}

	boolean isCancelled = false;
	
	private synchronized RetStream openDownloadStream(String inurl) throws Exception
	{
		URL url = null;

		url = new URL(inurl);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		if(isCancelled)
			return null;


		if(isCancelled)
			return null;

        connection.setInstanceFollowRedirects(false);
        connection.setReadTimeout(5000);
        connection.setConnectTimeout(5000);
        
        // If we're updating publication, make sure to avoid downloading stuff that hasn't been updated.
        if(checkModify)
        {
        	connection.setIfModifiedSince(modifyDate.getTime());
        }
        
        connection.connect();
        
        while(connection.getHeaderField("Location") != null && connection.getHeaderField("Location").length() > 4)
        {
			url = new URL(connection.getHeaderField("Location"));
        	
        	connection = (HttpURLConnection) url.openConnection();
        	connection.setInstanceFollowRedirects(false);
            connection.setReadTimeout(5000);
            connection.setConnectTimeout(5000);


            if(checkModify)
            	connection.setIfModifiedSince(modifyDate.getTime());
            
            connection.connect();
        }

        // this will be useful so that you can show a typical 0-100% progress bar
        int fileLength = connection.getContentLength();

        // download the file
        InputStream input = new BufferedInputStream(connection.getInputStream());
        RetStream r = new RetStream();
        r.input = input;
        r.length = fileLength;
        
        return r;
	}

	public void resume() {
		paused = false;
	}
	
}
