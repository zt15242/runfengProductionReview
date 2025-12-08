package other.rainbow.cpzerooneone.pojo;

public class ReturnResult {
    // 表示操作是否成功
    private Boolean isSuccess;
    // 返回的消息
    private String message;

    // 构造函数
    public ReturnResult() {

    }

    // Getter 和 Setter 方法
    public Boolean getIsSuccess() {
        return isSuccess;
    }

    public void setIsSuccess(Boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
