package io.tiklab.hadess.leadIn.model;

import io.tiklab.postin.annotation.ApiModel;
import io.tiklab.postin.annotation.ApiProperty;

@ApiModel
public class LeadInQuery {

    @ApiProperty(name ="url",desc = "路径")
    private String url;

    @ApiProperty(name ="type",desc = "仓库类型 maven、npm")
    private String type;

    @ApiProperty(name ="versionType",desc = "maven仓库版本类型 Release、Snapshot")
    private String versionType;

    @ApiProperty(name ="targetRepId",desc = "目标仓库id")
    private String targetRepId;

    @ApiProperty(name ="sourceRepName",desc = "源仓库名字")
    private String sourceRepName;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTargetRepId() {
        return targetRepId;
    }

    public void setTargetRepId(String targetRepId) {
        this.targetRepId = targetRepId;
    }

    public String getSourceRepName() {
        return sourceRepName;
    }

    public void setSourceRepName(String sourceRepName) {
        this.sourceRepName = sourceRepName;
    }

    public String getVersionType() {
        return versionType;
    }

    public void setVersionType(String versionType) {
        this.versionType = versionType;
    }
}
