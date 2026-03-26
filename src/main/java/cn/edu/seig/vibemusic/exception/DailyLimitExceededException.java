
package cn.edu.seig.vibemusic.exception;
 
/**
 * 每日抽取次数超限异常
 */
public class DailyLimitExceededException extends BlindBoxException {
    
    private final Integer remainingDraws;
    
    public DailyLimitExceededException(Integer remainingDraws) {
        super("DAILY_LIMIT_EXCEEDED", 
              String.format("今日抽取次数已达上限，剩余次数：%d", remainingDraws));
        this.remainingDraws = remainingDraws;
    }
    
    public Integer getRemainingDraws() {
        return remainingDraws;
    }
}
