package com.app.secondstory.connection.callbacks;

import java.io.Serializable;

public class RespOrder implements Serializable {

    public String status = "";
    public String msg = "";
    public DataResp data = new DataResp();

    public static class DataResp implements Serializable {
        public Long id = -1L;
        public String code = "";
    }

}
