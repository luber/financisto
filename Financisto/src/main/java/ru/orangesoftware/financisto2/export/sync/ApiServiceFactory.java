package ru.orangesoftware.financisto2.export.sync;

import retrofit.RestAdapter;

/**
 * Created by Lyubomyr on 03.03.2015.
 */
public class ApiServiceFactory {
    private ApiServiceFactory(){};

    private static String API_BASE_URL = "http://kampot/Financisto.Api";

    private static RestAdapter restAdapter = new RestAdapter.Builder()
            .setEndpoint(API_BASE_URL)
            .setLogLevel(RestAdapter.LogLevel.FULL)
            .setErrorHandler(new MyErrorHandler())
            .build();

    public static <T> T createService(Class<T> serviceClass){
        return restAdapter.create(serviceClass);
    }
}
