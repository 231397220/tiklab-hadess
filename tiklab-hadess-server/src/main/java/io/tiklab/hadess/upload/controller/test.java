package io.tiklab.hadess.upload.controller;



import io.tiklab.hadess.common.RepositoryUtil;
import io.tiklab.hadess.upload.common.response.RpmResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class test {

    public static void main(String[] args) throws Exception {




       /* LocalDate currentDate = LocalDate.now();
        LocalDate localDate = currentDate.minusDays(30);
        ZoneId zoneId = ZoneId.systemDefault(); // 使用系统默认时区
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(zoneId); // 转换为ZonedDateTime表示当天的开始时间
        long millis = zonedDateTime.toInstant().toEpochMilli(); // 转换为毫秒

        long l = System.currentTimeMillis();
        System.out.println();*/

       /* String path="/Users/limingliang/.conan/data/zlib/1.2.11/_/_/package/095512ed878f14a09dd732e9f6868729dd437529/conaninfo.txt";
        // 创建 MessageDigest 实例（指定 MD5 算法）
        MessageDigest md = MessageDigest.getInstance("MD5");
        try {

            // 读取文件内容并更新哈希计算
            try (FileInputStream fis = new FileInputStream(path)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }

            // 获取哈希字节数组并转换为十六进制字符串
            byte[] hashBytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            String string = sb.toString();
            System.out.println("");

        } catch (Exception e) {
            throw new RuntimeException("计算 MD5 失败: " + e.getMessage(), e);
        }*/


        String filePath ="/Users/limingliang/tiklab/tiklab-hadess/repository/k29xO1Qv2qbd/repodata/repomd_simple.xml";

        StringBuilder xmlBuilder = new StringBuilder();
        // 构建 XML 内容
       /* xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");*/
        xmlBuilder.append("<repomd xmlns=\"http://linux.duke.edu/metadata/repo\" xmlns:rpm=\"http://linux.duke.edu/metadata/rpm\">\n");
        xmlBuilder.append("  <revision>8-stream</revision>\n");
        xmlBuilder.append("  <tags>\n");
        xmlBuilder.append("  <distro cpeid=\"cpe:/o:centos-stream:centos-stream:8\">CentOS Stream 8</distro>\n");
        xmlBuilder.append("  </tags>\n");
        xmlBuilder.append("  <data type=\"primary\">\n");
        xmlBuilder.append("  </data>\n");
        xmlBuilder.append("  <data type=\"filelists\">\n");
        xmlBuilder.append("  </data>\n");
        xmlBuilder.append("</repomd>");

        FileWriter writer = new FileWriter(filePath, StandardCharsets.UTF_8);
        writer.write(xmlBuilder.toString());
        writer.flush();
    }



}
