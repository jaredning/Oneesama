package catgirl.oneesama.activity.browseseriespage.fragment.view;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import catgirl.oneesama.R;
import catgirl.oneesama.activity.browseseriespage.fragment.data.model.BrowseSeriesPageVolume;

public class BrowseSeriesPageVolumeViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.VolumeHeader) TextView volumeHeader;

    public BrowseSeriesPageVolumeViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void bind(BrowseSeriesPageVolume volume) {
        volumeHeader.setText(volume.header);
    }
}
