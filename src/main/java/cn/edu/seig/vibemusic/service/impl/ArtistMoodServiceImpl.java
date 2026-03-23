package cn.edu.seig.vibemusic.service.impl;

import cn.edu.seig.vibemusic.mapper.ArtistMoodMapper;
import cn.edu.seig.vibemusic.model.entity.ArtistMood;
import cn.edu.seig.vibemusic.service.IArtistMoodService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ArtistMoodServiceImpl extends ServiceImpl<ArtistMoodMapper, ArtistMood> implements IArtistMoodService {
}
