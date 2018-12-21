package com.app.beb.bebapp.cache;

import android.content.Context;

import com.app.beb.bebapp.rss.DashboardItem;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class CacheManager {

    // Object lifecycle
    public static class CacheManagerHolder {

        static final CacheManager HOLDER_INSTANCE = new CacheManager();
    }
    public static CacheManager getInstance() {
        return CacheManagerHolder.HOLDER_INSTANCE;
    }

    private CacheManager() {

    }

    public void writeRssToCache(Context context, ArrayList<DashboardItem> dashboardItems, String userUid) {
        File cacheFile = getTempFile(context, userUid);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(cacheFile, false);
            JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(fileOutputStream, "UTF-8"));

            jsonWriter.beginArray();
            Gson gson = new Gson();
            for (int i = 0; i < 10; i++) {
                if (i >= dashboardItems.size()) {
                    break;
                }
                gson.toJson(dashboardItems.get(i), DashboardItem.class, jsonWriter);
            }
            jsonWriter.endArray();
            jsonWriter.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<DashboardItem> readRssCache(Context context, String userUid) {
        ArrayList<DashboardItem> dashboardItems = new ArrayList<>();
        File cacheFile = getTempFile(context, userUid);
        try {
            FileInputStream fi = new FileInputStream(cacheFile);
            JsonReader reader = new JsonReader(new InputStreamReader(fi, "UTF-8"));
            reader.beginArray();
            Gson gson = new Gson();
            while (reader.hasNext()) {
                DashboardItem item = gson.fromJson(reader, DashboardItem.class);
                dashboardItems.add(item);
            }
            reader.endArray();

        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            System.out.println("Error initializing stream");
        }
        return dashboardItems;
    }

    private File getTempFile(Context context, String fileName) {
        File cacheFile = new File(context.getCacheDir(), fileName);
        if (cacheFile.exists()) {
            return cacheFile;
        }
        try {
            //TODO: clear cache
            cacheFile.createNewFile();
        } catch (IOException e) {
            // Error while creating file
        }
        return cacheFile;
    }

    private void removeTempFile(Context context, String fileName) {
        File cacheFile = new File(context.getCacheDir(), fileName);
        if (cacheFile.exists()) {
            cacheFile.delete();
        }
    }

    public void removeCacheForUser(Context context, String userUid) {
        removeTempFile(context, userUid);
    }

    class JsonFeedItemsAdapter {
        public List<DashboardItem> getDashboardItems() {
            return dashboardItems;
        }

        public void setDashboardItems(List<DashboardItem> dashboardItems) {
            this.dashboardItems = dashboardItems;
        }

        @SerializedName("dashboardItems")
        @Expose
        private List<DashboardItem> dashboardItems;
    }
}
