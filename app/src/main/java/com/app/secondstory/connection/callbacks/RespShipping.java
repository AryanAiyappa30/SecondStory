package com.app.secondstory.connection.callbacks;

import com.app.secondstory.model.Shipping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RespShipping implements Serializable {

    public String status = "";
    public List<Shipping> shipping = new ArrayList<>();

}
