package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("tr_police_ticket_group")
public class TrPoliceTicketGroup {

    @TableId
    private Long id;

    private Long ticketId;

    private Long groupId;

}
