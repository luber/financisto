package ru.orangesoftware.financisto2.export.sync;

import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Lyubomyr on 03.03.2015.
 */
public class ApiServiceFactory {
    private ApiServiceFactory(){};

    private static String API_BASE_URL = "http://api.financistoapp.com";

    private static RestAdapter.Builder restAdapterBuilder = new RestAdapter.Builder()
            .setEndpoint(API_BASE_URL)
            .setLogLevel(RestAdapter.LogLevel.FULL)
            .setErrorHandler(new ErrorHandler(){
                @Override
                public Throwable handleError(RetrofitError cause) {
                    Response r = cause.getResponse();
                    if (r != null && r.getStatus() == 401) {
                        return new ApiUnauthorizedException(cause);
                    }
                    return cause;
                }
            });

    public static <T> T createService(Class<T> serviceClass){
        return createService(serviceClass, null);
    }

    public static <T> T createService(Class<T> serviceClass, final AccessToken accessToken){

        if (accessToken != null) {
            restAdapterBuilder.setRequestInterceptor(new RequestInterceptor() {
                @Override
                public void intercept(RequestFacade request) {
                    request.addHeader("Authorization", accessToken.getTokenType() +
                            " " + accessToken.getAccessToken());
                }
            });
        }

        RestAdapter adapter = restAdapterBuilder.build();
        return adapter.create(serviceClass);
    }
}
