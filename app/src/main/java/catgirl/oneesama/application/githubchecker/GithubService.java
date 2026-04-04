package catgirl.oneesama.application.githubchecker;

import com.google.gson.JsonObject;

import retrofit2.http.GET;
import rx.Observable;

public interface GithubService {
    @GET("repos/DefiantCatgirl/Oneesama/releases/latest")
    Observable<JsonObject> getLatestGithubRelease();
}
