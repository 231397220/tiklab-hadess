package io.tiklab.hadess.repository.service;

import io.tiklab.dal.jpa.criterial.condition.DeleteCondition;
import io.tiklab.dal.jpa.criterial.conditionbuilder.DeleteBuilders;
import io.tiklab.hadess.common.FileUtil;
import io.tiklab.hadess.common.HadessFinal;
import io.tiklab.hadess.common.RepositoryUtil;
import io.tiklab.hadess.common.XpackYamlDataMaService;
import io.tiklab.hadess.repository.model.RemoteProxy;
import io.tiklab.hadess.repository.model.RemoteProxyQuery;
import io.tiklab.hadess.repository.model.RepositoryRemoteProxy;
import io.tiklab.hadess.repository.model.RepositoryRemoteProxyQuery;
import io.tiklab.hadess.repository.dao.RepositoryRemoteProxyDao;
import io.tiklab.hadess.repository.entity.RepositoryRemoteProxyEntity;
import io.tiklab.toolkit.beans.BeanMapper;
import io.tiklab.core.page.Pagination;
import io.tiklab.core.page.PaginationBuilder;
import io.tiklab.toolkit.join.JoinTemplate;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

/**
* RepositoryRemoteProxyServiceImpl-远程库代理信息
*/
@Service
public class RepositoryRemoteProxyServiceImpl implements RepositoryRemoteProxyService {

    @Autowired
    RepositoryRemoteProxyDao repositoryRemoteProxyDao;

    @Autowired
    JoinTemplate joinTemplate;

    @Autowired
    RemoteProxyServiceImpl remoteProxyService;
    
    @Autowired
    XpackYamlDataMaService yamlDataMaService;

    @Override
    public String createRepositoryRemoteProxy(@NotNull @Valid RepositoryRemoteProxy repositoryRemoteProxy) {
        RepositoryRemoteProxyEntity repositoryRemoteProxyEntity = BeanMapper.map(repositoryRemoteProxy, RepositoryRemoteProxyEntity.class);
        repositoryRemoteProxyEntity.setCreateTime(new Timestamp(System.currentTimeMillis()));

        //更新
        if (("update").equals(repositoryRemoteProxy.getExecType())){
            DeleteCondition deleteCondition = DeleteBuilders.createDelete(RepositoryRemoteProxyEntity.class)
                    .eq("repositoryId", repositoryRemoteProxy.getRepository().getId())
                    .get();
            repositoryRemoteProxyDao.deleteRepositoryRemoteProxy(deleteCondition);
            List<String> remoteProxyIds = repositoryRemoteProxy.getRemoteProxyIds();
            for (String proxyId:remoteProxyIds){
                repositoryRemoteProxyEntity.setRemoteProxyId(proxyId);
                repositoryRemoteProxyDao.createRepositoryRemoteProxy(repositoryRemoteProxyEntity);
            }
            return null;
        }

        String remoteProxy = repositoryRemoteProxyDao.createRepositoryRemoteProxy(repositoryRemoteProxyEntity);

        //制品库类型为rmp时同步代理制品索引文件
        String type = repositoryRemoteProxy.getRepository().getType();
        if (("rpm").equals(type)){
            syncRpmIndex(repositoryRemoteProxy.getRepository().getId(),repositoryRemoteProxy.getRemoteProxy().getId());
        }

        return remoteProxy;
    }

    @Override
    public void updateRepositoryRemoteProxy(@NotNull @Valid RepositoryRemoteProxy repositoryRemoteProxy) {
        RepositoryRemoteProxyEntity repositoryRemoteProxyEntity = BeanMapper.map(repositoryRemoteProxy, RepositoryRemoteProxyEntity.class);
        repositoryRemoteProxyDao.updateRepositoryRemoteProxy(repositoryRemoteProxyEntity);
    }

    @Override
    public void deleteRepositoryRemoteProxy(@NotNull String id) {
        repositoryRemoteProxyDao.deleteRepositoryRemoteProxy(id);
    }

    @Override
    public void deleteRepositoryRemoteProxy(String type, String value) {
        DeleteCondition deleteCondition = DeleteBuilders.createDelete(RepositoryRemoteProxyEntity.class)
                .eq(type, value)
                .get();
        repositoryRemoteProxyDao.deleteRepositoryRemoteProxy(deleteCondition);
    }

    @Override
    public RepositoryRemoteProxy findOne(String id) {
        RepositoryRemoteProxyEntity repositoryRemoteProxyEntity = repositoryRemoteProxyDao.findRepositoryRemoteProxy(id);

        RepositoryRemoteProxy repositoryRemoteProxy = BeanMapper.map(repositoryRemoteProxyEntity, RepositoryRemoteProxy.class);
        return repositoryRemoteProxy;
    }

    @Override
    public List<RepositoryRemoteProxy> findList(List<String> idList) {
        List<RepositoryRemoteProxyEntity> repositoryRemoteProxyEntityList =  repositoryRemoteProxyDao.findRepositoryRemoteProxyList(idList);

        List<RepositoryRemoteProxy> repositoryRemoteProxyList =  BeanMapper.mapList(repositoryRemoteProxyEntityList,RepositoryRemoteProxy.class);
        return repositoryRemoteProxyList;
    }

    @Override
    public RepositoryRemoteProxy findRepositoryRemoteProxy(@NotNull String id) {
        RepositoryRemoteProxy repositoryRemoteProxy = findOne(id);

        joinTemplate.joinQuery(repositoryRemoteProxy,new String[]{"repository","remoteProxy"});

        return repositoryRemoteProxy;
    }

