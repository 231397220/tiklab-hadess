package io.tiklab.hadess.repository.service;

import io.tiklab.core.page.Pagination;
import io.tiklab.core.page.PaginationBuilder;
import io.tiklab.hadess.repository.dao.ThirdPathDao;
import io.tiklab.hadess.repository.entity.ThirdPathEntity;
import io.tiklab.hadess.repository.model.ThirdPath;
import io.tiklab.hadess.repository.model.ThirdPathQuery;
import io.tiklab.toolkit.beans.BeanMapper;
import io.tiklab.toolkit.join.JoinTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.List;

/**
* ThirdPathServiceImpl-第三方集成地址
*/
@Service
public class ThirdPathServiceImpl implements ThirdPathService {

    @Autowired
    ThirdPathDao thirdPathDao;

    @Autowired
    JoinTemplate joinTemplate;

    @Override
    public String createThirdPath(@NotNull @Valid ThirdPath thirdPath) {
        ThirdPathEntity thirdPathEntity = BeanMapper.map(thirdPath, ThirdPathEntity.class);

        thirdPathEntity.setCreateTime(new Timestamp(System.currentTimeMillis()));
        return thirdPathDao.createThirdPath(thirdPathEntity);
    }

    @Override
    public void updateThirdPath(@NotNull @Valid ThirdPath thirdPath) {
        ThirdPathEntity thirdPathEntity = BeanMapper.map(thirdPath, ThirdPathEntity.class);

        thirdPathDao.updateThirdPath(thirdPathEntity);
    }

    @Override
    public void deleteThirdPath(@NotNull String id) {
        thirdPathDao.deleteThirdPath(id);
    }

    @Override
    public ThirdPath findOne(String id) {
        ThirdPathEntity thirdPathEntity = thirdPathDao.findThirdPath(id);

        ThirdPath thirdPath = BeanMapper.map(thirdPathEntity, ThirdPath.class);
        return thirdPath;
    }

    @Override
    public List<ThirdPath> findList(List<String> idList) {
        List<ThirdPathEntity> thirdPathEntityList =  thirdPathDao.findThirdPathList(idList);

        List<ThirdPath> thirdPathList =  BeanMapper.mapList(thirdPathEntityList,ThirdPath.class);
        return thirdPathList;
    }

    @Override
    public ThirdPath findThirdPath(@NotNull String id) {
        ThirdPath thirdPath = findOne(id);

        return thirdPath;
    }

    @Override
    public List<ThirdPath> findAllThirdPath() {
        List<ThirdPathEntity> thirdPathEntityList =  thirdPathDao.findAllThirdPath();

        List<ThirdPath> thirdPathList =  BeanMapper.mapList(thirdPathEntityList,ThirdPath.class);

        return thirdPathList;
    }

    @Override
    public List<ThirdPath> findThirdPathList(ThirdPathQuery thirdPathQuery) {
        List<ThirdPathEntity> thirdPathEntityList = thirdPathDao.findThirdPathList(thirdPathQuery);

        List<ThirdPath> thirdPathList = BeanMapper.mapList(thirdPathEntityList,ThirdPath.class);

        return thirdPathList;
    }

    @Override
    public Pagination<ThirdPath> findThirdPathPage(ThirdPathQuery thirdPathQuery) {
        Pagination<ThirdPathEntity>  pagination = thirdPathDao.findThirdPathPage(thirdPathQuery);

        List<ThirdPath> thirdPathList = BeanMapper.mapList(pagination.getDataList(),ThirdPath.class);

        return PaginationBuilder.build(pagination,thirdPathList);
    }
}