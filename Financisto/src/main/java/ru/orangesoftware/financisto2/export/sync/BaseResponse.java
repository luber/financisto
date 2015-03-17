package ru.orangesoftware.financisto2.export.sync;

/**
 * Created by Lyubomyr on 17.03.2015.
 */
public abstract class BaseResponse {

    private String error = null;

    public String getError() {
        return error;
    }
}