package icu.freedomIntrovert.biliSendCommAntifraud.danmaku;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Inflater;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import icu.freedomIntrovert.biliSendCommAntifraud.okretro.OkHttpUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class DanmakuManipulator {
    OkHttpClient httpClient;

    public DanmakuManipulator() {
        this.httpClient = OkHttpUtil.getHttpClient();
    }

    public boolean findDanmaku(long oid,long dmid, String accessKey) throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Request request;
        byte[] decompress;
        if (accessKey == null){
            request = new Request.Builder().url("https://api.bilibili.com/x/v1/dm/list.so?oid="+oid).build();
        } else {
            request = new Request.Builder().url("https://api.bilibili.com/x/v1/dm/list.so?oid="+oid+"&access_key="+accessKey).build();
        }
        decompress = decompress(httpClient.newCall(request).execute().body().bytes());
        Document document = builder.parse(new ByteArrayInputStream(decompress));
        NodeList nodeList = document.getDocumentElement().getElementsByTagName("d");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            String p = element.getAttribute("p");
            String thisDmid = p.split(",")[7];
            System.out.println(thisDmid);
            if (Long.parseLong(thisDmid) == dmid){
                return true;
            }
        }
        return false;
    }

    public static byte[] decompress(byte[] data) throws IOException {
        byte[] decompressData = null;
        Inflater decompressor = new Inflater(true);
        decompressor.reset();
        decompressor.setInput(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        try {
            byte[] buf = new byte[1024];
            while (!decompressor.finished()) {
                int i = decompressor.inflate(buf);
                outputStream.write(buf, 0, i);
            }
            decompressData = outputStream.toByteArray();
        } catch (Exception e) {
        } finally {
            outputStream.close();
        }
        decompressor.end();
        return decompressData;
    }

}
