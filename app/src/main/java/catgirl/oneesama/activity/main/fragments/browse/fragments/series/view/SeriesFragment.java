package catgirl.oneesama.activity.main.fragments.browse.fragments.series.view;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import catgirl.oneesama.R;
import catgirl.oneesama.activity.browseseriespage.BrowseSeriesPageActivity;
import catgirl.oneesama.activity.common.view.LazyLoadFragment;
import catgirl.oneesama.activity.main.MainActivityModule;
import catgirl.oneesama.activity.main.fragments.browse.fragments.recent.view.ErrorViewHolder;
import catgirl.oneesama.activity.main.fragments.browse.fragments.series.BrowseSeriesComponent;
import catgirl.oneesama.activity.main.fragments.browse.fragments.series.BrowseSeriesModule;
import catgirl.oneesama.activity.main.fragments.browse.fragments.series.data.model.SeriesItem;
import catgirl.oneesama.activity.main.fragments.browse.fragments.series.presenter.SeriesPresenter;
import catgirl.oneesama.application.Application;

public class SeriesFragment
        extends LazyLoadFragment<SeriesItem, SeriesPresenter, BrowseSeriesComponent>
        implements SeriesView {

    @Nullable @BindView(R.id.PrevPage) Button prevPage;
    @Nullable @BindView(R.id.NextPage) Button nextPage;
    @Nullable @BindView(R.id.PageIndicator) TextView pageIndicator;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_series_with_pagination, container, false);
        return super.setupUI(view);
    }

    @Override
    protected BrowseSeriesComponent createComponent() {
        return Application.getApplicationComponent().plus(new MainActivityModule()).plus(new BrowseSeriesModule());
    }

    @Override
    protected void onComponentCreated() {
        getComponent().inject(this);
    }

    @Override
    protected SeriesPresenter createPresenter() {
        return getComponent().getPresenter();
    }

    @Override
    protected int getItemCount() {
        return getPresenter().getItemCount();
    }

    @Override
    protected long getItemId(int position) {
        SeriesItem item = getPresenter().getItem(position);
        if (item != null && item.permalink != null) {
            return item.permalink.hashCode();
        }
        return position;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getPresenter().loadNew();
    }

    @Override
    protected void loadNew() {
        getPresenter().loadNew();
    }

    @Override
    protected void loadMore() {
        // Disabled for manual pagination
    }

    @Override
    protected RecyclerView.ViewHolder createViewHolder(ViewGroup parent) {
        return new SeriesViewHolder(
                getActivity().getLayoutInflater().inflate(R.layout.item_series, parent, false));
    }

    @Override
    protected void bindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((SeriesViewHolder) holder).bind(
                getPresenter().getItem(position),
                () -> getPresenter().itemClicked(position));
    }

    @Override
    protected RecyclerView.ViewHolder createErrorViewHolder(ViewGroup parent) {
        View errorView = getActivity().getLayoutInflater().inflate(R.layout.item_error_try_again, parent, false);
        errorView.findViewById(R.id.ReloadButton).setOnClickListener(view -> {
            onErrorReloadPressed();
        });
        return new ErrorViewHolder(errorView);
    }

    @Override
    protected View getEmptyMessage(ViewGroup parent) {
        return new View(parent.getContext());
    }

    @Override
    protected void showMoreItemsErrorToast() {
        Toast.makeText(getActivity(), R.string.fragment_browseseries_error, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void showNewItemsErrorToast() {
        Toast.makeText(getActivity(), R.string.fragment_browseseries_refresh_error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void switchToSeries(String seriesPermalink, String title) {
        Intent intent = new Intent(getActivity(), BrowseSeriesPageActivity.class);
        intent.putExtra(BrowseSeriesPageActivity.SERIES_PERMALINK, seriesPermalink);
        intent.putExtra(BrowseSeriesPageActivity.SERIES_TITLE, title);
        startActivity(intent);
    }

    @Override
    public void updatePagination(int currentPage, int totalPages) {
        if (pageIndicator != null) {
            pageIndicator.setText("Page " + currentPage + " of " + totalPages);
        }
        if (prevPage != null) {
            prevPage.setEnabled(currentPage > 1);
            prevPage.setOnClickListener(v -> getPresenter().prevPage());
        }
        if (nextPage != null) {
            nextPage.setEnabled(currentPage < totalPages);
            nextPage.setOnClickListener(v -> getPresenter().nextPage());
        }
    }
}
