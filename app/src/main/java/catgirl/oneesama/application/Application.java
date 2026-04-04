package catgirl.oneesama.application;

import android.content.Context;
import android.util.Log;

import io.appmetrica.analytics.AppMetrica;
import io.appmetrica.analytics.AppMetricaConfig;

import catgirl.oneesama.R;
import catgirl.oneesama.application.githubchecker.GithubReleaseChecker;
import catgirl.oneesama.application.migrations.MigrateChapterNames;
import catgirl.oneesama.application.migrations.MigrationManager;
import catgirl.oneesama.application.migrations.RemoveBrokenIdChaptersAndTags;
import io.realm.DynamicRealm;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;

public class Application extends android.app.Application {

    private static Application appContext;

    private ApplicationComponent applicationComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        appContext = this;

        // Database configuration
        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .schemaVersion(1)
                .migration(new RealmMigration() {
                    @Override
                    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
                        Log.v("Realm", "Migrating: " + oldVersion + " -> " + newVersion);

                        if (oldVersion == 0) {
                            realm.getSchema().get("Chapter").addField("volumeName", String.class);
                        }
                    }
                })
                .build();

        Realm.setDefaultConfiguration(config);

        // Migrations
        new MigrationManager().applyMigrations(this.getApplicationContext());

        // Check for updates
//        GithubReleaseChecker.checkForNewRelease();

        // Dagger component initialization
        applicationComponent = DaggerApplicationComponent.builder().build();

        // Analytics
//        AppMetricaConfig appMetricaConfig = AppMetricaConfig.newConfigBuilder(getString(R.string.metrics_token)).build();
//        AppMetrica.activate(getApplicationContext(), appMetricaConfig);
    }

    public static Context getContextOfApplication() {
        return appContext.getApplicationContext();
    }

    public static ApplicationComponent getApplicationComponent() {
        return appContext.applicationComponent;
    }
}
