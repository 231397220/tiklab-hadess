package io.tiklab.hadess.upload.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RpmUploadService {

    /**
     * 上传数据
     * @param request  request
     */
    void rpmUpload(HttpServletRequest request, HttpServletResponse response);

    /**
     * 拉取数据
     * @param request  request
     */
    void rpmDownload(HttpServletRequest request, HttpServletResponse response);
}
