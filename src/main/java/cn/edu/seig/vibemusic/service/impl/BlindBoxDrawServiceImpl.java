package cn.edu.seig.vibemusic.service.impl;
 
import cn.edu.seig.vibemusic.constant.BlindBoxMessageConstant;
import cn.edu.seig.vibemusic.exception.BlindBoxException;
import cn.edu.seig.vibemusic.model.dto.BlindBoxDrawRequest;
import cn.edu.seig.vibemusic.model.dto.SongWithWeight;
import cn.edu.seig.vibemusic.model.entity.BlindBoxRecord;
import cn.edu.seig.vibemusic.model.entity.Mood;
import cn.edu.seig.vibemusic.model.entity.Rarity;
import cn.edu.seig.vibemusic.model.entity.Song;
import cn.edu.seig.vibemusic.model.vo.BlindBoxResultVO;
import cn.edu.seig.vibemusic.model.vo.MoodVO;
import cn.edu.seig.vibemusic.model.vo.RarityVO;
import cn.edu.seig.vibemusic.model.vo.SongVO;
import cn.edu.seig.vibemusic.service.*;
import cn.edu.seig.vibemusic.service.builder.CandidatePoolBuilder;
import cn.edu.seig.vibemusic.service.handler.BlindBoxResultHandler;
import cn.edu.seig.vibemusic.service.selector.RarityWeightedSelector;
import cn.edu.seig.vibemusic.service.validator.BlindBoxValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.util.List;
import java.util.stream.Collectors;
 
/**
 * 盲盒抽取服务实现
 * 整合所有模块，对外提供统一的抽取接口
 */
@Slf4j
@Service
public class BlindBoxDrawServiceImpl implements IBlindBoxDrawService {
    
    @Autowired
    private BlindBoxValidator blindBoxValidator;
    
    @Autowired
    private CandidatePoolBuilder candidatePoolBuilder;
    
    @Autowired
    private RarityWeightedSelector rarityWeightedSelector;
    
    @Autowired
    private BlindBoxResultHandler blindBoxResultHandler;
    
    @Autowired
    private IBlindBoxRecordService blindBoxRecordService;
    
    @Autowired
    private IRarityService rarityService;
    
    // ✅ 修正：补充注入缺失的 Service
    @Autowired
    private ISongService songService;
    
    @Autowired
    private IMoodService moodService;
    
    /**
     * 抽取盲盒（核心接口）
     * 完整流程：
     * 1. 业务规则校验
     * 2. 构建候选歌曲池
     * 3. 加权随机选择歌曲
     * 4. 保存抽取记录
     * 5. 更新用户偏好
     * 6. 返回抽取结果
     * 
     * @param request 抽取请求
     * @return 抽取结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public BlindBoxResultVO drawBlindBox(BlindBoxDrawRequest request) {
        log.info("========== 开始盲盒抽取 ==========");
        log.info("用户ID: {}, 情绪ID: {}, 是否随机: {}", 
                request.getUserId(), request.getMoodId(), request.getIsRandom());
        
        Long userId = request.getUserId();
        Long moodId = request.getMoodId();
        Boolean isRandom = request.getIsRandom();
        
        // ==================== 步骤1：业务规则校验 ====================
        log.info("步骤1：开始业务规则校验");
        blindBoxValidator.checkFeatureEnabled();
        blindBoxValidator.checkDailyLimit(userId);
        blindBoxValidator.validateMoodId(moodId);
        log.info("步骤1：业务规则校验通过");
        
        // ==================== 步骤2：构建候选歌曲池 ====================
        log.info("步骤2：开始构建候选歌曲池");
        List<SongWithWeight> candidates = candidatePoolBuilder.buildPool(moodId, userId, isRandom);
        
        // 提取纯歌曲列表用于校验
        List<Song> candidateSongs = candidates.stream()
                .map(SongWithWeight::getSong)
                .collect(Collectors.toList());
        blindBoxValidator.validateCandidatePool(candidateSongs, moodId);
        log.info("步骤2：候选歌曲池构建完成 - 候选数: {}", candidates.size());
        
        // ==================== 步骤3：加权随机选择歌曲 ====================
        log.info("步骤3：开始加权随机选择");
        Song selectedSong = rarityWeightedSelector.selectByWeight(candidates, userId);
        
        if (selectedSong == null) {
            log.error("加权随机选择失败");
            throw new BlindBoxException("选择歌曲失败，请稍后重试");
        }
        
        log.info("步骤3：加权随机选择完成 - 歌曲: {}, 稀有度: {}", 
                selectedSong.getSongName(), selectedSong.getRarityId());
        
        // ==================== 步骤4：保存抽取记录 ====================
        log.info("步骤4：开始保存抽取记录");
        blindBoxResultHandler.saveRecord(request, selectedSong, moodId);
        log.info("步骤4：抽取记录已保存");
        
        // ==================== 步骤5：更新用户偏好 ====================
        log.info("步骤5：开始更新用户偏好");
        blindBoxResultHandler.updateUserMoodPreference(userId, moodId, isRandom);
        log.info("步骤5：用户偏好已更新");
        
        // ==================== 步骤6：触发异步分析 ====================
        log.info("步骤6：触发异步统计分析");
        blindBoxResultHandler.triggerStatisticsAnalysis(userId);
        
        // ==================== 步骤7：构建并返回结果 ====================
        log.info("步骤7：构建抽取结果VO");
        Integer remainingDraws = blindBoxValidator.getRemainingDraws(userId);
        BlindBoxResultVO resultVO = blindBoxResultHandler.buildResultVO(
                selectedSong, moodId, request, remainingDraws);
        
        log.info("========== 盲盒抽取完成 ==========");
        return resultVO;
    }
    
    /**
     * 获取用户今日剩余抽取次数
     * 
     * @param userId 用户ID
     * @return 剩余抽取次数
     */
    @Override
    public Integer getRemainingDraws(Long userId) {
        log.debug("查询用户 {} 剩余抽取次数", userId);
        
        // 先检查功能是否启用
        try {
            blindBoxValidator.checkFeatureEnabled();
        } catch (Exception e) {
            // 功能未启用，返回0
            return 0;
        }
        
        int remainingDraws = blindBoxValidator.getRemainingDraws(userId);
        log.debug("用户 {} 剩余抽取次数: {}", userId, remainingDraws);
        
        return remainingDraws;
    }
    
