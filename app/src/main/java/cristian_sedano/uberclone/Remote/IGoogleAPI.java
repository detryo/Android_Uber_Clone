package cristian_sedano.uberclone.Remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * Created by Christian on 06/02/2018.
 */

public interface IGoogleAPI {

    @GET
    Call<String> getPath(@Url String uri);
}