    @Override
    public List<RepositoryRemoteProxy> findAllRepositoryRemoteProxy() {
        List<RepositoryRemoteProxyEntity> repositoryRemoteProxyEntityList =  repositoryRemoteProxyDao.findAllRepositoryRemoteProxy();

        List<RepositoryRemoteProxy> repositoryRemoteProxyList =  BeanMapper.mapList(repositoryRemoteProxyEntityList,RepositoryRemoteProxy.class);

        joinTemplate.joinQuery(repositoryRemoteProxyList,new String[]{"repository","remoteProxy"});

        return repositoryRemoteProxyList;
    }

    @Override
    public List<RepositoryRemoteProxy> findRepositoryRemoteProxyList(RepositoryRemoteProxyQuery repositoryRemoteProxyQuery) {
        List<RepositoryRemoteProxyEntity> repositoryRemoteProxyEntityList = repositoryRemoteProxyDao.findRepositoryRemoteProxyList(repositoryRemoteProxyQuery);

        List<RepositoryRemoteProxy> repositoryRemoteProxyList = BeanMapper.mapList(repositoryRemoteProxyEntityList,RepositoryRemoteProxy.class);

        joinTemplate.joinQuery(repositoryRemoteProxyList,new String[]{"repository","remoteProxy"});

        return repositoryRemoteProxyList;
    }

    @Override
    public Pagination<RepositoryRemoteProxy> findRepositoryRemoteProxyPage(RepositoryRemoteProxyQuery repositoryRemoteProxyQuery) {
        Pagination<RepositoryRemoteProxyEntity>  pagination = repositoryRemoteProxyDao.findRepositoryRemoteProxyPage(repositoryRemoteProxyQuery);

        List<RepositoryRemoteProxy> repositoryRemoteProxyList = BeanMapper.mapList(pagination.getDataList(),RepositoryRemoteProxy.class);



        return PaginationBuilder.build(pagination,repositoryRemoteProxyList);
    }

    @Override
    public List<RepositoryRemoteProxy> findAgencyByRepId(String repositoryId) {
        List<RepositoryRemoteProxyEntity> remoteProxyEntities = repositoryRemoteProxyDao.findRemoteProxyRepId(repositoryId);

        List<RepositoryRemoteProxy> repositoryRemoteProxyList = BeanMapper.mapList(remoteProxyEntities,RepositoryRemoteProxy.class);
        joinTemplate.joinQuery(repositoryRemoteProxyList,new String[]{"repository","remoteProxy"});

        return repositoryRemoteProxyList;
    }

    @Override
    public RepositoryRemoteProxy findAgencyByRpyIdAndPath(String[] repositoryIds,String path) {
        List<RemoteProxy> remoteProxyList = remoteProxyService.findRemoteProxyList(new RemoteProxyQuery().setAgencyUrl(path));
        if (CollectionUtils.isNotEmpty(remoteProxyList)){
            String id = remoteProxyList.get(0).getId();
            List<RepositoryRemoteProxyEntity> agencyByRpyIdAndPath = repositoryRemoteProxyDao.findAgencyByRpyIdAndPath(repositoryIds, id);
            List<RepositoryRemoteProxy> repositoryRemoteProxyList = BeanMapper.mapList(agencyByRpyIdAndPath,RepositoryRemoteProxy.class);
            if (CollectionUtils.isNotEmpty(repositoryRemoteProxyList)){
                RepositoryRemoteProxy repositoryRemoteProxy = repositoryRemoteProxyList.get(0);
                joinTemplate.joinQuery(repositoryRemoteProxyList,new String[]{"repository","remoteProxy"});
                return repositoryRemoteProxy;
            }
        }

        return null;
    }

    @Override
    public List<RepositoryRemoteProxy> findAgencyByRpyIds(String[] repositoryIds) {
        List<RepositoryRemoteProxyEntity> agencyList = repositoryRemoteProxyDao.findAgencyByRpyIds(repositoryIds);

        List<RepositoryRemoteProxy> proxyList = BeanMapper.mapList(agencyList,RepositoryRemoteProxy.class);

        joinTemplate.joinQuery(proxyList,new String[]{"repository","remoteProxy"});


        return proxyList;
    }



    //同步rpm索引文件
    @Override
    public void syncRpmIndex(String repoId,String remoteId){
        RemoteProxy proxy = remoteProxyService.findOne(remoteId);

        try {

            String path = proxy.getAgencyUrl() + HadessFinal.REPO_MD_PATH;
            String restTemplate = RepositoryUtil.getRestTemplate(path);

            //本地存储位置
            String repoPath = yamlDataMaService.repositoryAddress() + "/" + repoId;
            String indexFilePath = repoPath+HadessFinal.REPO_MD_PATH;

            //写入索引文件
            FileUtil.writeStringToFile(restTemplate,indexFilePath);

            //写入软件包信息、文件列表、软件包分组等
            File xmlFile = new File(indexFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            Element rootElement = doc.getDocumentElement();
            NodeList dataList = rootElement.getElementsByTagName("data");
            for (int i = 0; i < dataList.getLength(); i++) {
                Node dataItems = dataList.item(i);
                Element dataElement = (Element) dataItems;
                NodeList locationList =dataElement.getElementsByTagName("location");
                Element locationItem = (Element)locationList.item(0);
                String href = locationItem.getAttribute("href");

                String filePath = proxy.getAgencyUrl() + "/"+href;
                ResponseEntity<byte[]> fileData = RepositoryUtil.getRestTemplateByte(filePath);
                byte[] dataBody = fileData.getBody();

                String localFilePath = repoPath+"/"+href;
                FileUtil.writeByteToFile(dataBody,localFilePath);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}