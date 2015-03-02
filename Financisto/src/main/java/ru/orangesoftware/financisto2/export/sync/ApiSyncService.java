package ru.orangesoftware.financisto2.export.sync;

import java.util.List;

import retrofit.http.*;
import ru.orangesoftware.financisto2.model.Currency;

/**
 * Created by Lyubomyr on 02.03.2015.
 */
public interface ApiSyncService {
    @GET("/api/Currencies")
    List<Currency> listCurrencies();

    @POST("/api/Currencies")
    Currency createCurrency(@Body Currency currency);

}