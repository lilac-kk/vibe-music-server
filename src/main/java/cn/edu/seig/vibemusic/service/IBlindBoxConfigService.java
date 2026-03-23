package cn.edu.seig.vibemusic.service;

import cn.edu.seig.vibemusic.model.entity.BlindBoxConfig;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 盲盒配置服务接口（添加便捷方法）
 */
public interface IBlindBoxConfigService extends IService<BlindBoxConfig> {
    
    /**
     * 获取当前启用的配置
     * @return 当前配置，如果没有启用配置则返回null
     */
    BlindBoxConfig getCurrentConfig();
}
