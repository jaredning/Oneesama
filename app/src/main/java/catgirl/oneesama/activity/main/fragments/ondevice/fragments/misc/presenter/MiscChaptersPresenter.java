package catgirl.oneesama.activity.main.fragments.ondevice.fragments.misc.presenter;

import catgirl.oneesama.data.controller.ChaptersController;
import catgirl.oneesama.activity.chapterlist.fragments.chapterlist.data.model.ChapterAuthor;
import catgirl.oneesama.activity.common.presenter.AutoRefreshableRecyclerPresenter;
import catgirl.oneesama.activity.main.fragments.ondevice.fragments.misc.data.MiscChaptersProvider;
import catgirl.oneesama.activity.main.fragments.ondevice.fragments.misc.view.MiscChaptersView;

public class MiscChaptersPresenter extends AutoRefreshableRecyclerPresenter<ChapterAuthor, MiscChaptersView, MiscChaptersProvider> {

    MiscChaptersProvider listProvider;
    ChaptersController chaptersController;

    boolean deleteConfirmationShown = false;
    int deleteConfirmationPosition;

    public MiscChaptersPresenter(MiscChaptersProvider listProvider, ChaptersController chaptersController) {
        this.chaptersController = chaptersController;
        this.listProvider = listProvider;
    }

    @Override
    public void bindView(MiscChaptersView view) {
        super.bindView(view);

        if (deleteConfirmationShown) {
            view.showDeleteConfirmation(deleteConfirmationPosition);
        }
    }

    @Override
    public MiscChaptersProvider getProvider() {
        return listProvider;
    }

    public void onItemClicked(int position) {
        if (getView() != null)
            getView().switchToReader(items.get(position).chapter.getId());
    }

    public void onItemDeleteClicked(int position) {
        if (getView() != null)
            getView().showDeleteConfirmation(position);

        deleteConfirmationShown = true;
        deleteConfirmationPosition = position;
    }

    public void onItemDeletionConfirmed(int position) {
        if (items == null || position < 0 || position >= items.size()) {
            onItemDeletionDismissed();
            return;
        }

        final int chapterId = items.get(position).chapter.getId();

        chaptersController.deleteChapter(chapterId, () -> {
            if (getView() != null) {
                getView().post(() -> {
                    if (items == null) return;
                    int currentPosition = -1;
                    for (int i = 0; i < items.size(); i++) {
                        if (items.get(i).chapter.getId() == chapterId) {
                            currentPosition = i;
                            break;
                        }
                    }
                    if (currentPosition != -1) {
                        items.remove(currentPosition);
                        if (getView() != null)
                            getView().showItemDeleted(currentPosition);
                    }
                });
            }
        });
        onItemDeletionDismissed();
    }

    public void onItemDeletionDismissed() {
        deleteConfirmationShown = false;
    }
}
