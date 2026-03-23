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
 * 歌曲情绪关联实体
 * 联合主键：(song_id, mood_id)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("tb_song_mood")
@IdClass(SongMoodPK.class)
public class SongMood implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "song_id", type = IdType.INPUT)
    private Long songId;

    @TableId(value = "mood_id", type = IdType.INPUT)
    private Long moodId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;
}
