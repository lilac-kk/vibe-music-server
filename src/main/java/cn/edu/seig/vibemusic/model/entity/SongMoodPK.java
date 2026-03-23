package cn.edu.seig.vibemusic.model.entity;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * 歌曲情绪关联复合主键类
 * 联合主键：(song_id, mood_id)
 */
@Data
public class SongMoodPK implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    private Long songId;
    private Long moodId;
}
