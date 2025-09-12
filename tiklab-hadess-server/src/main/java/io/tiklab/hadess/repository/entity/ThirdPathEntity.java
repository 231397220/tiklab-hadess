package io.tiklab.hadess.repository.entity;

import io.tiklab.core.BaseModel;
import io.tiklab.dal.jpa.annotation.*;

import java.sql.Timestamp;

/**
 * ThirdPathEntity-第三方集成地址
 */
@Entity
@Table(name="pack_third_path")
public class ThirdPathEntity extends BaseModel {

    @Id
    @GeneratorValue(length = 12)
    @Column(name = "id",length = 12)
    private String id;

    //名称
    @Column(name = "name",length = 64,notNull = true)
    private String name;

    //路径
    @Column(name = "address",length = 64,notNull = true)
    private String address;

    //类型
    @Column(name = "type",length = 32)
    private String type;

    //账号
    @Column(name = "account",length = 128)
    private String account;

    //密码
    @Column(name = "password",length = 128)
    private String password;

    @Column(name = "user_id",length = 32)
    private String userId;


    //创建时间
    @Column(name = "create_time")
    private Timestamp createTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
