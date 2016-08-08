package ru.orangesoftware.financisto.rates;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.orangesoftware.financisto.http.HttpClientWrapper;
import ru.orangesoftware.financisto.model.Currency;

/**
 * Created by luberello on 05.08.16.
 */
public class PrivatbankRateDownloader extends AbstractMultipleRatesDownloader {

    private static final String TAG = PrivatbankRateDownloader.class.getSimpleName();

    private final Pattern pattern = Pattern.compile("<double.*?>(.+?)</double>");
    private final HttpClientWrapper client;
    private final long dateTime;

    public PrivatbankRateDownloader(HttpClientWrapper client, long dateTime) {
        this.client = client;
        this.dateTime = dateTime;
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency) {
        ExchangeRate rate = createRate(fromCurrency, toCurrency);
        try {
            String s = getResponse(fromCurrency, toCurrency);

            boolean isFound = false;

            Gson gson = new GsonBuilder().create();
            PrivatExchangeRate[] pbExchangeRates = gson.fromJson(s, PrivatExchangeRate[].class);
            for (PrivatExchangeRate pbExchangeRate: pbExchangeRates) {
                if (fromCurrency.name.equals(pbExchangeRate.getCcy()) && toCurrency.name.equals(pbExchangeRate.getBaseCcy())){
                    rate.rate = parseRate(pbExchangeRate.getBuy());
                    isFound = true;
                    break;
                }
            }

            if (!isFound) {
                rate.error = parseError(s);
            }
            return rate;
        } catch (Exception e) {
            rate.error = "Unable to get exchange rates: "+e.getMessage();
        }
        return rate;
    }

    private ExchangeRate createRate(Currency fromCurrency, Currency toCurrency) {
        ExchangeRate rate = new ExchangeRate();
        rate.fromCurrencyId = fromCurrency.id;
        rate.toCurrencyId = toCurrency.id;
        rate.date = dateTime;
        return rate;
    }

    private String getResponse(Currency fromCurrency, Currency toCurrency) throws Exception {
        String url = buildUrl(fromCurrency, toCurrency);
        Log.i(TAG, url);
        String responceJson = client.getAsString(url);

        System.out.println(responceJson);

        Log.i(TAG, responceJson);
        return responceJson;
    }

    private double parseRate(String s) {
        return Double.parseDouble(s);
    }

    private String parseError(String s) {
        String[] x = s.split("\r\n");
        String error = "Service is not available, please try again later";
        if (x.length > 0) {
            error = "Something wrong with the exchange rates provider. Response from the service - "+x[0];
        }
        return error;
    }

    private String buildUrl(Currency fromCurrency, Currency toCurrency) {
        return "https://api.privatbank.ua/p24api/pubinfo?exchange&json&coursid=11";
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency, long atTime) {
        throw new UnsupportedOperationException("Not supported by WebserviceX.NET");
    }
}

