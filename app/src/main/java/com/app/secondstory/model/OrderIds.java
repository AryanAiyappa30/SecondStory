package com.app.secondstory.model;

import java.util.ArrayList;
import java.util.List;

public class OrderIds {

    public List<Long> ids = new ArrayList<>();

    public OrderIds(List<Order> orders) {
        ids = new ArrayList<>();
        for (Order order : orders) ids.add(order.id);
    }
}
