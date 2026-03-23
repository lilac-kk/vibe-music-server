package cn.edu.seig.vibemusic.mapper;
 
import cn.edu.seig.vibemusic.model.entity.SongMood;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
 
/**
 * 歌曲情绪关联Mapper
 */
@Mapper
public interface SongMoodMapper extends BaseMapper<SongMood> {
}
