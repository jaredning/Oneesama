package catgirl.oneesama.activity.browseseriespage.fragment.view;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import catgirl.oneesama.R;
import catgirl.oneesama.activity.browseseriespage.fragment.data.model.BrowseSeriesPageChapter;
import catgirl.oneesama.activity.common.view.ChapterViewHolderStatusDelegate;
import catgirl.oneesama.data.controller.ChaptersController;
import rx.subscriptions.CompositeSubscription;

public class BrowseSeriesPageChapterViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.Item_Chapter_Title) TextView title;

    ChapterViewHolderStatusDelegate statusDelegate;
    private ViewHolderDelegate delegate;

    public BrowseSeriesPageChapterViewHolder(View itemView, ChaptersController chaptersController, CompositeSubscription compositeSubscription) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        statusDelegate = new ChapterViewHolderStatusDelegate(itemView, chaptersController, compositeSubscription);
        itemView.setOnClickListener(this::onClick);
    }

    private void onClick(View view) {
        if (delegate != null)
            delegate.onClick();
    }

    public void bind(BrowseSeriesPageChapter chapter, ViewHolderDelegate delegate) {
        this.delegate = delegate;
        title.setText(chapter.title);
        statusDelegate.bind(chapter.chapter, chapter.permalink);
    }

    public interface ViewHolderDelegate {
        void onClick();
    }
}
