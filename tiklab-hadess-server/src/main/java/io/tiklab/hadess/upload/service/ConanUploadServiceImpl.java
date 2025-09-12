package io.tiklab.hadess.upload.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.tiklab.core.exception.ApplicationException;
import io.tiklab.core.exception.SystemException;
import io.tiklab.hadess.common.FileUtil;
import io.tiklab.hadess.common.RepositoryUtil;
import io.tiklab.hadess.common.UserCheckService;
import io.tiklab.hadess.common.XpackYamlDataMaService;
import io.tiklab.hadess.library.model.Library;
import io.tiklab.hadess.library.model.LibraryFile;
import io.tiklab.hadess.library.model.LibraryVersion;
import io.tiklab.hadess.library.service.LibraryFileService;
import io.tiklab.hadess.library.service.LibraryService;
import io.tiklab.hadess.library.service.LibraryVersionService;
import io.tiklab.hadess.repository.model.Repository;
import io.tiklab.hadess.repository.model.RepositoryGroup;
import io.tiklab.hadess.repository.model.RepositoryGroupQuery;
import io.tiklab.hadess.repository.model.RepositoryRemoteProxy;
import io.tiklab.hadess.repository.service.RepositoryGroupService;
import io.tiklab.hadess.repository.service.RepositoryRemoteProxyService;
import io.tiklab.hadess.repository.service.RepositoryService;
import io.tiklab.hadess.upload.common.response.ConanResponse;
import io.tiklab.hadess.upload.common.response.NugetResponse;
import io.tiklab.user.user.model.User;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConanUploadServiceImpl implements ConanUploadService{
    private static Logger logger = LoggerFactory.getLogger(ConanUploadServiceImpl.class);


    @Autowired
    RepositoryService repositoryService;

    @Autowired
    RepositoryRemoteProxyService remoteProxyService;

    @Autowired
    RepositoryGroupService groupService;

    @Autowired
    XpackYamlDataMaService yamlDataMaService;

    @Autowired
    LibraryService libraryService;

    @Autowired
    LibraryVersionService versionService;

    @Autowired
    LibraryFileService libraryFileService;

    @Autowired
    UserCheckService userCheckService;

    public static final Map<String,String> downloadPack = new HashMap();



    @Override
    public void dataVerifyGet(HttpServletRequest request, HttpServletResponse resp) {

        String requestURI = request.getRequestURI();
        try {
            logger.info("访问地址："+requestURI);
            Enumeration<String> headerNames = request.getHeaderNames();
            String header = request.getHeader("x-client-anonymous-id");


            //客户端检查服务器是否存活
            if (requestURI.endsWith("/v1/ping")){
                String[] split = requestURI.split("/");
                String repName = split[2];
                Repository repository = repositoryService.findRepository(repName, "conan");
                //仓库不存在
                if (ObjectUtils.isEmpty(repository)){
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().print("the warehouse doesn't exist.");
                    return;
                }

                //仓库配置为远程库
                if (("remote").equals(repository.getRepositoryType())){
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().print("Remote library configuration is not supported");
                    return ;
                }
                if (("group").equals(repository.getRepositoryType())){
                    //查询组合库关联的仓库
                    List<RepositoryGroup> groupList = groupService.findRepositoryGroupList(new RepositoryGroupQuery()
                            .setRepositoryGroupId(repository.getId()));

                    List<RepositoryGroup> repositoryGroups = groupList.stream().filter(a -> "local".equals(a.getRepository().getRepositoryType())).toList();
                    if (CollectionUtils.isEmpty(repositoryGroups)){
                        logger.info("conan 推送组合库中没有关联本地库");
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().print("there is no local library");
                        return;
                    }
                }

                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setHeader("X-Conan-Server-Capabilities", header);
                resp.getWriter().print("Useful to get server capabilities");
                return;
            }

            //检查包是否存在，摘要信息（digest），用于验证包的元数据或检查更新
            if (requestURI.endsWith("_/digest")){
                //读取上传的数据
                String readData = FileUtil.readInputStream(request.getInputStream());
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
               /* {
                    "conanfile.py": {
                    "sha1": "da39a3ee5e6b4b0d3255bfef95601890afd80709",
                     "md5": "d41d8cd98f00b204e9800998ecf8427e"
                }
                }*/
            }

            //校验用户凭证
            if (requestURI.endsWith("v1/users/check_credentials")){
                Enumeration<String> headerNames1 = request.getHeaderNames();
                String user = request.getHeader("x-client-id");
                if (StringUtils.isBlank(user)){
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                }else {
                    List<User> userList = userCheckService.npmUserCheckByName(user);
                    if (CollectionUtils.isEmpty(userList)){
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    resp.setStatus(HttpServletResponse.SC_OK);
                }
                return;
            }

            if (requestURI.endsWith("/_/_")){
                String readData = FileUtil.readInputStream(request.getInputStream());
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }


        }catch (Exception e){
            logger.info("Conan请求："+requestURI+"失败："+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String verifyPackage(HttpServletRequest request, HttpServletResponse response) {
        StringBuffer requestURL = request.getRequestURL();
        String path = StringUtils.substringBeforeLast(requestURL.toString(), "_/_/");

        String requestURI = request.getRequestURI();
        logger.info("访问地址："+requestURI);
        try {
            //截取请求路径中的数据,获取仓库名字、制品名字、版本、文件名字
            Map<String, String> outData = cutOutData(requestURI,requestURL.toString());
            String libraryName = outData.get("libraryName");
            String version = outData.get("version");
            String repName = outData.get("repName");
            Repository repository = repositoryService.findRepository(repName, "conan");

            List<LibraryFile> libraryFileList;
            //制品库类型为组合库
            if (("group").equals(repository.getRepositoryType())){
                //查询组合库关联的仓库
                List<RepositoryGroup> groupList = groupService.findRepositoryGroupList(new RepositoryGroupQuery()
                        .setRepositoryGroupId(repository.getId()));

                List<String> rpyIds = groupList.stream().map(a -> a.getRepository().getId()).collect(Collectors.toList());
                String[] repositoryIds = rpyIds.toArray(new String[rpyIds.size()]);

                //通过仓库id、 制品、版本查询文件，如果不存在转发远程
                 libraryFileList = libraryFileService.findFileByReAndLibraryAndVer(repositoryIds, libraryName, version);
                if (CollectionUtils.isEmpty(libraryFileList)){
                    List<RepositoryGroup> RemoteRepGroups = groupList.stream().filter(a -> "remote".equals(a.getRepository().getRepositoryType())).toList();
                    List<String> remoteRepIds = RemoteRepGroups.stream().map(a -> a.getRepository().getId()).collect(Collectors.toList());
                    String[] remoteRepIdList = remoteRepIds.toArray(new String[remoteRepIds.size()]);

                    //转发远程
                    String urls = forwardRemoteGetDownloadUrls(remoteRepIdList, outData);
                    return urls;
                }
            }else {
                /*---本地 local 库----*/
                //通过仓库id、 制品、版本查询文件
                libraryFileList = libraryFileService.findFileByReAndLibraryAndVer(repository.getId(), libraryName, version);
            }

            if (CollectionUtils.isEmpty(libraryFileList)){
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }

            //制品库类型为local
            String packageId = StringUtils.substringBeforeLast(requestURI, "/");
            List<LibraryFile> libraryFiles = libraryFileList.stream().filter(a -> a.getFileName().startsWith("packages/")).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(libraryFiles)){
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().print("Package"+packageId+" not found");
                return null;
            }

            Map<String, String> hashMap = new HashMap<>();
            for (LibraryFile libraryFile:libraryFiles){
                String name = StringUtils.substringAfter(libraryFile.getFileName(), "packages/");
                String filePath = yamlDataMaService.repositoryAddress() + "/" + libraryFile.getFileUrl();
                hashMap.put(name,RepositoryUtil.MD5Encryption(filePath));
            }

            // 使用 Jackson 将结果转换为 JSON
            String convert = convertToJson(hashMap);
            return convert;
        }catch (Exception e){
            logger.info("Conan请求："+requestURI+"失败："+e.getMessage());
            e.printStackTrace();
            throw new SystemException(e);
        }
    }

    @Override
    public void downloadFileData(HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        String[] split = requestURI.split("/_/");
        Enumeration<String> headerNames = request.getHeaderNames();
        //仓库名字
        String[] repSplit = split[0].split("/");
        String repName = repSplit[2];

        //制品名字、版本、文件名字
        String[] LibrarySplit = split[1].split("/");
        String libraryName = LibrarySplit[0];
        String version = LibrarySplit[1];
        String fileName = StringUtils.substringAfterLast(requestURI, "/");

        Repository repository = repositoryService.findRepository(repName, "conan");
        List<LibraryFile> libraryFileList = libraryFileService.findFileByReAndLibraryAndVer(repository.getId(), libraryName, version);
        List<LibraryFile> libraryFiles = libraryFileList.stream().filter(a -> (fileName).equals(a.getFileName())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(libraryFiles)){
            String filePath = yamlDataMaService.repositoryAddress() + "/" + libraryFiles.get(0).getFileUrl();
            try {
                ConanResponse.readFileData(new File(filePath),response);
            } catch (IOException e) {
                logger.info(libraryName+",文件内容不存在");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                throw new RuntimeException(e);
            }
            return;
        }

        //仓库地址为代理库
        if (("remote").equals(repository.getRepositoryType())){
            List<RepositoryRemoteProxy> remoteProxyList = remoteProxyService.findAgencyByRepId(repository.getId());
            String agencyUrl = remoteProxyList.get(0).getRemoteProxy().getAgencyUrl();

            String substringAfter = StringUtils.substringAfter(requestURI, repName);
            String forwardPath = agencyUrl + substringAfter;

            try {
                ResponseEntity<byte[]> restTemplateByte = RepositoryUtil.getRestTemplateByte(forwardPath);
                int codeValue = restTemplateByte.getStatusCodeValue();
                response.setStatus(codeValue);
                ServletOutputStream outputStream = response.getOutputStream();
                outputStream.write(restTemplateByte.getBody());

                //获取数据成功后 写入数据、添加数据库记录
                if (codeValue==200){
                 /*   String filePath = yamlDataMaService.repositoryAddress() + "/" + repository.getId() + "/" + libraryName+"/"+version+"/"+fileName;


                    //写入内容
                    FileUtil.writeDataToFile(request.getInputStream(),filePath);
                    //添加数据库表
                    Library library = libraryService.createLibraryData(libraryName, "conan", repository);
                    addLibraryData(library,version,fileName,"conanCenter");*/
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }


    @Override
    public String getUploadDataPath(HttpServletRequest request, HttpServletResponse response) {
        try {
            StringBuffer requestURL = request.getRequestURL();
            String path = StringUtils.substringBeforeLast(requestURL.toString(), "_/_/");
            logger.info("客户端上传请求地址："+requestURL);
            Enumeration<String> headerNames = request.getHeaderNames();
            String userName = request.getHeader("x-client-id");

            String readData = FileUtil.readInputStream(request.getInputStream());

            // 解析 JSON 数据
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(readData);
            LinkedHashMap linkedHashMap = objectMapper.convertValue(jsonNode, LinkedHashMap.class);


            Set keyList = linkedHashMap.keySet();
            Map<String, String> uploadUrls = new HashMap<>();
            //二进制包上传时候获取
            if (requestURL.toString().contains("/_/_/packages/")){
                for (Object key:keyList){
                   /* //在上传元数据和上传二进制包的时候都会有conanmanifest.txt,
                    if (key.equals("conanmanifest.txt")){
                        path+userName+"/_/_/"+key
                    }*/
                    uploadUrls.put(key.toString(), path+userName+"/_/_/packages/"+key);
                }
            }else {
                for (Object key:keyList){
                    uploadUrls.put(key.toString(), path+userName+"/_/_/"+key);
                }
            }
            // 使用 Jackson 将结果转换为 JSON
            ObjectMapper objectMapper1 = new ObjectMapper();
            String jsonResponse = objectMapper1.writeValueAsString(uploadUrls);

            JSONObject uploadUrls1 = new JSONObject();
            uploadUrls1.put("upload_urls",jsonResponse);
            return  jsonResponse;

        }catch (Exception e){
            e.printStackTrace();
            throw new SystemException(e);
        }
    }



    @Override
    public void uploadData(HttpServletRequest request, HttpServletResponse response) {
        Enumeration<String> headerNames = request.getHeaderNames();
        String requestURI = request.getRequestURI();
        logger.info("客户端上传请求地址："+requestURI);

        //文件名字
        String fileName = StringUtils.substringAfterLast(requestURI, "/_/_/");

        //仓库名字
        String[] split = requestURI.split("/v1/conans/");
        String repName = StringUtils.substringAfter(split[0], "conan/");

        //制品名字、版本、用户名字
        String before = StringUtils.substringBefore(split[1], "/_/_/");
        String[] libraryData = before.split("/");
        String libraryName = libraryData[0];
        String version = libraryData[1];
        String userName = libraryData[2];

        try {
            Repository repository = repositoryService.findRepository(repName, "conan");
            if (("group").equals(repository.getRepositoryType())){
                //查询组合库关联的仓库
                List<RepositoryGroup> groupList = groupService.findRepositoryGroupList(new RepositoryGroupQuery()
                        .setRepositoryGroupId(repository.getId()));

                List<RepositoryGroup> repositoryGroups = groupList.stream().filter(a -> "local".equals(a.getRepository().getRepositoryType())).toList();
                repository = repositoryGroups.get(0).getRepository();
            }

            //文件地址
            String filePath = yamlDataMaService.repositoryAddress() + "/" + repository.getId() + "/" + libraryName+"/"+version+"/"+fileName;

            //写入内容
            FileUtil.writeDataToFile(request.getInputStream(),filePath);

            //添加数据库表
            Library library = libraryService.createLibraryData(libraryName, "conan", repository);
            addLibraryData(library,version,fileName,userName);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print("\"status\": \"ok\"");

        }catch (Exception e){
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().print(e.getMessage());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }


    /**
     * conan拉取 -> 获取下载文件数据的urls并返回给客户端
     * @param request request
     *  @param resp resp
     */
    @Override
    public String getDownloadUrls(HttpServletRequest request, HttpServletResponse resp)  {
        StringBuffer requestURL = request.getRequestURL();
        String path = StringUtils.substringBeforeLast(requestURL.toString(), "/_/_/");
        logger.info("客户端拉取请求地址："+requestURL);

        String requestURI = request.getRequestURI();

        //截取请求路径中的数据,获取仓库名字、制品名字、版本、文件名字
        Map<String, String> outData = cutOutData(requestURI,requestURL.toString());
        String repName = outData.get("repName");
        String libraryName = outData.get("libraryName");
        String version = outData.get("version");
        Repository repository = repositoryService.findRepository(repName, "conan");


        List<RepositoryGroup> groupList=null;
        List<LibraryFile> libraryList;

        //组合库
        if (("group").equals(repository.getRepositoryType())){
            //查询组合库关联的仓库
             groupList = groupService.findRepositoryGroupList(new RepositoryGroupQuery()
                    .setRepositoryGroupId(repository.getId()));

            //组合库未关联其他仓库
            if (CollectionUtils.isEmpty(groupList)){
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return null;
            }

            List<String> rpyIds = groupList.stream().map(RepositoryGroup::getRepository).map(Repository::getId).collect(Collectors.toList());
            String[] repositoryIds = rpyIds.toArray(new String[rpyIds.size()]);
            libraryList = libraryFileService.findFileByReAndLibraryAndVer(repositoryIds,libraryName,version);

        }else {
             libraryList = libraryFileService.findFileByReAndLibraryAndVer(repository.getId(), libraryName, version);
        }


        //制品文件不存在
        if (CollectionUtils.isEmpty(libraryList)){
            if(("remote").equals(repository.getRepositoryType())){
                String[]  remoteIds = new String[] {repository.getId()};

                //转发远程
                String s = forwardRemoteGetDownloadUrls(remoteIds, outData);
                return s;
            }
            if(("group").equals(repository.getRepositoryType())){
                List<String> remoteRepIds = groupList.stream()
                        .map(RepositoryGroup::getRepository)
                        .filter(aRepository -> ("remote").equals(aRepository.getRepositoryType()))
                        .map(Repository::getId)
                        .collect(Collectors.toList());

                String[] remoteIds = remoteRepIds.toArray(new String[remoteRepIds.size()]);

                //转发远程
                String s = forwardRemoteGetDownloadUrls(remoteIds, outData);
                return s;
            }

            //制品数据不存在
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }


        //制品文件存在
        List<LibraryFile> libraryFiles;
        if (requestURI.contains("/_/_/packages/")){
            //获取二进制文件
            libraryFiles = libraryList.stream().filter(a -> a.getFileName().startsWith("packages/")).collect(Collectors.toList());
        }else {
             //获取元数据
             libraryFiles = libraryList.stream().filter(a -> !a.getFileName().startsWith("packages/")).collect(Collectors.toList());
        }

        //拼接二进制文件的上传临时 URL
        Map<String, String> urls = new HashMap<>();
        for (LibraryFile libraryFile:libraryFiles){
            String fileName = libraryFile.getFileName();

            if (fileName.startsWith("packages/")){
                fileName = StringUtils.substringAfter(fileName, "/");
            }

            //将客户端请求的仓库名字替换为文件实际存放的仓库名
            String replace = path.replace(repName, libraryFile.getRepository().getName());
            if (requestURL.toString().contains("/_/_/packages/")){
                replace = replace+"/_/_/download_package/packages/" + fileName;
            }else {
                replace = replace+"/_/_/download_package/" + fileName;
            }
            urls.put(fileName,replace);
        }

        // 使用 Jackson 将结果转换为 JSON
        String convert = convertToJson(urls);
        return convert;
    }

    @Override
    public void downloadPackage(HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        logger.info("客户端拉取请求地址："+requestURI);

        //截取请求路径中的数据,获取仓库名字、制品名字、版本、文件名字
        Map<String, String> outData = cutOutData(requestURI,request.getRequestURL().toString());
        String repName = outData.get("repName");
        String libraryName = outData.get("libraryName");
        String version = outData.get("version");
        String fileName = StringUtils.substringAfterLast(requestURI, "/");


        //下载二进制包
        if (requestURI.contains("/_/_/download_package/packages")){
            fileName="packages/"+fileName;
        }

        Repository repository = repositoryService.findRepository(repName, "conan");

        //查询文件地址
        List<LibraryFile> libraryFileList = libraryFileService.findFileByReAndLibraryAndVer(repository.getId(), libraryName, version);
        String finalFileName = fileName;
        List<LibraryFile> libraryFiles = libraryFileList.stream().filter(a -> (finalFileName).equals(a.getFileName())).collect(Collectors.toList());

        String fileUrl = libraryFiles.get(0).getFileUrl();
        String filePath = yamlDataMaService.repositoryAddress() + "/" + fileUrl;

        try {
            File file = new File(filePath);
            if (!file.exists()){
                logger.info(libraryName+",文件内容不存在");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().print("The content of the file does not exist");
            }
            ConanResponse.readFileData(new File(filePath),response);
        } catch (IOException e) {
            logger.info(libraryName+",文件内容不存在");
            throw new RuntimeException(e);
        }
    }



    /**
     * conan拉取 -> 转发远程获取拉取地址
     * @param remoteReIds 远程库ids
     */
    public String forwardRemoteGetDownloadUrls(String[]  remoteReIds,Map<String,String> dataMap)  {

        String v1EndPath = dataMap.get("v1EndPath");
        List<RepositoryRemoteProxy> remoteProxyList = remoteProxyService.findAgencyByRpyIds(remoteReIds);

         for (RepositoryRemoteProxy remoteProxy:remoteProxyList){
            String agencyUrl = remoteProxy.getRemoteProxy().getAgencyUrl();

            try {
                String forwardPath = agencyUrl + v1EndPath;
                String template = RepositoryUtil.getRestTemplate(forwardPath);
                //替换请求地址
                String replaced = replaceTarball(template, remoteProxy.getRepository().getName());
                return replaced;


            } catch (Exception e) {
                String message = e.getMessage();

                //如果不存在、或者packageID不存在
                if (message.startsWith("404")){
                    // 获取二进制包（Binary Package）的下载链接
                    if (v1EndPath.contains("/_/_/packages")&&v1EndPath.endsWith("download_urls")){
                        Map<String, String> map = new HashMap<>();
                        String data = dataMap.get("libraryName") + "/" + dataMap.get("version") + ":" + dataMap.get("packageId");
                        map.put("Missing binary",data);

                        String convert = convertToJson(map);
                        return convert;
                    }
                }
                throw new RuntimeException(e);
            }
        }
        return null;
    }





    //替换请求地址
    public String replaceTarball(String entityBody,String beforePath){
        String centerPath="https://center.conan.io";

        String replaced = entityBody.replaceAll(centerPath, beforePath);

        return replaced;
    }

    /**
     * conan推送 -> 添加制品表数据
     * @param library 制品
     *  @param version 版本
     * @param fileName 文件名字
     * @param userName 用户名
     */
    public void addLibraryData(Library library,String version,String fileName,String userName){
        Repository repository = library.getRepository();
        String libraryName = library.getName();

        //创建版本
        LibraryVersion libraryVersion = new LibraryVersion();
        //libraryVersion.setHash("SHA256 "+ RepositoryUtil.getSHA256ByPath(filePath));
        libraryVersion.setPusher(userName);
        libraryVersion.setLibrary(library);
        libraryVersion.setVersion(version);
        libraryVersion.setRepository(repository);
        libraryVersion.setLibraryType("conan");
        String versionId = versionService.redactLibraryVersion(libraryVersion);
        libraryVersion.setId(versionId);



        /*---创建文件----*/
        //文件地址
        String filePath = yamlDataMaService.repositoryAddress() + "/" + repository.getId() + "/" + libraryName+"/"+version+"/"+fileName;
        File file = new File(filePath);

        LibraryFile libraryFile = new LibraryFile();
        libraryFile.setLibrary(library);
        libraryFile.setRepository(repository);
        libraryFile.setLibraryVersion(libraryVersion);
        libraryFile.setFileName(fileName);
        String size = RepositoryUtil.formatSize(file.length());
        libraryFile.setFileSize(size);
        libraryFile.setSize(file.length());
        libraryFile.setFileUrl(repository.getId() + "/" + libraryName.toLowerCase()+"/"+version+"/"+fileName.toLowerCase());
        libraryFile.setRelativePath(fileName.toLowerCase());
        libraryFileService.redactLibraryFile(libraryFile);
    }


    /**
     * 截取数据
     * @param requestURI requestURI
     */
    public Map<String, String> cutOutData(String requestURI,String requestURL){
        Map<String, String> hashMap = new HashMap<>();
        String[] split = requestURI.split("/v1/conans/");

        //仓库名字
        String[] frontSplit = split[0].split("/");
        String repName = frontSplit[2];

        //制品名字、版本
        String[] lastSplit = split[1].split("/");
        String libraryName = lastSplit[0];
        String version = lastSplit[1];
        String v1EndPath = requestURI.substring(requestURI.indexOf("/v1/"));

        String beforePath = StringUtils.substringBefore(requestURL, "/v1/");

        // 获取二进制包（Binary Package）的下载链接
        if (requestURI.contains("/_/_/packages")&&requestURI.endsWith("download_urls")){
            String substringAfter = StringUtils.substringAfter(requestURI, "/_/_/packages/");
            String packageId = StringUtils.substringBefore(substringAfter, "/download_urls");
            hashMap.put("packageId",packageId);
        }

        hashMap.put("beforePath",beforePath);
        hashMap.put("v1EndPath",v1EndPath);
        hashMap.put("repName",repName);
        hashMap.put("libraryName",libraryName);
        hashMap.put("version",version);

        return hashMap;
    }


    /**
     * 使用 Jackson 将结果转换为 JSON
     * @param data data
     */
    public String convertToJson(Map<String, String> data){
        ObjectMapper objectMapper1 = new ObjectMapper();
        try {
            String jsonResponse = objectMapper1.writeValueAsString(data);
            return jsonResponse;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
