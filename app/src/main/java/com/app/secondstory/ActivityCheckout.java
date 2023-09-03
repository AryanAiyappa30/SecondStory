package com.app.secondstory;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.secondstory.adapter.AdapterShipping;
import com.app.secondstory.adapter.AdapterShoppingCart;
import com.app.secondstory.connection.API;
import com.app.secondstory.connection.RestAdapter;
import com.app.secondstory.connection.callbacks.RespOrder;
import com.app.secondstory.connection.callbacks.RespShipping;
import com.app.secondstory.data.Constant;
import com.app.secondstory.data.DatabaseHandler;
import com.app.secondstory.data.SharedPref;
import com.app.secondstory.model.BuyerProfile;
import com.app.secondstory.model.Cart;
import com.app.secondstory.model.Checkout;
import com.app.secondstory.model.Info;
import com.app.secondstory.model.Order;
import com.app.secondstory.model.ProductOrder;
import com.app.secondstory.model.ProductOrderDetail;
import com.app.secondstory.model.Shipping;
import com.app.secondstory.utils.CallbackDialog;
import com.app.secondstory.utils.DialogUtils;
import com.app.secondstory.utils.Tools;
import com.balysv.materialripple.MaterialRippleLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityCheckout extends AppCompatActivity {

    private View parent_view;
    private TextView shipping;
    private TextView date_shipping;
    private RecyclerView recyclerView;
    private MaterialRippleLayout lyt_add_cart;
    private TextView subtotal, tax, price_tax, shipping_rate, total;
    private TextInputLayout buyer_name_lyt, email_lyt, phone_lyt, address_lyt, comment_lyt;
    private EditText buyer_name, email, phone, address, comment;
    private RadioGroup shipping_class;
    private RadioButton radio_economy, radio_regular, radio_express;

    private DatePickerDialog datePickerDialog;
    private AdapterShoppingCart adapter;
    private DatabaseHandler db;
    private SharedPref sharedPref;
    private Info info;
    private BuyerProfile buyerProfile;
    private Shipping selected_shipping = null;
    private Long date_ship_millis = 0L;
    private Double _total = 0D;
    private String _total_str;
    private ProductOrder productOrder = new ProductOrder();

    private Call<RespOrder> callbackCall = null;
    // construct dialog progress
    ProgressDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        db = new DatabaseHandler(this);
        sharedPref = new SharedPref(this);
        info = sharedPref.getInfoData();
        buyerProfile = sharedPref.getBuyerProfile();

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
        actionBar.setTitle(R.string.title_activity_checkout);
        Tools.systemBarLolipop(this);
    }

    private void iniComponent() {
        parent_view = findViewById(android.R.id.content);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        lyt_add_cart = findViewById(R.id.lyt_add_cart);

        // cost view
        subtotal = findViewById(R.id.total_order);
        tax = findViewById(R.id.tax);
        price_tax = findViewById(R.id.price_tax);
        shipping_rate = findViewById(R.id.shipping_rate);
        total = findViewById(R.id.total_fees);

        // form view
        buyer_name = findViewById(R.id.buyer_name);
        email = findViewById(R.id.email);
        phone = findViewById(R.id.phone);
        address = findViewById(R.id.address);
        comment = findViewById(R.id.comment);

        buyer_name.addTextChangedListener(new CheckoutTextWatcher(buyer_name));
        email.addTextChangedListener(new CheckoutTextWatcher(email));
        phone.addTextChangedListener(new CheckoutTextWatcher(phone));
        address.addTextChangedListener(new CheckoutTextWatcher(address));
        comment.addTextChangedListener(new CheckoutTextWatcher(comment));

        buyer_name_lyt = findViewById(R.id.buyer_name_lyt);
        email_lyt = findViewById(R.id.email_lyt);
        phone_lyt = findViewById(R.id.phone_lyt);
        address_lyt = findViewById(R.id.address_lyt);
        comment_lyt = findViewById(R.id.comment_lyt);
        shipping = findViewById(R.id.shipping);
        date_shipping = findViewById(R.id.date_shipping);
        shipping_class = findViewById(R.id.shipping_class);
        radio_economy = findViewById(R.id.radio_economy);
        radio_regular = findViewById(R.id.radio_regular);
        radio_express = findViewById(R.id.radio_express);

        progressDialog = new ProgressDialog(ActivityCheckout.this);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(R.string.title_please_wait);
        progressDialog.setMessage(getString(R.string.content_submit_checkout));

        (findViewById(R.id.bt_date_shipping)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogDatePicker();
            }
        });
        (findViewById(R.id.bt_shipping_location)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialogShipping();
            }
        });

        lyt_add_cart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitForm();
            }
        });

        (findViewById(R.id.lyt_shipping)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogShipping();
            }
        });

        shipping_class.setVisibility(View.GONE);
        shipping_class.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {
                if(id == R.id.radio_economy){
                    productOrder.shipping = getString(R.string.shipping_economy);
                    productOrder.shipping_rate = selected_shipping.rate_economy;
                } else if(id == R.id.radio_regular){
                    productOrder.shipping = getString(R.string.shipping_regular);
                    productOrder.shipping_rate = selected_shipping.rate_regular;
                } else if(id == R.id.radio_express){
                    productOrder.shipping = getString(R.string.shipping_express);
                    productOrder.shipping_rate = selected_shipping.rate_express;
                }
                setTotalPrice();
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

    @Override
    protected void onResume() {
        super.onResume();
        displayData();
    }

    private void displayData() {
        List<Cart> items = db.getActiveCartList();
        adapter = new AdapterShoppingCart(this, false, items);
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);
        setTotalPrice();
        if (buyerProfile != null) {
            buyer_name.setText(buyerProfile.name);
            email.setText(buyerProfile.email);
            phone.setText(buyerProfile.phone);
            address.setText(buyerProfile.address);
        }
    }

    private void setTotalPrice() {
        List<Cart> items = adapter.getItem();
        Double _subtotal = 0D, _price_tax = 0D, _shipping_rate = 0D;
        String _total_order_str, _price_tax_str, _shipping_rate_str = "";
        for (Cart c : items) {
            _subtotal = _subtotal + (c.amount * c.price_item);
        }
        _price_tax = _subtotal * info.tax / 100;
        _total = _subtotal + _price_tax;
        if(selected_shipping != null && !TextUtils.isEmpty(productOrder.shipping)){
            _shipping_rate = productOrder.shipping_rate;
            _total = _subtotal + _price_tax + _shipping_rate;
            _shipping_rate_str = Tools.getFormattedPrice(_shipping_rate, this);
        }
        _price_tax_str = Tools.getFormattedPrice(_price_tax, this);
        _total_order_str = Tools.getFormattedPrice(_subtotal, this);
        _total_str = Tools.getFormattedPrice(_total, this);

        // set to display
        subtotal.setText(_total_order_str);
        tax.setText(getString(R.string.tax) + info.tax + "%");
        shipping_rate.setText(_shipping_rate_str);
        price_tax.setText(_price_tax_str);
        total.setText(_total_str);
    }


    private void submitForm() {
        if (!validateName()) {
            Snackbar.make(parent_view, R.string.invalid_name, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (!validateEmail()) {
            Snackbar.make(parent_view, R.string.invalid_email, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (!validatePhone()) {
            Snackbar.make(parent_view, R.string.invalid_phone, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (!validateAddress()) {
            Snackbar.make(parent_view, R.string.invalid_address, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (!validateShipping()) {
            Snackbar.make(parent_view, R.string.invalid_shipping, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (!validateDateShip()) {
            Snackbar.make(parent_view, R.string.invalid_date_ship, Snackbar.LENGTH_SHORT).show();
            return;
        }

        buyerProfile = new BuyerProfile();
        buyerProfile.name = buyer_name.getText().toString();
        buyerProfile.email = email.getText().toString();
        buyerProfile.phone = phone.getText().toString();
        buyerProfile.address = address.getText().toString();
        sharedPref.setBuyerProfile(buyerProfile);

        // hide keyboard
        hideKeyboard();

        // show dialog confirmation
        dialogConfirmCheckout();
    }

    private void submitOrderData() {
        // prepare checkout data
        Checkout checkout = new Checkout();
        productOrder.setBuyerProfile(buyerProfile);
        productOrder.date_ship = date_ship_millis;
        productOrder.comment = comment.getText().toString().trim();
        productOrder.status = "WAITING";
        productOrder.total_fees = _total;
        productOrder.tax = info.tax;
        // to support notification
        productOrder.serial = Tools.getDeviceID(this);

        checkout.product_order = productOrder;
        checkout.product_order_detail = new ArrayList<>();
        for (Cart c : adapter.getItem()) {
            ProductOrderDetail pod = new ProductOrderDetail(c.product_id, c.product_name, c.amount, c.price_item);
            checkout.product_order_detail.add(pod);
        }

        // submit data to server
        API api = RestAdapter.createAPI();
        callbackCall = api.submitProductOrder(checkout);
        callbackCall.enqueue(new Callback<RespOrder>() {
            @Override
            public void onResponse(Call<RespOrder> call, Response<RespOrder> response) {
                RespOrder resp = response.body();
                if (resp != null && resp.status.equals("success")) {
                    Order order = new Order(resp.data.id, resp.data.code, _total.toString());
                    for (Cart c : adapter.getItem()) {
                        c.order_id = order.id;
                        order.cart_list.add(c);
                    }
                    db.saveOrder(order);
                    dialogSuccess(order.code);
                } else {
                    dialogFailedRetry();
                }

            }

            @Override
            public void onFailure(Call<RespOrder> call, Throwable t) {
                Log.e("onFailure", t.getMessage());
                if (!call.isCanceled()) dialogFailedRetry();
            }
        });
    }

    // give delay when submit data to give good UX
    private void delaySubmitOrderData() {
        progressDialog.show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                submitOrderData();
            }
        }, 2000);
    }

    public void dialogConfirmCheckout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirmation);
        builder.setMessage(getString(R.string.confirm_checkout));
        builder.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                delaySubmitOrderData();
            }
        });
        builder.setNegativeButton(R.string.NO, null);
        builder.show();
    }

    public void dialogFailedRetry() {
        progressDialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.failed);
        builder.setMessage(getString(R.string.failed_checkout));
        builder.setPositiveButton(R.string.TRY_AGAIN, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                delaySubmitOrderData();
            }
        });
        builder.setNegativeButton(R.string.SETTING, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivity(new Intent(getApplicationContext(), ActivitySettings.class));
            }
        });
        builder.show();
    }

    public void dialogSuccess(final String code) {
        progressDialog.dismiss();
        Dialog dialog = new DialogUtils(this).buildDialogInfo(
                getString(R.string.success_checkout),
                String.format(getString(R.string.msg_success_checkout), code),
                getString(R.string.OK),
                getString(R.string.PAY_NOW),
                R.drawable.img_checkout_success,
                new CallbackDialog() {
                    @Override
                    public void onPositiveClick(Dialog dialog) {
                        finish();
                        dialog.dismiss();
                    }

                    @Override
                    public void onNegativeClick(Dialog dialog) {
                        Tools.directLinkToCustomTabs(ActivityCheckout.this, Constant.getURLPayment(code));
                    }
                });
        dialog.show();
    }

    private void dialogDatePicker() {
        Calendar cur_calender = Calendar.getInstance();
        datePickerDialog = new DatePickerDialog(this, R.style.DatePickerTheme, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int _year, int _month, int _day) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, _year);
                calendar.set(Calendar.MONTH, _month);
                calendar.set(Calendar.DAY_OF_MONTH, _day);
                date_ship_millis = calendar.getTimeInMillis();
                date_shipping.setText(Tools.getFormattedDateSimple(date_ship_millis));
                datePickerDialog.dismiss();
            }
        }, cur_calender.get(Calendar.YEAR), cur_calender.get(Calendar.MONTH), cur_calender.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.setCancelable(true);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private Call<RespShipping> callbackShipping;

    private void searchShippingLocationAPI(String query, final RequestShippingListener listener) {
        listener.loading(true);
        API api = RestAdapter.createAPI();
        callbackShipping = api.getShipping(query);
        callbackShipping.enqueue(new Callback<RespShipping>() {
            @Override
            public void onResponse(Call<RespShipping> call, Response<RespShipping> response) {
                listener.loading(false);
                RespShipping resp = response.body();
                if (resp != null && resp.status.equals("success")) {
                    listener.onSuccess(new ArrayList<Shipping>(resp.shipping));
                } else {
                    listener.onFailed();
                }
            }

            @Override
            public void onFailure(Call<RespShipping> call, Throwable t) {
                listener.loading(false);
                listener.onFailed();
            }

        });
    }


    private void showDialogShipping() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_shipping_location);
        dialog.setCancelable(true);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;

        final View progress_loading = dialog.findViewById(R.id.progress_loading);
        final View lyt_no_item = dialog.findViewById(R.id.lyt_no_item);

        EditText et_search = dialog.findViewById(R.id.et_search);
        et_search.setHint(R.string.hint_input_location);

        RecyclerView recycler = dialog.findViewById(R.id.recyclerView);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setHasFixedSize(true);

        //set data and list adapter
        final AdapterShipping mAdapter = new AdapterShipping(this);
        recycler.setAdapter(mAdapter);

        // on item list clicked
        mAdapter.setOnItemClickListener(new AdapterShipping.OnItemClickListener() {
            @Override
            public void onItemClick(View view, Shipping obj) {
                selected_shipping = obj;
                shipping.setText(obj.location);
                populateRadioShippingClass();
                dialog.dismiss();
            }
        });
        final RequestShippingListener listener = new RequestShippingListener() {

            @Override
            public void loading(boolean show) {
                progress_loading.setVisibility(show ? View.VISIBLE : View.GONE);
                lyt_no_item.setVisibility(show ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onSuccess(List<Shipping> shipping) {
                progress_loading.setVisibility(View.GONE);
                mAdapter.setItems(shipping);
                lyt_no_item.setVisibility(shipping.size() == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailed() {
                Toast.makeText(ActivityCheckout.this, R.string.failed_text, Toast.LENGTH_LONG).show();
            }
        };


        et_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(TextUtils.isEmpty(s)){
                    mAdapter.setItems(new ArrayList<Shipping>());
                } else {
                    searchShippingLocationAPI(s.toString(), listener);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        (dialog.findViewById(R.id.btn_close)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    private void populateRadioShippingClass() {
        shipping_class.setVisibility(View.VISIBLE);
        shipping_class.clearCheck();
        productOrder.shipping = "";
        productOrder.shipping_location = selected_shipping.location;
        radio_economy.setText(getString(R.string.shipping_economy)+" ( "+Tools.getFormattedPrice(selected_shipping.rate_economy, this)+" )");
        radio_regular.setText(getString(R.string.shipping_regular)+" ( "+Tools.getFormattedPrice(selected_shipping.rate_regular, this)+" )");
        radio_express.setText(getString(R.string.shipping_express)+" ( "+Tools.getFormattedPrice(selected_shipping.rate_express, this)+" )");
        radio_economy.setVisibility(selected_shipping.active_eco == 1 ? View.VISIBLE : View.GONE);
        radio_regular.setVisibility(selected_shipping.active_reg == 1 ? View.VISIBLE : View.GONE);
        radio_express.setVisibility(selected_shipping.active_exp == 1 ? View.VISIBLE : View.GONE);
        setTotalPrice();
    }

    // validation method
    private boolean validateEmail() {
        String str = email.getText().toString().trim();
        if (str.isEmpty() || !Tools.isValidEmail(str)) {
            email_lyt.setError(getString(R.string.invalid_email));
            requestFocus(email);
            return false;
        } else {
            email_lyt.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validateName() {
        String str = buyer_name.getText().toString().trim();
        if (str.isEmpty()) {
            buyer_name_lyt.setError(getString(R.string.invalid_name));
            requestFocus(buyer_name);
            return false;
        } else {
            buyer_name_lyt.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validatePhone() {
        String str = phone.getText().toString().trim();
        if (str.isEmpty()) {
            phone_lyt.setError(getString(R.string.invalid_phone));
            requestFocus(phone);
            return false;
        } else {
            phone_lyt.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validateAddress() {
        String str = address.getText().toString().trim();
        if (str.isEmpty()) {
            address_lyt.setError(getString(R.string.invalid_address));
            requestFocus(address);
            return false;
        } else {
            address_lyt.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validateShipping() {
        if (selected_shipping == null || TextUtils.isEmpty(productOrder.shipping)) {
            return false;
        }
        return true;
    }

    private boolean validateDateShip() {
        if (date_ship_millis == 0L) {
            return false;
        }
        return true;
    }


    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private class CheckoutTextWatcher implements TextWatcher {
        private View view;

        private CheckoutTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.email:
                    validateEmail();
                    break;
                case R.id.name:
                    validateName();
                    break;
                case R.id.phone:
                    validatePhone();
                    break;
                case R.id.address:
                    validateAddress();
                    break;
            }
        }
    }

    private interface RequestShippingListener {
        void onSuccess(List<Shipping> shipping);

        void loading(boolean show);

        void onFailed();
    }
}
