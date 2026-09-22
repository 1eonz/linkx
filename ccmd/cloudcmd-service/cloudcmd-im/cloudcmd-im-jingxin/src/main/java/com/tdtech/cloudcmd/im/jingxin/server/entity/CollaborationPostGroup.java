package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_collaboration_post_group")
public class CollaborationPostGroup {

    private Long id;

    private Long postId;

    private Long groupId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long supportUserId;
}
