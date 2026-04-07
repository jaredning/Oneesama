package catgirl.oneesama.activity.main.fragments.browse.fragments.recent.view;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import catgirl.oneesama.R;
import catgirl.oneesama.activity.common.view.ChapterViewHolderStatusDelegate;
import catgirl.oneesama.activity.main.fragments.browse.fragments.recent.data.model.RecentChapter;
import catgirl.oneesama.data.controller.ChaptersController;
import catgirl.oneesama.data.model.chapter.ui.UiTag;
import rx.subscriptions.CompositeSubscription;

public class RecentViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.Item_Chapter_Title) TextView title;
    @BindView(R.id.Item_Chapter_AuthorAndSeries) TextView authorsAndDoujins;
    @BindView(R.id.Item_Chapter_Tags) TextView tags;

    private RecentViewHolderDelegate delegate;
    private ChapterViewHolderStatusDelegate statusDelegate;

    public RecentViewHolder(View itemView, ChaptersController chaptersController, CompositeSubscription compositeSubscription) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        itemView.setOnClickListener(this::onClick);

        statusDelegate = new ChapterViewHolderStatusDelegate(itemView, chaptersController, compositeSubscription);
    }

    private void onClick(View view) {
        if (delegate != null)
            delegate.onClick();
    }

    public void bind(RecentChapter chapter, RecentViewHolderDelegate delegate) {
        this.delegate = delegate;

        title.setText(chapter.title);

        List<String> generalTags = new ArrayList<>();
        List<String> authorTags = new ArrayList<>();
        List<String> doujinTags = new ArrayList<>();

        for (UiTag tag : chapter.tags) {
            if (tag.getType().equals(UiTag.GENERAL)) {
                generalTags.add(tag.getName());
            } else if (tag.getType().equals(UiTag.AUTHOR)) {
                authorTags.add(tag.getName());
            } else if (tag.getType().equals(UiTag.DOUJIN)) {
                doujinTags.add(tag.getName());
            }
        }

        if (!generalTags.isEmpty()) {
            tags.setVisibility(View.VISIBLE);
            tags.setText(join(generalTags, ",   "));
        } else {
            tags.setVisibility(View.GONE);
        }

        if (authorTags.isEmpty() && doujinTags.isEmpty()) {
            authorsAndDoujins.setVisibility(View.GONE);
        } else {
            authorsAndDoujins.setVisibility(View.VISIBLE);

            String authorList = join(authorTags, ", ");
            String doujinList = join(doujinTags, ", ") + " Doujin";

            if(authorTags.isEmpty()) {
                authorsAndDoujins.setText(doujinList);
            } else if (doujinTags.isEmpty()) {
                authorsAndDoujins.setText(authorList);
            } else {
                authorsAndDoujins.setText(authorList + " - " + doujinList);
            }
        }

        statusDelegate.bind(chapter.chapter, chapter.permalink);
    }

    private String join(List<String> list, String separator) {
        return TextUtils.join(separator, list);
    }

    public interface RecentViewHolderDelegate {
        void onClick();
    }
}
