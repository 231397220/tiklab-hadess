package io.tiklab.hadess.upload.controller;


import io.tiklab.hadess.upload.service.RpmUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

@RestController
@RequestMapping("/rpm")
public class RpmUploadController {

    @Autowired
    RpmUploadService rpmUploadService;


    //上传
    @RequestMapping(path="/**",method = {RequestMethod.POST,RequestMethod.PUT})
    public void doPost(HttpServletRequest request, HttpServletResponse response){
        System.out.println("请求地址1:"+request.getRequestURI());
        rpmUploadService.rpmUpload(request,response);
    }



    //拉取
    @RequestMapping(path="/**",method = {RequestMethod.GET})
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        Enumeration<String> headerNames = request.getHeaderNames();
        System.out.println("请求地址:"+request.getRequestURI());
        rpmUploadService.rpmDownload(request,response);
    }
}
