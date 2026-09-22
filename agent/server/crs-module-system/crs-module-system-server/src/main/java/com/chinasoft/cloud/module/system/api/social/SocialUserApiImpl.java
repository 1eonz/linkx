// package com.chinasoft.cloud.module.system.api.social;
//
// import static com.chinasoft.cloud.framework.common.pojo.CommonResult.success;
//
// import org.springframework.validation.annotation.Validated;
// import org.springframework.web.bind.annotation.RestController;
//
// import com.chinasoft.cloud.framework.common.pojo.CommonResult;
// import com.chinasoft.cloud.module.system.api.social.dto.SocialUserBindReqDTO;
// import com.chinasoft.cloud.module.system.api.social.dto.SocialUserRespDTO;
// import com.chinasoft.cloud.module.system.api.social.dto.SocialUserUnbindReqDTO;
// import com.chinasoft.cloud.module.system.service.social.SocialUserService;
//
// import jakarta.annotation.Resource;
//
// @RestController // 提供 RESTful API 接口，给 Feign 调用
// @Validated
// public class SocialUserApiImpl implements SocialUserApi {
//
// @Resource
// private SocialUserService socialUserService;
//
// @Override
// public CommonResult<String> bindSocialUser(SocialUserBindReqDTO reqDTO) {
// return CommonResult.error(-1, "nope");
// }
//
// @Override
// public CommonResult<Boolean> unbindSocialUser(SocialUserUnbindReqDTO reqDTO) {
// return CommonResult.error(-1, "nope");
// }
//
// @Override
// public CommonResult<SocialUserRespDTO> getSocialUserByUserId(Integer userType, Long userId, Integer socialType) {
// return success(socialUserService.getSocialUserByUserId(userType, userId, socialType));
// }
//
// @Override
// public CommonResult<SocialUserRespDTO> getSocialUserByCode(Integer userType, Integer socialType, String code,
// String state) {
// return success(socialUserService.getSocialUserByCode(userType, socialType, code, state));
// }
//
// }
