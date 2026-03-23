package cn.edu.seig.vibemusic.service.impl;

import cn.edu.seig.vibemusic.mapper.BlindBoxConfigMapper;
import cn.edu.seig.vibemusic.model.entity.BlindBoxConfig;
import cn.edu.seig.vibemusic.service.IBlindBoxConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 盲盒配置服务实现（添加便捷方法实现）
 */
@Service
public class BlindBoxConfigServiceImpl extends ServiceImpl<BlindBoxConfigMapper, BlindBoxConfig> implements IBlindBoxConfigService {

    @Override
    public BlindBoxConfig getCurrentConfig() {
        return lambdaQuery()
                .eq(BlindBoxConfig::getIsEnabled, true)
                .one();
    }
}
