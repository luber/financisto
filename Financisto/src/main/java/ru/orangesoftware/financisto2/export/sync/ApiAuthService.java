package ru.orangesoftware.financisto2.export.sync;

import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by Lyubomyr on 17.03.2015.
 */
public interface ApiAuthService {
    public static final String API_CLIENT_ID = "financistoAndroid";
    public static final String API_CLIENT_SECRET = "androApp2o15Secr@";

    @POST("/token")
    @FormUrlEncoded
    AccessToken getAccessToken(@Field("client_id") String clientId,
                               @Field("client_secret") String clientSecret,
                               @Field("grant_type") String grantType,
                               @Field("username") String username,
                               @Field("password") String password);

    @POST("/token")
    RefreshToken getRefreshToken();
}
