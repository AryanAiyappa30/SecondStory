package com.app.secondstory.connection.callbacks;

import com.app.secondstory.model.Product;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RespProduct implements Serializable {

    public String status = "";
    public int count = -1;
    public int count_total = -1;
    public int pages = -1;
    public List<Product> products = new ArrayList<>();

}
