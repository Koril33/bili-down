package cn.korilweb;

import picocli.CommandLine;

import java.nio.file.Paths;

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
     * B站视频的 URL
     */
    @CommandLine.Parameters(
            paramLabel = "<origin url>",
            description = "The origin video url to be downloaded",
            arity = "1"
    )
    private String originUrl;



    @Override
    public void run() {

        Video video = new Video(ffmpegPath, originUrl);
        video.init();
        video.parse();

        long start = System.currentTimeMillis();
        video.download(Paths.get(savePath), removeFlag);
        long end = System.currentTimeMillis();

        System.out.println(
                ConsoleColors.GREEN +
                "\n所有任务下载完成，共耗时 " + (end - start) + " 毫秒"
        );

        System.out.println(ConsoleColors.RESET);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new BiliDown()).execute(args);
        System.exit(exitCode);
    }
}
