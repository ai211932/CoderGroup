package com.liuyanzhao.forum.vo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


/**
 * @author 言曌
 * @date 2018/3/18 上午9:28
 */

@Configuration
@ConfigurationProperties(prefix="com.liuyanzhao.opensource")
@PropertySource(value = "classpath:resource.properties")
@Data
public class Resource {

    private String name;
    private String website;
    private String language;
    private String keywords;
    private String description;
}
