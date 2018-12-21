package com.app.beb.bebapp.rss;

import android.content.Context;
import android.os.AsyncTask;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class RssParser extends AsyncTask<Void, Void, Void>  {

    protected String address;
    protected ArrayList<DashboardItem> dashboardItems;
    protected URL url;

    public RssParser(Context context, String address) {
        this.address = address;
        dashboardItems = new ArrayList<DashboardItem>();
        onItemsLoadedListeners = new ArrayList<>();
        rssReaderFeedItemLoadedListeners = new ArrayList<>();
        rssParserProgressListeners = new ArrayList<>();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        notifyOnProgressStarted();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        notifyOnProgressEnded();
    }



    public interface OnItemsLoadedListener {
        void onItemsLoaded();
        void onItemsLoadFailed(Exception e);
    }
    private ArrayList<RssParserProgressListener> rssParserProgressListeners;
    public void addOnProgressListener(RssParserProgressListener listener) {
        if (!rssParserProgressListeners.contains(listener)) {
            rssParserProgressListeners.add(listener);
        }
    }

    public void removeOnProgressListener(RssParserProgressListener listener) {
        rssParserProgressListeners.remove(listener);
    }

    public void notifyOnProgressStarted() {
        for (RssParserProgressListener listener: rssParserProgressListeners) {
            listener.onProgressStarted();
        }
    }

    public void notifyOnProgressEnded() {
        for (RssParserProgressListener listener: rssParserProgressListeners) {
            listener.onProgressEnded();
        }
    }


    private ArrayList<OnItemsLoadedListener> onItemsLoadedListeners;

    public void addOnExecutedListener(OnItemsLoadedListener listener) {
        if (!onItemsLoadedListeners.contains(listener)) {
            onItemsLoadedListeners.add(listener);
        }
    }

    public void removeOnExecutedListener(OnItemsLoadedListener listener) {
        onItemsLoadedListeners.remove(listener);
    }

    private void notifyOnItemsLoaded() {
        for (OnItemsLoadedListener listener: onItemsLoadedListeners) {
            listener.onItemsLoaded();
        }
    }

    private void notifyOnItemsLoadFailed(Exception e) {

        for (OnItemsLoadedListener listener: onItemsLoadedListeners) {
            listener.onItemsLoadFailed(e);
        }
    }

    public interface RssReaderFeedItemLoadedListener {
        void onFeedItemLoaded(DashboardItem item);
        void onFeedItemLoadFailed(Exception e);
    }

    private ArrayList<RssReaderFeedItemLoadedListener> rssReaderFeedItemLoadedListeners;
    public void addOnFeedItemLoadedListener(RssReaderFeedItemLoadedListener listener) {
        if (!rssReaderFeedItemLoadedListeners.contains(listener)) {
            rssReaderFeedItemLoadedListeners.add(listener);
        }
    }

    public void removeOnFeedItemLoadedListener(RssReaderFeedItemLoadedListener listener) {
        rssReaderFeedItemLoadedListeners.remove(listener);
    }

    private void notifyOnFeedItemLoaded(DashboardItem item) {

        for (RssReaderFeedItemLoadedListener listener: rssReaderFeedItemLoadedListeners) {
            listener.onFeedItemLoaded(item);
        }
    }

    private void notifyOnFeedItemLoadFailed(Exception e) {
        for (RssReaderFeedItemLoadedListener listener: rssReaderFeedItemLoadedListeners) {
            listener.onFeedItemLoadFailed(e);
        }
    }

    @Override
    protected Void doInBackground(Void... voids) {
        processXml(getData());
        return null;
    }

    private void processXml(Document data) {
        if (data == null){
            return;
        }
        Element root = data.getDocumentElement();
        Node channel = root.getChildNodes().item(1);
        NodeList items = channel.getChildNodes();
        for (int i = 0; i < items.getLength(); i++) {
            Node item = items.item(i);
            if (item.getNodeName().equalsIgnoreCase("item")) {
                DashboardItem dashboardItem = new DashboardItem();
                NodeList itemChildNodes = item.getChildNodes();
                for (int j = 0; j < itemChildNodes.getLength(); j++) {
                    Node node = itemChildNodes.item(j);
                    if (node.getNodeName().equalsIgnoreCase("title")) {
                        dashboardItem.setTitle(node.getTextContent());
                    } else if (node.getNodeName().equalsIgnoreCase("pubDate")) {
                        dashboardItem.setPubDate(node.getTextContent());
                    } else if (node.getNodeName().equalsIgnoreCase("link")) {
                        dashboardItem.setLink(node.getTextContent());
                    } else if (node.getNodeName().equalsIgnoreCase("media:thumbnail")
                            || node.getNodeName().equalsIgnoreCase("media:content")) {
                        String url = node.getAttributes().item(0).getTextContent();
                        dashboardItem.setThumbnailUrl(url);
                    }else if (node.getNodeName().equalsIgnoreCase("description")) {
                        String content = node.getTextContent();
                        String regexp = "<[^>]*>";
                        content = content.replaceAll(regexp, "");
                        dashboardItem.setDescription(content);
                    }
                }
                dashboardItems.add(dashboardItem);
                notifyOnFeedItemLoaded(dashboardItem);
            }
        }
        notifyOnItemsLoaded();
    }

    public Document getData() {
        try {
            url = new URL(address);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(1000);
            InputStream inputStream = connection.getInputStream();
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            return builder.parse(inputStream);

        } catch (MalformedURLException e) {
            e.printStackTrace();
            notifyOnItemsLoadFailed(e);
            return null;

        } catch (SocketTimeoutException e) {
            e.printStackTrace();
            notifyOnItemsLoadFailed(e);
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            notifyOnItemsLoadFailed(e);
            return null;
        }
    }
}
