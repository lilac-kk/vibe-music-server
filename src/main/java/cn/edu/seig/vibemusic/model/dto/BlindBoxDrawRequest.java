package cn.edu.seig.vibemusic.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 盲盒抽取请求DTO
 */
@Data
public class BlindBoxDrawRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 情绪ID（null表示随机抽取）
     */
    private Long moodId;
    
    /**
     * 是否随机抽取
     */
    private Boolean isRandom;
}
