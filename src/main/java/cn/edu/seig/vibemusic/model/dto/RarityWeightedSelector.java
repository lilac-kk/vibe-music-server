package cn.edu.seig.vibemusic.service.selector;
 
import cn.edu.seig.vibemusic.model.dto.RarityGroup;
import cn.edu.seig.vibemusic.model.dto.SongWithWeight;
import cn.edu.seig.vibemusic.model.entity.Rarity;
import cn.edu.seig.vibemusic.model.entity.Song;
import cn.edu.seig.vibemusic.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
 
import java.util.*;  // ✅ 修正：添加集合类导入
import java.util.stream.Collectors;
 
/**
 * 稀有度权重选择器
 * 核心功能：根据稀有度权重，从候选池中加权随机选择一首歌
 */
@Slf4j
@Component
public class RarityWeightedSelector {
    
    @Autowired
    private IRarityService rarityService;
    
    @Autowired
    private cn.edu.seig.vibemusic.service.tracker.UserLuckTracker userLuckTracker;
    
    @Autowired
    private ISongService songService;
    
    // 配置参数
    private static final Random RANDOM = new Random();
    private static final double EPSILON = 0.0001;  // 浮点数比较容差
    
    /**
     * 从候选歌曲池中根据稀有度权重选择一首歌
     * 
     * @param candidates 候选歌曲列表（带权重）
     * @param userId 用户ID（用于防沉迷）
     * @return 选中的歌曲，如果候选池为空则返回null
     */
    public Song selectByWeight(List<SongWithWeight> candidates, Long userId) {
        if (candidates == null || candidates.isEmpty()) {
            log.warn("候选歌曲池为空，无法选择");
            return null;
        }
        
        // 步骤1：按稀有度分组
        Map<Long, RarityGroup> rarityGroups = groupByRarity(candidates);
        
        if (rarityGroups.isEmpty()) {
            log.warn("稀有度分组为空，无法选择");
            return null;
        }
        
        // 步骤2：加载稀有度信息
        loadRarityInfo(rarityGroups);
        
        // 步骤3：计算权重（应用防沉迷机制）
        calculateWeights(rarityGroups, userId);
        
        // 步骤4：随机选择一个稀有度组
        RarityGroup selectedGroup = selectRarityGroup(rarityGroups);
        if (selectedGroup == null) {
            log.warn("随机选择稀有度组失败");
            return null;
        }
        
        // 步骤5：在选中稀有度组中随机选择一首歌
        Song selectedSong = selectSongFromGroup(selectedGroup);
        
        // 步骤6：记录用户的抽取（用于防沉迷）
        if (userId != null && selectedSong != null) {
            userLuckTracker.recordDraw(userId, selectedGroup.getRarity().getId());
        }
        
        log.info("加权随机选择完成 - 稀有度: {}, 歌曲: {}, 概率: {}", 
                selectedGroup.getRarity().getName(), 
                selectedSong != null ? selectedSong.getSongName() : "null",
                calculateSelectionProbability(selectedGroup, rarityGroups));
        
        return selectedSong;
    }
    
    /**
     * 将候选歌曲按稀有度分组
     */
    private Map<Long, RarityGroup> groupByRarity(List<SongWithWeight> candidates) {
        Map<Long, RarityGroup> groups = new LinkedHashMap<>();  // 保持插入顺序
        
        for (SongWithWeight candidate : candidates) {
            Song song = candidate.getSong();
            Long rarityId = song.getRarityId();
            
            RarityGroup group = groups.computeIfAbsent(rarityId, k -> new RarityGroup());
            group.addSong(song);
        }
        
        log.debug("稀有度分组完成 - 分组数: {}", groups.size());
        return groups;
    }
    
    /**
     * 加载稀有度信息（权重系数、名称等）
     */
    private void loadRarityInfo(Map<Long, RarityGroup> rarityGroups) {
        List<Long> rarityIds = new ArrayList<>(rarityGroups.keySet());
        List<Rarity> rarities = rarityService.listByIds(rarityIds);
        
        // 构建稀有度ID到实体映射
        Map<Long, Rarity> rarityMap = rarities.stream()
                .collect(Collectors.toMap(Rarity::getId, r -> r));
        
        // 为每个分组设置稀有度信息
        for (Map.Entry<Long, RarityGroup> entry : rarityGroups.entrySet()) {
            Long rarityId = entry.getKey();
            RarityGroup group = entry.getValue();
            Rarity rarity = rarityMap.get(rarityId);
            
            if (rarity != null) {
                group.setRarity(rarity);
            } else {
                // 如果稀有度信息不存在，使用默认值
                log.warn("稀有度ID {} 的信息不存在，使用默认值", rarityId);
                Rarity defaultRarity = new Rarity();
                defaultRarity.setId(rarityId);
                defaultRarity.setName("未知");
                defaultRarity.setWeight(1);
                group.setRarity(defaultRarity);
            }
        }
    }
    
