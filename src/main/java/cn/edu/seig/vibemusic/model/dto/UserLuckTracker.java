package cn.edu.seig.vibemusic.service.tracker;
 
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
 
import javax.annotation.Resource;
import java.util.*;  // ✅ 修正：添加集合类导入
import java.util.concurrent.TimeUnit;
 
/**
 * 用户幸运度追踪器
 * 核心功能：追踪用户最近的抽取记录，实现防沉迷机制
 */
@Slf4j
@Component
public class UserLuckTracker {
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    
    // 配置参数
    private static final String LUCK_PREFIX = "blindbox:luck:";
    private static final int HISTORY_SIZE = 10;              // 记录最近10次抽取
    private static final int SAME_RARITY_THRESHOLD = 5;      // 连续5次相同稀有度触发防沉迷
    private static final int BOOST_DURATION = 30;           // 提升效果持续30分钟
    private static final double BOOST_FACTOR = 1.5;         // 高稀有度权重提升50%
    
    /**
     * 记录用户的抽取稀有度
     * 
     * @param userId 用户ID
     * @param rarityId 抽取到的稀有度ID
     */
    public void recordDraw(Long userId, Long rarityId) {
        String key = LUCK_PREFIX + userId;
        
        // 从左侧添加新的稀有度记录
        redisTemplate.opsForList().leftPush(key, rarityId);
        
        // 只保留最近N次记录
        redisTemplate.opsForList().trim(key, 0, HISTORY_SIZE - 1);
        
        // 设置过期时间（24小时）
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
        
        log.debug("用户 {} 抽取记录已更新 - 稀有度: {}", userId, rarityId);
    }
    
    /**
     * 获取用户最近的抽取稀有度列表
     * 
     * @param userId 用户ID
     * @return 最近的稀有度ID列表（按时间倒序）
     */
    public List<Long> getRecentDraws(Long userId) {
        String key = LUCK_PREFIX + userId;
        
        List<Object> objects = redisTemplate.opsForList().range(key, 0, -1);
        
        if (objects == null || objects.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Long> rarityIds = new ArrayList<>();
        for (Object obj : objects) {
            if (obj != null) {
                rarityIds.add(Long.valueOf(obj.toString()));
            }
        }
        
        return rarityIds;
    }
    
    /**
     * 检查用户是否需要触发防沉迷机制
     * 
     * @param userId 用户ID
     * @return 需要提升的稀有度ID，如果不需要则返回null
     */
    public Long checkAntiAddiction(Long userId) {
        List<Long> recentDraws = getRecentDraws(userId);
        
        if (recentDraws.size() < SAME_RARITY_THRESHOLD) {
            return null;
        }
        
        // 检查最近N次是否都是同一稀有度
        Long lastRarity = recentDraws.get(0);
        boolean allSame = true;
        for (int i = 1; i < SAME_RARITY_THRESHOLD; i++) {
            if (!lastRarity.equals(recentDraws.get(i))) {
                allSame = false;
                break;
            }
        }
        
        if (allSame) {
            // 触发防沉迷，提升到高一级稀有度
            Long boostedRarityId = lastRarity + 1;
            log.info("触发防沉迷机制 - 用户: {}, 连续{}次稀有度: {}, 提升到: {}", 
                    userId, SAME_RARITY_THRESHOLD, lastRarity, boostedRarityId);
            return boostedRarityId;
        }
        
        return null;
    }
    
    /**
     * 获取高稀有度的权重提升倍数
     * 如果用户连续抽到低稀有度，返回提升倍数
     * 
     * @param userId 用户ID
     * @param rarityId 要查询的稀有度ID
     * @return 权重提升倍数（1.0表示无提升）
     */
    public double getWeightBoost(Long userId, Long rarityId) {
        List<Long> recentDraws = getRecentDraws(userId);
        
        if (recentDraws.size() < 3) {
            return 1.0;
        }
        
        // 统计最近N次各稀有度的出现次数
        Map<Long, Integer> rarityCount = new HashMap<>();  // ✅ 已有 HashMap，但需确保导入
        for (Long rId : recentDraws) {
            rarityCount.put(rId, rarityCount.getOrDefault(rId, 0) + 1);
        }
        
        // 如果该稀有度出现次数过多，降低其权重
        Integer count = rarityCount.get(rarityId);
        if (count != null && count >= SAME_RARITY_THRESHOLD) {
            // 降低该稀有度的权重
            return 1.0 / BOOST_FACTOR;
        }
        
        return 1.0;
    }
    
    /**
     * 清除用户的抽取记录
     * 用于测试或重置
     */
    public void clearHistory(Long userId) {
        String key = LUCK_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("用户 {} 的抽取记录已清除", userId);
    }
}
