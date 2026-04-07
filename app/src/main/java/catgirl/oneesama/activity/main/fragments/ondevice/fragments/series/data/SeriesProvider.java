package catgirl.oneesama.activity.main.fragments.ondevice.fragments.series.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import catgirl.oneesama.activity.common.data.AutoRefreshableRealmProvider;
import catgirl.oneesama.activity.main.fragments.ondevice.fragments.series.data.model.SeriesAuthor;
import catgirl.oneesama.data.model.chapter.serializable.Chapter;
import catgirl.oneesama.data.model.chapter.serializable.Tag;
import catgirl.oneesama.data.model.chapter.ui.UiTag;
import catgirl.oneesama.data.realm.RealmProvider;
import catgirl.oneesama.tools.NaturalOrderComparator;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import rx.Observable;

public class SeriesProvider extends AutoRefreshableRealmProvider<Tag, SeriesAuthor> {
    private RealmProvider realmProvider;
    public SeriesProvider(RealmProvider realmProvider) {
        this.realmProvider = realmProvider;
    }

    @Override
    public Realm getRealm() {
        return realmProvider.provideRealm();
    }

    @Override
    public RealmQuery<Tag> getQuery(Realm realm) {
        return realm.where(Tag.class)
                .equalTo("type", UiTag.SERIES);
    }

    @Override
    public List<SeriesAuthor> processQueryResults(Realm realm, RealmResults<Tag> results) {
        List<SeriesAuthor> result = new ArrayList<>();

        Observable.from(results)
                .map(series -> {
                    Chapter chapter = realm.where(Chapter.class)
                            .equalTo("tags.id", series.getId())
                            .findFirst();

                    if (chapter == null) {
                        return new SeriesAuthor(new UiTag(series), null);
                    }

                    Tag author = chapter.getTags()
                            .where()
                            .equalTo("type", UiTag.AUTHOR)
                            .findFirst();
                    if (author != null)
                        return new SeriesAuthor(new UiTag(series), new UiTag(author));
                    else
                        return new SeriesAuthor(new UiTag(series), null);
                })
                .toList()
                .subscribe(result::addAll);

        Collections.sort(result, (lhs, rhs) -> new NaturalOrderComparator().compare(lhs.series.getName(), rhs.series.getName()));

        return result;
    }
}
