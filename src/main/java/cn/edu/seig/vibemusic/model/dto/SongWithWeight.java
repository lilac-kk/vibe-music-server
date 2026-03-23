package cn.edu.seig.vibemusic.model.dto;

import cn.edu.seig.vibemusic.model.entity.Song;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 带权重的歌曲DTO
 * 用于候选池构建和加权随机选择
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongWithWeight implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 歌曲信息
     */
    private Song song;
    
    /**
     * 权重（用于加权随机）
     * 基础权重为1.0，可根据用户偏好、稀有度等因素调整
     */
    private Double weight;
    
    /**
     * 关联的情绪ID
     */
    private Long moodId;
    
    /**
     * 是否通过歌手关联获取的补充歌曲
     */
    private Boolean isFromArtist;
    
    /**
     * 构造基础权重歌曲
     */
    public static SongWithWeight of(Song song, Long moodId) {
        return new SongWithWeight(song, 1.0, moodId, false);
    }
    
    /**
     * 构造歌手补充歌曲
     */
    public static SongWithWeight ofArtist(Song song, Long moodId) {
        return new SongWithWeight(song, 0.8, moodId, true);
    }
    
    /**
     * 增加权重倍数
     */
    public void multiplyWeight(double multiplier) {
        this.weight = this.weight * multiplier;
    }
    
    /**
     * 获取歌曲ID（使用songId字段）
     */
    public Long getSongId() {
        return song.getSongId();
    }
}
