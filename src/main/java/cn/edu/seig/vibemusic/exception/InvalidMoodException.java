package cn.edu.seig.vibemusic.exception;
 
/**
 * 无效的情绪ID异常
 */
public class InvalidMoodException extends BlindBoxException {
    
    private final Long moodId;
    
    public InvalidMoodException(Long moodId) {
        super("INVALID_MOOD", 
              String.format("无效的情绪ID：%d", moodId));
        this.moodId = moodId;
    }
    
    public Long getMoodId() {
        return moodId;
    }
}
