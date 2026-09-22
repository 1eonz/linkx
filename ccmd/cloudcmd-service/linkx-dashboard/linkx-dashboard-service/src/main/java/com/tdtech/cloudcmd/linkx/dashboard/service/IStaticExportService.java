package com.tdtech.cloudcmd.linkx.dashboard.service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface IStaticExportService {

    void export(String departmentCode, String startTime, String endTime,
                Integer includeChildren, HttpServletResponse response) throws IOException;
}