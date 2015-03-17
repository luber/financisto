package ru.orangesoftware.financisto2.export.sync;

import retrofit.RetrofitError;

/**
 * Created by Lyubomyr on 17.03.2015.
 */
public class ApiUnauthorizedException extends Exception {
    private RetrofitError retrofitError;

    public ApiUnauthorizedException(RetrofitError retrofitError) {

        this.retrofitError = retrofitError;
    }


}
