package ru.orangesoftware.financisto2.export.sync;

import java.util.List;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.*;
import ru.orangesoftware.financisto2.model.Currency;

/**
 * Created by Lyubomyr on 02.03.2015.
 */
public interface ApiSyncService {
    // "/api/sync/Currencies?lastSync=0"
    @GET("/api/sync/currencies")
    List<Currency> getLastSyncCurrencies(@Query("lastSync") long lastSync);

    @GET("/api/sync/currencies/{id}")
    Currency getCurrency(@Path("id") long id);

    @POST("/api/sync/currencies")
    Currency createCurrency(@Body Currency currency);

    @PUT("/api/sync/currencies/{id}")
    Response updateCurrency(@Path("id") long id, @Body Currency currency);

    @DELETE("/api/sync/currencies/{id}")
    void deleteCurrency(@Path("id") long id, Callback<Response> callback);
}