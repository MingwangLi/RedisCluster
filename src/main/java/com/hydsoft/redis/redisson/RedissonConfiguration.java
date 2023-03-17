package com.hydsoft.redis.redisson;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @title: RedissionConfiguration
 * @Description:
 * @Author Jane
 * @Date: 2022/6/17 11:01
 * @Version 1.0
 */
@Configuration
public class RedissonConfiguration {

    @Autowired
    private RedisProperties redisProperties;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        RedisProperties.Cluster cluster = redisProperties.getCluster();
        String password = redisProperties.getPassword();
        if (null != cluster) {
            //集群模式
            List<String> nodes = cluster.getNodes().stream().map(node -> "redis://" + node).collect(Collectors.toList());
            String[] nodeArray = nodes.toArray(new String[0]);
            ClusterServersConfig clusterServersConfig = config.useClusterServers().addNodeAddress(nodeArray);
            if (StringUtils.hasText(password)) {
                clusterServersConfig.setPassword(password);
            }
        } else {
            //单机模式
            SingleServerConfig singleServerConfig = config.useSingleServer().setAddress("redis://" + this.redisProperties.getHost() + ":" + this.redisProperties
                    .getPort());
            if (StringUtils.hasText(password)) {
                singleServerConfig.setPassword(password);
            }
        }
        return Redisson.create(config);
    }
}
