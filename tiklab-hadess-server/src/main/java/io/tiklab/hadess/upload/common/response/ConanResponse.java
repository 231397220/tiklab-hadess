package io.tiklab.hadess.upload.common.response;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ConanResponse {


    public static void ConanUploadError(){

    }

    /**
     *  读取文件信息
     *  @param file     文件
     * @return
     */
    public static void readFileData(File file, HttpServletResponse response) throws IOException {
        ServletOutputStream outputStream = response.getOutputStream();

        InputStream inputStream = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        // 关闭输入流和输出流
        inputStream.close();
        outputStream.close();
    }
}
