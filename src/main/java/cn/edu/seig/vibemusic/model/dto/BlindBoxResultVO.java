package cn.edu.seig.vibemusic.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 盲盒抽取结果VO
 */
@Data
public class BlindBoxResultVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 抽取的歌曲信息
     */
    private SongVO song;
    
    /**
     * 情绪信息
     */
    private MoodVO mood;
    
    /**
     * 稀有度信息
     */
    private RarityVO rarity;
    
    /**
     * 抽取时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime drawTime;
    
    /**
     * 是否随机抽取
     */
    private Boolean isRandom;
    
    /**
     * 剩余抽取次数
     */
    private Integer remainingDraws;
}
