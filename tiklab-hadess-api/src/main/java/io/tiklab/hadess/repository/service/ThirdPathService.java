package io.tiklab.hadess.repository.service;


import io.tiklab.core.page.Pagination;
import io.tiklab.hadess.repository.model.ThirdPath;
import io.tiklab.hadess.repository.model.ThirdPathQuery;
import io.tiklab.toolkit.join.annotation.FindAll;
import io.tiklab.toolkit.join.annotation.FindList;
import io.tiklab.toolkit.join.annotation.FindOne;
import io.tiklab.toolkit.join.annotation.JoinProvider;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
* ThirdPathService-第三方集成地址
*/
@JoinProvider(model = ThirdPath.class)
public interface ThirdPathService {

    /**
    * 创建
    * @param thirdPath
    * @return
    */
    String createThirdPath(@NotNull @Valid ThirdPath thirdPath);

    /**
    * 更新
    * @param thirdPath
    */
    void updateThirdPath(@NotNull @Valid ThirdPath thirdPath);

    /**
    * 删除
    * @param id
    */
    void deleteThirdPath(@NotNull String id);

    @FindOne
    ThirdPath findOne(@NotNull String id);

    @FindList
    List<ThirdPath> findList(List<String> idList);

    /**
    * 查找
    * @param id
    * @return
    */
    ThirdPath findThirdPath(@NotNull String id);

    /**
    * 查找所有
    * @return
    */
    @FindAll
    List<ThirdPath> findAllThirdPath();

    /**
    * 查询列表
    * @param thirdPathQuery
    * @return
    */
    List<ThirdPath> findThirdPathList(ThirdPathQuery thirdPathQuery);

    /**
    * 按分页查询
    * @param thirdPathQuery
    * @return
    */
    Pagination<ThirdPath> findThirdPathPage(ThirdPathQuery thirdPathQuery);

}