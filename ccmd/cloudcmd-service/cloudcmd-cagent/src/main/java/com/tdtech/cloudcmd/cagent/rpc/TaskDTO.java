package com.tdtech.cloudcmd.cagent.rpc;

import java.util.List;

import com.tdtech.cloudcmd.cagent.server.frame.CdcFrame;
import com.tdtech.cloudcmd.cagent.service.UserInfo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {

    private CdcFrame cdcFrame;

    private List<UserInfo> target;
}
