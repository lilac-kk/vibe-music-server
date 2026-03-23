package cn.edu.seig.vibemusic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 盲盒抽取记录实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("tb_blind_box_record")
public class BlindBoxRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private long userId;

    @TableField("song_id")
    private long songId;

    @TableField("mood_id")
    private long moodId;

    @TableField("rarity_id")
    private long rarityId;

    @TableField(value = "is_random", el = "false")
    private Boolean isRandom = false;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;
}
