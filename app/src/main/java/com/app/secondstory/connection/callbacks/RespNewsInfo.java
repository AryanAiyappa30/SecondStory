package com.app.secondstory.connection.callbacks;

import com.app.secondstory.model.NewsInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RespNewsInfo implements Serializable {

    public String status = "";
    public int count = -1;
    public int count_total = -1;
    public int pages = -1;
    public List<NewsInfo> news_infos = new ArrayList<>();

}
