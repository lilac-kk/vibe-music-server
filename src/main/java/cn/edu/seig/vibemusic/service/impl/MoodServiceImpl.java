package cn.edu.seig.vibemusic.service.impl;

import cn.edu.seig.vibemusic.mapper.MoodMapper;
import cn.edu.seig.vibemusic.model.entity.Mood;
import cn.edu.seig.vibemusic.service.IMoodService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class MoodServiceImpl extends ServiceImpl<MoodMapper, Mood> implements IMoodService {
}
