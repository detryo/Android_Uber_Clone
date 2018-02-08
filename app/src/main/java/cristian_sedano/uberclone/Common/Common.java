package cristian_sedano.uberclone.Common;

import cristian_sedano.uberclone.Remote.IGoogleAPI;
import cristian_sedano.uberclone.Remote.RetrofitClient;

/**
 * Created by Christian on 06/02/2018.
 */

public class Common {

    public static final String baseURL = "https://maps.googleapis.com";
    public static IGoogleAPI getGoogleAPI()
    {
        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);
    }
}
