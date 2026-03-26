package cn.edu.seig.vibemusic.exception;
 
import lombok.Getter;
 
/**
 * 盲盒功能基础异常
 */
@Getter
public class BlindBoxException extends RuntimeException {
    
    /**
     * 错误代码
     */
    private final String code;
    
    /**
     * 错误消息
     */
    private final String message;
    
    public BlindBoxException(String message) {
        super(message);
        this.code = "BLIND_BOX_ERROR";
        this.message = message;
    }
    
    public BlindBoxException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    public BlindBoxException(String message, Throwable cause) {
        super(message, cause);
        this.code = "BLIND_BOX_ERROR";
        this.message = message;
    }
}
