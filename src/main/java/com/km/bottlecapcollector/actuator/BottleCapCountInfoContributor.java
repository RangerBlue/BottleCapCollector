package com.km.bottlecapcollector.actuator;

import com.km.bottlecapcollector.service.BottleCapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BottleCapCountInfoContributor implements InfoContributor {

    @Autowired
    private BottleCapService service;

    @Override
    public void contribute(Info.Builder builder) {
        long capCount = service.getAllCapItems().size();
        Map<String, Object> capMap = new HashMap<>();
        capMap.put("count", capCount);
        builder.withDetail("cap-stats", capMap);
    }
}
