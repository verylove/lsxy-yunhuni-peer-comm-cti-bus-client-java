package com.lsxy.app.area.cti.commander;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.lsxy.app.area.cti.busnetcli.Head;

class LibCallbackHandler implements com.lsxy.app.area.cti.busnetcli.Callbacks {

    private final Logger logger = LoggerFactory.getLogger(LibCallbackHandler.class);
    private final Logger jniLogger = LoggerFactory.getLogger("bus_net_cli");

    public void globalConnect(byte unitId, byte clientId, byte clientType, byte status, String addInfo) {
        logger.info("Bus global connection event: unitId={}, clientId={}, clientType={}, addInfo={}, status={}", unitId, clientId, clientType, addInfo, status);
    }

    public void connect(byte localClientId, int accessPointUnitId, int errorCode) {
        if (errorCode == 0) {
            // 客户端链接成功
            logger.info("[{}:{}] connection succeed. AccessPointUnitId={}", Commander.getUnitId(), localClientId, accessPointUnitId);
        } else {
            // 客户端链接失败
            logger.error("[{}:{}] connection failed. ErrorCode={}", Commander.getUnitId(), localClientId, errorCode);
        }
    }

    public void disconnect(byte localClientId) {
        logger.error("[{}:{}] connection lost", Commander.getUnitId(), localClientId);
    }

    public void data(Head head, byte[] bytes) {
        logger.debug(">>> data(head={}. dataLength={})", head, bytes.length);
        byte clientId = head.getDstClientId();
        Client client = Commander.clients.get(clientId);
        if (client == null) {
            logger.error("cannot find client<id={}>", clientId);
            return;
        }
        client.dataExecutor.execute(() -> {
            try {
                String rpcTxt = new String(bytes, "UTF-8");
                RpcRequest req = null;
                RpcResponse res = null;
                // 收到了RPC事件通知？
                if (client.eventListener != null) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        req = mapper.readValue(rpcTxt, RpcRequest.class);
                    } catch (JsonProcessingException ignore) {

                    }
                    if (req != null) {
                        client.logger.debug(">>> client.eventListener.onEvent({})", req);
                        client.eventListener.onEvent(req);
                        client.logger.debug("<<< client.eventListener.onEvent()");
                        return;
                    }
                }
                // 收到了RPC调用回复？
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    res = mapper.readValue(rpcTxt, RpcResponse.class);
                } catch (JsonProcessingException ignore) {
                }
                if (res != null) {
                    Commander.rpcResponded(res);
                    return;
                }
                // 既不是RPC事件通知，也不是RPC请求回复，只能忽略了。
                client.logger.warn("unsupported RPC content received: {}", rpcTxt);
            } catch (Exception e) {
                client.logger.error("error occurred in dataExecutor.execute()", e);
            }
        });
        logger.debug("<<< data()");
    }

    public void log(String msg, Boolean isErr) {
        if (isErr) {
            jniLogger.error(msg);
        } else {
            jniLogger.info(msg);
        }
    }

}