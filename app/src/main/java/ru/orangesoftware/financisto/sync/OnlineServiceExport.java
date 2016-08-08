package ru.orangesoftware.financisto.sync;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.utils.Utils;

import static ru.orangesoftware.financisto.backup.Backup.BACKUP_TABLES;
import static ru.orangesoftware.financisto.backup.Backup.tableHasSystemIds;

/**
 * Created with IntelliJ IDEA.
 * User: Lyubomyr Vyhovskyy
 * Date: 03.01.13
 * Time: 9:59
 */
public class OnlineServiceExport extends Export {

    private String syncApiURL = "http://banan/Financisto/api/values";

    private final Context context;
    private final SQLiteDatabase db;

    public OnlineServiceExport(Context context, SQLiteDatabase db, boolean useGZip) {
        super(context, useGZip);
        this.context = context;
        this.db = db;
    }

    public String UploadData() throws Exception {
        JsonObject uploadData = new JsonObject();

        for (String tableName : BACKUP_TABLES) {
            JsonArray tableData = new JsonArray();
            String sql = "select * from " + tableName + (tableHasSystemIds(tableName) ? " WHERE _id >= 0" : "");
            Cursor tableCursor = db.rawQuery(sql, null);
            try {
                String[] columnNames = tableCursor.getColumnNames();
                while (tableCursor.moveToNext()) {
                    JsonObject rowData = new JsonObject();
                    for (int i=0; i < columnNames.length; i++) {
                        String columnName = columnNames[i];
                        String value = tableCursor.getString(i);
                        rowData.addProperty(columnName, value);
                    }
                    tableData.add(rowData);
                }
            } finally {
                tableCursor.close();
            }

            String pluralizedTableName = getPluralizedTableName(tableName);
            uploadData.add(pluralizedTableName, tableData);
        }

        return makeRequest(syncApiURL, uploadData);

//        HttpClient httpClient = new DefaultHttpClient();
//        HttpGet httpGet = new HttpGet(syncApiURL);
//        HttpPost httpRequest = new HttpPost(syncApiURL);
//
//        ResponseHandler<String> handler = new BasicResponseHandler();
//
//        httpRequest.setHeader("User-Agent", "Android");
//        httpRequest.setHeader("Accept", "application/json");
//        httpRequest.setHeader("Content-Encoding", "UTF-8");
//        httpRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");
//
//        List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
//        postParameters.add(new BasicNameValuePair("", "Hello from Android"));
//
//        String result = "";
//
//        try {
//            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postParameters);
//            httpRequest.setEntity(entity);
//
//            result = httpClient.execute(httpRequest, handler);
//        } catch (ClientProtocolException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        httpClient.getConnectionManager().shutdown();
//        //Log.i(TAG, result);
//
//        return result;
    }

    private String getPluralizedTableName(String tableName) {
        if (tableName.endsWith("y"))
            return tableName.substring(0, tableName.lastIndexOf("y")) + "ies";

        if (tableName.endsWith("s")) //already pluralized
            return tableName;

        return tableName + "s";
    }

    public static String makeRequest(String path, JsonElement jsonData) throws Exception
    {
        //instantiates http client to make request
 /*       DefaultHttpClient httpClient = new DefaultHttpClient();

        //url with the post data
        HttpPost httpRequest = new HttpPost(path);

        //passes the results to a string builder/entity
        StringEntity se = new StringEntity(jsonData.toString(), "UTF-8");

        //sets the post request as the resulting string
        httpRequest.setEntity(se);
        //sets a request header so the page receiving the request
        //will know what to do with it
        httpRequest.setHeader("User-Agent", "Android");
        httpRequest.setHeader("Content-Encoding", "UTF-8");
        httpRequest.setHeader("Accept", "application/json");
        httpRequest.setHeader("Content-type", "application/json");

        //Handles what is returned from the page
        ResponseHandler<String> responseHandler = new BasicResponseHandler();

        String result = "";

        try {
          result = httpClient.execute(httpRequest, responseHandler);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        httpClient.getConnectionManager().shutdown();
        //Log.i(TAG, result);

        return result;*/
        return "";
    }

//    private static JSONObject getJsonObjectFromMap(Map params) throws JSONException {
//
//        //all the passed parameters from the post request
//        //iterator used to loop through all the parameters
//        //passed in the post request
//        Iterator iter = params.entrySet().iterator();
//
//        //Stores JSON
//        JSONObject holder = new JSONObject();
//
//        //using the earlier example your first entry would get email
//        //and the inner while would get the value which would be 'foo@bar.com'
//        //{ fan: { email : 'foo@bar.com' } }
//
//        //While there is another entry
//        while (iter.hasNext())
//        {
//            //gets an entry in the params
//            Map.Entry pairs = (Map.Entry)iter.next();
//
//            //creates a key for Map
//            String key = (String)pairs.getKey();
//
//            //Create a new map
//            Map m = (Map)pairs.getValue();
//
//            //object for storing Json
//            JSONObject data = new JSONObject();
//
//            //gets the value
//            Iterator iter2 = m.entrySet().iterator();
//            while (iter2.hasNext())
//            {
//                Map.Entry pairs2 = (Map.Entry)iter2.next();
//                data.put((String)pairs2.getKey(), pairs2.getValue());
//            }
//
//            //puts email and 'foo@bar.com'  together in map
//            holder.put(key, data);
//        }
//        return holder;
//    }

    @Override
    protected String getExtension() {
        return ".backup";
    }

    @Override
    protected void writeHeader(BufferedWriter bw) throws IOException, PackageManager.NameNotFoundException {
        PackageInfo pi = Utils.getPackageInfo(context);
        bw.write("PACKAGE:");bw.write(pi.packageName);bw.write("\n");
        bw.write("VERSION_CODE:");bw.write(String.valueOf(pi.versionCode));bw.write("\n");
        bw.write("VERSION_NAME:");bw.write(pi.versionName);bw.write("\n");
        bw.write("DATABASE_VERSION:");bw.write(String.valueOf(db.getVersion()));bw.write("\n");
        bw.write("#START\n");
    }

    public static void copy(File source, File dest) throws IOException {
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();

            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);

            out.write(buf);

        } finally {
            if (in != null)          in.close();
            if (out != null)     out.close();
        }
    }

    @Override
    protected void writeBody(BufferedWriter bw) throws IOException  {
        for (String tableName : BACKUP_TABLES) {
            exportTable(bw, tableName);
        }
    }

    @Override
    protected void writeFooter(BufferedWriter bw) throws IOException {
        bw.write("#END");
    }

    private void exportTable(BufferedWriter bw, String tableName) throws IOException {
        String sql = "select * from " + tableName + (tableHasSystemIds(tableName) ? " WHERE _id>=0" : "");
        Cursor c = db.rawQuery(sql, null);
        try {
            String[] columnNames = c.getColumnNames();
            int cols = columnNames.length;
            while (c.moveToNext()) {
                bw.write("$ENTITY:");bw.write(tableName);bw.write("\n");
                for (int i=0; i<cols; i++) {
                    String value = c.getString(i);
                    if (value != null) {
                        bw.write(columnNames[i]);bw.write(":");
                        bw.write(value);
                        bw.write("\n");
                    }
                }
                bw.write("$$\n");
            }
        } finally {
            c.close();
        }
    }

}
