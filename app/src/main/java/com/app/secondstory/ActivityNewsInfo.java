package com.app.secondstory;

import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.snackbar.Snackbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.app.secondstory.adapter.AdapterNewsInfo;
import com.app.secondstory.connection.API;
import com.app.secondstory.connection.RestAdapter;
import com.app.secondstory.connection.callbacks.RespNewsInfo;
import com.app.secondstory.data.Constant;
import com.app.secondstory.model.NewsInfo;
import com.app.secondstory.utils.NetworkCheck;
import com.app.secondstory.utils.Tools;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityNewsInfo extends AppCompatActivity {

    private View parent_view;
    private RecyclerView recyclerView;
    private AdapterNewsInfo mAdapter;
    private SwipeRefreshLayout swipe_refresh;
    private Call<RespNewsInfo> callbackCall = null;

    private int post_total = 0;
    private int failed_page = 0;
    private Snackbar failed_snackbar = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_info);
        parent_view = findViewById(android.R.id.content);
        initToolbar();
        iniComponent();
    }

    private void initToolbar() {
        ActionBar actionBar;
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.title_activity_news_info);
        Tools.systemBarLolipop(this);
    }

    public void iniComponent() {
        swipe_refresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //set data and list adapter
        mAdapter = new AdapterNewsInfo(this, recyclerView, new ArrayList<NewsInfo>());
        recyclerView.setAdapter(mAdapter);

        // on item list clicked
        mAdapter.setOnItemClickListener(new AdapterNewsInfo.OnItemClickListener() {
            @Override
            public void onItemClick(View v, NewsInfo obj, int position) {
                ActivityNewsInfoDetails.navigate(ActivityNewsInfo.this, obj.id, false);
            }
        });

        // detect when scroll reach bottom
        mAdapter.setOnLoadMoreListener(new AdapterNewsInfo.OnLoadMoreListener() {
            @Override
            public void onLoadMore(int current_page) {
                if (post_total > mAdapter.getItemCount() && current_page != 0) {
                    int next_page = current_page + 1;
                    requestAction(next_page);
                } else {
                    mAdapter.setLoaded();
                }
            }
        });

        // on swipe list
        swipe_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (callbackCall != null && callbackCall.isExecuted()) callbackCall.cancel();
                mAdapter.resetListData();
                requestAction(1);
            }
        });

        requestAction(1);
    }

    private void displayApiResult(final List<NewsInfo> items) {
        mAdapter.insertData(items);
        swipeProgress(false);
        if (items.size() == 0) {
            showNoItemView(true);
        }
    }

    private void requestListNewsInfo(final int page_no) {
        API api = RestAdapter.createAPI();
        callbackCall = api.getListNewsInfo(page_no, Constant.NEWS_PER_REQUEST, null);
        callbackCall.enqueue(new Callback<RespNewsInfo>() {
            @Override
            public void onResponse(Call<RespNewsInfo> call, Response<RespNewsInfo> response) {
                RespNewsInfo resp = response.body();
                if (resp != null && resp.status.equals("success")) {
                    post_total = resp.count_total;
                    displayApiResult(resp.news_infos);
                } else {
                    onFailRequest(page_no);
                }
            }

            @Override
            public void onFailure(Call<RespNewsInfo> call, Throwable t) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }

        });
    }

    private void onFailRequest(int page_no) {
        failed_page = page_no;
        mAdapter.setLoaded();
        swipeProgress(false);
        if (NetworkCheck.isConnect(this)) {
            showFailedView(getString(R.string.failed_text));
        } else {
            showFailedView(getString(R.string.no_internet_text));
        }
    }

    private void requestAction(final int page_no) {
        hideFailedView();
        showNoItemView(false);
        if (page_no == 1) {
            swipeProgress(true);
        } else {
            mAdapter.setLoading();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                requestListNewsInfo(page_no);
            }
        }, 2000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        swipeProgress(false);
        if (callbackCall != null && callbackCall.isExecuted()) {
            callbackCall.cancel();
        }
    }

    private void showFailedView(String message) {
        failed_snackbar = Snackbar.make(parent_view, message, Snackbar.LENGTH_INDEFINITE);
        failed_snackbar.setAction(R.string.TRY_AGAIN, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAction(failed_page);
                recyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
            }
        });
        failed_snackbar.show();
    }

    private void hideFailedView() {
        if (failed_snackbar != null && failed_snackbar.isShownOrQueued()) failed_snackbar.dismiss();
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = (View) findViewById(R.id.lyt_no_item);
        ((TextView) findViewById(R.id.no_item_message)).setText("no_post");
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_no_item.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_no_item.setVisibility(View.GONE);
        }
    }

    private void swipeProgress(final boolean show) {
        if (!show) {
            swipe_refresh.setRefreshing(show);
            return;
        }
        swipe_refresh.post(new Runnable() {
            @Override
            public void run() {
                swipe_refresh.setRefreshing(show);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int item_id = item.getItemId();
        if (item_id == android.R.id.home) {
            super.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
