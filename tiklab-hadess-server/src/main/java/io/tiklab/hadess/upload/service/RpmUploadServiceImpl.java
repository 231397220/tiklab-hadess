package io.tiklab.hadess.upload.service;

import io.tiklab.core.Result;
import io.tiklab.hadess.common.*;
import io.tiklab.hadess.library.model.Library;
import io.tiklab.hadess.library.model.LibraryFile;
import io.tiklab.hadess.library.model.LibraryVersion;
import io.tiklab.hadess.library.service.LibraryFileService;
import io.tiklab.hadess.library.service.LibraryService;
import io.tiklab.hadess.library.service.LibraryVersionService;
import io.tiklab.hadess.repository.model.*;
import io.tiklab.hadess.repository.service.RepositoryGroupService;
import io.tiklab.hadess.repository.service.RepositoryRemoteProxyService;
import io.tiklab.hadess.repository.service.RepositoryService;
import io.tiklab.hadess.upload.common.response.RpmResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RpmUploadServiceImpl implements RpmUploadService{

    private static Logger logger = LoggerFactory.getLogger(RpmUploadServiceImpl.class);

    @Autowired
    XpackYamlDataMaService yamlDataMaService;

    @Autowired
    UserCheckService userCheckService;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    RepositoryGroupService repositoryGroupService;

    @Autowired
    LibraryService libraryService;

    @Autowired
    LibraryVersionService libraryVersionService;

    @Autowired
    LibraryFileService libraryFileService;

    @Autowired
    RepositoryRemoteProxyService remoteProxyService;

    @Override
    public void rpmUpload(HttpServletRequest request, HttpServletResponse response) {
        String contextPath = request.getRequestURI();
        String repositoryPath = yamlDataMaService.getUploadRepositoryUrl(contextPath,"rpm");
        response.setCharacterEncoding("UTF-8");

        //校验用户信息
        String authorization = request.getHeader("Authorization");
        Result userCheckResult = userCheck(authorization);
        if (userCheckResult.getCode()==401){
            RpmResponse.uploadErrorToClient(response,userCheckResult.getMsg());
            return;
        }

        //写入数据
        LibraryFile libraryFile = uploadFileWriteData(request, response, repositoryPath, userCheckResult.getData().toString());

        //创建索引文件
        createIndexData(libraryFile);

    }

    @Override
    public void rpmDownload(HttpServletRequest request, HttpServletResponse response) {
        String contextPath = request.getRequestURI();
        String repositoryPath = yamlDataMaService.getUploadRepositoryUrl(contextPath,"rpm");

        //校验用户信息
        String authorization = request.getHeader("Authorization");
        Result userCheckResult = userCheck(authorization);
        if (userCheckResult.getCode()==401){
            RpmResponse.downloadErrorToClient(response,401,"用户校验失败");
            return;
        }

        String[] split = repositoryPath.split("/");
        String repName = split[0];
        String fileName = split[split.length - 1];

        Repository repository = repositoryService.findRepository(repName, "rpm");
        String repositoryType = repository.getRepositoryType();
        if (ObjectUtils.isEmpty(repository)){
            RpmResponse.downloadErrorToClient(response,404,"制品库不存在");
            return;
        }



        //客户端请求元数据文件(仓库索引文件)、读取软件包信息、文件列表
        if (contextPath.contains(repName+"/repodata")){
            String filePath = yamlDataMaService.repositoryAddress() + "/" + repository.getId() + "/repodata/" + fileName;
            readFileData(response,filePath);
            return;
        }


        LibraryFile libraryFile=null;
        String remoteRepoId=null;
        //组合库
        if (("group").equals(repositoryType)){
            List<RepositoryGroup> groupList = repositoryGroupService.findRepositoryGroupList(
                    new RepositoryGroupQuery().setRepositoryGroupId(repository.getId()));

            //组合库未关联本地、远程制品库
            if (CollectionUtils.isEmpty(groupList)){
                RpmResponse.downloadErrorToClient(response,404,"制品库不存在");
                return;
            }

            //获取制品文件是否存在
            List<String> rpyIds = groupList.stream().map(a -> a.getRepository().getId()).collect(Collectors.toList());
            String[] repoIds = rpyIds.toArray(new String[rpyIds.size()]);
            libraryFile = libraryFileService.findFileByRepoAndFileName(repoIds, fileName);

            //组合库制品文件不存在获取关联的代理库
            if (ObjectUtils.isEmpty(libraryFile)){
                //获取远程库
                List<RepositoryGroup> remoteRepo = groupList.stream().filter(a -> ("remote").equals(a.getRepository().getRepositoryType())).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(remoteRepo)){
                    RpmResponse.downloadErrorToClient(response,404,"制品不存在且组合哭未关联远程库");
                    return;
                }
                 remoteRepoId = remoteRepo.get(0).getRepository().getId();
            }
        }
        
        //代理库
        if (("remote").equals(repositoryType)){
            String[] repoIds = {repository.getId()};
            libraryFile = libraryFileService.findFileByRepoAndFileName(repoIds,fileName);
            remoteRepoId=repository.getId();
        }

        //本地库
        if (("local").equals(repositoryType)){
            String[] repoIds = {repository.getId()};
            libraryFile = libraryFileService.findFileByRepoAndFileName(repoIds,fileName);
            if (ObjectUtils.isEmpty(libraryFile)){
                RpmResponse.downloadErrorToClient(response,404,"制品不存在");
                return;
            }
        }



        //文件的相对路径
        String fileRelaPath = StringUtils.substringAfterLast(contextPath, repName);

        //获取文件信息
        if (ObjectUtils.isEmpty(libraryFile)){
            //客户端请求的地址为组合库或者远程库，制品不存在直接转发远程
            forwardRemote(response,remoteRepoId,fileRelaPath);

            logger.info("rpm拉取，制品不存在转发代理拉取："+fileName);
            downloadAddData(remoteRepoId,fileRelaPath,userCheckResult.getData().toString());
        }else {
            String filePath = yamlDataMaService.repositoryAddress() + "/" + libraryFile.getFileUrl();

            //文件内容在本地不存在
            if (!new File(filePath).exists()){
                if (("local").equals(repositoryType)){
                    RpmResponse.downloadErrorToClient(response,404,"制品不存在");
                    return;
                }
                logger.info("rpm拉取，制品文件file不存在转发代理拉取："+fileName);
                //客户端请求的地址为组合库或者远程库，制品不存在直接转发远程
                forwardRemote(response,remoteRepoId,fileRelaPath);

                downloadAddData(remoteRepoId,fileRelaPath,userCheckResult.getData().toString());
                return;
            }

            //读取内容返回给客户端
            logger.info("rpm拉取，制品存在本地拉取："+fileName);
            readFileData(response,filePath);
        }
    }


    /**
     * forwardRemote  转发远程代理拉取
     * @param repoId 制品库id
     */
    public void forwardRemote(HttpServletResponse response,String repoId,String fileRelaPath){
        List<RepositoryRemoteProxy> remoteProxyList = remoteProxyService.findAgencyByRepId(repoId);
        RemoteProxy remoteProxy = remoteProxyList.get(0).getRemoteProxy();

        String filePath = remoteProxy.getAgencyUrl() + fileRelaPath;
        //制品存储本地地址
        String localStorePath = yamlDataMaService.repositoryAddress() + "/" + repoId + fileRelaPath;

        try {
            //转发到远程
            FileUtil.restTemplateGetByte(response,filePath,localStorePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * readFileData  读取文件内容
     * @param filePath 制品库id
     */
    public void readFileData(HttpServletResponse response,String filePath){
        try {
            File file = new File(filePath);
            if (!file.exists()){
                RpmResponse.downloadErrorToClient(response,404,"文件不存在");
                return;
            }
            FileUtil.readFileData(file,response);
        } catch (IOException e) {
            RpmResponse.downloadErrorToClient(response,500,"拉取失败");
            throw new RuntimeException(e);
        }
    }


    /**
     * 校验用户信息
     * @param authorization  客户端上传的用户信息
     */
    public Result userCheck(String authorization) {

        //docker第一次访问没有用户信息 为了获取支持的验证机制
        if (ObjectUtils.isEmpty(authorization)){
            logger.info("pypi拉取推送没有用户信息");
            return  Result.error(401,"{code:401,msg:用户信息不存在}");
        }

        try {
            String basic = authorization.replace("Basic", "").trim();
            byte[] decode = Base64.getDecoder().decode(basic);
            //用户信息
            String userData = new String(decode, "UTF-8");
            String[] split = userData.split(":");
            String userName = split[0];
            String password = split[1];

            //generic制品库里面上传 制品
            if (("xpackhand").equals(password)){
                return Result.ok(userName);
            }

            userCheckService.basicsUserCheck(userData);
            return Result.ok(userName);
        }catch (Exception e){
            return  Result.error(401,e.getMessage());
        }
    }


    /**
     * fileWriteData  写入文件
     * @param userName 用户
     * @param repositoryPath 制品库
     */
    public LibraryFile uploadFileWriteData(HttpServletRequest request,
                              HttpServletResponse response,
                              String repositoryPath,
                              String userName )  {

        try {
            InputStream inputStream = request.getInputStream();

            //获取制品库名字和文件名
            String[] split = repositoryPath.split("/");
            String repName = split[0];
            String fileName = split[1];

            Repository repository = repositoryService.findRepository(repName, "rpm");
            if (ObjectUtils.isEmpty(repository)){
                RpmResponse.uploadErrorToClient(response,"制品库不存在");
                return null;
            }

            //获取版本
            String beforeLast = StringUtils.substringBeforeLast(fileName, ".rpm");
            Pattern pattern = Pattern.compile(".*?-([0-9].*)");
            Matcher matcher = pattern.matcher(beforeLast);
            String version=null;
            if (matcher.find()) {
                version = matcher.group(1);
            }else {
                version = StringUtils.substringAfterLast(beforeLast, "-");
            }

            //制品名字
            String libraryName = StringUtils.substringBeforeLast(fileName, "-"+version);


            //写入文件
            String folderPath = yamlDataMaService.repositoryAddress() + "/" +repository.getId()+HadessFinal.RPM_PACKAGE;
            FileUtil.copyFileData(inputStream,folderPath,fileName);

            //文件大小
            String filePath = folderPath + "/" + fileName;
            File fileData = new File(filePath);
            long FileLength = fileData.length();
            String size = RepositoryUtil.formatSize(FileLength);

            //创建制品
            Library library = libraryService.createLibraryData(libraryName,"rpm",repository);

            //制品版本创建、修改
            LibraryVersion libraryVersion = new LibraryVersion();
            libraryVersion.setPusher(userName);
            libraryVersion.setLibrary(library);
            libraryVersion.setVersion(version);
            libraryVersion.setRepository(repository);
            libraryVersion.setSize(Long.valueOf(FileLength));
            libraryVersion.setLibraryType("rpm");
            String libraryVersionId = libraryVersionService.redactLibraryVersion(libraryVersion);
            libraryVersion.setId(libraryVersionId);

            //创建制品文件
            LibraryFile libraryFile = new LibraryFile();
            libraryFile.setRepository(repository);
            libraryFile.setLibrary(library);
            libraryFile.setLibraryVersion(libraryVersion);
            libraryFile.setFileName(fileName);
            libraryFile.setFileUrl(repository.getId()+ HadessFinal.RPM_PACKAGE+"/"+fileName);
            libraryFile.setRelativePath(fileName);
            libraryFile.setFileSize(size);
            libraryFile.setSize(FileLength);
            libraryFileService.redactLibraryFile(libraryFile);

            return libraryFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    /**
     * downloadAddData  下载数据添加数据
     * @param userName 用户
     * @param fileRelaPath 客户端请求地址
     */
    public void downloadAddData(String repoId,
                                  String fileRelaPath,
                                  String userName){

        String fileName=fileRelaPath;
        if (fileRelaPath.contains("/")){
            fileName=StringUtils.substringAfterLast(fileRelaPath,"/");
        }

        //获取版本
        String beforeLast = StringUtils.substringBeforeLast(fileName, ".rpm");
        Pattern pattern = Pattern.compile(".*?-([0-9].*)");
        Matcher matcher = pattern.matcher(beforeLast);
        String version=null;
        if (matcher.find()) {
            version = matcher.group(1);
        }else {
            version = StringUtils.substringAfterLast(beforeLast, "-");
        }

        //制品名字
        String libraryName = StringUtils.substringBeforeLast(fileName, "-"+version);

        //创建制品
        Repository repository = new Repository();
        repository.setId(repoId);
        Library library = libraryService.createLibraryData(libraryName,"rpm",repository);


        //获取本地文件存储文件大小
        String localStorePath = yamlDataMaService.repositoryAddress() + "/" + repoId + fileRelaPath;
        File fileData = new File(localStorePath);
        long FileLength = fileData.length();
        String size = RepositoryUtil.formatSize(FileLength);


        //制品版本创建、修改
        LibraryVersion libraryVersion = new LibraryVersion();
        libraryVersion.setPusher(userName);
        libraryVersion.setLibrary(library);
        libraryVersion.setVersion(version);
        libraryVersion.setRepository(repository);
        libraryVersion.setSize(Long.valueOf(FileLength));
        libraryVersion.setLibraryType("rpm");
        String libraryVersionId = libraryVersionService.redactLibraryVersion(libraryVersion);
        libraryVersion.setId(libraryVersionId);

        //创建制品文件
        LibraryFile libraryFile = new LibraryFile();
        libraryFile.setRepository(repository);
        libraryFile.setLibrary(library);
        libraryFile.setLibraryVersion(libraryVersion);
        libraryFile.setFileName(fileName);
        libraryFile.setFileUrl(repository.getId()+ fileRelaPath);
        libraryFile.setRelativePath(fileName);
        libraryFile.setFileSize(size);
        libraryFile.setSize(FileLength);
        libraryFileService.redactLibraryFile(libraryFile);

    }





    /**
     * createIndexData  创建索引文件
     * @param libraryFile libraryFile
     */
    public void createIndexData(LibraryFile libraryFile){
        String repoId = libraryFile.getRepository().getId();
        String libraryName = libraryFile.getLibrary().getName();
        String fileName = libraryFile.getFileName();
        String version = libraryFile.getLibraryVersion().getVersion();



        //rmp包路径
        String rmpPackagePath = yamlDataMaService.repositoryAddress() + "/" +repoId+HadessFinal.RPM_PACKAGE+"/"+fileName;
        String shaed = RepositoryUtil.sha256EncryptionPath(rmpPackagePath);


        String rel=null;
        if (version.contains("-")){
            String[] split = version.split("-");
            version=split[0];
            rel=split[1];
        }

        createFileListsXml(libraryFile,shaed,version,rel);


    }



    /**
     * createFileListsXml  创建索引文件的FileListsXml
     * @param libraryFile libraryFile
     */
    public void  createFileListsXml(LibraryFile libraryFile,String shaed,
                                    String version,String rel){
        String repoId = libraryFile.getRepository().getId();

        try {
            String repoPath = yamlDataMaService.repositoryAddress() + "/" + repoId;
            String filePath = repoPath + HadessFinal.RPM_FILE_PATH;

            // 读取XML文件
            File xmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);

            // 获取根元素
            Element rootElement = doc.getDocumentElement();

            // 创建 package 元素
            Element packageElement = doc.createElement("package");
            packageElement.setAttribute("pkgid", shaed);
            packageElement.setAttribute("name", libraryFile.getLibrary().getName());
            packageElement.setAttribute("arch", "x86_64");


            // 创建 version 元素
            Element versionElement = doc.createElement("version");
            versionElement.setAttribute("epoch", "0");

            versionElement.setAttribute("ver", version);
            if (!ObjectUtils.isEmpty(rel)){
                versionElement.setAttribute("rel", rel);
            }
            packageElement.appendChild(versionElement);

            // 创建 file 元素
          /*  Element fileElement = doc.createElement("file");
            fileElement.setTextContent("/etc/dbus-1/system.d/org.freedesktop.ModemManager1.conf");
            packageElement.appendChild(fileElement);*/

            // 将 package 元素添加到根元素
            rootElement.appendChild(packageElement);

            // 更新 packages 计数
            updatePackageCount(rootElement, 1);


            saveDocument(doc, filePath);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void  createPrimaryXml(LibraryFile libraryFile,String shaed,
                                  String version,String rel){

        String repoId = libraryFile.getRepository().getId();
        String libraryName = libraryFile.getLibrary().getName();

        try {
            String repoPath = yamlDataMaService.repositoryAddress() + "/" + repoId;
            String filePath = repoPath + HadessFinal.RPM_PRIMARY_PATH;

            // 读取XML文件
            File xmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);

            // 获取根元素
            Element rootElement = doc.getDocumentElement();

            // 创建 package 元素
            Element packageElement = doc.createElement("package");
            packageElement.setAttribute("type", "rpm");

            // 创建 name元素
            Element nameElement = doc.createElement("name");
            nameElement.setTextContent(libraryName);
            packageElement.appendChild(nameElement);

            //创建arch 元素
            Element archElement = doc.createElement("arch");
            archElement.setTextContent("x86_64");
            packageElement.appendChild(archElement);

            //创建version 元素
            Element versionElement = doc.createElement("version");
            versionElement.setAttribute("epoch", "0");
            versionElement.setAttribute("ver", version);
            if (!ObjectUtils.isEmpty(rel)){
                versionElement.setAttribute("rel", rel);
            }
            packageElement.appendChild(versionElement);

            //创建checksum元素
            Element checksumElement = doc.createElement("checksum");
            checksumElement.setAttribute("type","sha256");
            checksumElement.setAttribute("pkgid","YES");
            checksumElement.setTextContent(shaed);
            packageElement.appendChild(checksumElement);


            // 将 package 元素添加到根元素
            rootElement.appendChild(packageElement);

            // 更新 packages 计数
            updatePackageCount(rootElement, 1);


            saveDocument(doc, filePath);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    /*
    * 添加内容到xml中
    * */
    private static void saveDocument(Document doc, String filePath) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(filePath));
        transformer.transform(source, result);
    }

    /**
     * 更新 packages 计数
     */
    private static void updatePackageCount(Element rootElement, int increment) {
        String currentCountStr = rootElement.getAttribute("packages");
        int currentCount;

        try {
            currentCount = Integer.parseInt(currentCountStr);
        } catch (NumberFormatException e) {
            currentCount = 0;
        }

        rootElement.setAttribute("packages", String.valueOf(currentCount + increment));
    }
}
