package cn.edu.seig.vibemusic.service.builder;

import cn.edu.seig.vibemusic.model.dto.SongWithWeight;
import cn.edu.seig.vibemusic.model.entity.*;
import cn.edu.seig.vibemusic.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 候选池构建器
 * 核心功能：根据情绪ID、用户偏好构建盲盒抽取的候选歌曲池
 */
@Slf4j
@Component
public class CandidatePoolBuilder {
    
    @Autowired
    private ISongMoodService songMoodService;
    
    @Autowired
    private IArtistMoodService artistMoodService;
    
    @Autowired
    private IUserMoodPreferenceService userMoodPreferenceService;
    
    @Autowired
    private ISongService songService;
    
    // 配置参数
    private static final int MIN_POOL_SIZE = 20;           // 最小候选池大小
    private static final int MAX_POOL_SIZE = 100;          // 最大候选池大小
    private static final double PREFERENCE_BOOST = 1.5;     // 用户偏好情绪的权重倍增
    private static final double ARTIST_SONG_WEIGHT = 0.7;   // 歌手补充歌曲的基础权重
    
    /**
     * 构建候选歌曲池
     * 
     * @param moodId 情绪ID（null表示随机模式）
     * @param userId 用户ID（用于个性化调整）
     * @param isRandom 是否随机模式
     * @return 候选歌曲列表（带权重）
     */
    public List<SongWithWeight> buildPool(Long moodId, Long userId, boolean isRandom) {
        log.info("开始构建候选池 - moodId: {}, userId: {}, isRandom: {}", moodId, userId, isRandom);
        
        List<SongWithWeight> candidates = new ArrayList<>();
        
        // 步骤1：构建基础候选池
        if (isRandom) {
            candidates = buildRandomPool();
        } else {
            candidates = buildMoodPool(moodId);
        }
        
        // 步骤2：候选池不足时，通过歌手情绪补充
        if (candidates.size() < MIN_POOL_SIZE && !isRandom) {
            log.info("候选池不足（{}），通过歌手情绪补充", candidates.size());
            List<SongWithWeight> supplement = supplementFromArtistMood(moodId, candidates);
            candidates.addAll(supplement);
        }
        
        // 步骤3：根据用户偏好调整权重
        if (userId != null) {
            adjustWeightsByUserPreference(candidates, userId);
        }
        
        // 步骤4：去重和大小控制
        candidates = deduplicateAndLimit(candidates);
        
        log.info("候选池构建完成 - 最终大小: {}", candidates.size());
        return candidates;
    }
    
    /**
     * 构建随机模式的候选池
     * 返回所有关联了情绪标签的歌曲
     */
    private List<SongWithWeight> buildRandomPool() {
        // 查询所有关联了情绪的歌曲ID（去重）
        LambdaQueryWrapper<SongMood> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SongMood::getSongId)
               .groupBy(SongMood::getSongId);
        
        List<SongMood> songMoods = songMoodService.list(wrapper);
        
        if (songMoods.isEmpty()) {
            log.warn("没有找到任何关联了情绪标签的歌曲");
            return new ArrayList<>();
        }
        
        // 批量查询歌曲信息
        List<Long> songIds = songMoods.stream()
                .map(SongMood::getSongId)
                .collect(Collectors.toList());
        
        List<Song> songs = songService.listByIds(songIds);
        
        // 为每首歌关联对应的情绪ID
        Map<Long, List<Long>> songMoodMap = buildSongMoodMap(songIds);
        
        List<SongWithWeight> candidates = new ArrayList<>();
        for (Song song : songs) {
            List<Long> moodIds = songMoodMap.get(song.getSongId());
            if (moodIds != null && !moodIds.isEmpty()) {
                // 随机选择一个情绪ID作为主情绪
                Long primaryMoodId = moodIds.get(new Random().nextInt(moodIds.size()));
                candidates.add(SongWithWeight.of(song, primaryMoodId));
            }
        }
        
