package cn.edu.seig.vibemusic.controller;
 
import cn.edu.seig.vibemusic.model.dto.BlindBoxDrawRequest;
import cn.edu.seig.vibemusic.model.vo.BlindBoxResultVO;
import cn.edu.seig.vibemusic.service.IBlindBoxDrawService;
import cn.edu.seig.vibemusic.result.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
 
import javax.servlet.http.HttpServletRequest;
 
/**
 * 盲盒功能控制器
 * 对外暴露盲盒抽取相关接口
 */
@Slf4j
@RestController
@RequestMapping("/blindbox")
@Api(tags = "盲盒功能接口")
public class BlindBoxController {
    
    @Autowired
    private IBlindBoxDrawService blindBoxDrawService;
    
    /**
     * 抽取盲盒
     * 
     * @param request 抽取请求
     * @param httpRequest HTTP请求
     * @return 抽取结果
     */
    @PostMapping("/draw")
    @ApiOperation("抽取盲盒")
    public Result<BlindBoxResultVO> drawBlindBox(@RequestBody BlindBoxDrawRequest request,
                                                   HttpServletRequest httpRequest) {
        try {
            log.info("收到盲盒抽取请求 - 用户ID: {}, 情绪ID: {}, 是否随机: {}", 
                    request.getUserId(), request.getMoodId(), request.getIsRandom());
            
            // 从请求中获取用户ID（如果未传递）
            // 实际项目中应该从JWT token中获取
            if (request.getUserId() == null) {
                // 示例：从 ThreadLocal 获取
                // Long userId = UserContext.getCurrentUserId();
                // request.setUserId(userId);
            }
            
            // 调用服务抽取
            BlindBoxResultVO result = blindBoxDrawService.drawBlindBox(request);
            
            // ✅ 修正：使用项目统一的 Result.success(data) 方式
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("盲盒抽取失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取用户今日剩余抽取次数
     * 
     * @param userId 用户ID
     * @param httpRequest HTTP请求
     * @return 剩余抽取次数
     */
    @GetMapping("/remaining/{userId}")
    @ApiOperation("获取剩余抽取次数")
    public Result<Integer> getRemainingDraws(@PathVariable Long userId,
                                              HttpServletRequest httpRequest) {
        try {
            log.info("查询用户 {} 剩余抽取次数", userId);
            
            Integer remainingDraws = blindBoxDrawService.getRemainingDraws(userId);
            
            // ✅ 修正：使用项目统一的 Result.success(data) 方式
            return Result.success(remainingDraws);
            
        } catch (Exception e) {
            log.error("查询剩余抽取次数失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取用户抽取历史（分页）
     * 
     * @param userId 用户ID
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @param httpRequest HTTP请求
     * @return 抽取历史
     */
    @GetMapping("/history/{userId}")
    @ApiOperation("获取抽取历史")
    public Result<Page<BlindBoxResultVO>> getUserHistory(@PathVariable Long userId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size,
                                                          HttpServletRequest httpRequest) {
        try {
            log.info("查询用户 {} 抽取历史 - 页码: {}, 每页: {}", userId, page, size);
            
            Page<BlindBoxResultVO> history = blindBoxDrawService.getUserHistory(userId, page, size);
            
            // ✅ 修正：使用项目统一的 Result.success(data) 方式
            return Result.success(history);
            
        } catch (Exception e) {
            log.error("查询抽取历史失败", e);
            return Result.error(e.getMessage());
        }
    }
}
