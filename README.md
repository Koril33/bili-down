# bili-down
Bilibili 视频下载器的命令行版本

---

## 使用方式及参数说明

### 查看可用参数列表
```shell
bili-download.exe -h
```

### 最简单的使用，下载视频
指定下载的 URL 即可，无需其他参数
```shell
bili-download.exe "https://www.bilibili.com/video/BV1TD4y1W7PS"
```

### 指定 ffmpeg 的执行文件路径
程序会从当前文件夹下找 ffmpeg.exe 去合成音视频文件，如果当前文件夹没有的话，可以手动指定 ffmpeg 的 exe 文件路径。
```shell
bili-download.exe -f "C:\Software\ffmpeg\bin\ffmpeg.exe" "https://www.bilibili.com/video/BV1TD4y1W7PS"
```

### 指定集数
如果目标是多集视频的话，可以通过 -n 或者 --num，指定下载的集数区间（如果不指定该参数的话，遇到多集视频会自动全部下载）
比如有一个 180 集的视频，只想下载其中的第三集至第五集，可以输入以下命令：
```shell
bili-download.exe -n "3-5" "https://www.bilibili.com/video/BV1UQ4y1B7d7"
```

### 指定下载目录
如果希望将下载的视频放在某个目录下，通过 -s 或者 --save 参数实现：
```shell
bili-download.exe -s ".\中国通史" "https://www.bilibili.com/video/BV1UQ4y1B7d7"
```