package com.tdtech.cloudcmd.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Custom department duty schedule user view object.
 */
@Schema(description = "自定义通讯录部门排班人员视图对象")
public class DepartmentCustomDutyScheduleVO {

    @Schema(description = "人员ID")
    private Long id;

    @Schema(description = "警信用户ID")
    private Long userId;

    @Schema(description = "警信用户名称")
    private String userName;

    @Schema(description = "警信部门ID")
    private Long departmentId;

    @Schema(description = "警信部门名称")
    private String departmentName;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "排班时间段列表")
    private List<DutyTimeVO> dutyTimeList = new ArrayList<>();

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

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public List<DutyTimeVO> getDutyTimeList() {
        return dutyTimeList;
    }

    public void setDutyTimeList(List<DutyTimeVO> dutyTimeList) {
        this.dutyTimeList = dutyTimeList;
    }

    @Schema(description = "排班时间段")
    public static class DutyTimeVO {

        @Schema(description = "排班ID")
        private Long id;

        @Schema(description = "排班开始日期")
        private LocalDate dutyStartDate;

        @Schema(description = "排班结束日期")
        private LocalDate dutyEndDate;

        @Schema(description = "排班开始时间")
        private LocalTime dutyStartTime;

        @Schema(description = "排班结束时间")
        private LocalTime dutyEndTime;

        @Schema(description = "排班类型标识")
        private Long dutyType;

        @Schema(description = "排班类型名称")
        private String dutyTypeName;

        @Schema(description = "排班内容")
        private String dutyContent;

        @Schema(description = "导入批次")
        private Long importBatch;

        @Schema(description = "创建时间")
        private Date gmtCreated;

        @Schema(description = "最后修改时间")
        private Date gmtModified;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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
    }
}