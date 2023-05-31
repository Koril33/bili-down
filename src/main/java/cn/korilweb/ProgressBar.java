package cn.korilweb;

public class ProgressBar {

    /**
     * 进度条长度
     */
    private static final int PROGRESS_SIZE = 50;


    /**
     * 单位长度
     */
    private static final int UNIT_SIZE = 100 / PROGRESS_SIZE;


    /**
     * 总大小
     */
    private final long total;


    public ProgressBar(long total) {
        this.total = total;
    }

    private String getNChar(long num, char ch){
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < num; i++){
            builder.append(ConsoleColors.BLUE_BOLD_BRIGHT)
                   .append(ch);
        }
        return builder.toString();
    }

    public void printProgress(long index) {

        index = index * 100 / total;

        // 已完成的字符
        String finish = getNChar(index / UNIT_SIZE, '█');

        // 未完成的字符
        String unFinish = getNChar(PROGRESS_SIZE - index / UNIT_SIZE, '─');

        String target = String.format("%3d%%├%s%s┤", index, finish, unFinish);

        System.out.print(getNChar(PROGRESS_SIZE + 6, '\b'));
        System.out.print(target);

    }
}
