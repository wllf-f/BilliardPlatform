package com.billiard;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * 台球器材商城后端启动类
 */
@SpringBootApplication
@MapperScan("com.billiard.mapper")
@EnableScheduling
public class BilliardApplication {
    public static void main(String[] args) {
        SpringApplication.run(BilliardApplication.class, args);
        System.out.println("\n===== 台球器材商城后端启动成功 =====");
        System.out.println("本机访问: http://127.0.0.1:8080");
        try {
            String ip = getLocalIp();
            if (ip != null) {
                System.out.println("手机访问: http://" + ip + ":8080");
                System.out.println("请修改前端 request.js 的 BASE_URL 为: http://" + ip + ":8080");
            }
        } catch (Exception e) {
            System.out.println("获取本机IP失败，请手动修改前端 request.js");
        }
        System.out.println("图片访问: http://127.0.0.1:8080/billiard_img/");
        System.out.println("====================================\n");
    }

    private static String getLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    String ip = address.getHostAddress();
                    if (ip != null && ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && !ip.startsWith("127")) {
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
