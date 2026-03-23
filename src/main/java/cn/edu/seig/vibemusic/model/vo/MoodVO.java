
package cn.edu.seig.vibemusic.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 情绪VO
 */
@Data
public class MoodVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 情绪ID
     */
    private Long id;
    
    /**
     * 情绪名称
     */
    private String name;
    
    /**
     * 情绪描述
     */
    private String description;
    
    /**
     * 情绪图标
     */
    private String icon;
}
