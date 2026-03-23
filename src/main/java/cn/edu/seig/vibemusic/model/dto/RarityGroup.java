package cn.edu.seig.vibemusic.model.dto;
 
import cn.edu.seig.vibemusic.model.entity.Rarity;
import cn.edu.seig.vibemusic.model.entity.Song;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
 
/**
 * 稀有度分组DTO
 * 将候选歌曲按稀有度分组
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RarityGroup implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 稀有度信息
     */
    private Rarity rarity;
    
    /**
     * 该稀有度下的歌曲列表
     */
    private List<Song> songs;
    
    /**
     * 该稀有度组的累计权重（计算后）
     * 公式：组内歌曲数 × 稀有度权重系数
     */
    private double cumulativeWeight;
    
    /**
     * 该稀有度组的起始权重范围（用于随机选择）
     */
    private double weightStart;
    
    /**
     * 该稀有度组的结束权重范围（用于随机选择）
     */
    private double weightEnd;
    
    /**
     * 添加歌曲到分组
     */
    public void addSong(Song song) {
        if (this.songs == null) {
            this.songs = new ArrayList<>();
        }
        this.songs.add(song);
    }
    
    /**
     * 获取分组内歌曲数量
     */
    public int getSongCount() {
        return songs != null ? songs.size() : 0;
    }
}
