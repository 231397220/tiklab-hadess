package io.tiklab.hadess.upload.common.response;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class RpmResponse {


    //拉取错误信息
    public static void downloadErrorToClient(HttpServletResponse resp,Integer code, String message) {
        try {
            resp.setContentType("text/plain; charset=utf-8");
            PrintWriter out = resp.getWriter();
            resp.setStatus(code);
            out.println(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    //上传错误信息
    public static void uploadErrorToClient(HttpServletResponse resp, String message) {
        try {
            resp.setStatus(500);
            resp.getWriter().write(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
