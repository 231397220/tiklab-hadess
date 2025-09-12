package io.tiklab.hadess.repository.dao;

import io.tiklab.core.page.Pagination;
import io.tiklab.dal.jpa.JpaTemplate;
import io.tiklab.dal.jpa.criterial.condition.DeleteCondition;
import io.tiklab.dal.jpa.criterial.condition.QueryCondition;
import io.tiklab.dal.jpa.criterial.conditionbuilder.QueryBuilders;
import io.tiklab.hadess.repository.entity.ThirdPathEntity;
import io.tiklab.hadess.repository.model.ThirdPathQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ThirdPathDao-第三方集成地址数据访问
 */
@Repository
public class ThirdPathDao {

    private static Logger logger = LoggerFactory.getLogger(ThirdPathDao.class);

    @Autowired
    JpaTemplate jpaTemplate;

    /**
     * 创建
     * @param thirdPathEntity
     * @return
     */
    public String createThirdPath(ThirdPathEntity thirdPathEntity) {
        return jpaTemplate.save(thirdPathEntity,String.class);
    }

    /**
     * 更新
     * @param thirdPathEntity
     */
    public void updateThirdPath(ThirdPathEntity thirdPathEntity){
        jpaTemplate.update(thirdPathEntity);
    }

    /**
     * 删除
     * @param id
     */
    public void deleteThirdPath(String id){
        jpaTemplate.delete(ThirdPathEntity.class,id);
    }

    /**
     * 条件删除第三方集成地址
     * @param deleteCondition
     */
    public void deleteThirdPath(DeleteCondition deleteCondition){
        jpaTemplate.delete(deleteCondition);
    }

    /**
     * 查找
     * @param id
     * @return
     */
    public ThirdPathEntity findThirdPath(String id){
        return jpaTemplate.findOne(ThirdPathEntity.class,id);
    }

    /**
    * 查询所有第三方集成地址
    * @return
    */
    public List<ThirdPathEntity> findAllThirdPath() {
        return jpaTemplate.findAll(ThirdPathEntity.class);
    }

    /**
     * 通过ids查询第三方集成地址
     * @param idList
     * @return List <ThirdPathEntity>
     */
    public List<ThirdPathEntity> findThirdPathList(List<String> idList) {
        return jpaTemplate.findList(ThirdPathEntity.class,idList);
    }

    /**
     * 条件查询第三方集成地址
     * @param thirdPathQuery
     * @return List <ThirdPathEntity>
     */
    public List<ThirdPathEntity> findThirdPathList(ThirdPathQuery thirdPathQuery) {
        QueryCondition queryCondition = QueryBuilders.createQuery(ThirdPathEntity.class)
                .eq("type",thirdPathQuery.getType())
                .orders(thirdPathQuery.getOrderParams())
                .get();
        return jpaTemplate.findList(queryCondition,ThirdPathEntity.class);
    }

    /**
     * 条件分页查询第三方集成地址
     * @param thirdPathQuery
     * @return Pagination <ThirdPathEntity>
     */
    public Pagination<ThirdPathEntity> findThirdPathPage(ThirdPathQuery thirdPathQuery) {
        QueryCondition queryCondition = QueryBuilders.createQuery(ThirdPathEntity.class)
                .eq("type",thirdPathQuery.getType())
                .orders(thirdPathQuery.getOrderParams())
                .pagination(thirdPathQuery.getPageParam())
                .get();
        return jpaTemplate.findPage(queryCondition,ThirdPathEntity.class);
    }
}