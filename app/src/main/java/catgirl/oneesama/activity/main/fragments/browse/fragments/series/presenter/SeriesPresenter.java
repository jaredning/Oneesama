package catgirl.oneesama.activity.main.fragments.browse.fragments.series.presenter;

import java.util.List;

import catgirl.oneesama.activity.common.data.model.LazyLoadResult;
import catgirl.oneesama.activity.common.presenter.ReplaceOnRefreshPresenter;
import catgirl.oneesama.activity.main.fragments.browse.fragments.series.data.SeriesProvider;
import catgirl.oneesama.activity.main.fragments.browse.fragments.series.data.model.SeriesItem;
import catgirl.oneesama.activity.main.fragments.browse.fragments.series.view.SeriesView;
import rx.Observable;

public class SeriesPresenter extends ReplaceOnRefreshPresenter<SeriesItem, SeriesView> {

    private SeriesProvider seriesProvider;
    private int currentPage = 1;
    private int totalPages = 1;

    public SeriesPresenter(SeriesProvider seriesProvider) {
        this.seriesProvider = seriesProvider;
    }

    @Override
    protected Observable<LazyLoadResult<SeriesItem>> getMoreItemsObservable() {
        return seriesProvider.getNewSeries(currentPage)
                .doOnNext(result -> {
                    this.totalPages = result.totalPages;
                });
    }

    @Override
    protected Observable<List<SeriesItem>> getNewItemsObservable() {
        return seriesProvider.getNewSeries(currentPage)
                .doOnNext(result -> {
                    this.totalPages = result.totalPages;
                })
                .map(result -> result.elements);
    }

    public void nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadNew();
        }
    }

    public void prevPage() {
        if (currentPage > 1) {
            currentPage--;
            loadNew();
        }
    }

    @Override
    protected void onItemsUpdated() {
        if (getView() != null) {
            getView().updatePagination(currentPage, totalPages);
        }
    }

    @Override
    public void itemClicked(int position) {
        if (getView() != null)
            getView().switchToSeries(items.get(position).permalink, items.get(position).name);
    }
}
