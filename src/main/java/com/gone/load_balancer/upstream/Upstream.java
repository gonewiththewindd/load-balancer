package com.gone.load_balancer.upstream;

import com.gone.load_balancer.common.LoadBalanceEnums;
import jakarta.annotation.Nullable;
import lombok.Data;

import java.util.List;
import java.util.Objects;

/**
 * TODO upstream配置
 */
@Data
public class Upstream {

    private String id;
    private LoadBalanceEnums lbe = LoadBalanceEnums.POLL;
    private int[] weightsBitmap;
    private List<Service> services;

    public Upstream(String id, @Nullable LoadBalanceEnums lbe, List<Service> services) {
        this.services = services;
        this.id = id;
        if (Objects.nonNull(lbe)) {
            this.lbe = lbe;
        }
        if (LoadBalanceEnums.POLL.equals(lbe)) {
            int weightSum = this.services.stream()
                    .mapToInt(Service::getWeight)
                    .sum();
            this.weightsBitmap = new int[weightSum];
            int weight = 0;
            for (int i = 0; i < services.size(); i++) {
                int sw = services.get(i).getWeight();
                for (int j = 0; j < sw; j++) {
                    weightsBitmap[weight++] = i;
                }
            }
        }
    }

}
