package cn.korilweb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class Video {

    private static final String MULTI_VIDEO_ID = "multi_page";


    private static final OkHttpClient client = new OkHttpClient();

    private static final ObjectMapper mapper = new ObjectMapper();


    /**
     * 视频原始下载地址
     */
    private final String originUrl;


    /**
     * 视频名称
     */
    private String name;


    /**
     * 是否是视频集视频
     * true：视频集，包含多个视频
     * false：普通视频，仅包含一个视频
     */
    private Boolean isMulti = false;


    private List<String> parsedUrls = new ArrayList<>();


    private List<Episode> episodes = new ArrayList<>();


    private Path ffmpegPath;


    /**
     * 下载集数区间，如：3-12，表示从第 3 集下载到第 12 集，包括 3 和 12
     */
    private String episodeNumStr;


    public Video(String ffmpegPathStr, String originUrl, String numStr) {
        this.ffmpegPath = Paths.get(ffmpegPathStr);

        if (Files.notExists(this.ffmpegPath)) {
            throw new IllegalArgumentException("指定的 ffmpeg.exe 不存在");
        }

        this.originUrl = originUrl;
        this.episodeNumStr = numStr;
    }


    public void init() {
        Request request = new Request.Builder()
                .url(originUrl)
                .build();


        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && Objects.nonNull(response.body())) {
                String bodyContent = response.body().string();

                Document document = Jsoup.parse(bodyContent);

                // 处理视频标题，去掉特殊的符号
                this.name = document.title()
                        .replace("_哔哩哔哩_bilibili", "")
                        .replace("/", "-")
                        .replace("\\\\", "-");

                // 是否是多集视频
                if (bodyContent.contains(MULTI_VIDEO_ID)) {
                    this.isMulti = true;

                    Elements elementsByClass = document.getElementsByClass("cur-page");
                    if (elementsByClass.size() == 0) {
                        throw new RuntimeException("目标为多集视频，但无法找到 cur-page 元素");
                    }

                    // 获取总集数信息
                    String pageNum = elementsByClass.get(0).text().split("/")[1].replace(")", "");

                    int totalNum = Integer.parseInt(pageNum);

                    int startNum = Objects.isNull(episodeNumStr) ?
                            1 : Integer.parseInt(episodeNumStr.split("-")[0]);

                    int endNum = Objects.isNull(episodeNumStr) ?
                            totalNum : Integer.parseInt(episodeNumStr.split("-")[1]);

                    for (int i = startNum; i <= endNum; i++) {
                        // 集数拼接到原始 URL 后面，构成每一集的 URL
                        this.parsedUrls.add(originUrl + "?p=" + i);
                    }

                }
                else {
                    if (Objects.nonNull(episodeNumStr)) {
                        throw new IllegalArgumentException("解析结果为单个视频，不允许指定下载集数区间");
                    }
                    this.parsedUrls.add(originUrl);
                }
            }
            else {
                throw new RuntimeException("解析原始 URL 失败，HTTP 响应错误");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 通过原始视频获取真实的下载路径
     */
    public void parse() {

        if (parsedUrls.size() == 0) {
            throw new RuntimeException("未包含待处理的 URL");
        }

        int startNum = Objects.isNull(episodeNumStr) ?
                1 : Integer.parseInt(episodeNumStr.split("-")[0]);

        for (int i = 0; i < parsedUrls.size(); i++) {
            String url = parsedUrls.get(i);
            Request request = new Request.Builder().url(url).build();

            try (Response response = client.newCall(request).execute()) {
                // 找到指定的 json 字符串
                Pattern p = Pattern.compile("__playinfo__=(.*?)</script><script>");

                if (response.isSuccessful() && Objects.nonNull(response.body())) {
                    String bodyContent = response.body().string();
                    Matcher m = p.matcher(bodyContent);

                    if (m.find()) {
                        String group = m.group();

                        String jsonStr = group.replace("__playinfo__=", "")
                                .replace("</script><script>", "");

                        ObjectNode objectNode = mapper.readTree(mapper.createParser(jsonStr));
                        String videoUrl = objectNode.get("data").get("dash").get("video").get(0).get("baseUrl").asText();
                        String audioUrl = objectNode.get("data").get("dash").get("audio").get(0).get("baseUrl").asText();

                        Episode episode = new Episode();
                        episode.setOriginUrl(url);
                        episode.setVideoUrl(videoUrl);
                        episode.setAudioUrl(audioUrl);
                        episode.setQuality(0);

                        if (this.isMulti) {
                            episode.setName(this.name + "-" + (startNum + i));
                        }
                        else {
                            episode.setName(this.name);
                        }

                        this.episodes.add(episode);
                    }
                    else {
                        System.out.println("无法找到 " + url + " 中的 Json 信息");
                    }
                }
                else {
                    System.out.println("解析" + url + " 失败，HTTP 响应错误");
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public void download(Path path, Boolean removeFlag) {
        int size = episodes.size();

        System.out.println("任务开始，共计 " + size + " 个视频");

        int taskIndex = 1;

        for (Episode episode : episodes) {
            String videoUrl = episode.getVideoUrl();
            String audioUrl = episode.getAudioUrl();

            Request videoRequest = new Request.Builder()
                    .header("Referer", originUrl)
                    .url(videoUrl)
                    .build();

            Request audioRequest = new Request.Builder()
                    .header("Referer", originUrl)
                    .url(audioUrl)
                    .build();

            System.out.printf(
                    ConsoleColors.YELLOW +
                    "\n开始下载: (%d/%d) %s\n",
                    taskIndex++,
                    size,
                    episode.getName()
            );

            System.out.println(ConsoleColors.RESET);

            // 下载视频文件
            try (Response videoResponse = client.newCall(videoRequest).execute()) {
                if (videoResponse.isSuccessful() && Objects.nonNull(videoResponse.body())) {

                    long videoLength = Long.parseLong(Objects.requireNonNull(videoResponse.header("Content-Length")));

                    long start = System.currentTimeMillis();

                    Path videoSavePath = path.resolve(episode.getName() + "_video.mp4");

                    try (
                        InputStream inputStream = videoResponse.body().byteStream();
                        OutputStream videoOutputStream = Files.newOutputStream(
                                videoSavePath,
                                StandardOpenOption.CREATE
                        )
                    ) {

                        ProgressBar bar = new ProgressBar(videoLength);

                        byte[] buf = new byte[1024];
                        int length;
                        long cnt = 0;

                        while ((length = inputStream.read(buf)) != -1) {
                            videoOutputStream.write(buf, 0, length);
                            cnt += length;
                            bar.printProgress(cnt);
                        }
                        System.out.println(ConsoleColors.RESET);
                    }

                    long end = System.currentTimeMillis();

                    System.out.printf(
                            ConsoleColors.GREEN +
                            "\n%s 的[视频文件]下载完成, 大小: %d 字节, 耗时: %d 毫秒\n",
                            episode.getName(),
                            videoLength,
                            (end -start)
                    );
                    System.out.println(ConsoleColors.RESET);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // 下载音频文件
            try (Response audioResponse = client.newCall(audioRequest).execute()) {
                if (audioResponse.isSuccessful() && Objects.nonNull(audioResponse.body())) {

                    long start = System.currentTimeMillis();

                    long audioLength = Long.parseLong(Objects.requireNonNull(audioResponse.header("Content-Length")));

                    Path audioSavePath = path.resolve(episode.getName() + "_audio.mp4");

                    try (
                        InputStream inputStream = audioResponse.body().byteStream();
                        OutputStream audioOutputStream = Files.newOutputStream(
                                audioSavePath,
                                StandardOpenOption.CREATE
                        )
                    ) {

                        ProgressBar bar = new ProgressBar(audioLength);

                        byte[] buf = new byte[1024];
                        int length;
                        long cnt = 0;

                        while ((length = inputStream.read(buf)) != -1) {
                            audioOutputStream.write(buf, 0, length);
                            cnt += length;
                            bar.printProgress(cnt);
                        }
                        System.out.println(ConsoleColors.RESET);
                    }


                    long end = System.currentTimeMillis();

                    System.out.printf(
                            ConsoleColors.GREEN +
                            "\n%s 的[音频文件]下载完成, 大小: %s 字节, 耗时: %d 毫秒\n",
                            episode.getName(),
                            audioResponse.header("Content-Length"),
                            (end -start)
                    );

                    System.out.println(ConsoleColors.RESET);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            String prefix = episode.getName();
            Process process = merge(
                    ffmpegPath,
                    path.resolve(prefix + "_video.mp4"),
                    path.resolve(prefix + "_audio.mp4"),
                    path.resolve(prefix + ".mp4")
            );

            // 是否删除临时文件
            if (removeFlag) {
                try {
                    System.out.println("合成视频成功，开始删除临时文件");
                    // ffmpeg 子进程正常退出
                    if (process.waitFor() == 0) {
                        Files.deleteIfExists(path.resolve(prefix + "_video.mp4"));
                        Files.deleteIfExists(path.resolve(prefix + "_audio.mp4"));
                    }
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException("删除文件失败", e);
                }
                System.out.println(prefix + " 的临时文件已被删除");
            }
        }
    }



    private Process merge(Path FFmpegPath, Path videoPath, Path audioPath, Path outputPath) {
        String cmd =
                FFmpegPath.toString() +
                " -i \"" + videoPath.toString() + "\"" +
                " -i \"" + audioPath.toString() + "\"" +
                " -map 0:0 -map 1:0 " +
                " -c copy \"" + outputPath.toString() + "\"";

        System.out.println("开始合成视频和音频文件: " + cmd);

        try {
            Process process = Runtime.getRuntime().exec(cmd);

            InputStream inputStream = process.getInputStream();
            InputStream errorStream = process.getErrorStream();

            Thread t1 = new Thread(new ProcessStreamConsumer(inputStream));
            Thread t2 = new Thread(new ProcessStreamConsumer(errorStream));
            t1.start();
            t2.start();

            return process;

        } catch (IOException e) {
            throw new RuntimeException("ffmpeg 合成视频出错", e);
        }
    }
}
