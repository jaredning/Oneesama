package catgirl.oneesama.activity.main.fragments.browse.fragments.series.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import catgirl.oneesama.activity.common.data.model.LazyLoadResult;
import catgirl.oneesama.activity.main.fragments.browse.fragments.series.data.model.SeriesItem;
import catgirl.oneesama.data.network.api.DynastyService;
import rx.Observable;

public class SeriesProvider {

    private final DynastyService service;

    public SeriesProvider(DynastyService service) {
        this.service = service;
    }

    public Observable<LazyLoadResult<SeriesItem>> getMoreSeries(int page) {
        return getNewSeries(page);
    }

    public Observable<LazyLoadResult<SeriesItem>> getNewSeries(int page) {
        return service.getAllSeries(page)
                .map(result -> {
                    List<SeriesItem> seriesList = new ArrayList<>();
                    Gson gson = new Gson();

                    Object tags = result.get("tags");
                    if (tags instanceof List) {
                        List<?> tagsList = (List<?>) tags;
                        for (Object tagElement : tagsList) {
                            if (tagElement instanceof Map) {
                                Map<?, ?> tagMap = (Map<?, ?>) tagElement;
                                for (Object value : tagMap.values()) {
                                    if (value instanceof List) {
                                        String json = gson.toJson(value);
                                        SeriesItem[] items = gson.fromJson(json, SeriesItem[].class);
                                        if (items != null) {
                                            for (SeriesItem item : items) {
                                                if (item.name != null && item.permalink != null) {
                                                    seriesList.add(item);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    int totalPages = 1;
                    Object totalPagesObj = result.get("total_pages");
                    if (totalPagesObj instanceof Double) {
                        totalPages = ((Double) totalPagesObj).intValue();
                    } else if (totalPagesObj instanceof Integer) {
                        totalPages = (Integer) totalPagesObj;
                    }

                    return new LazyLoadResult<>(seriesList, page >= totalPages, totalPages);
                });
    }
}
