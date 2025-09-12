package io.tiklab.hadess.leadIn.service;

import com.alibaba.fastjson.JSONObject;
import io.tiklab.core.exception.SystemException;
import io.tiklab.hadess.common.HadessFinal;
import io.tiklab.hadess.common.RepositoryUtil;
import io.tiklab.hadess.common.XpackYamlDataMaService;
import io.tiklab.hadess.leadIn.model.LeadInQuery;
import io.tiklab.hadess.library.model.Library;
import io.tiklab.hadess.library.model.LibraryFile;
import io.tiklab.hadess.library.model.LibraryMaven;
import io.tiklab.hadess.library.model.LibraryVersion;
import io.tiklab.hadess.library.service.LibraryFileService;
import io.tiklab.hadess.library.service.LibraryMavenService;
import io.tiklab.hadess.library.service.LibraryService;
import io.tiklab.hadess.library.service.LibraryVersionService;
import io.tiklab.hadess.repository.model.Repository;
import io.tiklab.hadess.repository.service.RepositoryMavenService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeadInLibraryServiceImpl implements LeadInLibraryService {

    @Autowired
    XpackYamlDataMaService yamlDataMaService;

    @Autowired
    LibraryService libraryService;

    @Autowired
    LibraryVersionService libraryVersionService;

    @Autowired
    LibraryMavenService libraryMavenService;

    @Autowired
    LibraryFileService libraryFileService;

    @Autowired
    RepositoryMavenService repositoryMavenService;

    public static Map<String , Boolean> leadInState = new HashMap<>();

    @Override
    public List<LinkedHashMap> findRepositoryList(LeadInQuery leadInQuery) {
        String queryUrl = leadInQuery.getUrl();
        if (!queryUrl.endsWith("/")){
            queryUrl=queryUrl+"/";
        }

        //仓库类型
        String type = leadInQuery.getType();
        if (("maven").equals(type)){
            leadInQuery.setType("maven2");
        }

        String findRepPath = queryUrl + HadessFinal.NEXUS_FIND_REP;
        try {
            List<LinkedHashMap> restTemplateList = RepositoryUtil.getRestTemplateList(findRepPath);


            //只获取对应类型的本地库
            List<LinkedHashMap> linkedHashMaps = restTemplateList.stream().filter(a -> leadInQuery.getType().equals(a.get("format")) &&
                    ("hosted").equals(a.get("type"))).collect(Collectors.toList());

         /*   if (StringUtils.isNotBlank(leadInQuery.getVersionType())){
                linkedHashMaps.stream().filter(b->leadInQuery.getVersionType().equals(b.get("type")))
            }*/
            return  linkedHashMaps;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void leadInRepLibrary(LeadInQuery leadInQuery) {
        String targetRepId = leadInQuery.getTargetRepId();
        String type = leadInQuery.getType();

        leadInState.put(targetRepId,true);

        Thread thread = new Thread() {
            public void run() {
                String queryUrl = leadInQuery.getUrl();
                if (!queryUrl.endsWith("/")){
                    queryUrl=queryUrl+"/";
                }

                //获取制品文件地址
                String filePath = queryUrl + HadessFinal.NEXUS_FIND_LIBRARY_FILE+"?repository="+leadInQuery.getSourceRepName();

                //获取制品地址
                String libraryPath = queryUrl + HadessFinal.NEXUS_FIND_LIBRARY+"?repository="+leadInQuery.getSourceRepName();
                try {
                    //获取制品信息
                    getNexusLibrary(leadInQuery,libraryPath);

                    //maven存在	maven-metadata.xml校验数据
                    if (type.equals("maven")){
                        List<LibraryVersion> versionList = libraryVersionService.findLibraryVersionByRepId(targetRepId);
                        List<LibraryMaven> libraryMavenList = libraryMavenService.findLibraryMavenByRepId(targetRepId);

                        getNexusLibraryMetadata(leadInQuery,filePath,versionList,libraryMavenList);
                    }
                    leadInState.put(targetRepId,false);
                } catch (Exception e) {
                    leadInState.put(targetRepId,false);
                    throw new RuntimeException(e);
                }

            }};
        thread.start();
    }

    @Override
    public Boolean findLeadInState(String targetRepId) {
        Boolean aBoolean = leadInState.get(targetRepId);

        return aBoolean;
    }

    /**
     * 获取nexus的制品列表并写入
     * @param leadInQuery leadInQuery
     * @param libraryPath  查询制品的地址
     */
    public void getNexusLibrary(LeadInQuery leadInQuery,String libraryPath ){

        JSONObject templateObject = RepositoryUtil.getRestTemplateObject(libraryPath);
        Object items = templateObject.get("items");
        if (!ObjectUtils.isEmpty(items)){
            List<LinkedHashMap> libraryList = (ArrayList) items;
            for (LinkedHashMap library:libraryList){

                //添加数据
               addLibraryData(leadInQuery,library);
            }

            //API分页查询。存在continuationToken，代表有下一页
            Object continuationToken = templateObject.get("continuationToken");
            if (!ObjectUtils.isEmpty(continuationToken)){
                String substringBefore = StringUtils.substringBefore(libraryPath, "&continuationToken=");
                String path = substringBefore + "&continuationToken=" + continuationToken;
                getNexusLibrary(leadInQuery,path);
            }
        }
    }

    /**
     * 获取nexus的制品文件列表并写入
     * @param leadInQuery leadInQuery
     * @param findRepPath findRepPath 查询制品的地址
     */
    public void getNexusLibraryMetadata(LeadInQuery leadInQuery,
                                        String findRepPath,
                                        List<LibraryVersion> versionList,
                                        List<LibraryMaven> libraryMavenList)  {

        String targetRepId = leadInQuery.getTargetRepId();
        JSONObject templateObject = RepositoryUtil.getRestTemplateObject(findRepPath);
        Object items = templateObject.get("items");
        if (!ObjectUtils.isEmpty(items)){

            List<LinkedHashMap> libraryHashList = (ArrayList) items;
            //过滤只存储maven-metadata.xml 的数据
            List<LinkedHashMap> linkedHashMaps = libraryHashList.stream().filter(a -> a.get("path").toString().contains("maven-metadata.xml"))
                    .collect(Collectors.toList());
            for (LinkedHashMap libraryMap:linkedHashMaps){
                String path = libraryMap.get("path").toString();
                if (path.startsWith(".index/")){
                    continue;
                }
                if (path.startsWith("archetype-catalog.xml")){
                    continue;
                }

                //文件名字
                String fileName = StringUtils.substringAfterLast(path, "/");
                if (!fileName.startsWith("maven-metadata.xml")){
                    continue;
                }

                //快照版本添加表数据
                if ((("snapshot").equals(leadInQuery.getVersionType().toLowerCase()))){
                    addMetadataData(libraryMap,leadInQuery,versionList,libraryMavenList);
                }

                //写入数据
                writeData(libraryMap,targetRepId);
            }

            //API分页查询。存在continuationToken，代表有下一页
            Object continuationToken = templateObject.get("continuationToken");
            if (!ObjectUtils.isEmpty(continuationToken)){
                String substringBefore = StringUtils.substringBefore(findRepPath, "&continuationToken=");
                String path = substringBefore + "&continuationToken=" + continuationToken;
                getNexusLibraryMetadata(leadInQuery,path,versionList,libraryMavenList);
            }
        }
    }



    /**
     * 添加制品数据到数据库中
     * @param libraryMap libraryMap
     * @param  leadInQuery leadInQuery
     */
    public void  addLibraryData(LeadInQuery leadInQuery,LinkedHashMap libraryMap)  {
        String repId = leadInQuery.getTargetRepId();
        String type = leadInQuery.getType();

        Repository repository = new Repository();
        repository.setId(repId);
        String libraryName = libraryMap.get("name").toString();

        Library library;
        String version;
        String snapshotVersion=null;
        if (type.equals("maven")){
            String groupId = libraryMap.get("group").toString();
            version = libraryMap.get("version").toString();

            //maven类型的快照版
            if (("snapshot").equals(leadInQuery.getVersionType().toLowerCase())){
                if (version.endsWith("-SNAPSHOT")){
                    return;
                }
                String beforeLast = StringUtils.substringBeforeLast(version, ".");
                String ver = StringUtils.substringBeforeLast(beforeLast, "-");
                snapshotVersion = StringUtils.substringAfterLast(version, ver + "-");
                version=ver+"-SNAPSHOT";
            }

            //创建制品
             library = libraryService.createMvnLibrary(repository, libraryName, groupId);
             //创建制品maven数据
            libraryMavenService.libraryMavenSplice(libraryName,groupId,library);
        }else {
            version = libraryMap.get("version").toString();
            library= libraryService.createLibraryData(libraryName,type,repository);
        }

        //制品maven  创建、更新
        LibraryVersion libraryVersion = new LibraryVersion();
        libraryVersion.setLibrary(library);
        libraryVersion.setRepository(repository);
        libraryVersion.setVersion(version);
        libraryVersion.setLibraryType(type);
        libraryVersion.setPusher("nexus");
        String libraryVersionId = libraryVersionService.redactLibraryVersion(libraryVersion);
        libraryVersion.setId(libraryVersionId);

        //获取文件
        List<LinkedHashMap> fileList =(ArrayList) libraryMap.get("assets");
        for (LinkedHashMap file:fileList){
            //写入数据
            writeData(file,repId);

            //创建文件数据
            Object pathObj = file.get("path");
            if (!ObjectUtils.isEmpty(pathObj)){
                String path = pathObj.toString();

                Object sizeObj = file.get("fileSize");
                String fileSize = ObjectUtils.isEmpty(sizeObj) ? "1" : file.get("fileSize").toString();

                String fileName = StringUtils.substringAfterLast(path, "/");
                //制品文件 创建、更新
                LibraryFile libraryFile = new LibraryFile();
                libraryFile.setRepository(repository);
                libraryFile.setLibrary(library);
                libraryFile.setLibraryVersion(libraryVersion);
                libraryFile.setSnapshotVersion(snapshotVersion);

                libraryFile.setFileName(fileName);
                String size = RepositoryUtil.formatSize(Long.valueOf(fileSize));
                libraryFile.setFileSize(size);
                libraryFile.setSize(Long.valueOf(fileSize));
                libraryFile.setFileUrl(repId+"/"+path);
                libraryFile.setRelativePath(path);
                libraryFileService.redactLibraryFile(libraryFile);
            }
        }
    }

    /**
     * 添加制品数据到数据库中
     * @param libraryMap libraryMap
     */
    public void  addMetadataData(LinkedHashMap libraryMap,
                                 LeadInQuery leadInQuery,
                                 List<LibraryVersion> versionList,
                                 List<LibraryMaven> libraryMavenList){
        String repId = leadInQuery.getTargetRepId();
        String path = libraryMap.get("path").toString();


        //解析path获取artifactId、groupId
        String beforePath = StringUtils.substringBeforeLast(path, "/");
        String beforeLast = StringUtils.substringBeforeLast(beforePath, "/");
        String version = StringUtils.substringAfterLast(beforePath, "/");

        //如果是制品的maven-metadata 数据直接返回，版本的maven-metadata 数据才添加
        if (!version.endsWith("-SNAPSHOT")){
            return;
        }

        String artifactId = StringUtils.substringAfterLast(beforeLast, "/");
        String substringed = StringUtils.substringBeforeLast(beforeLast, "/");
        String groupId=substringed.replaceAll("/", ".");
        List<LibraryMaven> libraryMavens = libraryMavenList.stream().filter(a -> (groupId).equals(a.getGroupId())
                && (artifactId).equals(a.getArtifactId())).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(libraryMavens)){
            String libraryId = libraryMavens.get(0).getLibraryId();
            List<LibraryVersion> versions = versionList.stream().filter(a -> (version).equals(a.getVersion()) && (libraryId).equals(a.getLibrary().getId())).
                    collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(versions)){

                LibraryVersion libraryVersion = versions.get(0);
                Repository repository = new Repository();
                repository.setId(repId);
                Library library = new Library();
                library.setId(libraryId);

                //制品文件 创建、更新
                LibraryFile libraryFile = new LibraryFile();
                libraryFile.setRepository(repository);
                libraryFile.setLibrary(library);
                libraryFile.setLibraryVersion(libraryVersion);

                //文件名
                String fileName = StringUtils.substringAfterLast(path, "/");
                String fileSize = libraryMap.get("fileSize").toString();

                //写入数据
                writeData(libraryMap,repId);

                libraryFile.setFileName(fileName);
                String size = RepositoryUtil.formatSize(Long.valueOf(fileSize));
                libraryFile.setFileSize(size);
                libraryFile.setSize(Long.valueOf(fileSize));
                libraryFile.setFileUrl(repId+"/"+path);
                libraryFile.setRelativePath(path);
                libraryFileService.redactLibraryFile(libraryFile);
            }
        }
    }


    //写入内容到文件
    public void writeData(LinkedHashMap file,String targetRepId)  {

        try {
            String path = file.get("path").toString();

            //转发获取数据
            String downloadUrl = file.get("downloadUrl").toString();
            ResponseEntity<byte[]> restTemplateByte = RepositoryUtil.getRestTemplateByte(downloadUrl);

            //文件存储位置
            String filePath = yamlDataMaService.repositoryAddress() + "/"+targetRepId +"/"+path;
            Path storagePath = Paths.get(filePath);
            // 1. 确保父目录存在
            Path parentDir = storagePath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            byte[] body = restTemplateByte.getBody();
            // 2. 写入数据 (默认覆盖模式)
            Files.write(storagePath, body);
        }catch (Exception e){
            throw new SystemException(e.getMessage());
        }
    }
}
