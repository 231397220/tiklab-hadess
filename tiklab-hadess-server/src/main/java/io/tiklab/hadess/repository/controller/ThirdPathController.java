package io.tiklab.hadess.repository.controller;

import io.tiklab.core.Result;
import io.tiklab.core.page.Pagination;
import io.tiklab.hadess.repository.model.ThirdPath;
import io.tiklab.hadess.repository.model.ThirdPathQuery;
import io.tiklab.hadess.repository.service.ThirdPathService;
import io.tiklab.postin.annotation.ApiMethod;
import io.tiklab.postin.annotation.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * ThirdPathController
 */
@RestController
@RequestMapping("/thirdPath")
//@Api(name = "ThirdPathController",desc = "第三方集成地址管理")
public class ThirdPathController {

    private static Logger logger = LoggerFactory.getLogger(ThirdPathController.class);

    @Autowired
    private ThirdPathService thirdPathService;

    @RequestMapping(path="/createThirdPath",method = RequestMethod.POST)
    @ApiMethod(name = "createThirdPath",desc = "创建第三方集成地址")
    @ApiParam(name = "thirdPath",desc = "thirdPath",required = true)
    public Result<String> createThirdPath(@RequestBody @NotNull @Valid ThirdPath thirdPath){
        String id = thirdPathService.createThirdPath(thirdPath);

        return Result.ok(id);
    }

    @RequestMapping(path="/updateThirdPath",method = RequestMethod.POST)
    @ApiMethod(name = "updateThirdPath",desc = "更新第三方集成地址")
    @ApiParam(name = "thirdPath",desc = "thirdPath",required = true)
    public Result<Void> updateThirdPath(@RequestBody @NotNull @Valid ThirdPath thirdPath){
        thirdPathService.updateThirdPath(thirdPath);

        return Result.ok();
    }

    @RequestMapping(path="/deleteThirdPath",method = RequestMethod.POST)
    @ApiMethod(name = "deleteThirdPath",desc = "删除第三方集成地址")
    @ApiParam(name = "id",desc = "id",required = true)
    public Result<Void> deleteThirdPath(@NotNull String id){
        thirdPathService.deleteThirdPath(id);

        return Result.ok();
    }

    @RequestMapping(path="/findThirdPath",method = RequestMethod.POST)
    @ApiMethod(name = "findThirdPath",desc = "通过id查询第三方集成地址")
    @ApiParam(name = "id",desc = "id",required = true)
    public Result<ThirdPath> findThirdPath(@NotNull String id){
        ThirdPath thirdPath = thirdPathService.findThirdPath(id);

        return Result.ok(thirdPath);
    }

    @RequestMapping(path="/findAllThirdPath",method = RequestMethod.POST)
    @ApiMethod(name = "findAllThirdPath",desc = "查询所有第三方集成地址")
    public Result<List<ThirdPath>> findAllThirdPath(){
        List<ThirdPath> thirdPathList = thirdPathService.findAllThirdPath();

        return Result.ok(thirdPathList);
    }

    @RequestMapping(path = "/findThirdPathList",method = RequestMethod.POST)
    @ApiMethod(name = "findThirdPathList",desc = "条件查询第三方集成地址")
    @ApiParam(name = "thirdPathQuery",desc = "thirdPathQuery",required = true)
    public Result<List<ThirdPath>> findThirdPathList(@RequestBody @Valid @NotNull ThirdPathQuery thirdPathQuery){
        List<ThirdPath> thirdPathList = thirdPathService.findThirdPathList(thirdPathQuery);

        return Result.ok(thirdPathList);
    }

    @RequestMapping(path = "/findThirdPathPage",method = RequestMethod.POST)
    @ApiMethod(name = "findThirdPathPage",desc = "条件分页查询第三方集成地址")
    @ApiParam(name = "thirdPathQuery",desc = "thirdPathQuery",required = true)
    public Result<Pagination<ThirdPath>> findThirdPathPage(@RequestBody @Valid @NotNull ThirdPathQuery thirdPathQuery){
        Pagination<ThirdPath> pagination = thirdPathService.findThirdPathPage(thirdPathQuery);

        return Result.ok(pagination);
    }

}
