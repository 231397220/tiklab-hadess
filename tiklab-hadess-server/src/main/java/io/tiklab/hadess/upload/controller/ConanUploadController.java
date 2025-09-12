package io.tiklab.hadess.upload.controller;

import com.alibaba.fastjson.JSONObject;
import io.tiklab.hadess.upload.service.ConanUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/conan")
public class ConanUploadController {

    @Autowired
    ConanUploadService conanUploadService;


    @RequestMapping(path="/**",method = RequestMethod.GET)
    public Object doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestURI = request.getRequestURI();

        //获取元数据、二进制文件下载链接
        if (requestURI.endsWith("/download_urls")){
            String downloadUrls = conanUploadService.getDownloadUrls(request, response);
            JSONObject object = JSONObject.parseObject(downloadUrls);
            return object;
        }

        //客户端拉取->拉取元数据、二进制文件
        if (requestURI.contains("/download_package/")){
            conanUploadService.downloadPackage(request, response);
            return null;
        }

        if (requestURI.contains("/_/_/packages/")&&!requestURI.endsWith("/download_urls")){
            String a=conanUploadService.verifyPackage(request, response);

            JSONObject object = JSONObject.parseObject(a);
            return object;
        }

        //拉取远程的拉取文件内容
        if (requestURI.contains("/v1/files/")){
            conanUploadService.downloadFileData(request, response);
        }


        conanUploadService.dataVerifyGet(request,response);
        return null;
    }

    @RequestMapping(path="/**",method = RequestMethod.POST)
    public String doPost(HttpServletRequest request, HttpServletResponse response){
        String uploaded = conanUploadService.getUploadDataPath(request, response);
        return uploaded;
    }
    @RequestMapping(path="/**",method = RequestMethod.PUT)
    public void doPut(HttpServletRequest request, HttpServletResponse response){
         conanUploadService.uploadData(request, response);
    }
}
