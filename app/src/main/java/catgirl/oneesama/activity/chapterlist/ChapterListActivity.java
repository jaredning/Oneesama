package catgirl.oneesama.activity.chapterlist;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import io.appmetrica.analytics.AppMetrica;

import butterknife.BindView;
import butterknife.ButterKnife;
import catgirl.mvp.implementations.BaseCacheActivity;
import catgirl.oneesama.R;
import catgirl.oneesama.activity.chapterlist.fragments.chapterlist.view.ChapterListFragment;

public class ChapterListActivity extends BaseCacheActivity {

    public static final String TAG_ID = "tag_id";

    @BindView(R.id.toolbar_layout)
    Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapters);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());

        if(getIntent().getExtras() == null || !getIntent().getExtras().containsKey(TAG_ID)) {
            finish();
            return;
        }

        if(savedInstanceState == null) {
            Bundle bundle = new Bundle(getIntent().getExtras());
            ChapterListFragment fragment = new ChapterListFragment();
            fragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        AppMetrica.resumeSession(this);
    }

    @Override
    protected void onPause() {
//        AppMetrica.pauseSession(this);
        super.onPause();
    }
}
