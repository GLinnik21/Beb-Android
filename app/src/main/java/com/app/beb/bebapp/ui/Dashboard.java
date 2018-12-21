package com.app.beb.bebapp.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.app.beb.bebapp.R;
import com.app.beb.bebapp.cache.CacheManager;
import com.app.beb.bebapp.rss.DashboardItem;
import com.app.beb.bebapp.rss.DashboardAdapter;
import com.app.beb.bebapp.rss.RssParser;
import com.app.beb.bebapp.rss.RssParserProgressListener;
import com.app.beb.bebapp.user_manager.UserManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class Dashboard extends Fragment implements RssParser.RssReaderFeedItemLoadedListener,
        RssParser.OnItemsLoadedListener, RssParserProgressListener {


    private RecyclerView recyclerView;
    private DashboardAdapter dashboardAdapter;
    private RssParser rssParser;
    private ProgressDialog progressDialog;
    private boolean loadedFromCache = false;
    private DashboardAdapter.OnItemClickListener onItemClickListener;

    private SharedPreferences sharedPref;

    public Dashboard() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    public boolean isOnline() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int     exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        }
        catch (IOException e)          { e.printStackTrace(); }
        catch (InterruptedException e) { e.printStackTrace(); }

        return false;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.dashboard_recyclerView);
        int orientation = getContext().getResources().getConfiguration().orientation;

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), orientation));

        onItemClickListener = new DashboardAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(DashboardItem item) {
                if (isOnline()) {
                    Intent intent = new Intent(getContext(), RssWebViewActivity.class);
                    intent.putExtra("URL", item.getLink());
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), R.string.no_internet, Toast.LENGTH_LONG).show();
                }
            }
        };
        sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        String lastUserId = sharedPref.getString(getString(R.string.preference_last_userId), null);
        String rssUrl = sharedPref.getString(getString(R.string.preference_rssUrl), null);
        if (lastUserId == null) {
            askToInputNewUrl(getString(R.string.new_to_rss));
        } else if (!lastUserId.equals(UserManager.getInstance().getCurrentUser().getId())) {
            askToInputNewUrl(getString(R.string.new_to_rss));
        } else {
            doRss(rssUrl);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.dashboard_menu:
                showRssSourceInputDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void doRss(String address) {
        dashboardAdapter = new DashboardAdapter(getContext(), new ArrayList<DashboardItem>(), onItemClickListener);
        recyclerView.setAdapter(dashboardAdapter);
        ConnectivityManager cm =
                (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();
        if (isConnected) {
            loadRssFromTheInternet(address);
        } else {
            loadRssFromCache();
        }
    }

    private void loadRssFromTheInternet(String address) {
        rssParser = new RssParser(getContext(), address);
        rssParser.addOnFeedItemLoadedListener(this);
        rssParser.addOnExecutedListener(this);
        rssParser.addOnProgressListener(this);
        rssParser.execute();
    }

    private void loadRssFromCache() {
        loadedFromCache = true;
        ArrayList<DashboardItem> items = CacheManager.getInstance().readRssCache(getContext(),
                UserManager.getInstance().getCurrentUser().getId());
        dashboardAdapter.setDashboardItems(items);
        Toast.makeText(getContext(), R.string.feed_loaded_from_cache, Toast.LENGTH_SHORT).show();
    }

    private void askToInputNewUrl(String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setCancelable(false)
                .setMessage(R.string.rss_correct_url_request)
                .setTitle(title)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                showRssSourceInputDialog();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
        builder.create().show();

    }

    private void showRssSourceInputDialog() {
        LayoutInflater li = LayoutInflater.from(getContext());
        View dialogView = li.inflate(R.layout.rss_source_input_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(
                getContext());

        builder.setView(dialogView);

        final EditText sourceInput = dialogView.findViewById(R.id.rssSourseInputDialog__editText);
        sourceInput.setText(sharedPref.getString(getString(R.string.preference_rssUrl), null));

        builder.setCancelable(false)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String url = sourceInput.getText().toString();
                                setRssUrlPreference(url);
                                doRss(url);
                            }
                        })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onFeedItemLoaded(final DashboardItem item) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dashboardAdapter.addItem(item);
            }
        });

    }

    @Override
    public void onFeedItemLoadFailed(Exception e) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), R.string.feed_item_loading_failed, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void setRssUrlPreference(String url) {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.preference_rssUrl), url);
        editor.commit();
    }

    @Override
    public void onItemsLoaded() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), R.string.feed_loaded, Toast.LENGTH_LONG).show();
                String uid = UserManager.getInstance().getCurrentUser().getId();
                CacheManager.getInstance().writeRssToCache(getContext(), dashboardAdapter.getDashboardItems(), uid);
            }
        });

    }

    @Override
    public void onItemsLoadFailed(Exception e) {
        if (e instanceof MalformedURLException) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    askToInputNewUrl(getString(R.string.incorrect_rss_url));
                }
            });
        } else {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), getString(R.string.loading_failed), Toast.LENGTH_LONG).show();
                    loadRssFromCache();
                }
            });
        }
    }

    @Override
    public void onProgressStarted() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
        }
        progressDialog.show();
        progressDialog.setMessage(getString(R.string.loading));
        progressDialog.setCancelable(false);
    }

    @Override
    public void onProgressEnded() {
        if (progressDialog != null) {
            progressDialog.hide();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.dashboard_menu, menu);
    }

}
