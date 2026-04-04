package catgirl.oneesama.activity.legacythumbnails;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import io.appmetrica.analytics.AppMetrica;

import butterknife.BindView;
import butterknife.ButterKnife;
import catgirl.oneesama.R;
import catgirl.oneesama.data.controller.ChaptersController;
import catgirl.oneesama.data.controller.FileManager;
import catgirl.oneesama.data.controller.legacy.Book;
import catgirl.oneesama.data.controller.legacy.BookStateDelegate;
import catgirl.oneesama.activity.legacyreader.activityreader.MiniBitmapCache;
import catgirl.oneesama.activity.legacyreader.activityreader.ReaderActivity;
import catgirl.oneesama.activity.legacyreader.tools.ActivityUtils;
import catgirl.oneesama.activity.legacyreader.widgets.airviewer.AirPage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReaderThumbnailsActivity extends AppCompatActivity implements BookStateDelegate {

    @BindView(R.id.toolbar_layout) Toolbar toolbar;
    @BindView(R.id.PageOverviewGrid) GridView grid;

    Book book;
    LayoutInflater inflater;

    int selectedPage = 0;
    boolean deleteImages = false;

    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_reader_thumbnails);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        int id = getIntent().getExtras().getInt(ReaderActivity.PUBLICATION_ID, 0);
        selectedPage = getIntent().getExtras().getInt(ReaderActivity.CURRENT_PAGE, 0);
        book = ChaptersController.getInstance().getChapterController(id);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.core_reader_thumbnails_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(view -> onBackPressed());

        inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        gridWidth = getResources().getDimensionPixelSize(R.dimen.thumbnails_width);
        gridHeight = calculateLayoutHeight(gridWidth, getResources().getDimensionPixelSize(R.dimen.thumbnail_border));

        grid.setColumnWidth(gridWidth);
        grid.setAdapter(adapter);

        book.addBookStateDelegate(this);

        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int id,
                                    long rowId) {
                selectedPage = id;
                adapter.notifyDataSetChanged();

                Bundle resultBundle = new Bundle();
                resultBundle.putInt(ReaderActivity.CURRENT_PAGE, id);
                Intent intent = new Intent();
                intent.putExtras(resultBundle);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        grid.setSelection(selectedPage);
    }


    public int gridHeight;
    public int gridWidth;

    public int calculateLayoutHeight(int width, int sideMargin)
    {
        int margin = sideMargin * 2;
        float proportions = 4f / 3f;//(float) book.data.dheight / (float) book.data.dwidth;

        int thumbWidth = width - margin;
        int thumbHeight = (int) (thumbWidth * proportions);

        return thumbHeight + margin;
    }


    @Override
    protected void onDestroy() {
        MiniBitmapCache.getInstance().mMemoryCache.evictAll();
        executorService.shutdown();
        super.onDestroy();
    }

    BaseAdapter adapter = new BaseAdapter()
    {
        @Override
        public int getCount() {
            return book.bookPages.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if(position >= book.bookPages.size())
                return null;

            View gridView;

            // inflating grid view item
            if(convertView == null)
                gridView = inflater.inflate(R.layout.item_thumbnail, parent, false);
            else
                gridView = convertView;

            if(deleteImages)
                return gridView;

            gridView.setTag(position);

            ViewGroup.LayoutParams params = gridView.findViewById(R.id.PagePreviewLayout).getLayoutParams();
            params.height = gridHeight;
            params.width = gridWidth;
            gridView.findViewById(R.id.PagePreviewLayout).setLayoutParams(params);
            gridView.findViewById(R.id.ThumbnailPagePreview).bringToFront();
            gridView.findViewById(R.id.ThumbnailFrameBorder).bringToFront();

            gridView.findViewById(R.id.ThumbnailHighlight).clearAnimation();
            if(position == selectedPage)
            {
                gridView.findViewById(R.id.ThumbnailHighlight).setVisibility(View.VISIBLE);
            }
            else
                gridView.findViewById(R.id.ThumbnailHighlight).setVisibility(View.GONE);

            // set value into textview
            TextView textView = (TextView) gridView.findViewById(R.id.ThumbnailPageLabel);

            textView.setText(book.getContentsPageName(position));

            ImageView img = (ImageView) gridView.findViewById(R.id.ThumbnailPagePreview);

            Bitmap b = MiniBitmapCache.getInstance().getBitmapFromMemCache(position);

            if(b == null)
            {
                img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                img.setImageBitmap(null);
                img.setBackgroundColor(Color.TRANSPARENT);
                
                final int pos = position;
                executorService.submit(() -> {
                    Bitmap decoded = ActivityUtils.decodeBitmap(FileManager.getInputStream(book.data.getId(), book.data.getPages().get(pos)), ActivityUtils.dpToPx(120), ActivityUtils.dpToPx(160));
                    if (decoded != null) {
                        MiniBitmapCache.getInstance().addBitmapToMemoryCache(pos, decoded);
                        runOnUiThread(() -> {
                            if (gridView.getTag() != null && (int) gridView.getTag() == pos) {
                                img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                img.setImageBitmap(decoded);
                                img.setBackgroundColor(Color.WHITE);
                            }
                        });
                    }
                });
            }
            else
            {
                img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                img.setImageBitmap(b);
                img.setBackgroundColor(Color.WHITE);
            }

            return gridView;
        }

    };

    @Override
    public void pageDownloaded(int id, boolean bookDownloaded, int pageId, boolean onlyProgress) {
        runOnUiThread(adapter::notifyDataSetChanged);
    }

    @Override
    public void completelyDownloaded(int id, boolean success) {

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
