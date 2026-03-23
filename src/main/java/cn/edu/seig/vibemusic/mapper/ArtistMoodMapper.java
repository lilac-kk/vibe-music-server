package cn.edu.seig.vibemusic.mapper;
 
import cn.edu.seig.vibemusic.model.entity.ArtistMood;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
 
/**
 * 歌手情绪关联Mapper
 */
@Mapper
public interface ArtistMoodMapper extends BaseMapper<ArtistMood> {
}
