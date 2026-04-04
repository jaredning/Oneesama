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

    public Observable<LazyLoadResult<SeriesItem>> getMoreSeries() {
        return getNewSeries().map(result -> new LazyLoadResult<>(result, true));
    }

    public Observable<List<SeriesItem>> getNewSeries() {
        return service.getAllSeries()
                .map(result -> {
                    List<SeriesItem> seriesList = new ArrayList<>();
                    Gson gson = new Gson();

                    for (Map.Entry<String, Object> entry : result.entrySet()) {
                        if (entry.getValue() instanceof List) {
                            String json = gson.toJson(entry.getValue());
                            SeriesItem[] items = gson.fromJson(json, SeriesItem[].class);
                            for (SeriesItem item : items) {
                                if (item.name != null && item.permalink != null) {
                                    seriesList.add(item);
                                }
                            }
                        }
                    }

                    return seriesList;
                });
    }
}
