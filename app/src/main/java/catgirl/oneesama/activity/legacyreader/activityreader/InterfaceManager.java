package catgirl.oneesama.activity.legacyreader.activityreader;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.ProgressBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import catgirl.oneesama.R;
import catgirl.oneesama.data.controller.legacy.Book;
import catgirl.oneesama.activity.legacyreader.tools.ActivityUtils;

public class InterfaceManager {

	Book book;
	ReaderActivity activity;
	
	boolean interfaceAnimating = false;
	
	@BindView(R.id.InterfaceLayout) ViewGroup interfaceLayout;
	@BindView(R.id.InterfaceTopBar) ViewGroup interfaceTopBar;

	float mdx = 0;
	float scale = 1;

	@BindView(R.id.pageLabel) TextView pageLabel;
	@BindView(R.id.bookTitle) TextView bookTitle;
	@BindView(R.id.downloadProgress) ProgressBar progressBar;
	@BindView(R.id.DownloadProgressLayout) View downloadProgressLayout;

	public InterfaceManager self = this;
	
	public InterfaceManager(ReaderActivity activity, Book book) {
		this.book = book;
		this.activity = activity;
		ButterKnife.bind(this, activity);
	}
	
	public void setupInterface()
	{
		bookTitle.setText((activity.updateBook != null ? "Updating: " : "") + book.data.getTitle());

		interfaceLayout.clearAnimation();
		interfaceLayout.setVisibility(View.GONE);
		interfaceTopBar.clearAnimation();
		interfaceTopBar.setVisibility(View.GONE);
		ActivityUtils.enableDisableViewGroup(interfaceLayout, false);

		activity.findViewById(R.id.BookTitleLayout).bringToFront();

		updateProgress();

		h.postDelayed(r, 10000);
	}
	
	public void showInterface()
	{
			interfaceLayout.clearAnimation();
			interfaceLayout.setVisibility(View.VISIBLE);
			interfaceTopBar.setVisibility(View.VISIBLE);
			ActivityUtils.enableDisableViewGroup(interfaceLayout, true);
			
			activity.findViewById(R.id.InterfaceTopBar).clearAnimation();
			activity.findViewById(R.id.InterfaceTopBar).setVisibility(View.VISIBLE);
			activity.findViewById(R.id.InterfaceBottomBar).clearAnimation();
			activity.findViewById(R.id.InterfaceBottomBar).setVisibility(View.VISIBLE);
			
			if (downloadProgressLayout != null) {
				if(book.completelyDownloaded && !(activity.updateBook != null && !activity.updateBook.completelyDownloaded))
					downloadProgressLayout.setVisibility(View.GONE);
				else
					downloadProgressLayout.setVisibility(View.VISIBLE);
			}
		    
    		activity.findViewById(R.id.BookTitleLayout).bringToFront();
//    		MApplication.interfaceActive = true;
	}
	
