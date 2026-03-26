package cn.edu.seig.vibemusic.exception;
 
/**
 * 盲盒功能未启用异常
 */
public class BlindBoxDisabledException extends BlindBoxException {
    
    public BlindBoxDisabledException() {
        super("BLIND_BOX_DISABLED", "盲盒功能暂未启用，请稍后再试");
    }
}
