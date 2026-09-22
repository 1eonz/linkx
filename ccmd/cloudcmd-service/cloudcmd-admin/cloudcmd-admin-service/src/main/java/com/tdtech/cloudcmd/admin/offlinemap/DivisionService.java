package com.tdtech.cloudcmd.admin.offlinemap;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.admin.offlinemap.repo.Division;
import com.tdtech.cloudcmd.admin.offlinemap.repo.DivisionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DivisionService extends ServiceImpl<DivisionMapper, Division>  {

    public Division selectGeo() {
        QueryWrapper<Division> queryWrapper = new QueryWrapper<>();
        return super.baseMapper.selectOne(queryWrapper);
    }
}
