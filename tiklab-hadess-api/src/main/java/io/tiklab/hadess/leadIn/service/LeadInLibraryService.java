package io.tiklab.hadess.leadIn.service;

import io.tiklab.hadess.leadIn.model.LeadInQuery;

import java.util.LinkedHashMap;
import java.util.List;

public interface LeadInLibraryService {

    /**
     * 查询仓库
     * @param leadInQuery leadInQuery
     */
    List<LinkedHashMap> findRepositoryList(LeadInQuery leadInQuery);



    /**
     * 导入仓库制品
     * @param leadInQuery leadInQuery
     */
    void leadInRepLibrary(LeadInQuery leadInQuery);


    /**
     * 查询导入状态
     * @param targetRepId targetRepId
     */
    Boolean findLeadInState(String targetRepId);
}
