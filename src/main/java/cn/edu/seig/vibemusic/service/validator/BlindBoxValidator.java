package cn.edu.seig.vibemusic.service.validator;
 
import cn.edu.seig.vibemusic.exception.*;
import cn.edu.seig.vibemusic.model.entity.BlindBoxConfig;
import cn.edu.seig.vibemusic.model.entity.BlindBoxRecord;
import cn.edu.seig.vibemusic.model.entity.Mood;
import cn.edu.seig.vibemusic.model.entity.Song;
import cn.edu.seig.vibemusic.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; 
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
 
/**
 * 盲盒业务规则校验器
 * 核心功能：检查各种业务规则，确保抽取操作符合要求
 */
@Slf4j
@Component
public class BlindBoxValidator {
    
    @Autowired
    private IBlindBoxConfigService blindBoxConfigService;
    
    @Autowired
    private IMoodService moodService;
    
    @Autowired
    private IBlindBoxRecordService blindBoxRecordService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Redis Key 前缀
    private static final String USER_DAILY_COUNT_PREFIX = "blindbox:daily:";
    private static final String DAILY_LIMIT_CACHE_PREFIX = "blindbox:config:";
    
    /**
     * 检查盲盒功能是否启用
     * 
     * @throws BlindBoxDisabledException 功能未启用时抛出
     */
    public void checkFeatureEnabled() {
        BlindBoxConfig config = getCurrentConfig();
        
        if (config == null || !config.getIsEnabled()) {
            log.warn("盲盒功能未启用");
            throw new BlindBoxDisabledException();
        }
        
        log.debug("盲盒功能已启用 - 每日限额: {}", config.getDailyLimit());
    }
    
    /**
     * 检查用户今日抽取次数是否超限
     * 
     * @param userId 用户ID
     * @throws DailyLimitExceededException 超限时抛出
     */
    public void checkDailyLimit(Long userId) {
        BlindBoxConfig config = getCurrentConfig();
        
        if (config == null) {
            log.warn("盲盒配置不存在");
            throw new BlindBoxDisabledException();
        }
        
        int dailyLimit = config.getDailyLimit();
        int currentCount = getUserTodayDrawCount(userId);
        
        if (currentCount >= dailyLimit) {
            log.warn("用户 {} 今日抽取次数已达上限: {}/{}", userId, currentCount, dailyLimit);
            throw new DailyLimitExceededException(0);
        }
        
        int remainingDraws = dailyLimit - currentCount;
        log.debug("用户 {} 今日抽取次数: {}/{}, 剩余: {}", userId, currentCount, dailyLimit, remainingDraws);
    }
    
    /**
     * 检查情绪ID是否有效
     * 
     * @param moodId 情绪ID（null表示随机，不校验）
     * @throws InvalidMoodException 无效时抛出
     */
    public void validateMoodId(Long moodId) {
        // 随机模式不校验情绪ID
        if (moodId == null) {
            log.debug("随机模式，不校验情绪ID");
            return;
        }
        
        Mood mood = moodService.getById(moodId);
        
        if (mood == null) {
            log.warn("情绪ID {} 不存在", moodId);
            throw new InvalidMoodException(moodId);
        }
        
        log.debug("情绪ID {} 有效 - 名称: {}", moodId, mood.getName());
    }
    
    /**
     * 检查候选歌曲池是否为空
     * 
     * @param candidates 候选歌曲列表
     * @param moodId 情绪ID（null表示随机）
     * @throws EmptyPoolException 为空时抛出
     */
    public void validateCandidatePool(List<Song> candidates, Long moodId) {
        if (candidates == null || candidates.isEmpty()) {
            log.warn("候选歌曲池为空 - moodId: {}", moodId);
            throw new EmptyPoolException(moodId);
        }
        
        log.debug("候选歌曲池校验通过 - 歌曲数: {}", candidates.size());
    }
    
    /**
     * 获取当前启用的配置
     * 
     * @return 当前配置，如果没有启用配置则返回null
     */
    private BlindBoxConfig getCurrentConfig() {
        // 先尝试从缓存获取
        String cacheKey = DAILY_LIMIT_CACHE_PREFIX + "current";
        BlindBoxConfig cachedConfig = (BlindBoxConfig) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedConfig != null) {
            return cachedConfig;
        }
        
        // 缓存未命中，从数据库获取
        BlindBoxConfig config = blindBoxConfigService.getCurrentConfig();
        
        if (config != null) {
            // 缓存5分钟
            redisTemplate.opsForValue().set(cacheKey, config, 5, TimeUnit.MINUTES);
        }
        
        return config;
    }
    
    /**
     * 获取用户今日的抽取次数
     * 
     * @param userId 用户ID
     * @return 今日抽取次数
     */
    public int getUserTodayDrawCount(Long userId) {
        String key = buildDailyCountKey(userId);
        
        // 从Redis获取今日计数
        Object countObj = redisTemplate.opsForValue().get(key);
        if (countObj != null) {
            return Integer.parseInt(countObj.toString());
        }
        
        // Redis未命中，从数据库查询
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        
        LambdaQueryWrapper<BlindBoxRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlindBoxRecord::getUserId, userId)
               .between(BlindBoxRecord::getCreateTime, todayStart, todayEnd);
        
        long count = blindBoxRecordService.count(wrapper);
        
        // 缓存到Redis，过期时间到明天凌晨
        long ttl = LocalDateTime.now().until(LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIN), 
                                         java.time.temporal.ChronoUnit.SECONDS);
        redisTemplate.opsForValue().set(key, count, ttl, TimeUnit.SECONDS);
        
        return (int) count;
    }
    
    /**
     * 获取用户今日剩余抽取次数
     * 
     * @param userId 用户ID
     * @return 剩余抽取次数
     */
    public int getRemainingDraws(Long userId) {
        BlindBoxConfig config = getCurrentConfig();
        
        if (config == null || !config.getIsEnabled()) {
            return 0;
        }
        
        int dailyLimit = config.getDailyLimit();
        int currentCount = getUserTodayDrawCount(userId);
        int remaining = Math.max(0, dailyLimit - currentCount);
        
        return remaining;
    }
    
    /**
     * 增加用户今日的抽取次数
     * 
     * @param userId 用户ID
     */
    public void incrementUserDailyCount(Long userId) {
        String key = buildDailyCountKey(userId);
        
        // Redis计数加1
        redisTemplate.opsForValue().increment(key);
        
        // 设置过期时间到明天凌晨（如果key是新建的）
        redisTemplate.expire(key, getSecondsUntilTomorrow(), TimeUnit.SECONDS);
        
        log.debug("用户 {} 今日抽取次数已增加", userId);
    }
    
    /**
     * 构建用户每日计数的Redis Key
     */
    private String buildDailyCountKey(Long userId) {
        return USER_DAILY_COUNT_PREFIX + userId + ":" + LocalDate.now().toString();
    }
    
    /**
     * 获取距离明天凌晨的秒数
     */
    private long getSecondsUntilTomorrow() {
        LocalDateTime tomorrow = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIN);
        return java.time.Duration.between(LocalDateTime.now(), tomorrow).getSeconds();
    }
}
