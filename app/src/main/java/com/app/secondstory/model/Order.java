package com.app.secondstory.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Order implements Serializable {

    public Long id;
    public String code;
    public String buyer;
    public String total_fees;
    public String status = "";
    public String payment_status = "";
    public Long created_at = System.currentTimeMillis();
    public Long last_update = System.currentTimeMillis();
    public List<Cart> cart_list = new ArrayList<>();

    public Order() {
    }

    public Order(Long id, String code, String total_fees) {
        this.id = id;
        this.code = code;
        this.total_fees = total_fees;
    }
}



