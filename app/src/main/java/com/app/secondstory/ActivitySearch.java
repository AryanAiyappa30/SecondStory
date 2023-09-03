package com.app.secondstory;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.snackbar.Snackbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.app.secondstory.adapter.AdapterProduct;
import com.app.secondstory.connection.API;
import com.app.secondstory.connection.RestAdapter;
import com.app.secondstory.connection.callbacks.RespProduct;
import com.app.secondstory.data.Constant;
import com.app.secondstory.model.Category;
import com.app.secondstory.model.Product;
import com.app.secondstory.model.SortBy;
import com.app.secondstory.utils.NetworkCheck;
import com.app.secondstory.utils.Tools;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivitySearch extends AppCompatActivity {

    private static final String EXTRA_CATEGORY_ID = "key.EXTRA_CATEGORY_ID";
    private static final String EXTRA_CATEGORY_NAME = "key.EXTRA_CATEGORY_NAME";

    // activity transition
    public static void navigate(Activity activity, Category category) {
        Intent i = new Intent(activity, ActivitySearch.class);
        i.putExtra(EXTRA_CATEGORY_ID, category.id);
        i.putExtra(EXTRA_CATEGORY_NAME, category.name);
        activity.startActivity(i);
    }

    // activity transition
    public static void navigate(Activity activity) {
        Intent i = new Intent(activity, ActivitySearch.class);
        i.putExtra(EXTRA_CATEGORY_NAME, activity.getString(R.string.ALL));
        activity.startActivity(i);
    }

    private Toolbar toolbar;
    private ActionBar actionBar;
    private EditText et_search;
    private RecyclerView recyclerView;
    private AdapterProduct mAdapter;
    private ImageButton bt_clear;
    private View parent_view;
    private SwipeRefreshLayout swipe_refresh;
    private Call<RespProduct> callbackCall = null;

    private int post_total = 0;
    private int failed_page = 0;

    private String query = "";
    private SortBy sort;
    private int sort_selected = 0;
    private long category_id = -1L;

    private String category_name;
    private Snackbar failed_snackbar = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        category_name = getString(R.string.ALL);
        category_id = getIntent().getLongExtra(EXTRA_CATEGORY_ID, -1L);
        category_name = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
        sort = ThisApplication.getInstance().getSorts().get(0);

        initComponent();
        setupToolbar();
    }

    private void initComponent() {
        parent_view = findViewById(android.R.id.content);
        swipe_refresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        et_search = (EditText) findViewById(R.id.et_search);
        et_search.addTextChangedListener(textWatcher);

        bt_clear = (ImageButton) findViewById(R.id.bt_clear);
        ((TextView) findViewById(R.id.category)).setText(getString(R.string.Category) + category_name);
        bt_clear.setVisibility(View.GONE);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new GridLayoutManager(this, Tools.getGridSpanCount(this)));
        recyclerView.setHasFixedSize(true);
        //set data and list adapter
        mAdapter = new AdapterProduct(this, recyclerView, new ArrayList<Product>());
        recyclerView.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener(new AdapterProduct.OnItemClickListener() {
            @Override
            public void onItemClick(View v, Product obj, int pos) {
                ActivityProductDetails.navigate(ActivitySearch.this, obj.id, false);
            }
        });

        // detect when scroll reach bottom
        mAdapter.setOnLoadMoreListener(new AdapterProduct.OnLoadMoreListener() {
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

        bt_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                et_search.setText("");
                mAdapter.resetListData();
                showNoItemView(true);
            }
        });

        et_search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    hideKeyboard();
                    searchAction();
                    return true;
                }
                return false;
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

        showNoItemView(true);
    }

    private void searchAction() {
        query = et_search.getText().toString().trim();
        if (!query.equals("")) {
            mAdapter.resetListData();
            // request action will be here
            requestAction(1);
        } else {
            Toast.makeText(this, R.string.please_fill, Toast.LENGTH_SHORT).show();
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

        // analytics track
        //ThisApplication.getInstance().saveCustomLogEvent("SEARCH_PRODUCT", "keyword", query);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                requestListProduct(page_no);
            }
        }, 1000);
    }

    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }


    private void requestListProduct(final int page_no) {
        API api = RestAdapter.createAPI();
        callbackCall = api.getListProduct(page_no, Constant.PRODUCT_PER_REQUEST, query, category_id, sort.column, sort.order);
        callbackCall.enqueue(new Callback<RespProduct>() {
            @Override
            public void onResponse(Call<RespProduct> call, Response<RespProduct> response) {
                RespProduct resp = response.body();
                if (resp != null && resp.status.equals("success")) {
                    post_total = resp.count_total;
                    displayApiResult(resp.products);
                } else {
                    onFailRequest(page_no);
                }
            }

            @Override
            public void onFailure(Call<RespProduct> call, Throwable t) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }

        });
    }

    private void displayApiResult(final List<Product> items) {
        mAdapter.insertData(items);
        swipeProgress(false);
        if (items.size() == 0) showNoItemView(true);
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


    @Override
    protected void onResume() {
        mAdapter.notifyDataSetChanged();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        } else if (id == R.id.action_sort) {
            showDialogProductSort(item);
        }
        return super.onOptionsItemSelected(item);
    }

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence c, int i, int i1, int i2) {
            if (c.toString().trim().length() == 0) {
                bt_clear.setVisibility(View.GONE);
            } else {
                bt_clear.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence c, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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

    private void showDialogProductSort(final MenuItem item) {
        final List<SortBy> sorts = ThisApplication.getInstance().getSorts();
        String[] sort_choice = new String[sorts.size()];
        for (int i = 0; i < sorts.size(); i++) sort_choice[i] = sorts.get(i).label;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_sort);
        builder.setSingleChoiceItems(sort_choice, sort_selected, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                sort_selected = i;
                sort = sorts.get(sort_selected);
                dialogInterface.dismiss();
                initComponent();
                searchAction();
            }
        });
        builder.show();
    }

}

