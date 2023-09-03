package com.app.secondstory.connection.callbacks;

import com.app.secondstory.model.Category;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RespCategory implements Serializable {

    public String status = "";
    public List<Category> categories = new ArrayList<>();

}
