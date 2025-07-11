package io.tiklab.hadess.starter.config;

import io.tiklab.dsm.model.DsmConfig;
import io.tiklab.dsm.model.DsmVersion;
import io.tiklab.dsm.support.DsmConfigBuilder;
import io.tiklab.dsm.support.DsmVersionBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HadessDsmAutoConfiguration {

    @Bean
    DsmConfig dsmConfig(){
        DsmConfig dsmConfig = new DsmConfig();

        dsmConfig.setVersionList(versionList());
        return dsmConfig;
    }

    /**
     * 初始化Dsm版本列表
     * @return
     */
    List<DsmVersion> versionList() {
        List<DsmVersion> versionList = new ArrayList<>();
        DsmVersion dsmVersion = DsmVersionBuilder.instance()
                .version("1.0.0")
                .db(new String[]{
                        "user_1.0.0",
                        //PrivilegeDsm
                        "privilege_1.0.0",
                        //LicenceDsm
                        "app-authorization_1.0.0",
                        //MessageDsm
                        "message_1.0.0",
                        //SecurityDsm
                        "oplog_1.0.0",
                        //TodoTaskDsm
                        "todotask_1.0.0",
                        "openapi_1.0.0",


                        //xpack
                        "xpack_1.0.0",
                        "xprivilege_1.0.0",
                        "hadess_1.0.0",
                }).get();
        versionList.add(dsmVersion);

         dsmVersion = DsmVersionBuilder.instance()
                .version("1.0.1")
                .db(new String[]{
                        "xpack_1.0.1",
                        "xprivilege_1.0.1",
                        "hadess_1.0.1",
                        "scan_1.0.1",
                }).get();
        versionList.add(dsmVersion);

        dsmVersion = DsmVersionBuilder.instance()
                .version("1.0.2")
                .db(new String[]{
                        "xpack_1.0.2",
                        "xprivilege_1.0.2",
                        "hadess_1.0.2",
                }).get();
        versionList.add(dsmVersion);

        dsmVersion = DsmVersionBuilder.instance()
                .version("1.0.3")
                .db(new String[]{
                        "xpack_1.0.3",
                        "hadess_1.0.3",
                }).get();
        versionList.add(dsmVersion);

        dsmVersion = DsmVersionBuilder.instance()
                .version("1.0.4")
                .db(new String[]{
                        "hadess_1.0.4",
                }).get();
        versionList.add(dsmVersion);



        DsmVersion message_109 = DsmVersionBuilder.instance()
                .version("message_1.0.9")
                .db(new String[]{
                        "message_1.0.9",
                }).get();
        versionList.add(message_109);

        return versionList;
    }
}