    /**
     * 计算权重（应用防沉迷机制）
     */
    private void calculateWeights(Map<Long, RarityGroup> rarityGroups, Long userId) {
        double weightStart = 0.0;
        
        for (RarityGroup group : rarityGroups.values()) {
            Rarity rarity = group.getRarity();
            int songCount = group.getSongCount();
            
            // 基础权重 = 组内歌曲数 × 稀有度权重系数
            double baseWeight = songCount * rarity.getWeight();
            
            // 应用防沉迷权重调整
            double weightBoost = 1.0;
            if (userId != null) {
                weightBoost = userLuckTracker.getWeightBoost(userId, rarity.getId());
            }
            
            // 最终权重
            double finalWeight = baseWeight * weightBoost;
            
            group.setWeightStart(weightStart);
            group.setWeightEnd(weightStart + finalWeight);
            group.setCumulativeWeight(finalWeight);
            
            weightStart += finalWeight;
        }
    }
    
    /**
     * 随机选择一个稀有度组（累计权重法）
     */
    private RarityGroup selectRarityGroup(Map<Long, RarityGroup> rarityGroups) {
        // ✅ 修正：显式累加计算总权重，代码意图更清晰
        double totalWeight = 0.0;
        for (RarityGroup group : rarityGroups.values()) {
            totalWeight = group.getWeightEnd();  // 因为LinkedHashMap保持插入顺序且weightEnd是累加值
        }
        
        if (totalWeight <= 0) {
            log.warn("总权重为0，无法选择稀有度组");
            return null;
        }
        
        // 生成随机数 [0, totalWeight)
        double randomValue = RANDOM.nextDouble() * totalWeight;
        
        // 查找命中的稀有度组
        for (RarityGroup group : rarityGroups.values()) {
            if (randomValue >= group.getWeightStart() - EPSILON && 
                randomValue < group.getWeightEnd() + EPSILON) {
                log.debug("随机选择结果 - 随机值: {}, 命中稀有度: {}", randomValue, group.getRarity().getName());
                return group;
            }
        }
        
        // 如果随机数正好等于总权重（极端情况），返回最后一个组
        RarityGroup lastGroup = null;
        for (RarityGroup group : rarityGroups.values()) {
            lastGroup = group;
        }
        return lastGroup;
    }
    
    /**
     * 在选中稀有度组中随机选择一首歌
     */
    private Song selectSongFromGroup(RarityGroup group) {
        List<Song> songs = group.getSongs();
        
        if (songs == null || songs.isEmpty()) {
            log.warn("稀有度组 {} 没有歌曲", group.getRarity().getName());
            return null;
        }
        
        // 均匀随机选择
        int randomIndex = RANDOM.nextInt(songs.size());
        Song selectedSong = songs.get(randomIndex);
        
        log.debug("从稀有度组 {} 中选择歌曲: {}, 索引: {}/{}", 
                group.getRarity().getName(), selectedSong.getSongName(), randomIndex, songs.size());
        
        return selectedSong;
    }
    
    /**
     * 计算选中该稀有度组的概率
     */
    private double calculateSelectionProbability(RarityGroup selectedGroup, Map<Long, RarityGroup> rarityGroups) {
        if (selectedGroup == null) {
            return 0.0;
        }
        
        double groupWeight = selectedGroup.getCumulativeWeight();
        
        // ✅ 修正：显式累加计算总权重
        double totalWeight = 0.0;
        for (RarityGroup group : rarityGroups.values()) {
            totalWeight = group.getWeightEnd();  // 因为LinkedHashMap保持插入顺序且weightEnd是累加值
        }
        
        if (totalWeight <= 0) {
            return 0.0;
        }
        
        return (groupWeight / totalWeight) * 100.0;
    }
    
    /**
     * 获取各稀有度的抽取概率分布
     * 用于展示和调试
     */
    public Map<String, Double> getProbabilityDistribution(List<SongWithWeight> candidates, Long userId) {
        if (candidates == null || candidates.isEmpty()) {
            return new HashMap<>();
        }
        
        // 分组和计算权重（复用已有逻辑）
        Map<Long, RarityGroup> rarityGroups = groupByRarity(candidates);
        loadRarityInfo(rarityGroups);
        calculateWeights(rarityGroups, userId);
        
        // 计算概率
        Map<String, Double> distribution = new LinkedHashMap<>();
        for (RarityGroup group : rarityGroups.values()) {
            double probability = calculateSelectionProbability(group, rarityGroups);
            distribution.put(group.getRarity().getName(), probability);
        }
        
        return distribution;
    }
}
