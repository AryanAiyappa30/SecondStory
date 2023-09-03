package com.app.secondstory;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.secondstory.adapter.AdapterOrderHistory;
import com.app.secondstory.adapter.AdapterShoppingCart;
import com.app.secondstory.connection.API;
import com.app.secondstory.connection.RestAdapter;
import com.app.secondstory.connection.callbacks.RespOrderHistory;
import com.app.secondstory.data.Constant;
import com.app.secondstory.data.DatabaseHandler;
import com.app.secondstory.data.SharedPref;
import com.app.secondstory.model.Info;
import com.app.secondstory.model.Order;
import com.app.secondstory.model.OrderIds;
import com.app.secondstory.utils.NetworkCheck;
import com.app.secondstory.utils.Tools;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityOrderHistory extends AppCompatActivity {

    private View parent_view;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipe_refresh;
    private DatabaseHandler db;
    private AdapterOrderHistory mAdapter;
    private SharedPref sharedPref;
    private Info info;
    private Snackbar failed_snackbar = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);
        db = new DatabaseHandler(this);
        sharedPref = new SharedPref(this);
        info = sharedPref.getInfoData();

        initToolbar();
        iniComponent();
    }

    private void iniComponent() {
        parent_view = findViewById(android.R.id.content);
        swipe_refresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //set data and list adapter
        mAdapter = new AdapterOrderHistory(this, recyclerView, new ArrayList<Order>());
        recyclerView.setAdapter(mAdapter);
        recyclerView.setNestedScrollingEnabled(false);

        mAdapter.setOnItemClickListener(new AdapterOrderHistory.OnItemClickListener() {
            @Override
            public void onItemClick(View view, Order obj, int position) {
                dialogOrderHistoryDetails(obj, position);
            }
        });

        final int item_count = (int) db.getOrderSize();
        // detect when scroll reach bottom
        mAdapter.setOnLoadMoreListener(new AdapterOrderHistory.OnLoadMoreListener() {
            @Override
            public void onLoadMore(final int current_page) {
                if (item_count > mAdapter.getItemCount() && current_page != 0) {
                    requestActionDelay(current_page);
                } else {
                    mAdapter.setLoaded();
                }
            }
        });

        // on swipe list
        swipe_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onSwipeRefresh(false);
            }
        });

        requestActionDelay(1);
    }

    private void onSwipeRefresh(boolean dialog) {
        if (callbackCall != null && callbackCall.isExecuted()) callbackCall.cancel();
        if (dialog) swipeProgress(true);
        mAdapter.resetListData();
        requestActionDelay(1);
    }

    private void initToolbar() {
        ActionBar actionBar;
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.title_activity_history);
        Tools.systemBarLolipop(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_order_history, menu);
        return true;
    }

    private void requestActionDelay(final int next_page) {
        mAdapter.setLoading();
        hideFailedView();
        showNoItemView(false);
        if (next_page == 1) {
            swipeProgress(true);
        } else {
            mAdapter.setLoading();
        }
        int offset = Constant.ORDER_HISTORY_PAGE * (next_page - 1);
        List<Order> items = db.getOrderList(Constant.ORDER_HISTORY_PAGE, offset);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getListProductOrder(items, next_page);
            }
        }, 500);
    }

    private Call<List<Order>> callbackCall;

    private void getListProductOrder(List<Order> items, final int page_no) {
        OrderIds ids = new OrderIds(items);
        API api = RestAdapter.createAPI();
        callbackCall = api.getListProductOrder(ids);
        callbackCall.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.body().size() > 0) {
                    db.saveOrders(response.body());
                }
                displayResult(page_no);
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }
        });
    }

    private void displayResult(int page_no) {
        int offset = Constant.ORDER_HISTORY_PAGE * (page_no - 1);
        List<Order> items = db.getOrderList(Constant.ORDER_HISTORY_PAGE, offset);
        mAdapter.insertData(items);
        mAdapter.setLoaded();
        swipeProgress(false);
        showNoItemView(items.size() == 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int item_id = item.getItemId();
        if (item_id == android.R.id.home) {
            super.onBackPressed();
        } else if (item_id == R.id.action_delete) {
            if (mAdapter.getItemCount() == 0) {
                Snackbar.make(parent_view, R.string.msg_history_empty, Snackbar.LENGTH_SHORT).show();
                return true;
            }
            dialogDeleteConfirmation();
        } else if (item_id == R.id.action_add) {
            dialogOrderHistoryAdd();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = (View) findViewById(R.id.lyt_no_item);
        if (show) {
            lyt_no_item.setVisibility(View.VISIBLE);
        } else {
            lyt_no_item.setVisibility(View.GONE);
        }
    }

    private void onFailRequest(int failed_page) {
        mAdapter.setLoaded();
        swipeProgress(false);
        String message;
        if (NetworkCheck.isConnect(this)) {
            message = getString(R.string.failed_text);
        } else {
            message = getString(R.string.no_internet_text);
        }

        failed_snackbar = Snackbar.make(parent_view, message, Snackbar.LENGTH_INDEFINITE);
        failed_snackbar.setAction(R.string.TRY_AGAIN, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestActionDelay(failed_page);
                if (failed_page != 0) recyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
            }
        });
        failed_snackbar.show();
    }

    private void hideFailedView() {
        if (failed_snackbar != null && failed_snackbar.isShownOrQueued()) failed_snackbar.dismiss();
    }

    private void swipeProgress(final boolean show) {
        if (show && swipe_refresh.isRefreshing()) return;
        if (!show && !swipe_refresh.isRefreshing()) return;
        swipe_refresh.post(new Runnable() {
            @Override
            public void run() {
                swipe_refresh.setRefreshing(show);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void dialogDeleteConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_delete_confirm);
        builder.setMessage(getString(R.string.content_delete_confirm) + getString(R.string.title_activity_history));
        builder.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface di, int i) {
                di.dismiss();
                db.deleteOrder();
                onSwipeRefresh(true);
                Snackbar.make(parent_view, R.string.delete_success, Snackbar.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.CANCEL, null);
        builder.show();
    }

    public void dialogDeleteOneConfirmation(Order order, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_delete_confirm);
        String msg = String.format(getString(R.string.content_delete_one_confirm), order.code);
        builder.setMessage(msg);
        builder.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface di, int i) {
                di.dismiss();
                db.deleteOrder(order.id);
                mAdapter.removeItem(position);
                Snackbar.make(parent_view, R.string.delete_success, Snackbar.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.CANCEL, null);
        builder.show();
    }

    private void dialogOrderHistoryAdd() {
        final Dialog dialog = new Dialog(ActivityOrderHistory.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.dialog_order_history_add);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        final EditText tv_code = dialog.findViewById(R.id.code);
        final TextView tv_message = dialog.findViewById(R.id.tv_message);
        final View progress_loading = dialog.findViewById(R.id.progress_loading);
        final Button bt_submit = dialog.findViewById(R.id.bt_submit);

        ((ImageView) dialog.findViewById(R.id.img_close)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        final RequestOrderHistoryListener listener = new RequestOrderHistoryListener() {

            @Override
            public void loading(boolean show) {
                progress_loading.setVisibility(show ? View.VISIBLE : View.GONE);
                bt_submit.setVisibility(show ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onSuccess(Order order) {
                progress_loading.setVisibility(View.GONE);
                db.saveOrder(order);
                dialog.dismiss();
                String message = String.format(getResources().getString(R.string.order_added), order.code);
                Toast.makeText(ActivityOrderHistory.this, message, Toast.LENGTH_LONG).show();
                onSwipeRefresh(true);
                dialogOrderHistoryDetails(order, -1);
            }

            @Override
            public void onFailed() {
                tv_message.setText(R.string.data_not_found);
            }
        };

        bt_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tv_message.setText("");
                String code = tv_code.getText().toString();
                if (TextUtils.isEmpty(code)) {
                    tv_message.setText(R.string.please_fill_order_code);
                } else {
                    if (db.isOrderExist(code)) {
                        tv_message.setText(R.string.order_already_exist);
                        return;
                    }
                    searchOrderHistory(code, listener);
                }
            }
        });

        tv_code.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable et) {
                String s = et.toString();
                if (!s.equals(s.toUpperCase())) {
                    s = s.toUpperCase();
                    tv_code.setText(s);
                    tv_code.setSelection(tv_code.length());
                }
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    private void dialogOrderHistoryDetails(final Order order, int position) {
        final Dialog dialog = new Dialog(ActivityOrderHistory.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.dialog_order_history_details);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        Button bt_pay_now = dialog.findViewById(R.id.bt_pay_now);
        if (order.payment_status.equals("") && order.status.equals("WAITING")) {
            bt_pay_now.setVisibility(View.VISIBLE);
        }
        ((TextView) dialog.findViewById(R.id.code)).setText(order.code);
        String total = order.total_fees;
        if (Tools.isDouble(order.total_fees))
            total = Tools.getFormattedPrice(Double.parseDouble(order.total_fees), this);
        ((TextView) dialog.findViewById(R.id.buyer_name)).setText(order.buyer);
        ((TextView) dialog.findViewById(R.id.status)).setText(Tools.getOrderStatus(this, order.status));
        ((TextView) dialog.findViewById(R.id.payment_status)).setText(Tools.getOrderPaymentStatus(this, order.payment_status));
        ((TextView) dialog.findViewById(R.id.code)).setText(order.code);
        ((TextView) dialog.findViewById(R.id.total)).setText(total);

        RecyclerView recyclerView = (RecyclerView) dialog.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        AdapterShoppingCart _adapter = new AdapterShoppingCart(this, false, order.cart_list);
        recyclerView.setAdapter(_adapter);
        recyclerView.setNestedScrollingEnabled(false);

        (dialog.findViewById(R.id.img_close)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        if(position == -1) (dialog.findViewById(R.id.img_delete)).setVisibility(View.GONE);
        (dialog.findViewById(R.id.img_delete)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                dialogDeleteOneConfirmation(order, position);
            }
        });

        (dialog.findViewById(R.id.copy)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Tools.copyToClipboard(getApplicationContext(), order.code);
            }
        });

        bt_pay_now.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Tools.directLinkToCustomTabs(ActivityOrderHistory.this, Constant.getURLPayment(order.code));
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    private Call<RespOrderHistory> callbackOrderHistory;

    private void searchOrderHistory(String code, final RequestOrderHistoryListener listener) {
        listener.loading(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                API api = RestAdapter.createAPI();
                callbackOrderHistory = api.getProductOrder(code);
                callbackOrderHistory.enqueue(new Callback<RespOrderHistory>() {
                    @Override
                    public void onResponse(Call<RespOrderHistory> call, Response<RespOrderHistory> response) {
                        listener.loading(false);
                        RespOrderHistory resp = response.body();
                        if (resp != null && resp.status.equals("success")) {
                            listener.onSuccess(resp.product_order);
                        } else {
                            listener.onFailed();
                        }
                    }

                    @Override
                    public void onFailure(Call<RespOrderHistory> call, Throwable t) {
                        listener.loading(false);
                        listener.onFailed();
                    }

                });
            }
        }, 1000);
    }

    private interface RequestOrderHistoryListener {
        void onSuccess(Order order);

        void loading(boolean show);

        void onFailed();
    }

}