    /**
     * 获取用户抽取历史（分页）
     * 
     * @param userId 用户ID
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 抽取历史（分页）
     */
    @Override
    public Page<BlindBoxResultVO> getUserHistory(Long userId, int page, int size) {
        log.debug("查询用户 {} 抽取历史 - 页码: {}, 每页: {}", userId, page, size);
        
        // 创建分页对象
        Page<BlindBoxRecord> pageParam = new Page<>(page, size);
        
        // 查询抽取记录（按时间倒序）
        LambdaQueryWrapper<BlindBoxRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlindBoxRecord::getUserId, userId)
               .orderByDesc(BlindBoxRecord::getCreateTime);
        
        Page<BlindBoxRecord> recordPage = blindBoxRecordService.page(pageParam, wrapper);
        
        // 转换为VO
        Page<BlindBoxResultVO> resultPage = new Page<>(page, size);
        resultPage.setTotal(recordPage.getTotal());
        resultPage.setCurrent(recordPage.getCurrent());
        resultPage.setSize(recordPage.getSize());
        resultPage.setPages(recordPage.getPages());
        
        List<BlindBoxResultVO> records = recordPage.getRecords().stream()
                .map(record -> convertToResultVO(record))
                .collect(Collectors.toList());
        
        resultPage.setRecords(records);
        
        log.debug("查询完成 - 总记录数: {}", recordPage.getTotal());
        
        return resultPage;
    }
    
    /**
     * 将抽取记录转换为结果VO
     * ✅ 修正：完整实现转换方法
     */
    private BlindBoxResultVO convertToResultVO(BlindBoxRecord record) {
        BlindBoxResultVO resultVO = new BlindBoxResultVO();
        
        // 设置基础信息
        resultVO.setDrawTime(record.getCreateTime());
        resultVO.setIsRandom(record.getIsRandom());
        resultVO.setRemainingDraws(blindBoxValidator.getRemainingDraws(record.getUserId()));
        
        // 查询并设置歌曲信息
        Song song = songService.getById(record.getSongId());
        if (song != null) {
            resultVO.setSong(convertToSongVO(song));
        }
        
        // 查询并设置情绪信息
        Mood mood = moodService.getById(record.getMoodId());
        if (mood != null) {
            resultVO.setMood(convertToMoodVO(mood));
        }
        
        // 查询并设置稀有度信息
        Rarity rarity = rarityService.getById(record.getRarityId());
        if (rarity != null) {
            resultVO.setRarity(convertToRarityVO(rarity));
        }
        
        return resultVO;
    }
    
    /**
     * 转换为SongVO
     */
    private SongVO convertToSongVO(Song song) {
        SongVO songVO = new SongVO();
        songVO.setSongId(song.getSongId());
        songVO.setSongName(song.getSongName());
        songVO.setAlbum(song.getAlbum());
        songVO.setDuration(song.getDuration());
        songVO.setCoverUrl(song.getCoverUrl());
        songVO.setAudioUrl(song.getAudioUrl());
        songVO.setReleaseTime(song.getReleaseTime());
        return songVO;
    }
    
    /**
     * 转换为MoodVO
     */
    private MoodVO convertToMoodVO(Mood mood) {
        MoodVO moodVO = new MoodVO();
        moodVO.setId(mood.getId());
        moodVO.setName(mood.getName());
        moodVO.setIcon(mood.getIcon());
        return moodVO;
    }
    
    /**
     * 转换为RarityVO
     */
    private RarityVO convertToRarityVO(Rarity rarity) {
        RarityVO rarityVO = new RarityVO();
        rarityVO.setId(rarity.getId());
        rarityVO.setName(rarity.getName());
        rarityVO.setColor(rarity.getColor());
        rarityVO.setIcon(rarity.getIcon());
        return rarityVO;
    }
}
