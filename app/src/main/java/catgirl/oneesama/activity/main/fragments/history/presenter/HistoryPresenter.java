package catgirl.oneesama.activity.main.fragments.history.presenter;

import catgirl.oneesama.activity.common.presenter.AutoRefreshableRecyclerPresenter;
import catgirl.oneesama.activity.main.fragments.history.data.HistoryProvider;
import catgirl.oneesama.activity.main.fragments.history.view.HistoryView;
import catgirl.oneesama.activity.main.fragments.ondevice.fragments.misc.view.MiscChaptersView;
import catgirl.oneesama.data.controller.ChaptersController;
import catgirl.oneesama.data.model.chapter.ui.UiChapter;

public class HistoryPresenter extends AutoRefreshableRecyclerPresenter<UiChapter, HistoryView, HistoryProvider> {
    private HistoryProvider historyProvider;
    ChaptersController chaptersController;

    boolean deleteConfirmationShown = false;
    int deleteConfirmationPosition;

    public HistoryPresenter(HistoryProvider historyProvider, ChaptersController chaptersController) {
        this.historyProvider = historyProvider;
        this.chaptersController = chaptersController;
    }

    @Override
    public void bindView(HistoryView view) {
        super.bindView(view);

        if (deleteConfirmationShown) {
            view.showDeleteConfirmation(deleteConfirmationPosition);
        }
    }

    public void onItemClicked(int position) {
        if (getView() != null)
            getView().switchToReader(items.get(position).getId());
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

        final int chapterId = items.get(position).getId();

        chaptersController.deleteChapter(chapterId, () -> {
            if (getView() != null) {
                getView().post(() -> {
                    if (items == null) return;
                    int currentPosition = -1;
                    for (int i = 0; i < items.size(); i++) {
                        if (items.get(i).getId() == chapterId) {
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

    @Override
    public HistoryProvider getProvider() {
        return historyProvider;
    }

    public void onResume() {
        historyProvider.onDestroy();
        historyProvider.subscribeForItems();
    }
}
