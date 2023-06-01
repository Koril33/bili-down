# bili-down
Bilibili 视频下载器的命令行版本

---

## 使用方式

最简单的使用
```shell
bili-download.exe "https://www.bilibili.com/video/BV1TD4y1W7PS"
```

程序会从当前文件夹下找 ffmpeg.exe 去合成音视频文件，如果当前文件夹没有的话，可以手动指定路径。
```shell
bili-download.exe -f "C:\Software\ffmpeg\bin\ffmpeg.exe" "https://www.bilibili.com/video/BV1TD4y1W7PS"
```