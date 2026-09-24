package ru.library.config;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EtcdConfig {
  @Bean(destroyMethod = "close")
  Client etcdClient(AppProperties props) {
    return Client.builder().endpoints(props.etcd().endpoint()).build();
  }

  @Bean
  KV etcdKv(Client client) {
    return client.getKVClient();
  }
}