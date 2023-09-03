package com.app.secondstory.connection.callbacks;

import com.app.secondstory.model.Order;

import java.io.Serializable;

public class RespOrderHistory implements Serializable {

    public String status = "";
    public Order product_order = new Order();

}
