package ru.orangesoftware.financisto2.export.sync;

import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Lyubomyr on 03.03.2015.
 */
public class MyErrorHandler implements ErrorHandler {
    @Override public Throwable handleError(RetrofitError cause) {
        Response r = cause.getResponse();
        if (r != null && r.getStatus() == 401) {
//            return new UnauthorizedException(cause);
            //TODO
        }
        return cause;
    }
}