	private void hideInterfaceAnimation()
	{
		Animation fadeOutAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, -1f);
//		fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
	    fadeOutAnimation.setDuration(200);
	    fadeOutAnimation.setAnimationListener(new AnimationListener() {
	    	@Override
	    	public void onAnimationEnd(Animation animation) {
	    		activity.findViewById(R.id.InterfaceTopBar).clearAnimation();
	    		activity.findViewById(R.id.InterfaceTopBar).setVisibility(View.GONE);
	    		
	    		Animation slideBottom = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 1f);
	    		slideBottom.setDuration(200);
	    		slideBottom.setAnimationListener(new AnimationListener() {
					@Override
					public void onAnimationEnd(Animation animation) {
						interfaceLayout.clearAnimation();
			    		interfaceLayout.setVisibility(View.GONE);
						interfaceTopBar.setVisibility(View.GONE);
			    		ActivityUtils.enableDisableViewGroup(interfaceLayout, false);
			    		interfaceAnimating = false;
					}
					@Override
					public void onAnimationRepeat(Animation animation) {}
					@Override
					public void onAnimationStart(Animation animation) {}});
	    		activity.findViewById(R.id.InterfaceBottomBar).startAnimation(slideBottom);
	    		
	    	}
	    	@Override
	    	public void onAnimationRepeat(Animation animation) {}
	    	@Override
	    	public void onAnimationStart(Animation animation) {}
	    });

	    activity.findViewById(R.id.InterfaceTopBar).startAnimation(fadeOutAnimation);
	}
	
	Handler h = new Handler();
	Runnable r = new Runnable(){

		@Override
		public void run() {
			self.reactInterface();
		}
		
	};
	
	public void reactInterface()
	{
		if(interfaceAnimating)
			return;
		
		h.removeCallbacks(r);
		
		interfaceAnimating = true;
		
//		MApplication.interfaceActive = !MApplication.interfaceActive;
		
		if(interfaceLayout.getVisibility() == View.VISIBLE)
		{
			if(book.completelyDownloaded && !(activity.updateBook != null && !activity.updateBook.completelyDownloaded))
				hideInterfaceAnimation();
			else
			{
				activity.findViewById(R.id.BookTitleLayout).bringToFront();
				interfaceAnimating = true;
				Animation dlout = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, -1.25f);
				dlout.setDuration(200);
				dlout.setAnimationListener(new AnimationListener() {
			    	@Override
			    	public void onAnimationEnd(Animation animation) {
			    		activity.findViewById(R.id.DownloadProgressLayout).clearAnimation();
			    		activity.findViewById(R.id.DownloadProgressLayout).setVisibility(View.GONE);
			    		hideInterfaceAnimation();
			    	}
			    	@Override
			    	public void onAnimationRepeat(Animation animation) {}
			    	@Override
			    	public void onAnimationStart(Animation animation) {}
			    });
				activity.findViewById(R.id.DownloadProgressLayout).startAnimation(dlout);
			}
		}
		else
		{
			interfaceLayout.clearAnimation();
			interfaceLayout.setVisibility(View.VISIBLE);
			interfaceTopBar.setVisibility(View.VISIBLE);
			activity.findViewById(R.id.InterfaceTopBar).clearAnimation();
			activity.findViewById(R.id.InterfaceTopBar).setVisibility(View.GONE);
			ActivityUtils.enableDisableViewGroup(interfaceLayout, true);
//			pageSeekBar.requestLayout();
			
    		if(book.completelyDownloaded && !(activity.updateBook != null && !activity.updateBook.completelyDownloaded))
    		{
    			activity.findViewById(R.id.DownloadProgressLayout).clearAnimation();
    			activity.findViewById(R.id.DownloadProgressLayout).setVisibility(View.GONE);
    		}
			
			Animation fadeInAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF, 0f);
