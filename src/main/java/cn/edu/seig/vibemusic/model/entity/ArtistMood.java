package cn.edu.seig.vibemusic.model.entity;

import com.baomidou.mybatisplus.annotation.IdClass;
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
 * 歌手情绪关联实体
 * 联合主键：(artist_id, mood_id)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("tb_artist_mood")
@IdClass(ArtistMoodPK.class)
public class ArtistMood implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "artist_id", type = IdType.INPUT)
    private Long artistId;

    @TableId(value = "mood_id", type = IdType.INPUT)
    private Long moodId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;
}