        log.info("随机模式候选池构建完成 - 歌曲: {}", candidates.size());
        return candidates;
    }
    
    /**
     * 构建指定情绪的候选池
     */
    private List<SongWithWeight> buildMoodPool(Long moodId) {
        // 查询该情绪关联的歌曲
        LambdaQueryWrapper<SongMood> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SongMood::getMoodId, moodId);
        
        List<SongMood> songMoods = songMoodService.list(wrapper);
        
        if (songMoods.isEmpty()) {
            log.warn("情绪ID {} 没有关联的歌曲", moodId);
            return new ArrayList<>();
        }
        
        // 批量查询歌曲信息
        List<Long> songIds = songMoods.stream()
                .map(SongMood::getSongId)
                .collect(Collectors.toList());
        
        List<Song> songs = songService.listByIds(songIds);
        
        // 构建候选池（基础权重1.0）
        List<SongWithWeight> candidates = songs.stream()
                .map(song -> SongWithWeight.of(song, moodId))
                .collect(Collectors.toList());
        
        log.info("情绪 {} 候选池构建完成 - 歌曲: {}", moodId, candidates.size());
        return candidates;
    }
    
    /**
     * 通过歌手情绪补充候选池
     * 当指定情绪的歌曲不足时，查询关联该情绪的歌手的所有歌曲
     */
    private List<SongWithWeight> supplementFromArtistMood(Long moodId, List<SongWithWeight> existingCandidates) {
        // 查询该情绪关联的歌手
        LambdaQueryWrapper<ArtistMood> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArtistMood::getMoodId, moodId);
        
        List<ArtistMood> artistMoods = artistMoodService.list(wrapper);
        
        if (artistMoods.isEmpty()) {
            log.info("情绪 {} 没有关联的歌手，无法补充", moodId);
            return new ArrayList<>();
        }
        
        // 查询这些歌手的所有歌曲
        List<Long> artistIds = artistMoods.stream()
                .map(ArtistMood::getArtistId)
                .collect(Collectors.toList());
        
        LambdaQueryWrapper<Song> songWrapper = new LambdaQueryWrapper<>();
        songWrapper.in(Song::getArtistId, artistIds);
        
        List<Song> artistSongs = songService.list(songWrapper);
        
        // 排除已存在的歌曲
        Set<Long> existingSongIds = existingCandidates.stream()
                .map(SongWithWeight::getSongId)
                .collect(Collectors.toSet());
        
        // 计算可补充的数量
        int limit = Math.max(0, MAX_POOL_SIZE - existingCandidates.size());
        if (limit == 0) {
            return new ArrayList<>();
        }
        
        List<SongWithWeight> supplements = artistSongs.stream()
                .filter(song -> !existingSongIds.contains(song.getSongId()))
                .map(song -> SongWithWeight.ofArtist(song, moodId))
                .limit(limit)
                .collect(Collectors.toList());
        
        log.info("通过歌手情绪补充了 {} 首歌曲", supplements.size());
        return supplements;
    }
    
    /**
     * 根据用户情绪偏好调整候选池权重
     * 用户偏好的情绪对应歌曲权重提升1.5倍，优先级越高提升越多
     */
    private void adjustWeightsByUserPreference(List<SongWithWeight> candidates, Long userId) {
        // 查询用户的情绪偏好
        LambdaQueryWrapper<UserMoodPreference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserMoodPreference::getUserId, userId);
        
        List<UserMoodPreference> preferences = userMoodPreferenceService.list(wrapper);
        
        if (preferences.isEmpty()) {
            log.debug("用户 {} 没有情绪偏好设置", userId);
            return;
        }
        
        // 构建用户偏好的情绪集合（带优先级）
        Map<Long, Integer> preferenceMap = preferences.stream()
                .collect(Collectors.toMap(
                        UserMoodPreference::getMoodId,
                        UserMoodPreference::getPriority,
                        (v1, v2) -> v1  // 如果重复，保留第一个
                ));
        
        // 调整候选池权重
        int adjustedCount = 0;
        for (SongWithWeight candidate : candidates) {
            Long moodId = candidate.getMoodId();
            Integer priority = preferenceMap.get(moodId);
            if (priority != null) {
                // 归一化优先级（假设优先级范围 1-10，最小0，最大1）
                double factor = Math.min(1.0, Math.max(0.0, priority / 10.0));
                double multiplier = 1.0 + (PREFERENCE_BOOST - 1.0) * factor;
                candidate.multiplyWeight(multiplier);
                adjustedCount++;
            }
        }
        
        log.info("用户 {} 偏好调整完成 - 调整了 {} 首歌曲的权重", userId, adjustedCount);
    }
    
    /**
     * 构建歌曲-情绪映射
     * 一首歌可能关联多个情绪
     */
    private Map<Long, List<Long>> buildSongMoodMap(List<Long> songIds) {
        if (songIds.isEmpty()) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<SongMood> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SongMood::getSongId, songIds);
        
        List<SongMood> songMoods = songMoodService.list(wrapper);
        
        return songMoods.stream()
                .collect(Collectors.groupingBy(
                        SongMood::getSongId,
                        Collectors.mapping(SongMood::getMoodId, Collectors.toList())
                ));
    }
    
    /**
     * 去重和控制候选池大小
     */
    private List<SongWithWeight> deduplicateAndLimit(List<SongWithWeight> candidates) {
        // 使用LinkedHashSet去重（保持顺序）
        Set<Long> seenIds = new HashSet<>();
        List<SongWithWeight> result = new ArrayList<>();
        
        for (SongWithWeight candidate : candidates) {
            Long songId = candidate.getSongId();
            if (!seenIds.contains(songId)) {
                seenIds.add(songId);
                result.add(candidate);
                
                // 达到最大数量则停止
                if (result.size() >= MAX_POOL_SIZE) {
                    break;
                }
            }
        }
        
        log.debug("候选池去重和控制大小 - 原始: {}, 去重后: {}", candidates.size(), result.size());
        return result;
    }
}
