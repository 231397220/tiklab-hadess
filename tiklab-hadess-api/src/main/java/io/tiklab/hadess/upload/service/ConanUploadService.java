package io.tiklab.hadess.upload.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ConanUploadService {



    /**
     * 数据执行的校验步骤 get请求
     * @param request  request
     * @param  response response
     */
    void dataVerifyGet(HttpServletRequest request, HttpServletResponse response);

    /**
     * 上传写入制品数据
     * @param request  request
     * @param  response response
     */
    void uploadData(HttpServletRequest request, HttpServletResponse response);


    /**
     * 获取上传文件的路径
     * @param request  request
     * @param  response response
     */
    String getUploadDataPath(HttpServletRequest request, HttpServletResponse response);


    /**
     * 获取元数据、二进制文件下载链接L（客户端会发起两次请求，第一次获取元数据、第二次二进制包）
     * @param request  request
     * @param  response response
     */
    String getDownloadUrls(HttpServletRequest request, HttpServletResponse response);


    /**
     * 下载元数据、二进制包
     * @param request  request
     * @param  response response
     */
    void downloadPackage(HttpServletRequest request, HttpServletResponse response);


    /**
     * 上传、下载 校验二进制包数据       客户端执行 --all时候上传会把二进制包上传
     * @param request  request
     * @param  response response
     */
    String verifyPackage(HttpServletRequest request, HttpServletResponse response);

    /**
     * 拉取文件内容
     * @param request  request
     * @param  response response
     */
    void downloadFileData(HttpServletRequest request, HttpServletResponse response);
}
