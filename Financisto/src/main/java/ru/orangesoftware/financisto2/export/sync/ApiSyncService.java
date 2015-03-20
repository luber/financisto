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
    List<Currency> getChangedOrAddedCurrencies(@Query("lastSync") long lastSync);

    @GET("/api/sync/currencies/removed")
    List<Currency> getRemovedCurrencies(@Query("lastSync") long lastSync);

    @GET("/api/sync/currencies/{id}")
    Currency getCurrency(@Path("id") String id);

    @POST("/api/sync/currencies")
    Currency createCurrency(@Body Currency currency);

    @PUT("/api/sync/currencies/{id}")
    Response updateCurrency(@Path("id") String id, @Body Currency currency);

    @DELETE("/api/sync/currencies/{id}")
    Response deleteCurrency(@Path("id") String id);

    @DELETE("/api/sync/currencies")
    Response deleteCurrencies(@Body List<String> ids);

}