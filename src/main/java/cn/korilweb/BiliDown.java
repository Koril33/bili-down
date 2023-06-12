package cn.korilweb;

import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@CommandLine.Command(
        name = "BiliDown",
        version = {
            "@|yellow Version 0.0.1|@",
            "@|blue Author Ding Jinghui|@"
        },
        mixinStandardHelpOptions = true,
        description = "一个简单的 Bilibili 视频下载器"
)
public class BiliDown implements Runnable {

    /**
     * 是否删除临时文件
     */
    @CommandLine.Option(
            names = { "-r", "--remove" },
            paramLabel = "REMOVE_TEMP_FILE",
            description = "remove temp files(include _video.mp4, _audio.mp4)"
    )
    Boolean removeFlag = true;


    /**
     * 选择要保存的路径
     */
    @CommandLine.Option(
            names = { "-s", "--save-dir" },
            paramLabel = "SAVE_DIR",
            description = "The directory to save video"
    )
    String savePath = ".";


    /**
     * FFmpeg 路径
     */
    @CommandLine.Option(
            names = { "-f", "--ffmpeg" },
            paramLabel = "FFMPEG_EXE_PATH",
            description = "ffmpeg execute file path"
    )
    String ffmpegPath = "ffmpeg.exe";


    /**
     * 多集视频，选定的集数
     */
    @CommandLine.Option(
            names = { "-n", "--num" },
            paramLabel = "EPISODE_NUM",
            description = "download episode num, split char is '-'"
    )
    String num = null;


    /**
     * 有些视频需要大会员账号才能获取
     */
    @CommandLine.Option(
            names = { "-c", "--cookie" },
            paramLabel = "COOKIE_VALUE",
            description = "download cookie header"
    )
    String cookie = "";


    /**
     * 从特定文件读取下载的 URL 链接
     */
    @CommandLine.Option(
            names = { "-o", "--origin"},
            paramLabel = "ORIGIN_VIDEO_FILE",
            description = "download video urls from specific file"
    )
    String filePathStr = null;


    /**
     * B站视频的 URL
     */
    @CommandLine.Parameters(
            paramLabel = "<origin url>",
            description = "The origin video url to be downloaded"
    )
    private List<String> originUrl;



    private void app() {

        if (Objects.isNull(filePathStr) && Objects.isNull(originUrl)) {
            throw new RuntimeException("下载任务列表为空");
        }


        List<Video> videos = new ArrayList<>();

        if (Objects.nonNull(filePathStr)) {
            Path filePath = Paths.get(filePathStr);
            if (Files.notExists(filePath)) {
                throw new RuntimeException("无法找到该文件: " + filePathStr);
            }

            List<String> videoUrls;
            try {
                videoUrls = Files.readAllLines(filePath);
            } catch (IOException e) {
                throw new RuntimeException("读取该文件出错: " + filePathStr);
            }

            videoUrls.forEach(s -> {
                Video video = new Video(ffmpegPath, s, num);
                videos.add(video);
            });
        }
        else {

            originUrl.forEach(originUrl -> {
                Video video = new Video(ffmpegPath, originUrl, num);
                videos.add(video);
            });
        }


        long start = System.currentTimeMillis();

        System.out.println("共计 " + videos.size() + " 个任务");

        videos.forEach(video -> {
            video.init();
            video.parse(cookie);
            video.download(Paths.get(savePath), removeFlag);

        });

        long end = System.currentTimeMillis();

        System.out.println(
                ConsoleColors.GREEN +
                        "\n所有任务下载完成，共耗时 " + (end - start) + " 毫秒"
        );
        System.out.println(ConsoleColors.RESET);
    }



    @Override
    public void run() {
        this.app();
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new BiliDown()).execute(args);
        System.exit(exitCode);
    }
}
