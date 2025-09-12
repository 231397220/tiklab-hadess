package io.tiklab.hadess.leadIn.controller;


import io.tiklab.core.Result;
import io.tiklab.hadess.leadIn.model.LeadInQuery;
import io.tiklab.hadess.leadIn.service.LeadInLibraryService;
import io.tiklab.hadess.library.model.Library;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/leanInLibrary")
public class LeanInLibraryController {

    @Autowired
    LeadInLibraryService leadInLibraryService;


    @RequestMapping(path="/findRepository",method = RequestMethod.POST)
    public Result<LinkedHashMap> findLibrary(@RequestBody @NotNull @Valid LeadInQuery leadInQuery){
        List<LinkedHashMap> repositoryList = leadInLibraryService.findRepositoryList(leadInQuery);

        return Result.ok(repositoryList);
    }

    @RequestMapping(path="/leadInRepLibrary",method = RequestMethod.POST)
    public Result<Void> leadInRepLibrary(@RequestBody @NotNull @Valid LeadInQuery leadInQuery){
      leadInLibraryService.leadInRepLibrary(leadInQuery);

        return Result.ok();
    }

    @RequestMapping(path="/findLeadInState",method = RequestMethod.POST)
    public Result<Boolean> findLeadInState( @NotNull  String targetRepId){
        Boolean stata=leadInLibraryService.findLeadInState(targetRepId);

        return Result.ok(stata);
    }
}

