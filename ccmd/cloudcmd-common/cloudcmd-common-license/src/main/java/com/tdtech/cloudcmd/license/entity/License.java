package com.tdtech.cloudcmd.license.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@ToString
@Accessors(chain = true)
public class License implements Serializable {
    private static final long serialVersionUID = 1L;

    private String disableDate;

    private String esn;

    private String expireDate;

    private String licType;

    private String lsn;

    private String productName;

    private String revokeCode;

    @NotNull
    private StatusEnum status;

    private List<LicenseItem> itemList;

    private Date instant;

    private String source;
}
