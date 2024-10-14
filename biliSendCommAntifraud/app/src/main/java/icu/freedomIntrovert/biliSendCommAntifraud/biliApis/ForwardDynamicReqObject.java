package icu.freedomIntrovert.biliSendCommAntifraud.biliApis;

import com.alibaba.fastjson.JSON;

import java.util.List;

public class ForwardDynamicReqObject {
    public DynReq dyn_req;
    public WebRepostSrc web_repost_src;

    public static ForwardDynamicReqObject create(long uid, String dynIdStr){
        ForwardDynamicReqObject object = JSON.parseObject("{\n" +
                "  \"dyn_req\": {\n" +
                "    \"content\": {\n" +
                "      \"contents\": []\n" +
                "    },\n" +
                "    \"scene\": 4,\n" +
                "    \"attach_card\": null,\n" +
                "    \"upload_id\": null,\n" +
                "    \"meta\": {\n" +
                "      \"app_meta\": {\n" +
                "        \"from\": \"create.dynamic.web\",\n" +
                "        \"mobi_app\": \"web\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"web_repost_src\": {\n" +
                "    \"dyn_id_str\": null\n" +
                "  }\n" +
                "}",ForwardDynamicReqObject.class);
        object.web_repost_src.dyn_id_str = dynIdStr;
        long currentTimeMillis = System.currentTimeMillis();
        String t = String.valueOf(currentTimeMillis);
        object.dyn_req.upload_id = uid+"_"+(currentTimeMillis/1000)+"_"+t.substring(t.length() - 3)+"0";
        return object;
    }

    public ForwardDynamicReqObject(){
    }

    public static class DynReq {
        public Content content;
        public int scene;
        public String attach_card;
        public String upload_id;
        public Meta meta;

        public static class Content {
            public List<Object> contents;
        }

        public static class Meta {
            public AppMeta app_meta;

            public static class AppMeta {
                public String from;
                public String mobi_app;
            }
        }
    }

    public static class WebRepostSrc {
        public String dyn_id_str;
    }
}
