package catgirl.oneesama.activity.main.fragments.ondevice.fragments.doujins.view;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import catgirl.oneesama.R;
import catgirl.oneesama.data.model.chapter.ui.UiTag;

public class DoujinsViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.Item_Series_Author) TextView author;
    @BindView(R.id.Item_Series_Title) TextView title;

    private SeriesViewHolderDelegate delegate;

    public DoujinsViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        itemView.setOnClickListener(this::onClick);
    }

    public void bind(UiTag data, SeriesViewHolderDelegate delegate) {
        this.delegate = delegate;

        author.setText(null);

        title.setText(data.getName());
    }

    public void onClick(View view) {
        if (delegate != null) {
            delegate.onClick();
        }
    }

    public interface SeriesViewHolderDelegate {
        void onClick();
    }
}
