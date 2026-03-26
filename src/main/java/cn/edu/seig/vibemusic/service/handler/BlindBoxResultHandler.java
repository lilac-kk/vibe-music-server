package cn.edu.seig.vibemusic.service.handler;

import cn.edu.seig.vibemusic.model.dto.BlindBoxDrawRequest;
import cn.edu.seig.vibemusic.model.entity.*;
import cn.edu.seig.vibemusic.model.vo.BlindBoxResultVO;
import cn.edu.seig.vibemusic.model.vo.MoodVO;
import cn.edu.seig.vibemusic.model.vo.RarityVO;
import cn.edu.seig.vibemusic.model.vo.SongVO;
import cn.edu.seig.vibemusic.service.*;
import cn.edu.seig.vibemusic.service.validator.BlindBoxValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Optional;

/**
 * 抽取结果处理器
 * 核心功能：处理抽取后的各种业务操作
 */
@Slf4j
@Component
public class BlindBoxResultHandler {
    
    @Autowired
    private IBlindBoxRecordService blindBoxRecordService;
    
    @Autowired
    private IUserMoodPreferenceService userMoodPreferenceService;
    
    @Autowired
    private BlindBoxValidator blindBoxValidator;
    
    @Autowired
    private IMoodService moodService;
    
    @Autowired
    private IRarityService rarityService;
    
    @Autowired
    private ISongService songService;
    
    /**
     * 保存抽取记录
     * 
     * @param request 抽取请求
     * @param selectedSong 选中的歌曲
     * @param moodId 选择的情绪ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveRecord(BlindBoxDrawRequest request, Song selectedSong, Long moodId) {
        BlindBoxRecord record = new BlindBoxRecord();
        record.setUserId(request.getUserId());
        record.setSongId(selectedSong.getSongId());
        record.setMoodId(moodId);
        record.setRarityId(selectedSong.getRarityId());
        record.setIsRandom(request.getIsRandom());
        
        blindBoxRecordService.save(record);
        
        // 增加用户今日抽取次数
        blindBoxValidator.incrementUserDailyCount(request.getUserId());
        
        log.info("抽取记录已保存 - 用户: {}, 歌曲: {}, 稀有度: {}", 
                request.getUserId(), selectedSong.getSongName(), selectedSong.getRarityId());
    }
    
    /**
     * 更新用户情绪偏好
     * 基于抽取结果动态调整用户对情绪的偏好优先级
     * 
     * @param userId 用户ID
     * @param moodId 选择的情绪ID
     * @param isRandom 是否随机抽取
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserMoodPreference(Long userId, Long moodId, Boolean isRandom) {
        // 随机模式下，不更新偏好
        if (isRandom != null && isRandom) {
            log.debug("随机抽取模式，不更新用户情绪偏好");
            return;
        }
        
        // 查询或创建用户情绪偏好
        LambdaQueryWrapper<UserMoodPreference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserMoodPreference::getUserId, userId)
               .eq(UserMoodPreference::getMoodId, moodId);
        
        UserMoodPreference preference = userMoodPreferenceService.getOne(wrapper);
        
        if (preference == null) {
            // 首次选择该情绪，创建新记录
            preference = new UserMoodPreference();
            preference.setUserId(userId);
            preference.setMoodId(moodId);
            preference.setPriority(5);  // 默认优先级
            userMoodPreferenceService.save(preference);
            log.info("创建用户情绪偏好 - 用户: {}, 情绪: {}", userId, moodId);
        } else {
            // 已存在，使用指数加权移动平均（EWMA）算法动态调整优先级
            int currentPriority = preference.getPriority() != null ? preference.getPriority() : 5;
            int newPriority = calculateEWMA(currentPriority);
            preference.setPriority(newPriority);
            userMoodPreferenceService.updateById(preference);
            log.info("更新用户情绪偏好 - 用户: {}, 情绪: {}, 优先级: {} -> {}", 
                    userId, moodId, currentPriority, newPriority);
        }
    }
    
    /**
     * 使用指数加权移动平均（EWMA）算法计算新的优先级
     * 公式：newPriority = alpha * currentPriority + (1 - alpha) * basePriority
     * 
     * @param currentPriority 当前优先级
     * @return 新的优先级
     */
    private int calculateEWMA(int currentPriority) {
        final double alpha = 0.8;  // 平滑因子，越大越重视历史数据
        final int basePriority = 10;  // 基础优先级
        
        double newPriority = alpha * currentPriority + (1 - alpha) * basePriority;
        
        // 限制在1-10之间
        return (int) Math.max(1, Math.min(10, newPriority));
    }
    