//			fadeInAnimation = new AlphaAnimation(0.0f, 1.0f);
			fadeInAnimation.setDuration(200);
			fadeInAnimation.setAnimationListener(new AnimationListener() {
		    	@Override
		    	public void onAnimationEnd(Animation animation) {
		    		activity.findViewById(R.id.InterfaceTopBar).clearAnimation();
		    		activity.findViewById(R.id.DownloadProgressLayout).clearAnimation();
		    		activity.findViewById(R.id.InterfaceTopBar).setVisibility(View.VISIBLE);
		    		activity.findViewById(R.id.DownloadProgressLayout).setVisibility(View.GONE);
//		    		interfaceAnimating = false;
		    		
		    		Animation slideBottom = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, -1f, Animation.RELATIVE_TO_SELF, 0f);
		    		slideBottom.setDuration(200);
		    		slideBottom.setAnimationListener(new AnimationListener() {
						@Override
						public void onAnimationEnd(Animation animation) {
				    		if(book.completelyDownloaded && !(activity.updateBook != null && !activity.updateBook.completelyDownloaded))
				    		{
				    			interfaceAnimating = false;
				    		}
				    		else
				    		{
				    			activity.findViewById(R.id.BookTitleLayout).bringToFront();
				    			activity.findViewById(R.id.DownloadProgressLayout).clearAnimation();
					    		activity.findViewById(R.id.DownloadProgressLayout).setVisibility(View.VISIBLE);
								Animation dlout = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, -1.25f, Animation.RELATIVE_TO_SELF, 0f);
								dlout.setDuration(200);
								dlout.setAnimationListener(new AnimationListener() {
							    	@Override
							    	public void onAnimationEnd(Animation animation) {
							    		interfaceAnimating = false;
							    	}
							    	@Override
							    	public void onAnimationRepeat(Animation animation) {}
							    	@Override
							    	public void onAnimationStart(Animation animation) {}
							    });
								activity.findViewById(R.id.DownloadProgressLayout).startAnimation(dlout);
				    		}
						}
						@Override
						public void onAnimationRepeat(Animation animation) {}
						@Override
						public void onAnimationStart(Animation animation) {}});
		    		activity.findViewById(R.id.InterfaceTopBar).startAnimation(slideBottom);
		    	}
		    	@Override
		    	public void onAnimationRepeat(Animation animation) {}
		    	@Override
		    	public void onAnimationStart(Animation animation) {}
		    });
	
			activity.findViewById(R.id.InterfaceBottomBar).startAnimation(fadeInAnimation);
		}

	}

	public void hideDownloadbar()
	{
		activity.runOnUiThread(new Runnable(){

			@Override
			public void run() {
				if(downloadProgressLayout != null && downloadProgressLayout.getVisibility() == View.VISIBLE && !interfaceAnimating)
				{
					activity.findViewById(R.id.BookTitleLayout).bringToFront();
					interfaceAnimating = true;
					Animation dlout = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, -1.25f);
					dlout.setDuration(200);
					dlout.setAnimationListener(new AnimationListener() {
				    	@Override
				    	public void onAnimationEnd(Animation animation) {
				    		if (downloadProgressLayout != null) {
								downloadProgressLayout.clearAnimation();
								downloadProgressLayout.setVisibility(View.GONE);
							}
				    		interfaceAnimating = false;
				    	}
				    	@Override
				    	public void onAnimationRepeat(Animation animation) {}
				    	@Override
				    	public void onAnimationStart(Animation animation) {}
				    });
					downloadProgressLayout.startAnimation(dlout);
				}
			}
		});
	}
	
	public void updateProgress()
	{
		if(progressBar != null)
		{
			if(activity.updateBook != null)
			{
				progressBar.setMax(activity.updateBook.totalFiles);
				progressBar.setProgress(activity.updateBook.pagesDownloaded);
			}
			else
			{
				progressBar.setMax(book.totalFiles);
				progressBar.setProgress(book.pagesDownloaded);
			}
		}
	}
	
	public void updateLabel(int pageId)
	{
		if(pageLabel != null)
		{
			pageLabel.setText(activity.getResources().getString(R.string.PAGE_TITLE) + " " + book.getContentsPageName(pageId) + " [ " + (pageId + 1) + " / " + book.bookPages.size() + " ]");
		}
	}

	public void showDownloadBar() {
		if(downloadProgressLayout != null && downloadProgressLayout.getVisibility() != View.VISIBLE && !interfaceAnimating)
		{
			activity.findViewById(R.id.BookTitleLayout).bringToFront();
			interfaceAnimating = true;
			Animation dlin = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, -1.25f, Animation.RELATIVE_TO_SELF, 0f);
			dlin.setDuration(200);
			dlin.setAnimationListener(new AnimationListener() {
		    	@Override
		    	public void onAnimationEnd(Animation animation) {
		    		if (downloadProgressLayout != null) {
						downloadProgressLayout.clearAnimation();
					}
		    		interfaceAnimating = false;
		    	}
		    	@Override
		    	public void onAnimationRepeat(Animation animation) {}
		    	@Override
		    	public void onAnimationStart(Animation animation) {}
		    });

    		downloadProgressLayout.setVisibility(View.VISIBLE);
			downloadProgressLayout.startAnimation(dlin);
		}
	}

	public void startUpdate() {
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				if(interfaceLayout.getVisibility() != View.VISIBLE)
					reactInterface();
				h.removeCallbacks(r);
				
				bookTitle.setText("Updating: " + book.data.getTitle());
				showDownloadBar();
				updateProgress();
			}
		});
		
	}

	public void endUpdate() {
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				bookTitle.setText(book.data.getTitle());
			}
		});
	}

	public void updateName() {
		bookTitle.setText(book.data.getTitle());
		bookTitle.invalidate();
	}

	public void abortHideInterface() {
		h.removeCallbacks(r);
	}
}
