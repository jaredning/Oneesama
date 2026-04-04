package catgirl.oneesama.application.githubchecker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.app.NotificationCompat;

import catgirl.oneesama.application.Application;
import catgirl.oneesama.BuildConfig;
import catgirl.oneesama.R;
import catgirl.oneesama.application.Config;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class GithubReleaseChecker {
    public static void checkForNewRelease() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Config.githubEndpoint)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();

        GithubService service = retrofit.create(GithubService.class);

        service.getLatestGithubRelease().subscribe(object -> {
            String tag = object.getAsJsonPrimitive("tag_name").getAsString();
            String url = object.getAsJsonArray("assets").get(0).getAsJsonObject().getAsJsonPrimitive("browser_download_url").getAsString();
            String body = object.getAsJsonPrimitive("body").getAsString();
            String name =  object.getAsJsonPrimitive("name").getAsString();
            boolean equalsCurrent = ("v" + BuildConfig.VERSION_NAME).equals(tag);

            String lastNotifiedVersion = Application.getContextOfApplication().getSharedPreferences("githubReleaseChecker", Context.MODE_PRIVATE).getString("lastNotifiedVersion", "");

            // Only show notification once per version, if not current version
            if(!equalsCurrent && !lastNotifiedVersion.equals(tag)) {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(Application.getContextOfApplication());

                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                PendingIntent p = PendingIntent.getActivity(Application.getContextOfApplication(), 0, i, PendingIntent.FLAG_IMMUTABLE);

                Notification notification = new NotificationCompat.Builder(Application.getContextOfApplication(), "updates")
                        .setContentIntent(p)
                        .setSmallIcon(R.mipmap.ic_launcher).setTicker("Oneesama update available").setWhen(0)
                        .setAutoCancel(true).setContentTitle("Oneesama " + name + " is available")
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setContentText(body).build();

                ((NotificationManager) Application.getContextOfApplication().getSystemService(Context.NOTIFICATION_SERVICE)).notify(0, notification);

                Application.getContextOfApplication().getSharedPreferences("githubReleaseChecker", Context.MODE_PRIVATE).edit().putString("lastNotifiedVersion", tag).commit();
            }
        }, Throwable::printStackTrace);
    }
}
