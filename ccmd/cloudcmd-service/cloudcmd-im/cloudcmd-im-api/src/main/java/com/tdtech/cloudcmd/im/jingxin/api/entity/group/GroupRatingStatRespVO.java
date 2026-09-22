package com.tdtech.cloudcmd.im.jingxin.api.entity.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRatingStatRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * X轴数据（标签列表）
     */
    @JsonProperty("xAxis")
    private List<String> xAxis;

    /**
     * 图例数据（维度名称）
     */
    private List<String> legend;

    /**
     * 系列数据
     */
    private List<SeriesItemVO> series;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeriesItemVO implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 系列名称（维度名称）
         */
        private String name;

        /**
         * 系列数据
         */
        private List<BigDecimal> data;
    }
}
