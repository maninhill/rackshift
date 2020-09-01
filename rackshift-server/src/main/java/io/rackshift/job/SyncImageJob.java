package io.rackshift.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.rackshift.constants.RackHDConstants;
import io.rackshift.manager.BareMetalManager;
import io.rackshift.model.MachineEntity;
import io.rackshift.mybatis.domain.Endpoint;
import io.rackshift.service.RackHDService;
import io.rackshift.utils.RackHDHttpClientUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;

@Service
public class SyncImageJob {
    @Resource
    private RackHDService rackHDService;
    @Resource
    private BareMetalManager bareMetalManager;
    @Resource
    private List<Endpoint> endPoints;

//    @Scheduled(fixedDelay = 1000)
//    @Scheduled(fixedDelay = 60000)
    public void run() {
        List<MachineEntity> entities = new LinkedList<>();
        JSONArray nodesArr = null;

        for (Endpoint endPoint : endPoints) {

            nodesArr = JSONArray.parseArray(RackHDHttpClientUtil.get("http://" + endPoint.getIp() + ":9090" + RackHDConstants.NODES_URL, null));

            for (int i = 0; i < nodesArr.size(); i++) {
                JSONObject nodeObj = nodesArr.getJSONObject(i);
                String type = nodeObj.getString("type");
                if ("compute".equalsIgnoreCase(type)) {
                    MachineEntity en = rackHDService.getNodeEntity("http://" + endPoint.getIp() + ":9090", nodeObj.getString("id"), null);
                    if (en != null) {
                        en.setEndPoint(endPoint.getId());
                        entities.add(en);
                    }
                }
            }

            entities.forEach(e -> {
                bareMetalManager.saveOrUpdateEntity(e);
            });
        }
    }


}
