package catgirl.oneesama.data.network.scraper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DynastyPage {
    public static String getBody(String url) throws Exception {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();

        return response.body().string();
    }
}
