


-- ---------------------------
-- 第三方集成地址
-- ----------------------------
CREATE TABLE pack_third_path(
      id VARCHAR(12) PRIMARY KEY,
      name varchar(64) NOT NULL,
      address varchar(32) NOT NULL,
      type varchar(32) NOT NULL,
      account varchar(128),
      password varchar(128),
      user_id varchar(12),
      create_time   timestamp
);



