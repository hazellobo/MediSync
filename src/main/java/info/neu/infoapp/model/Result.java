package info.neu.infoapp.model;

import lombok.Data;

@Data
public class Result {
    private String message;
    private int status;
    private Object result;

    public Result(String message, int status, Object result) {
        this.message = message;
        this.status = status;
        this.result = result;
    }

    @Override
    public String toString() {
        return "Response{" +
                " message :'" + message + '\'' +
                ", status :" + status +
                ", result :" + result +
                '}';
    }
}
