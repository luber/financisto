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
    // "/api/Currencies?lastSync=23123"
    @GET("/api/Currencies")
    List<Currency> listCurrencies(@Query("lastSync") long lastSync);

    @GET("/api/Currencies/{id}")
    Currency getCurrency(@Path("id") long id);

    @POST("/api/Currencies")
    Currency createCurrency(@Body Currency currency);

    @PUT("/api/Currencies/{id}")
    Currency updateCurrency(@Path("id") long id, @Body Currency currency);

    @DELETE("/api/Currencies/{id}")
    void deleteCurrency(@Path("id") long id, Callback<Response> callback);
}