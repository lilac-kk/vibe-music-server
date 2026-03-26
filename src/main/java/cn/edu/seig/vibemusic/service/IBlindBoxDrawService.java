package cn.edu.seig.vibemusic.service;

import cn.edu.seig.vibemusic.model.dto.BlindBoxDrawRequest;
import cn.edu.seig.vibemusic.model.vo.BlindBoxResultVO;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
/**
 * 盲盒抽取服务接口
 * 对外提供统一的抽取功能
 */
public interface IBlindBoxDrawService {
    
    /**
     * 抽取盲盒
     * 
     * @param request 抽取请求
     * @return 抽取结果
     */
    BlindBoxResultVO drawBlindBox(BlindBoxDrawRequest request);
    
    /**
     * 获取用户今日剩余抽取次数
     * 
     * @param userId 用户ID
     * @return 剩余抽取次数
     */
    Integer getRemainingDraws(Long userId);
    
    /**
     * 获取用户抽取历史（分页）
     * 
     * @param userId 用户ID
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 抽取历史
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<BlindBoxResultVO> getUserHistory(Long userId, int page, int size);
}
