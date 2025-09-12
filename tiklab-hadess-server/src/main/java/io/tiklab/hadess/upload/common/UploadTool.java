package io.tiklab.hadess.upload.common;

import io.tiklab.core.exception.SystemException;
import io.tiklab.hadess.common.HadessFinal;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class UploadTool {

    /**
     *  读取文件信息
     *  @param filePath     文件路径
     * @return byte
     */
    public static byte[] readFileDataByte(String filePath) {
        try {
            File file = new File(filePath);
            ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length());
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
            int buf_size = 1024;
            byte[] buffer = new byte[buf_size];
            int len = 0;
            while (-1 != (len = in.read(buffer, 0, buf_size))) {
                bos.write(buffer, 0, len);
            }
            byte[] bytes = bos.toByteArray();
            return  bytes;
        } catch (IOException e) {
            throw new SystemException(HadessFinal.FILE_EXCEPTION,"读取文件失败:"+e.getMessage());
        }
    }


    /**
     *  读取yaml文件
     *  @param filePath     yaml文件路径
     */
    public static Map<String, Object> readYamlFile(String filePath) {
        try (FileInputStream inputStream = new FileInputStream(filePath)) {
            Yaml yaml = new Yaml();
            /*
            * 设置访问对象
            * BeanAccess.STANDARD（默认值）: SnakeYAML 使用 JavaBean 规范,通过 getter/setter 方法访问对象属性。
            * BeanAccess.FIELD: SnakeYAML 直接访问对象的字段,而不使用 getter/setter 方法。
            * BeanAccess.PROPERTY: SnakeYAML 使用 Java Beans 属性访问器（通过 Field 和 Method 对象）访问对象属性。
            * */
            yaml.setBeanAccess(BeanAccess.FIELD);
            return yaml.load(inputStream);
        } catch (IOException e) {
            throw new SystemException("读取yaml文件"+filePath+"失败："+e);

        }
    }

    /**
     *  写入yaml数据
     *  @param filePath     写入yaml文件路径
     * @param
     */
    public static void writeYamlFile(Map<String, Object> data,String filePath){
        try {
            FileWriter writer = new FileWriter(filePath);

            //将默认的yaml输出格式设置为块式
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            //启用Pretty打印模式
            options.setPrettyFlow(true);

            Yaml yaml = new Yaml(options);
            yaml.dump(data, writer);
        } catch (IOException e) {
            throw new RuntimeException("向"+filePath+"路径中写入yaml内容失败："+e);
        }
    }

    /**
     * 生成基于HMAC-SHA256的签名
     * @param fileSize 文件大小
     * @param timestamp 当前时间戳
     * @return 十六进制格式的签名
     */
   /* private String generateSignature(int fileSize, long timestamp) {
        try {
            String data = fileSize + ":" + timestamp;
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(serverKey.getBytes(), "HmacSHA256");
            hmac.init(keySpec);

            byte[] signatureBytes = hmac.doFinal(data.getBytes());
            return bytesToHex(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new SecurityException("Failed to generate signature", e);
        }
    }*/
    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
