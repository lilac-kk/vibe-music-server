package cn.edu.seig.vibemusic.model.entity;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * 歌手情绪关联复合主键类
 * 联合主键：(artist_id, mood_id)
 */
@Data
public class ArtistMoodPK implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    private Long artistId;
    private Long moodId;
}
