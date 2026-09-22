package com.example.mall.file.controller;

import com.example.mall.common.Result;
import io.minio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import io.minio.PutObjectArgs;

import java.util.UUID;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private MinioClient minioClient;      // MinioConfig 里创建好的那个对象

    @Value("${minio.bucket}")
    private String bucket;                // 桶名：mall-images

    @Value("${minio.endpoint}")
    private String endpoint;              // http://127.0.0.1:9000

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            // ① 检查桶存不存在，不存在就创建一个（第一次会创建）
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            // ①.5 设置桶为「公开读」：允许匿名访问，浏览器才能直接打开图片
            String policyJson = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::" + bucket + "/*\"]}]}";
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucket)
                    .config(policyJson)
                    .build());


            // ② 生成唯一文件名，防止两张图片重名互相覆盖
            String originalName = file.getOriginalFilename();
            String ext = originalName.substring(originalName.lastIndexOf(".")); // 取后缀 .jpg
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

            // ③ 上传到 MinIO
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)          // 放进哪个桶
                    .object(fileName)        // 文件叫什么名
                    .stream(file.getInputStream(), file.getSize(), -1) // 文件内容
                    .contentType(file.getContentType()) // 文件类型
                    .build());

            // ④ 拼出可访问的 URL 返回给前端
            String url = endpoint + "/" + bucket + "/" + fileName;
            return Result.success(url);

        } catch (Exception e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }
}
