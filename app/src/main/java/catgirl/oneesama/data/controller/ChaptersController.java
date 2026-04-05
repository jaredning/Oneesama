package catgirl.oneesama.data.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catgirl.oneesama.application.Application;
import catgirl.oneesama.data.network.api.DynastyService;
import catgirl.oneesama.data.controller.legacy.Book;
import catgirl.oneesama.data.controller.legacy.BookStateDelegate;
import catgirl.oneesama.data.controller.legacy.CacherDelegate;
import catgirl.oneesama.data.model.chapter.serializable.Chapter;
import catgirl.oneesama.data.model.chapter.serializable.Page;
import catgirl.oneesama.data.model.chapter.serializable.Tag;
import catgirl.oneesama.data.model.chapter.ui.UiChapter;
import catgirl.oneesama.data.network.scraper.chaptername.DynastySeriesPage;
import catgirl.oneesama.data.network.scraper.chaptername.DynastySeriesPageProvider;
import io.realm.Realm;
import io.realm.RealmObject;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class ChaptersController implements BookStateDelegate, CacherDelegate {
    private static ChaptersController ourInstance = new ChaptersController();

    private PublishSubject<UiChapter> publisher = PublishSubject.create();

    public static ChaptersController getInstance() {
        return ourInstance;
    }

    private Map<Integer, Book> controllers = new HashMap<>();

    private ChaptersController() {

    }

    public Book getChapterController(int id) {
        if(controllers.containsKey(id))
            return controllers.get(id);
        else {
            Realm realm = Realm.getDefaultInstance();
            try {
                Chapter chapter = realm.where(Chapter.class).equalTo("id", id).findFirst();
                if (chapter == null)
                    return null;

                UiChapter uiChapter = new UiChapter(chapter);
                Book book = new Book(uiChapter, this, this, false, null);
                book.startDownload();
                controllers.put(id, book);
                publisher.onNext(uiChapter);
                return book;
            } finally {
                realm.close();
            }
        }
    }

    public boolean isChapterControllerActive(int id) {
        return controllers.containsKey(id);
    }

    // TODO create a data abstraction layer
    public Observable<Book> requestChapterController(final String uri) {

        for (Book book : controllers.values()) {
            if (book.data.getPermalink().equals(uri)) {
                return Observable.just(book).observeOn(AndroidSchedulers.mainThread());
            }
        }

        DynastyService service = Application.getApplicationComponent().getDynastyService();

        return service.getChapter(uri)
                .subscribeOn(Schedulers.io())
                .doOnNext(this::checkChapterIdAgainstLocalDatabase)
                .doOnNext(response -> checkTagIdsAgainstLocalDatabase(response.getTags()))
                .doOnNext(this::findRealChapterName)
                .doOnNext(response -> {
                    Realm realm = Realm.getDefaultInstance();
                    try {
                        realm.beginTransaction();
                        realm.copyToRealmOrUpdate(response);
                        realm.commitTransaction();
                    } finally {
                        realm.close();
                    }
                })
                .map(response -> {
                    if (controllers.containsKey(response.getId())) {
                        return controllers.get(response.getId());
                    }
                    UiChapter chapter = new UiChapter(response);
                    Book book = new Book(chapter, this, this, false, null);
                    controllers.put(response.getId(), book);
                    book.startDownload();
                    publisher.onNext(chapter);
                    return book;
                })
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void pageDownloaded(int id, boolean bookDownloaded, int pageId, boolean onlyProgress) {

    }

    @Override
    public void completelyDownloaded(int id, boolean success) {
        if(success) {
            Realm realm = Realm.getDefaultInstance();
            try {
                Chapter chapter = realm.where(Chapter.class).equalTo("id", id).findFirst();
                if (chapter != null) {
                    realm.beginTransaction();
                    chapter.setCompletelyDownloaded(true);
                    realm.copyToRealmOrUpdate(chapter);
                    realm.commitTransaction();
                }
            } finally {
                realm.close();
            }
        }
    }

    @Override
    public void onCacheUpdated() {

    }

    @Override
    public void onPageDimensionsChanged(int pageId) {

    }

    public interface DeletionListener {
        void onDeletionFinished();
    }

    public void deleteChapter(int id) {
        deleteChapter(id, null);
    }

    public void deleteChapter(int id, DeletionListener listener) {
        if(controllers.containsKey(id)) {
            Book book = controllers.get(id);
            book.cancelDownload();
            controllers.remove(id);
        }
        
        // Run on a background thread to avoid UI block and Realm "writes on UI thread" exception
        new Thread(() -> {
            Realm realm = Realm.getDefaultInstance();
            try {
                realm.executeTransaction(r -> {
                    Chapter chapter = r.where(Chapter.class).equalTo("id", id).findFirst();
                    if (chapter != null) {
                        List<RealmObject> toRemove = new ArrayList<>();

                        // Collect orphaned pages
                        toRemove.addAll(chapter.getPages());

                        List<catgirl.oneesama.data.model.chapter.serializable.Tag> tags = new ArrayList<>(chapter.getTags());

                        chapter.deleteFromRealm();

                        // Collect orphaned tags
                        for (catgirl.oneesama.data.model.chapter.serializable.Tag tag : tags) {
                            if (r.where(Chapter.class).equalTo("tags.id", tag.getId()).count() == 0)
                                toRemove.add(tag);
                        }

                        // Clean orphaned tags and pages
                        for (RealmObject object : toRemove)
                            object.deleteFromRealm();
                    }
                });
            } finally {
                realm.close();
            }

            FileManager.deleteFolder(id);

            if (listener != null) {
                listener.onDeletionFinished();
            }
        }).start();
    }

    public void checkTagIdsAgainstLocalDatabase(List<Tag> tags) {
        // Dynasty API does not have IDs anymore
        // It makes sense, but now we have to assign IDs manually
        String name;
        String type;
        String permalink;

        Realm realm = Realm.getDefaultInstance();
        try {
            int maxTagId = 1;

            if (realm.where(Tag.class).findAll().size() > 0)
                maxTagId = realm.where(Tag.class).max("id").intValue() + 1;

            for (Tag tag : tags) {
                name = tag.getName();
                type = tag.getType();
                permalink = tag.getPermalink();

                Tag existing = realm.where(Tag.class)
                        .equalTo("name", name)
                        .equalTo("type", type)
                        .equalTo("permalink", permalink)
                        .findFirst();

                if (existing == null) {
                    tag.setId(maxTagId);
                    maxTagId++;
                } else {
                    tag.setId(existing.getId());
                }
            }
        } finally {
            realm.close();
        }
    }

    public synchronized void checkChapterIdAgainstLocalDatabase(Chapter chapter) {
        // Dynasty API does not have IDs anymore
        // It makes sense, but now we have to assign IDs manually

        Realm realm = Realm.getDefaultInstance();
        try {
            Chapter existing = realm.where(Chapter.class)
                    .equalTo("permalink", chapter.getPermalink())
                    .findFirst();

            if (existing == null) {
                int maxChapterId = 1;
                Number max = realm.where(Chapter.class).max("id");
                if (max != null) {
                    maxChapterId = max.intValue() + 1;
                }
                
                // Extra safety: check if the directory exists on disk even if not in DB
                while (FileManager.chapterFolderExists(maxChapterId)) {
                    maxChapterId++;
                }

                chapter.setId(maxChapterId);
            } else {
                chapter.setId(existing.getId());
            }
        } finally {
            realm.close();
        }
    }

    public void findRealChapterName(Chapter chapter) {
        // Find out the real, full chapter name and the corresponding volume
        // This is only available on the series page, does not apply if not part of a series
        String series = null;
        for(Tag tag : chapter.getTags()) {
            if(tag.getType().equals("Series")) {
                series = tag.getPermalink();
                break;
            }
        }

        if(series != null) {
            try {
                DynastySeriesPage.Chapter c = DynastySeriesPageProvider.provideChapterInfo(series, chapter.getPermalink());
                chapter.setTitle(c.chapterName);
                chapter.setVolumeName(c.volumeName);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    public Observable<UiChapter> subscribeForChapterControllerActivation() {
        return publisher;
    }
}
