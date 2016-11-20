/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.http;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 2/17/13
 * Time: 1:55 AM
 */
public class HttpClientWrapper {

    public HttpClientWrapper() {
    }

    public JSONObject getAsJson(String url) throws Exception {
        String s = getAsString(url);
        return new JSONObject(s);
    }

    public String getAsString(String urlString) throws Exception {
        URL url = new URL(urlString);

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setInstanceFollowRedirects(false);

        try {
            InputStream in = urlConnection.getInputStream();
            String theString = getStringFromInputStream(in);

            return theString;
        } finally {
            urlConnection.disconnect();
        }
    }

    public String getAsStringIfOk(String urlString) throws Exception {
        URL url = new URL(urlString);

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setInstanceFollowRedirects(false);

        try {
            if (urlConnection.getResponseCode() == 200) {
                InputStream in = urlConnection.getInputStream();
                String theString = getStringFromInputStream(in);

                return theString;
            } else {
                throw new RuntimeException(urlConnection.getResponseMessage());
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }
}
