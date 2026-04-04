package catgirl.oneesama.activity.common.view;

import androidx.annotation.NonNull;

import java.util.List;

public interface SimpleRecyclerView<T> {
    void showContents(@NonNull List<T> contents);
}
