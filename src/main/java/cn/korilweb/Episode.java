package cn.korilweb;

import lombok.Data;

@Data
public class Episode {

    /**
     * 视频名称
     */
    private String name;


    /**
     * 视频的原始 URL
     */
    private String originUrl;


    /**
     * 视频下载 URL
     */
    private String videoUrl;


    /**
     * 音频下载 URL
     */
    private String audioUrl;


    /**
     * 质量
     */
    private Integer quality;



}
