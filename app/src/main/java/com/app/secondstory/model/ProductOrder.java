package com.app.secondstory.model;

import java.io.Serializable;

public class ProductOrder implements Serializable {

    public String buyer;
    public String address;
    public String email;
    public String shipping;
    public Double shipping_rate;
    public String shipping_location;
    public Long date_ship;
    public String phone;
    public String comment;
    public String status;
    public Double total_fees;
    public Double tax;
    public String serial;
    public Long created_at = System.currentTimeMillis();
    public Long last_update = System.currentTimeMillis();

    public ProductOrder() {
    }

    public void setBuyerProfile(BuyerProfile buyerProfile) {
        this.buyer = buyerProfile.name;
        this.address = buyerProfile.address;
        this.email = buyerProfile.email;
        this.phone = buyerProfile.phone;
    }
}
