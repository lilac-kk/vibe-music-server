package cn.edu.seig.vibemusic.exception;
 
/**
 * 候选歌曲池为空异常
 */
public class EmptyPoolException extends BlindBoxException {
    
    private final Long moodId;
    
    public EmptyPoolException(Long moodId) {
        super("EMPTY_POOL", 
              moodId != null ? 
              String.format("情绪ID %d 没有可用的候选歌曲", moodId) : 
              "当前没有可用的候选歌曲");
        this.moodId = moodId;
    }
    
    public EmptyPoolException() {
        this(null);
    }
    
    public Long getMoodId() {
        return moodId;
    }
}
