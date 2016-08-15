package com.lsxy.app.area.cti;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A very simple sample program shows how to use the package.
 * <p>
 * Created by liuxy on 16-7-13.
 */
public class MainClass {
    private static final Logger logger = LoggerFactory.getLogger(MainClass.class);

    public static void main(String[] args) throws Exception {
        logger.debug("initiate...");
        Unit.initiate((byte) 20);
        logger.debug("initiate OK!");
        Commander commander = Unit.createCommander(
                (byte) 0,
                "127.0.0.1",
                (source, request) -> logger.debug("收到事件：source={}, request={}", source, request)
        );

        for (int i = 0; i < 1; ++i) {
            Map<String, Object> params = new HashMap<>();
            params.put("from_uri", "");
            params.put("to_uri", "192.168.2.100:5062");
            params.put("max_answer_seconds", (int) (50 * Math.random()));
            params.put("max_ring_seconds", (int) (10 * Math.random()));

            commander.createResource(
                    new BusAddress((byte) 0, (byte) 0),
                    "sys.call",
                    params,
                    new RpcResultListener() {
                        @Override
                        protected void onResult(Object result) {
                            logger.debug("呼出 返回值：(result={})", result);
                            String callId = (String) result;
                            Map<String, Object> params = new HashMap<>();
                            try {
                                commander.operateResource(
                                        new BusAddress((byte) 0, (byte) 0),
                                        callId,
                                        "sys.call.drop",
                                        params,
                                        new RpcResultListener() {
                                            @Override
                                            protected void onResult(Object result) {
                                                logger.debug("挂机 返回值：(result={})", result);
                                            }

                                            @Override
                                            protected void onError(RpcError error) {
                                                logger.debug("挂机 错误:{}", error);
                                            }

                                            @Override
                                            protected void onTimeout() {
                                                logger.debug("挂机 超时");
                                            }
                                        });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        protected void onError(RpcError error) {
                            logger.error("呼出 错误:{}", error);
                        }

                        @Override
                        protected void onTimeout() {
                            logger.error("呼出 超时");
                        }
                    });
        }

        String inputStr;
        Scanner scanner = new Scanner(System.in);
        System.out.printf("started! Input \"quit\" or \"q\" to quit.\n");
        while (true) {
            inputStr = scanner.nextLine();
            if (inputStr.toLowerCase().startsWith("q"))
                break;
        }
    }
}
