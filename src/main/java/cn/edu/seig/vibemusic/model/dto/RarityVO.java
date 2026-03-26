package cn.edu.seig.vibemusic.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 稀有度VO
 */
@Data
public class RarityVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 稀有度ID
     */
    private Long id;
    
    /**
     * 稀有度名称
     */
    private String name;
    
    /**
     * 稀有度权重
     */
    private Integer weight;
    
    /**
     * 显示颜色
     */
    private String color;
    
    /**
     * 稀有度图标
     */
    private String icon;
}
