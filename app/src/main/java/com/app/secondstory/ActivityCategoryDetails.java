package com.app.secondstory;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.view.MenuItemCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.secondstory.adapter.AdapterProduct;
import com.app.secondstory.connection.API;
import com.app.secondstory.connection.RestAdapter;
import com.app.secondstory.connection.callbacks.RespProduct;
import com.app.secondstory.data.AppConfig;
import com.app.secondstory.data.Constant;
import com.app.secondstory.data.DatabaseHandler;
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

public class ActivityCategoryDetails extends AppCompatActivity {
    private static final String EXTRA_OBJECT = "key.EXTRA_OBJECT";

    // activity transition
    public static void navigate(Activity activity, Category obj) {
        Intent i = new Intent(activity, ActivityCategoryDetails.class);
        i.putExtra(EXTRA_OBJECT, obj);
        activity.startActivity(i);
    }

    // extra obj
    private Category category;

    private Toolbar toolbar;
    private ActionBar actionBar;
    private View parent_view;
    private FloatingActionButton fab;
    private SwipeRefreshLayout swipe_refresh;
    private Call<RespProduct> callbackCall = null;

    private RecyclerView recyclerView;
    private AdapterProduct mAdapter;
    private DatabaseHandler db;
    private Snackbar failed_snackbar = null;

    private int post_total = 0;
    private int failed_page = 0;
    private SortBy sort;
    private int sort_selected = 0;

    private View cart_badge;
    private int cart_count = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_details);
        parent_view = findViewById(R.id.parent_view);
        category = (Category) getIntent().getSerializableExtra(EXTRA_OBJECT);
        sort = ThisApplication.getInstance().getSorts().get(sort_selected);
        db = new DatabaseHandler(this);

        initComponent();
        initToolbar();

        displayCategoryData(category);

        requestAction(1);

        Tools.RTLMode(getWindow());
    }

    private void initComponent() {
        fab = (FloatingActionButton) findViewById(R.id.fab);
        swipe_refresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, Tools.getGridSpanCount(this)));
        recyclerView.setHasFixedSize(true);

        //set data and list adapter
        mAdapter = new AdapterProduct(this, recyclerView, new ArrayList<Product>());
        recyclerView.setAdapter(mAdapter);

        // on item list clicked
        mAdapter.setOnItemClickListener(new AdapterProduct.OnItemClickListener() {
            @Override
            public void onItemClick(View v, Product obj, int position) {
                ActivityProductDetails.navigate(ActivityCategoryDetails.this, obj.id, false);
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

        // on swipe list
        swipe_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (callbackCall != null && callbackCall.isExecuted()) callbackCall.cancel();
                mAdapter.resetListData();
                requestAction(1);
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView v, int state) {
                super.onScrollStateChanged(v, state);
                if (state == v.SCROLL_STATE_DRAGGING || state == v.SCROLL_STATE_SETTLING) {
                    animateFab(true);
                } else {
                    animateFab(false);
                }
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivitySearch.navigate(ActivityCategoryDetails.this, category);
            }
        });
    }

    private void displayCategoryData(Category c) {
        ((AppBarLayout) findViewById(R.id.app_bar_layout)).setBackgroundColor(Color.parseColor(c.color));
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(c.color)));
        ((TextView) findViewById(R.id.name)).setText(c.name);
        ((TextView) findViewById(R.id.brief)).setText(c.brief);
        ImageView icon = (ImageView) findViewById(R.id.icon);
        Tools.displayImageOriginal(this, icon, Constant.getURLimgCategory(c.icon));
        Tools.setSystemBarColorDarker(this, c.color);
        if (AppConfig.TINT_CATEGORY_ICON) {
            icon.setColorFilter(Color.WHITE);
        }

        // analytics track
        //ThisApplication.getInstance().saveLogEvent(c.id, c.name, "CATEGORY_DETAILS");
    }


    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_category_details, menu);

        final MenuItem menuItem = menu.findItem(R.id.action_cart);
        View actionView = MenuItemCompat.getActionView(menuItem);
        cart_badge = actionView.findViewById(R.id.cart_badge);

        setupBadge();

        actionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(menuItem);
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int item_id = item.getItemId();
        if (item_id == android.R.id.home) {
            super.onBackPressed();
        } else if (item_id == R.id.action_sort) {
            showDialogProductSort(item);
        } else if (item_id == R.id.action_cart) {
            Intent i = new Intent(this, ActivityShoppingCart.class);
            startActivity(i);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int new_cart_count = db.getActiveCartSize();
        if (new_cart_count != cart_count) {
            cart_count = new_cart_count;
            invalidateOptionsMenu();
        }
    }


    private void displayApiResult(final List<Product> items) {
        mAdapter.insertData(items);
        swipeProgress(false);
        if (items.size() == 0) showNoItemView(true);
    }

    private void requestListProduct(final int page_no) {
        API api = RestAdapter.createAPI();
        callbackCall = api.getListProduct(page_no, Constant.PRODUCT_PER_REQUEST, null, category.id, sort.column, sort.order);
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
                requestListProduct(page_no);
            }
        }, 1000);
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
                requestAction(1);
            }
        });
        builder.show();
    }

    private void setupBadge() {
        if (cart_badge == null) return;
        if (cart_count == 0) {
            cart_badge.setVisibility(View.GONE);
        } else {
            cart_badge.setVisibility(View.VISIBLE);
            String count_txt = cart_count + "";
            if (cart_count > 9) count_txt = "9+";
            ((TextView) cart_badge.findViewById(R.id.counter)).setText(count_txt);
        }
    }

    boolean isFabHide = false;

    private void animateFab(final boolean hide) {
        if (isFabHide && hide || !isFabHide && !hide) return;
        isFabHide = hide;
        int moveY = hide ? (2 * fab.getHeight()) : 0;
        fab.animate().translationY(moveY).setStartDelay(100).setDuration(300).start();
    }
}
