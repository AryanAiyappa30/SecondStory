package com.app.secondstory.model;

import java.io.Serializable;

public class Shipping implements Serializable {
    public Long id;
    public String location;
    public String location_id;
    public Double rate_economy;
    public Double rate_regular;
    public Double rate_express;
    public int active_eco;
    public int active_reg;
    public int active_exp;
}
