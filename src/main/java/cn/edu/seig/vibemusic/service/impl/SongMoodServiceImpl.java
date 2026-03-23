package cn.edu.seig.vibemusic.service.impl;

import cn.edu.seig.vibemusic.mapper.SongMoodMapper;
import cn.edu.seig.vibemusic.model.entity.SongMood;
import cn.edu.seig.vibemusic.service.ISongMoodService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SongMoodServiceImpl extends ServiceImpl<SongMoodMapper, SongMood> implements ISongMoodService {
}
