package cloudcmd.service.rpc;

import cloudcmd.dto.SystemConfigDto;


/**
 * 系统配置rpc接口
 */
public interface SystemConfigRpcService {

    /**
     * 通过key获取系统配置值
     *
     * @param key key
     * @return 系统配置的值
     */
    String getValueByKey(String key);

    /**
     * 通过key更新或者新增配置
     *
     * @param configDto 配置内容
     */
    void saveOrUpdate(SystemConfigDto configDto);
}
