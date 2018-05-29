package com.alibaba.mesh.remoting.transport;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.extension.ExtensionLoader;
import com.alibaba.mesh.common.serialize.Serialization;
import com.alibaba.mesh.remoting.Codeable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CodecSupport {

    private static final Logger logger = LoggerFactory.getLogger(CodecSupport.class);
//    private static Map<Byte, Serialization> ID_SERIALIZATION_MAP = new HashMap<Byte, Serialization>();
//    private static Map<Byte, String> ID_SERIALIZATIONNAME_MAP = new HashMap<Byte, String>();
//
//    private static Map<Byte, Codeable> ID_CODEABLE_MAP = new HashMap<>();
//    private static Map<Byte, String> ID_CODEABLENAME_MAP = new HashMap<>();
//
//    static {
//        Set<String> supportedExtensions = ExtensionLoader.getExtensionLoader(Serialization.class).getSupportedExtensions();
//        for (String name : supportedExtensions) {
//            Serialization serialization = ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(name);
//            byte idByte = serialization.getContentTypeId();
//            if (ID_SERIALIZATION_MAP.containsKey(idByte)) {
//                logger.error("Serialization extension " + serialization.getClass().getName()
//                        + " has duplicate id to Serialization extension "
//                        + ID_SERIALIZATION_MAP.get(idByte).getClass().getName()
//                        + ", ignore this Serialization extension");
//                continue;
//            }
//            ID_SERIALIZATION_MAP.put(idByte, serialization);
//            ID_SERIALIZATIONNAME_MAP.put(idByte, name);
//        }
//
//        Set<String> supportedCodeables = ExtensionLoader.getExtensionLoader(Codeable.class).getSupportedExtensions();
//        for (String name : supportedCodeables) {
//            Codeable codeable = ExtensionLoader.getExtensionLoader(Codeable.class).getExtension(name);
//            byte idByte = codeable.getCodecTypeId();
//            if (ID_CODEABLE_MAP.containsKey(idByte)) {
//                logger.error("Codeable extension " + ID_CODEABLE_MAP.getClass().getName()
//                        + " has duplicate id to Codeable extension "
//                        + ID_CODEABLENAME_MAP.get(idByte).getClass().getName()
//                        + ", ignore this Codeable extension");
//                continue;

//            ID_CODEABLE_MAP.put(idByte, codeable);
//            ID_CODEABLENAME_MAP.put(idByte, name);
//        }
//    }

    private CodecSupport() {
    }

//    public static Serialization getSerializationById(Byte id) {
//        return ID_SERIALIZATION_MAP.get(id);
//    }

    public static Serialization getSerialization(URL url) {
        return ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(
                url.getParameter(Constants.SERIALIZATION_KEY, Constants.DEFAULT_HTTP_SERIALIZATION));
    }

//    public static Codeable getCodeableById(Byte id) {
//        return ID_CODEABLE_MAP.get(id);
//    }

    public static Codeable getCodeable(URL url) {
        return ExtensionLoader.getExtensionLoader(Codeable.class).getExtension(
                url.getParameter(Constants.CODEABLE_KEY, Constants.DEFAULT_REMOTING_CODEC));
    }

//    public static Serialization getSerialization(URL url, Byte id) throws IOException {
//        Serialization serialization = getSerializationById(id);
//        String serializationName = url.getParameter(Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION);
//        // Check if "serialization id" passed from network matches the id on this side(only take effect for JDK serialization), for security purpose.
//        if (serialization == null
//                || ((id == 3 || id == 7 || id == 4) && !(serializationName.equals(ID_SERIALIZATIONNAME_MAP.get(id))))) {
//            throw new IOException("Unexpected serialization id:" + id + " received from network, please check if the peer send the right id.");
//        }
//        return serialization;
//    }

}
