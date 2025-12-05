package other.rainbow.orderapp.pojo;

public class orderMsg {
    private String msg;
    private String status;
    public orderMsg(){

    }
    public orderMsg(String msg,String status){
        this.msg = msg;
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
