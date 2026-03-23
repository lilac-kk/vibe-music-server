package cn.edu.seig.vibemusic.mapper;
 
import cn.edu.seig.vibemusic.model.entity.Mood;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
 
/**
 * 情绪标签Mapper
 */
@Mapper
public interface MoodMapper extends BaseMapper<Mood> {
}
