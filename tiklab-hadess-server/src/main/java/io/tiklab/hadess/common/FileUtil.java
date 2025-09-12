package io.tiklab.hadess.common;

import io.tiklab.core.exception.SystemException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class FileUtil {


    /**
     * 读取文件信息并写入response
     *
     * @param file 文件
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


    /**
     * 读取输入流中的数据
     *
     * @param inputStream 数据流
     * @return data 读取的数据
     */
    public static String readInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(inputStream.available());
        BufferedInputStream in = new BufferedInputStream(inputStream);
        int buf_size = 1024;
        byte[] buffer = new byte[buf_size];
        int len = 0;
        while (-1 != (len = in.read(buffer, 0, buf_size))) {
            bos.write(buffer, 0, len);
        }
        return bos.toString();
    }


    /**
     * copy文件内容
     *
     * @param inputStream 内容流
     * @param folderPath  文件需要复制到的地址
     * @param fileName    文件名字
     */
    public static Path copyFileData(InputStream inputStream, String folderPath, String fileName) {
        try {
            // 规范化路径并创建目录（如果不存在）
            Path dirPath = Paths.get(folderPath);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // 构建目标文件路径
            Path filePath = dirPath.resolve(fileName);

            // 使用 try-with-resources 确保流关闭，并直接复制输入流到文件
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            return filePath;
        } catch (IOException e) {
            throw new SystemException(HadessFinal.WRITE_EXCEPTION, "写入数据失败");
        }
    }

    /**
     * 流写入文件
     *
     * @param inputStream 内容流
     * @param filePath  写入文件的地址
     */
    public static void writeDataToFile(InputStream inputStream, String filePath) {
        Path path = Paths.get(filePath);

        try {
            // 创建所有必要的目录
            Files.createDirectories(path.getParent());

            // 写入数据
            try (FileOutputStream outputStream = new FileOutputStream(path.toFile())) {
                byte[] buffer = new byte[4096]; // 设置缓冲区大小
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead); // 写入数据
                }
                outputStream.flush(); // 确保所有数据都写入
            }
        } catch (IOException e) {
            e.printStackTrace(); // 处理异常
            throw new SystemException(HadessFinal.WRITE_EXCEPTION, "写入数据失败");
        }
}


    /**
     * 写入string到文件路径中
     *
     * @param content  内容
     * @param filePath 文件路径
     */
    public static void writeStringToFile(String content, String filePath) throws IOException {
        Path path = createBorderAndFile(filePath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
            writer.write(content);
        }
    }

    /**
     * 写入byte到文件路径中
     * @param content  内容
     * @param filePath 文件路径
     */
    public static void writeByteToFile(byte[] content, String filePath) throws IOException {
        Path path = createBorderAndFile(filePath);
        Files.write(path, content);
    }

    /**
     * 读取zip压缩包里面的文件内容
     *
     * @param zipFilePath   压缩包地址
     * @param fileNameInZip 文件名字
     */
    public static String readFileInZip(String zipFilePath, String fileNameInZip) {
        try (

                ZipFile zipFile = new ZipFile(zipFilePath)) {
            ZipEntry entry = zipFile.getEntry(fileNameInZip);
            if (entry != null) {
                StringBuilder fileContent = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        fileContent.append(line).append(System.lineSeparator()); // 添加内容和换行符
                    }
                }
                return fileContent.toString();
            } else {
                return "500";
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new SystemException(HadessFinal.READ_LOCAL_EXCEPTION, e.getMessage());
        }
    }

    /**
     * 用于nuget读取.nupkg 压缩包里面的文件内容
     * @param filePath   fileData
     */
    public static String readFileInNupkg(String filePath)  {
        // 解压 .nupkg 并读取 .nuspec 文件
        try (ZipFile zipFile = new ZipFile(filePath)) {
            // 1. 查找 .nuspec 文件（通常位于根目录）
            ZipEntry nuspecEntry = zipFile.stream()
                    .filter(e -> e.getName().endsWith(".nuspec"))
                    .findFirst()
                    .orElseThrow(() -> new Exception("No .nuspec file found"));
            try (InputStream nuspecStream = zipFile.getInputStream(nuspecEntry)) {
                String nuspecContent = IOUtils.toString(nuspecStream, StandardCharsets.UTF_8);
                return  nuspecContent;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 转发远程获取（写入本地、返回给客户端）
     * @param relativeAbsoluteUrl 文件请求路径
     * @param  storePath 本地存储路径
     */
    public static void restTemplateGetByte(HttpServletResponse response,String relativeAbsoluteUrl,String storePath) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        Path localFilePath = createBorderAndFile(storePath);
        restTemplate.execute(relativeAbsoluteUrl, HttpMethod.GET, null, clientHttpResponse -> {
            try (InputStream inputStream = clientHttpResponse.getBody();
                 OutputStream outputStream = response.getOutputStream();
                 OutputStream fileOutput = Files.newOutputStream(localFilePath, StandardOpenOption.CREATE)) {

                byte[] buffer = new byte[8192]; // 8KB缓冲区
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    fileOutput.write(buffer, 0, bytesRead);

                    // 立即刷新确保数据发送
                    outputStream.flush();
                    fileOutput.flush();
                }
                // inputStream.transferTo(outputStream);
            }
            return null;
        });
    }

    //创建文件夹文件
    public static  Path  createBorderAndFile(String filePath)  {
        try {
            // 创建 Path 对象
            Path path = Paths.get(filePath);
            // 获取父目录
            Path parentDir = path.getParent();

            // 创建父目录（如果不存在）
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }

            // 创建文件（如果不存在）
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            return path;
        }catch (Exception e){
            throw new SystemException(e.getMessage());
        }

    }
}
