package cn.edu.seig.vibemusic.mapper;
 
import cn.edu.seig.vibemusic.model.entity.UserMoodPreference;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
 
/**
 * 用户情绪偏好Mapper
 */
@Mapper
public interface UserMoodPreferenceMapper extends BaseMapper<UserMoodPreference> {
}
