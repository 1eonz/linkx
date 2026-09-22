package com.tdtech.cloudcmd.im.jingxin.api.entity.duty;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

public class DutyScheduleUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String userName;

    private Long departmentId;

    private String departmentName;

    private LocalDate dutyStartDate;

    private LocalDate dutyEndDate;

    private LocalTime dutyStartTime;

    private LocalTime dutyEndTime;

    private Long dutyType;

    private String dutyTypeName;

    private String dutyContent;

    private Long importBatch;

    private Date gmtCreated;

    private Date gmtModified;

    /**
     * 用户头像URL路径
     * 由 IM 服务层通过 FileUtil.getAvatarPathOrDownload() 处理后返回
     * 格式：/collaboration/static/{userId}_{fileId}
     */
    private String avatar;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public LocalDate getDutyStartDate() {
        return dutyStartDate;
    }

    public void setDutyStartDate(LocalDate dutyStartDate) {
        this.dutyStartDate = dutyStartDate;
    }

    public LocalDate getDutyEndDate() {
        return dutyEndDate;
    }

    public void setDutyEndDate(LocalDate dutyEndDate) {
        this.dutyEndDate = dutyEndDate;
    }

    public LocalTime getDutyStartTime() {
        return dutyStartTime;
    }

    public void setDutyStartTime(LocalTime dutyStartTime) {
        this.dutyStartTime = dutyStartTime;
    }

    public LocalTime getDutyEndTime() {
        return dutyEndTime;
    }

    public void setDutyEndTime(LocalTime dutyEndTime) {
        this.dutyEndTime = dutyEndTime;
    }

    public Long getDutyType() {
        return dutyType;
    }

    public void setDutyType(Long dutyType) {
        this.dutyType = dutyType;
    }

    public String getDutyTypeName() {
        return dutyTypeName;
    }

    public void setDutyTypeName(String dutyTypeName) {
        this.dutyTypeName = dutyTypeName;
    }

    public String getDutyContent() {
        return dutyContent;
    }

    public void setDutyContent(String dutyContent) {
        this.dutyContent = dutyContent;
    }

    public Long getImportBatch() {
        return importBatch;
    }

    public void setImportBatch(Long importBatch) {
        this.importBatch = importBatch;
    }

    public Date getGmtCreated() {
        return gmtCreated;
    }

    public void setGmtCreated(Date gmtCreated) {
        this.gmtCreated = gmtCreated;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}