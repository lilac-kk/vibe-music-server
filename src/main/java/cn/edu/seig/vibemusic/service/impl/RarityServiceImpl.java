package cn.edu.seig.vibemusic.service.impl;

import cn.edu.seig.vibemusic.mapper.RarityMapper;
import cn.edu.seig.vibemusic.model.entity.Rarity;
import cn.edu.seig.vibemusic.service.IRarityService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class RarityServiceImpl extends ServiceImpl<RarityMapper, Rarity> implements IRarityService {
}