    /**
     * 异步触发统计分析
     * 用于后台统计用户抽取行为、稀有度分布等
     * 
     * @param userId 用户ID
     */
    @Async
    public void triggerStatisticsAnalysis(Long userId) {
        // 这里可以添加各种统计分析逻辑
        // 例如：更新用户画像、计算幸运值、记录行为特征等
        
        log.debug("异步统计分析已触发 - 用户: {}", userId);
        
        // 示例：统计用户抽取的稀有度分布
        // 可以定期生成报表，用于运营分析
    }
    
    /**
     * 构建抽取结果VO
     * 
     * @param selectedSong 选中的歌曲
     * @param moodId 选择的情绪ID
     * @param request 抽取请求
     * @param remainingDraws 剩余抽取次数
     * @return 抽取结果VO
     */
    public BlindBoxResultVO buildResultVO(Song selectedSong, Long moodId, 
                                           BlindBoxDrawRequest request, Integer remainingDraws) {
        BlindBoxResultVO resultVO = new BlindBoxResultVO();
        
        // 转换歌曲信息
        SongVO songVO = convertToSongVO(selectedSong);
        resultVO.setSong(songVO);
        
        // 转换情绪信息
        Mood mood = moodService.getById(moodId);
        if (mood != null) {
            MoodVO moodVO = convertToMoodVO(mood);
            resultVO.setMood(moodVO);
        }
        
        // 转换稀有度信息
        Rarity rarity = rarityService.getById(selectedSong.getRarityId());
        if (rarity != null) {
            RarityVO rarityVO = convertToRarityVO(rarity);
            resultVO.setRarity(rarityVO);
        }
        
        // 设置其他信息
        resultVO.setDrawTime(java.time.LocalDateTime.now());
        resultVO.setIsRandom(request.getIsRandom());
        resultVO.setRemainingDraws(remainingDraws);
        
        log.info("抽取结果VO构建完成 - 用户: {}, 歌曲: {}, 稀有度: {}", 
                request.getUserId(), selectedSong.getSongName(), 
                rarity != null ? rarity.getName() : "未知");
        
        return resultVO;
    }
    
    /**
     * 转换为SongVO
     */
    private SongVO convertToSongVO(Song song) {
        SongVO songVO = new SongVO();
        	  songVO.setSongId(song.getSongId());
      	  songVO.setSongName(song.getSongName());
        songVO.setId(song.getSongId());           // 修正
         songVO.setName(song.getSongName());       // 修正
        songVO.setAlbum(song.getAlbum());
        songVO.setDuration(song.getDuration());
        songVO.setCoverUrl(song.getCoverUrl());
        songVO.setAudioUrl(song.getAudioUrl());
        songVO.setReleaseTime(song.getReleaseTime());
        
        // 查询歌手名称（可选）
        // Artist artist = artistService.getById(song.getArtistId());
        // if (artist != null) {
        //     songVO.setArtistName(artist.getArtistName());
        // }
        
        return songVO;
    }
    
    /**
     * 转换为MoodVO
     */
    private MoodVO convertToMoodVO(Mood mood) {
        MoodVO moodVO = new MoodVO();
        BeanUtils.copyProperties(mood, moodVO);
        return moodVO;
    }
    
    /**
     * 转换为RarityVO
     */
    private RarityVO convertToRarityVO(Rarity rarity) {
        RarityVO rarityVO = new RarityVO();
        BeanUtils.copyProperties(rarity, rarityVO);
        return rarityVO;
    }
}
