package cn.edu.seig.vibemusic.service.impl;

import cn.edu.seig.vibemusic.mapper.UserMoodPreferenceMapper;
import cn.edu.seig.vibemusic.model.entity.UserMoodPreference;
import cn.edu.seig.vibemusic.service.IUserMoodPreferenceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class UserMoodPreferenceServiceImpl extends ServiceImpl<UserMoodPreferenceMapper, UserMoodPreference> implements IUserMoodPreferenceService {
}
