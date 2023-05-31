package cn.korilweb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ProcessStreamConsumer implements Runnable {

    private final InputStream inputStream;

    public ProcessStreamConsumer(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        try (
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))
        ) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
//                System.out.println("ffmpeg log: " + line);
                // do nothing
                ;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
